package com.nkd.medicare.service.impl;

import com.nkd.medicare.service.EmailService;
import com.nkd.medicare.utils.ConfirmationUtils;
import com.nkd.medicare.utils.EmailUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String NEW_ACCOUNT_VERIFICATION = "NEW ACCOUNT ACTIVATION";
    private static final String TOKEN_RENEW = "NEW CONFIRMATION TOKEN";
    private final JavaMailSender javaMailSender;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;
    @Value("${spring.mail.verify.host}")
    private String host;

    @Override
    public void sendRegistrationEmail(String token, String email, LocalDateTime expirationTime) {
        try {
            sendEmail(email, EmailUtils.getRegistrationMessage(email, host, token, expirationTime), NEW_ACCOUNT_VERIFICATION);
        }catch(Exception e){
            EmailUtils.handleEmailException();
        }
    }

    @Override
    public void sendRenewTokenEmail(String token, String email, LocalDateTime expirationTime) {
        try {
            sendEmail(email, EmailUtils.getRenewTokenEmail(email, host, token, expirationTime), TOKEN_RENEW);
        }catch(Exception e){
            EmailUtils.handleEmailException();
        }
    }

    @Override
    public void sendAppointmentConfirmationEmail(String patientEmail, String doctorEmail, String patientName, String date, String time, String reason) {
        try {
            sendEmail(doctorEmail, EmailUtils.getAppointmentConfirmationMessage(patientName, patientEmail, date, time, reason), "APPOINTMENT REQUEST");
        }catch(Exception e){
            EmailUtils.handleEmailException();
        }
    }

    @Override
    public void sendChangePasswordEmail(String email) {
        try {
            String OTP = ConfirmationUtils.generateOTP();
            redisTemplate.opsForValue().set(OTP, email, ConfirmationUtils.DEFAULT_EXPIRED_TIME);
            sendEmail(email, EmailUtils.getChangePasswordMessage(OTP), "CHANGE PASSWORD CONFIRM");
        } catch (Exception e){
            EmailUtils.handleEmailException();
        }
    }

    @Override
    public void sendReminderEmail(String email, String date, String time) {
        try {
            sendEmail(email, EmailUtils.getReminderMessage(date, time), "APPOINTMENT REMINDER");
        }catch(Exception e){
            EmailUtils.handleEmailException();
        }
    }

    private void sendEmail(String toEmail, String content, String subject) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true);

            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error sending email");
        }
    }
}
