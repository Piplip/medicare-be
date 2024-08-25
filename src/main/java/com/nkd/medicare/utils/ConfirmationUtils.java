package com.nkd.medicare.utils;

import com.nkd.medicare.tables.records.ConfirmationRecord;

import java.time.LocalDateTime;

public class ConfirmationUtils {

    private static final int DEFAULT_EXPIRED_TIME = 15;
    private static final String TOKEN_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static LocalDateTime calculateExpiredTime(){
        return LocalDateTime.now().plusMinutes(15);
    }

    public static String generateToken(){
        StringBuilder token = new StringBuilder();
        for(int i = 0; i < 20; i++){
            token.append(TOKEN_POOL.charAt((int) (Math.random() * TOKEN_POOL.length())));
        }
        return token.toString();
    }

    public static boolean checkTokenExpired(LocalDateTime expiredTime){
        return LocalDateTime.now().isAfter(expiredTime);
    }
}
