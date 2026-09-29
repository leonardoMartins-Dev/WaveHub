package com.example.RadioBrowserAPI.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.RadioBrowserAPI.model.AppUser;
import com.example.RadioBrowserAPI.repository.UserRepository;


@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void createUser(String email, String senha, String nome) {
        userRepository.save(new AppUser(normalize(email), passwordEncoder.encode(senha), nome));
    }

    public boolean exists(String email) {
        return userRepository.existsByEmail(normalize(email));
    }

    public String getName(String email) {
        return userRepository.findByEmail(normalize(email))
                .map(AppUser::getName)
                .orElse(null);
    }

    public void updatePassword(String email, String novaSenha) {
        AppUser user = userRepository.findByEmail(normalize(email)).orElseThrow();
        user.setPasswordHash(passwordEncoder.encode(novaSenha));
        userRepository.save(user);
    }

    // "Leo@Gmail.com " e "leo@gmail.com" precisam ser o mesmo usuário
    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}

