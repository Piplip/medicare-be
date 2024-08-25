package com.nkd.medicare.service;

import java.time.LocalDateTime;

public interface EmailService {
    void sendRegistrationEmail(String token, String email, LocalDateTime expiredTime);
}
