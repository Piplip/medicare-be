package com.nkd.medicare.service.impl;

import com.nkd.medicare.domain.Credential;
import com.nkd.medicare.enumeration.EventType;
import com.nkd.medicare.enums.AccountAccountRole;
import com.nkd.medicare.event.AccountEvent;
import com.nkd.medicare.exception.ApiException;
import com.nkd.medicare.exception.DuplicateEmailException;
import com.nkd.medicare.exception.ExpiredTokenException;
import com.nkd.medicare.service.AccountService;
import com.nkd.medicare.tables.records.AccountRecord;
import com.nkd.medicare.tables.records.ConfirmationRecord;
import com.nkd.medicare.utils.ConfirmationUtils;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
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
            throw new ApiException("Invalid or missing token");
        }

        if(ConfirmationUtils.checkTokenExpired(confirmationRecord.getConfirmationExpiredTime())){
            dslContext.delete(CONFIRMATION)
                    .where(CONFIRMATION.TOKEN.eq(token))
                    .execute();

            throw new ExpiredTokenException("EXPIRED-" + confirmationRecord.getAccountId()); // EXPIRED-{accountID} : Token expired
        }

        dslContext.delete(CONFIRMATION)
                .where(CONFIRMATION.TOKEN.eq(token))
                .execute();

        dslContext.update(ACCOUNT)
                .set(ACCOUNT.IS_ENABLE, (byte)1)
                .where(ACCOUNT.ACCOUNT_ID.eq(confirmationRecord.getAccountId()))
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

// TODO : Implement a prevent multiple renew token for server (Renew can only make once per a certain duration)
// TODO: Implement login functionality

