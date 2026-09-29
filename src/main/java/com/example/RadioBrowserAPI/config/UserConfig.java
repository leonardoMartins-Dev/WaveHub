package com.example.RadioBrowserAPI.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserConfig {

    // =========================================================
    // USUÁRIO
    // =========================================================



    // =========================================================
    // GOOGLE reCAPTCHA
    // =========================================================

    @Value("${recaptcha.site-key}")
    private String recaptchaSiteKey;

    @Value("${recaptcha.secret-key}")
    private String recaptchaSecretKey;


    // =========================================================
    // GETTERS - USUÁRIO
    // =========================================================




    // =========================================================
    // GETTERS - reCAPTCHA
    // =========================================================

    public String getRecaptchaSiteKey() {
        return recaptchaSiteKey;
    }

    public String getRecaptchaSecretKey() {
        return recaptchaSecretKey;
    }
}