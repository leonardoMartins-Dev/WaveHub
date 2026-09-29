package com.example.RadioBrowserAPI.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.RadioBrowserAPI.service.RecaptchaService;

//SEMPRE ADICIONAR UM LINHA NOVA NO SECURITY CONFIG PARA CADA PAGINA CRIADA

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final RecaptchaFilter recaptchaFilter;

    public SecurityConfig(RecaptchaService recaptchaService) {
        this.recaptchaFilter = new RecaptchaFilter(recaptchaService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/login/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/login/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/css/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/imgs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/register").permitAll()
                .requestMatchers(HttpMethod.GET, "/recoverpassword").permitAll()
                .requestMatchers(HttpMethod.POST, "/recoverpassword").permitAll()
                .requestMatchers(HttpMethod.GET, "/resetpassword").permitAll()
                .requestMatchers(HttpMethod.POST, "/resetpassword").permitAll()
                .requestMatchers(HttpMethod.GET, "/error").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(recaptchaFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
                .successHandler((request, response, authentication) -> {
                        response.sendRedirect("/home");
                })
                .failureHandler((request, response, authentication) -> {
                    response.sendRedirect("/error");
                })
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            );

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}