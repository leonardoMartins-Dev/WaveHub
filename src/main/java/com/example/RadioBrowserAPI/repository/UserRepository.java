package com.example.RadioBrowserAPI.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.RadioBrowserAPI.model.AppUser;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional <AppUser> findByEmail(String email);
    boolean existsByEmail(String email);

}
