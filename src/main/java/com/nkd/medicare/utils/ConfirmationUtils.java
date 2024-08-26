package com.nkd.medicare.utils;

import com.nkd.medicare.enums.ConfirmationTokenType;
import com.nkd.medicare.tables.records.ConfirmationRecord;

import java.time.LocalDateTime;

public class ConfirmationUtils {

    public static final int DEFAULT_EXPIRED_TIME = 1;
    private static final String TOKEN_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static LocalDateTime calculateExpiredTime(){
        return LocalDateTime.now().plusMinutes(DEFAULT_EXPIRED_TIME);
    }

    private static String generateToken(){
        StringBuilder token = new StringBuilder();
        for(int i = 0; i < 20; i++){
            token.append(TOKEN_POOL.charAt((int) (Math.random() * TOKEN_POOL.length())));
        }
        return token.toString();
    }

    public static boolean checkTokenExpired(LocalDateTime expiredTime){
        return LocalDateTime.now().isAfter(expiredTime);
    }

    public static ConfirmationRecord generateConfirmationRecord(Integer accountID){
        ConfirmationRecord newConfirmation = new ConfirmationRecord();
        newConfirmation.setAccountId(accountID);
        newConfirmation.setTokenType(ConfirmationTokenType.EMAIL);

        LocalDateTime expiredTime = calculateExpiredTime();
        newConfirmation.setConfirmationExpiredTime(expiredTime);

        String token = generateToken();
        newConfirmation.setToken(token);

        return newConfirmation;
    }
}
