package com.nkd.medicare.service.impl;

import com.nkd.medicare.domain.Credential;
import com.nkd.medicare.domain.Session;
import com.nkd.medicare.enumeration.EventType;
import com.nkd.medicare.enums.AccountAccountRole;
import com.nkd.medicare.event.AccountEvent;
import com.nkd.medicare.exception.ApiException;
import com.nkd.medicare.exception.DuplicateEmailException;
import com.nkd.medicare.service.AccountService;
import com.nkd.medicare.tables.records.AccountRecord;
import com.nkd.medicare.tables.records.ConfirmationRecord;
import com.nkd.medicare.utils.ConfirmationUtils;
import com.nkd.medicare.utils.SessionUtils;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

import static com.nkd.medicare.Tables.ACCOUNT;
import static com.nkd.medicare.Tables.CONFIRMATION;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final PasswordEncoder encoder;
    private final DSLContext dslContext;
    private final ApplicationEventPublisher publisher;
    private final RedisService redisService;

    @Override
    public void createAccount(Credential credential) {
        if(checkEmailExist(credential.getEmail())){
            throw new DuplicateEmailException();
        }

        AccountRecord newAccount = new AccountRecord();
        newAccount.setAccountEmail(credential.getEmail());
        newAccount.setAccountPassword(encoder.encode(credential.getPassword()));
        newAccount.setAccountRole(AccountAccountRole.USER);
        newAccount.setIsLocked((byte)0);
        newAccount.setIsEnable((byte)0);
        newAccount.setIsCredentialNonExpired((byte)1);

        int accountID = Objects.requireNonNull(dslContext.insertInto(ACCOUNT).set(newAccount)
                .returning(ACCOUNT.ACCOUNT_ID).fetchOne()).getAccountId();

        generateAndSendConfirmation(accountID, credential.getEmail(), EventType.REGISTRATION);
    }

    @Override
    public void deleteAccount(String username) {

    }

    @Override
    public void updatePassword(String email, String password) {

    }

    @Transactional(noRollbackFor = ApiException.class)
    @Override
    public void activateAccount(String token, String email) {
        AccountRecord accountRecord = dslContext.selectFrom(ACCOUNT)
                .where(ACCOUNT.ACCOUNT_EMAIL.eq(email))
                .fetchOne();

        if(accountRecord == null){
            throw new ApiException("ERR-1001"); // ERR-1001 : Account not found
        }
        if(accountRecord.getIsEnable() == 1){
            throw new ApiException("ERR-1002"); // ERR-1002 : Account already activated
        }

        ConfirmationRecord confirmationRecord = dslContext.selectFrom(CONFIRMATION)
                .where(CONFIRMATION.TOKEN.eq(token))
                .fetchOne();

        if(confirmationRecord == null){
            throw new ApiException("ERR-1003"); // ERR-1003 : Invalid token
        }

        boolean isTokenExpired = ConfirmationUtils.checkTokenExpired(confirmationRecord.getConfirmationExpiredTime());

        if(isTokenExpired){
            dslContext.delete(CONFIRMATION)
                    .where(CONFIRMATION.TOKEN.eq(token))
                    .execute();
            throw new ApiException("EXPIRED-" + confirmationRecord.getAccountId()); // EXPIRED-{accountID} : Token expired
        }

        dslContext.update(ACCOUNT)
                .set(ACCOUNT.IS_ENABLE, (byte)1)
                .where(ACCOUNT.ACCOUNT_ID.eq(confirmationRecord.getAccountId()))
                .execute();

        dslContext.delete(CONFIRMATION)
                .where(CONFIRMATION.TOKEN.eq(token))
                .execute();
    }

    @Override
    public void renewConfirmationToken(Integer accountID) {
        AccountRecord accountRecord = dslContext.selectFrom(ACCOUNT)
                .where(ACCOUNT.ACCOUNT_ID.eq(accountID))
                .fetchOne();

        if(accountRecord == null){
            throw new ApiException("Account not found");
        }
        else if(accountRecord.getIsEnable() == 1){
            throw new ApiException("Account already activated");
        }

        generateAndSendConfirmation(accountID, accountRecord.getAccountEmail(), EventType.RENEW_TOKEN);
    }

    @Override
    public Credential login(Credential credential) {
        AccountRecord accountRecord = dslContext.selectFrom(ACCOUNT)
                .where(ACCOUNT.ACCOUNT_EMAIL.eq(credential.getEmail()))
                .fetchOne();

        if(accountRecord == null){
            throw new ApiException("Not found an account with email: " + credential.getEmail());
        }
        if(accountRecord.getIsEnable() == 0){
            throw new ApiException("Account is not activated! Please head to your email to activate your account");
        }
        if(!encoder.matches(credential.getPassword(), accountRecord.getAccountPassword())){
            throw new ApiException("Invalid password for account: " + credential.getEmail());
        }

        String sessionID = SessionUtils.generateSessionToken();
        Session newSession = SessionUtils.generateLoginSession(credential.getEmail(), sessionID, accountRecord.getAccountRole());
        redisService.save(sessionID, newSession);

        credential.setSessionID(sessionID);
        return credential;
    }

    @Override
    public Credential loginWithSessionID(Credential credential) {
        String sessionID = credential.getSessionID();
        Session currentSession = redisService.get(sessionID);

        if(currentSession == null){
            throw new ApiException("Invalid session! Please login again");
        }
        if(LocalDateTime.now().isAfter(currentSession.getExpirationTime())){
            redisService.delete(sessionID);
            throw new ApiException("Session expired! Please login again");
        }

        var now = LocalDateTime.now();
        currentSession.setLastAccessTime(now);
        currentSession.setExpirationTime(now.plusMinutes(SessionUtils.DEFAULT_SESSION_EXPIRED_TIME));
        redisService.save(sessionID, currentSession);

        credential.setEmail(currentSession.getEmail());
        return credential;
    }

    private boolean checkEmailExist(String email){
        return dslContext.fetchExists(ACCOUNT, ACCOUNT.ACCOUNT_EMAIL.eq(email));
    }

    private void generateAndSendConfirmation(Integer accountID, String email, EventType eventType) {
        ConfirmationRecord newConfirmation = ConfirmationUtils.generateConfirmationRecord(accountID);

        dslContext.insertInto(CONFIRMATION).set(newConfirmation).execute();

        AccountEvent signUpEvent = new AccountEvent();
        signUpEvent.setEventType(eventType);
        signUpEvent.setData(Map.of("token", newConfirmation.getToken(),
                "email", email, "expirationTime", newConfirmation.getConfirmationExpiredTime()));

        publisher.publishEvent(signUpEvent);
    }
}

// TODO : Logout
