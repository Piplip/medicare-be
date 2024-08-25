package com.nkd.medicare.utils;

import com.nkd.medicare.exception.ApiException;

public class EmailUtils {
    public static String getRegistrationMessage(String email, String host, String token){
        return "Hello " + email + ",\n\n" +
                "Thank you for registering with Medicare. Please click the link below to activate your account.\n\n" +
                getVerificationUrl(host, token) + "\n\n" +
                "If you did not register with Medicare, please ignore this email.\n\n" +
                "Regards,\n" +
                "Medicare Team";
    }

    private static String getVerificationUrl(String host, String token) {
        return host + "/api/user/verify?token=" + token;
    }

    public static void handleEmailException(Exception e) {
        throw new ApiException("Error sending email");
    }
}
