package com.compute.rental.modules.auth.service;

import com.compute.rental.modules.auth.support.AuthProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LoggingEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final AuthProperties authProperties;

    public LoggingEmailService(JavaMailSender mailSender, AuthProperties authProperties) {
        this.mailSender = mailSender;
        this.authProperties = authProperties;
    }

    @Override
    public void sendSignupCode(String email, String code) {
        sendCode(email, code, "Your signup verification code is: %s");
    }

    @Override
    public void sendResetPasswordCode(String email, String code) {
        sendCode(email, code, "Your password reset verification code is: %s");
    }

    private void sendCode(String email, String code, String title) {
        var message = new SimpleMailMessage();
        if (StringUtils.hasText(authProperties.from())) {
            message.setFrom(authProperties.from());
        }
        message.setTo(email);
        message.setSubject(authProperties.subject());
        message.setText("""
                %s

                This code expires in %d minutes. If you did not request this code, please ignore this email.
                """.formatted(title.formatted(code), authProperties.codeTtl().toMinutes()));
        mailSender.send(message);
    }
}
