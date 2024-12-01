package com.nkd.medicare.service;

import com.nkd.medicare.domain.Credential;
import com.nkd.medicare.domain.Session;

public interface AccountService {
    void createAccount(Credential credential);
    String activateAccount(String token, String email);
    void renewConfirmationToken(Integer accountID);
    Session login(Credential credential);
    Session staffLogin(Credential credential);
    Session loginWithSessionID(Credential credential);
    void checkCCCD(String account_id, String url);
}
