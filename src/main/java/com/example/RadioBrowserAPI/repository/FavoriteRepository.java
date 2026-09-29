package com.example.RadioBrowserAPI.repository;

import java.util.Optional;

import org.springframework.data.repository.query.Param;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.RadioBrowserAPI.model.FavoriteStation;

public interface FavoriteRepository extends JpaRepository<FavoriteStation, Long> {

    // Mais recentes primeiro (ordem usada na aba Favoritas)
    @Query("select f.stationUuid from FavoriteStation f where f.user.email = :email order by f.createdAt desc")
    List<String> findStationUuidsByUserEmail(@Param("email") String email);

    Optional<FavoriteStation> findByUserEmailAndStationUuid(String email, String stationUuid);
}
