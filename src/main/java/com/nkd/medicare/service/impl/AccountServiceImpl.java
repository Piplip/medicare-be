package com.nkd.medicare.service.impl;

import com.nkd.medicare.domain.Credential;
import com.nkd.medicare.enumeration.EventType;
import com.nkd.medicare.enums.AccountAccountRole;
import com.nkd.medicare.enums.ConfirmationTokenType;
import com.nkd.medicare.event.AccountEvent;
import com.nkd.medicare.exception.ApiException;
import com.nkd.medicare.exception.DuplicateEmailException;
import com.nkd.medicare.exception.ExpiredTokenException;
import com.nkd.medicare.service.AccountService;
import com.nkd.medicare.tables.Confirmation;
import com.nkd.medicare.tables.records.AccountRecord;
import com.nkd.medicare.tables.records.ConfirmationRecord;
import com.nkd.medicare.utils.ConfirmationUtils;
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

    @Override
    public void createAccount(Credential credential) {
        if(checkEmailExist(credential.getEmail())){
            throw new DuplicateEmailException();
        }

        // TODO: Handle the case where email exists

        AccountRecord newAccount = new AccountRecord();
        newAccount.setAccountEmail(credential.getEmail());
        newAccount.setAccountPassword(encoder.encode(credential.getPassword()));
        newAccount.setAccountRole(AccountAccountRole.USER);
        newAccount.setIsLocked((byte)0);
        newAccount.setIsEnable((byte)0);
        newAccount.setIsCredentialNonExpired((byte)1);

        int accountID = Objects.requireNonNull(dslContext.insertInto(ACCOUNT).set(newAccount)
                .returning(ACCOUNT.ACCOUNT_ID).fetchOne()).getAccountId();

        ConfirmationRecord newConfirmation = new ConfirmationRecord();
        newConfirmation.setAccountId(accountID);
        newConfirmation.setTokenType(ConfirmationTokenType.EMAIL);

        LocalDateTime expiredTime = ConfirmationUtils.calculateExpiredTime();
        newConfirmation.setConfirmationExpiredTime(expiredTime);

        String token = ConfirmationUtils.generateToken();
        newConfirmation.setToken(token);

        dslContext.insertInto(CONFIRMATION).set(newConfirmation).execute();

        AccountEvent signUpEvent = new AccountEvent();
        signUpEvent.setEventType(EventType.REGISTRATION);
        signUpEvent.setData(Map.of("token", token, "email", credential.getEmail(), "expirationTime", expiredTime));

        publisher.publishEvent(signUpEvent);
    }

    @Override
    public void deleteAccount(String username) {

    }

    @Override
    public void updatePassword(String email, String password) {

    }

    @Transactional
    @Override
    public void activateAccount(String token) {
        ConfirmationRecord confirmationRecord = dslContext.selectFrom(CONFIRMATION)
                .where(CONFIRMATION.TOKEN.eq(token))
                .fetchAny();

        if(confirmationRecord != null){
            if(ConfirmationUtils.checkTokenExpired(confirmationRecord.getConfirmationExpiredTime())){
                throw new ExpiredTokenException();
            }
            dslContext.delete(CONFIRMATION)
                    .where(CONFIRMATION.TOKEN.eq(token))
                    .execute();
        }
        else{
            throw new ApiException("Invalid or missing token");
        }

        dslContext.update(ACCOUNT)
                .set(ACCOUNT.IS_ENABLE, (byte)1)
                .where(ACCOUNT.ACCOUNT_ID.eq(confirmationRecord.getAccountId()))
                .execute();
    }

    private boolean checkEmailExist(String email){
        return dslContext.fetchExists(ACCOUNT, ACCOUNT.ACCOUNT_EMAIL.eq(email));
    }
}
