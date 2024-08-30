package com.nkd.medicare.utils;

import com.nkd.medicare.domain.Session;
import com.nkd.medicare.enums.AccountAccountRole;

public class SessionUtils {

    private static final String SESSION_TOKEN_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int MAX_SESSION_LENGTH = 20;
    public static final int DEFAULT_SESSION_EXPIRED_TIME = 3;

    public static String generateSessionToken(){
        StringBuilder token = new StringBuilder();

        for(int i = 0; i < MAX_SESSION_LENGTH; i++){
            token.append(SESSION_TOKEN_POOL.charAt((int) (Math.random() * SESSION_TOKEN_POOL.length())));
        }

        return token.toString();
    }

    public static Session generateLoginSession(String email, String sessionID, AccountAccountRole accountRole){
        Session newSession = new Session();

        newSession.setEmail(email);
        newSession.setSessionID(sessionID);
        newSession.setOnline(true);
        newSession.setAccountRole(accountRole);
        newSession.setCreatedTime(java.time.LocalDateTime.now());
        newSession.setLastAccessTime(java.time.LocalDateTime.now());
        newSession.setExpirationTime(java.time.LocalDateTime.now().plusHours(DEFAULT_SESSION_EXPIRED_TIME));

        return newSession;
    }
}
