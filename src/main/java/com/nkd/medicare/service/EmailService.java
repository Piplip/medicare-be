package com.nkd.medicare.service;

import java.time.LocalDateTime;

public interface EmailService {
    void sendRegistrationEmail(String token, String email, LocalDateTime expiredTime);
    void sendRenewTokenEmail(String token, String email, LocalDateTime expirationTime);
    void sendAppointmentConfirmationEmail(String patientEmail, String doctorEmail, String patientName, String date, String time, String reason);
}
