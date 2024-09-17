package com.nkd.medicare.utils;

import com.nkd.medicare.exception.ApiException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EmailUtils {

    public static String getRegistrationMessage(String email, String host, String token, LocalDateTime expiredTime){
        return "Hello " + email + ",<br/><br/>" +
                "Thank you for registering with Medicare. Please click this " +
                "<a href='" + getVerificationUrl(email, host, token) + "'>link</a>" +
                " to activate your account.<br/><br/>" +
                "NOTE: this link will expire on <b><p style='color: red;'>" + convertDatetime(expiredTime) + ".</p></b><br/><br/>" +
                "If you did not register with Medicare, please ignore this email.<br/><br/>" +
                "Regards,<br/>" +
                "Medicare Team";
    }

    private static String convertDatetime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        return dateTime.format(formatter);
    }

    public static String getRenewTokenEmail(String email, String host, String token, LocalDateTime expiredTime){
        return "Hello " + email + ",<br/><br/>" +
                "You are requesting a new token to verify your account. Please click this " +
                "<a href='" + getVerificationUrl(email, host, token) + "'>link</a>" +
                " to activate your account.<br/>" +
                "<p style='color: red;'>NOTE: this link will expire on " + convertDatetime(expiredTime) + ".</p>" +
                "IMPORTANT: a token can only be generated once every " + ConfirmationUtils.DEFAULT_EXPIRED_TIME + " minutes." +
                "<br/><br/>Regards," +
                "<br/>Medicare Team";
    }

    private static String getVerificationUrl(String email, String host, String token) {
        return host + "/api/user/token/verify?token=" + token + "&email=" + email;
    }

    public static void handleEmailException(Exception e) {
        throw new ApiException("Error sending email");
    }
}
