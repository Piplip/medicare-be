package com.nkd.medicare.service;

import com.nkd.medicare.domain.Credential;

public interface AccountService {
    void createAccount(Credential credential);
    void deleteAccount(String email);
    void updatePassword(String email, String password);
    void activateAccount(String token, String email);
    void renewConfirmationToken(Integer accountID);
    Credential login(Credential credential);
    Credential loginWithSessionID(Credential credential);
}
