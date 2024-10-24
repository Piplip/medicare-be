package com.nkd.medicare.service.impl;

import com.nkd.medicare.domain.Credential;
import com.nkd.medicare.domain.Session;
import com.nkd.medicare.enumeration.EventType;
import com.nkd.medicare.enums.AccountAccountRole;
import com.nkd.medicare.event.AccountEvent;
import com.nkd.medicare.exception.ApiException;
import com.nkd.medicare.exception.DuplicateEmailException;
import com.nkd.medicare.service.AccountService;
import com.nkd.medicare.service.cccd.ScanCCCD;
import com.nkd.medicare.tables.records.*;
import com.nkd.medicare.utils.ConfirmationUtils;
import com.nkd.medicare.utils.SessionUtils;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

import static com.nkd.medicare.Tables.*;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final PasswordEncoder encoder;
    private final DSLContext context;
    private final ApplicationEventPublisher publisher;
    private final RedisService redisService;
    private final AuthenticationManager authenticationManager;

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

        int accountID = Objects.requireNonNull(context.insertInto(ACCOUNT).set(newAccount)
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
    public String activateAccount(String token, String email) {
        AccountRecord accountRecord = context.selectFrom(ACCOUNT)
                .where(ACCOUNT.ACCOUNT_EMAIL.eq(email))
                .fetchOne();

        if(accountRecord == null){
            throw new ApiException("ERR-1001"); // ERR-1001 : Account not found
        }
        if(accountRecord.getIsEnable() == 1){
            throw new ApiException("ERR-1002"); // ERR-1002 : Account already activated
        }

        ConfirmationRecord confirmationRecord = context.selectFrom(CONFIRMATION)
                .where(CONFIRMATION.TOKEN.eq(token))
                .fetchOne();

        if(confirmationRecord == null){
            throw new ApiException("ERR-1003"); // ERR-1003 : Invalid token
        }

        boolean isTokenExpired = ConfirmationUtils.checkTokenExpired(confirmationRecord.getConfirmationExpiredTime());

        if(isTokenExpired){
            context.delete(CONFIRMATION)
                    .where(CONFIRMATION.TOKEN.eq(token))
                    .execute();
            throw new ApiException("EXPIRED-" + confirmationRecord.getAccountId()); // EXPIRED-{accountID} : Token expired
        }

        context.update(ACCOUNT)
                .set(ACCOUNT.IS_ENABLE, (byte)1)
                .where(ACCOUNT.ACCOUNT_ID.eq(confirmationRecord.getAccountId()))
                .execute();

        context.delete(CONFIRMATION)
                .where(CONFIRMATION.TOKEN.eq(token))
                .execute();

        return accountRecord.getAccountId().toString();
    }

    @Override
    public void renewConfirmationToken(Integer accountID) {
        AccountRecord accountRecord = context.selectFrom(ACCOUNT)
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
    public Session login(Credential credential) {
        AccountRecord accountRecord = context.selectFrom(ACCOUNT)
                .where(ACCOUNT.ACCOUNT_EMAIL.eq(credential.getEmail()))
                .fetchOne();

        if(accountRecord == null){
            throw new ApiException("Not found an account with email: " + credential.getEmail());
        }
        if(accountRecord.getIsEnable() == 0){
            throw new ApiException("Account is not activated! Please head to your gmail to activate your account");
        }
        if(!encoder.matches(credential.getPassword(), accountRecord.getAccountPassword())){
            throw new ApiException("Invalid password for account: " + credential.getEmail());
        }

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(credential.getEmail(), credential.getPassword());
        authenticationManager.authenticate(token);

        if(redisService.hasKey(credential.getEmail())){
            Session oldSession = redisService.get(credential.getEmail());
            oldSession.setExpirationTime(oldSession.getExpirationTime().plusDays(1));
            oldSession.setOnline(true);
            redisService.update(credential.getEmail(), oldSession);

            return new Session(oldSession);
        }

        String sessionID = SessionUtils.generateSessionToken();
        Session newSession = SessionUtils.generateLoginSession(credential.getEmail(), sessionID,
                accountRecord.getAccountRole(), accountRecord.getAccountId());

        Integer personID = context.select(PATIENT.PERSON_ID)
                .from(PATIENT)
                .where(PATIENT.PATIENT_ID.eq(accountRecord.getOwnerId()))
                .fetchOneInto(Integer.class);

        PersonRecord personRecord = context.selectFrom(PERSON)
                        .where(PERSON.PERSON_ID.eq(personID))
                        .fetchOne();

        assert personRecord != null;
        newSession.setFirstName(personRecord.getFirstName());
        newSession.setLastName(personRecord.getLastName());
        redisService.save(credential.getEmail(), newSession);

        return Session.builder()
                .firstName(personRecord.getFirstName())
                .lastName(personRecord.getLastName())
                .email(accountRecord.getAccountEmail())
                .sessionID(sessionID)
                .build();
    }

    @Override
    public Session staffLogin(Credential credential) {
        AccountRecord accountRecord = context.selectFrom(ACCOUNT)
                .where(ACCOUNT.ACCOUNT_EMAIL.eq(credential.getEmail()))
                .fetchOne();

        assert accountRecord != null;
        if(!encoder.matches(credential.getPassword(), accountRecord.getAccountPassword())){
            throw new ApiException("Invalid password for account: " + credential.getEmail());
        }

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(credential.getEmail(), credential.getPassword());
        authenticationManager.authenticate(token);

        Object[] staffInfo = context.select(STAFF.STAFF_ID, STAFF.PERSON_ID, STAFF.STAFF_IMAGE)
                .from(STAFF)
                .where(STAFF.STAFF_ID.eq(accountRecord.getOwnerId()))
                .fetchOneArray();

        assert staffInfo != null;
        PersonRecord personRecord = context.selectFrom(PERSON)
                .where(PERSON.PERSON_ID.eq((Integer) staffInfo[1]))
                .fetchOne();

        assert personRecord != null;
        return Session.builder()
                .userID(((Integer) staffInfo[0]))
                .firstName(personRecord.getFirstName())
                .lastName(personRecord.getLastName())
                .email(accountRecord.getAccountEmail())
                .imageURL((String) staffInfo[2])
                .isOnline(true)
                .accountRole(accountRecord.getAccountRole())
                .build();
    }

    @Override
    public Session loginWithSessionID(Credential credential) {
        String sessionID = credential.getSessionID();
        Session currentSession = redisService.get(sessionID);

        if(currentSession == null){
            throw new ApiException("Invalid session! Please login again");
        }
        if(LocalDateTime.now().isAfter(currentSession.getExpirationTime())){
            redisService.delete(sessionID);
            throw new ApiException("Session expired! Please login again");
        }

        currentSession.setOnline(true);
        var now = LocalDateTime.now();
        currentSession.setLastAccessTime(now);
        currentSession.setExpirationTime(now.plusMinutes(SessionUtils.DEFAULT_SESSION_EXPIRED_TIME));
        redisService.save(sessionID, currentSession);

        return currentSession;
    }

    @Override
    public void checkCCCD(String accountID, String url) {
        ScanCCCD scan = new ScanCCCD();
        scan.scan(url);

        AddressRecord address = new AddressRecord();
        address.setCity(scan.getCity());
        address.setDistrict(scan.getDistrict());
        address.setStreet(scan.getStreet());
        if(scan.getHouseNumber() != null) address.setHouseNumber(scan.getHouseNumber());

        int addressID = Objects.requireNonNull(context.insertInto(ADDRESS).set(address)
                .returning(ADDRESS.ADDRESS_ID).fetchOne()).getAddressId();

        PersonRecord person = new PersonRecord();
        person.setFirstName(scan.getFirstname());
        person.setLastName(scan.getLastname());
        person.setDateOfBirth(scan.getBirthday());
        person.setPrimaryLanguage("Vietnamese");
        person.setGender(scan.getGender());
        person.setAddressId(addressID);

        int personID = Objects.requireNonNull(context.insertInto(PERSON).set(person)
                .returning(PERSON.PERSON_ID).fetchOne()).getPersonId();

        PatientRecord patient = new PatientRecord();
        patient.setPersonId(personID);
        patient.setLastInfoUpdate(LocalDateTime.now());

        int patientID = Objects.requireNonNull(context.insertInto(PATIENT).set(patient)
                .returning(PATIENT.PATIENT_ID).fetchOne()).getPatientId();

        context.update(ACCOUNT)
                .set(ACCOUNT.OWNER_ID, patientID)
                .set(ACCOUNT.ID_CARD_NUMBER, scan.getCccd())
                .where(ACCOUNT.ACCOUNT_ID.eq(Integer.parseInt(accountID)))
                .execute();
    }

    private boolean checkEmailExist(String email){
        return context.fetchExists(ACCOUNT, ACCOUNT.ACCOUNT_EMAIL.eq(email));
    }

    private void generateAndSendConfirmation(Integer accountID, String email, EventType eventType) {
        ConfirmationRecord newConfirmation = ConfirmationUtils.generateConfirmationRecord(accountID);

        context.insertInto(CONFIRMATION).set(newConfirmation).execute();

        AccountEvent signUpEvent = new AccountEvent();
        signUpEvent.setEventType(eventType);
        signUpEvent.setData(Map.of("token", newConfirmation.getToken(),
                "email", email, "expirationTime", newConfirmation.getConfirmationExpiredTime()));

        publisher.publishEvent(signUpEvent);
    }
}

// TODO : Logout
