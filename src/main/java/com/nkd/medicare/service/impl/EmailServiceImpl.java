package com.nkd.medicare.service.impl;

import com.nkd.medicare.service.EmailService;
import com.nkd.medicare.utils.EmailUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
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

    @Value("${spring.mail.username}")
    private String fromEmail;
    @Value("${spring.mail.verify.host}")
    private String host;

    @Override
    public void sendRegistrationEmail(String token, String email, LocalDateTime expirationTime) {
        try {
            sendEmail(email, EmailUtils.getRegistrationMessage(email, host, token, expirationTime), NEW_ACCOUNT_VERIFICATION);
        }catch(Exception e){
            EmailUtils.handleEmailException(e);
        }
    }

    @Override
    public void sendRenewTokenEmail(String token, String email, LocalDateTime expirationTime) {
        try {
            sendEmail(email, EmailUtils.getRenewTokenEmail(email, host, token, expirationTime), TOKEN_RENEW);
        }catch(Exception e){
            EmailUtils.handleEmailException(e);
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
