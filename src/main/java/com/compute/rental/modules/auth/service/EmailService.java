package com.compute.rental.modules.auth.service;

public interface EmailService {

    void sendSignupCode(String email, String code);

    void sendResetPasswordCode(String email, String code);
}
