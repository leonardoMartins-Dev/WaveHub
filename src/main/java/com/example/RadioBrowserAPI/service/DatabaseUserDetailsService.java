package com.example.RadioBrowserAPI.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.RadioBrowserAPI.model.AppUser;
import com.example.RadioBrowserAPI.repository.UserRepository;

@Service
public class DatabaseUserDetailsService implements  UserDetailsService{
    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepository.findByEmail(username.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        return User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .roles("USER")
                .build();
    }
}
