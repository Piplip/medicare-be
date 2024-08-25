package com.nkd.medicare.service.impl;

import com.nkd.medicare.service.EmailService;
import com.nkd.medicare.utils.EmailUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String NEW_ACCOUNT_VERIFICATION = "NEW ACCOUNT ACTIVATION";
    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;
    @Value("${spring.mail.verify.host}")
    private String host;

    @Override
    public void sendRegistrationEmail(String token, String email) {
        try {
            sendEmail(email, EmailUtils.getRegistrationMessage(email, host, token));
        }catch(Exception e){
            EmailUtils.handleEmailException(e);
        }
    }

    private void sendEmail(String toEmail, String content) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setSubject(NEW_ACCOUNT_VERIFICATION);
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setText(content);

        javaMailSender.send(message);
    }
}
