package com.nkd.medicare.service;

public interface EmailService {
    void sendRegistrationEmail(String token, String email);
}
