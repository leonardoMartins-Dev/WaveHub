package com.example.RadioBrowserAPI.service;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.RadioBrowserAPI.model.AppUser;
import com.example.RadioBrowserAPI.model.FavoriteStation;
import com.example.RadioBrowserAPI.repository.FavoriteRepository;
import com.example.RadioBrowserAPI.repository.UserRepository;

/**
 * Guarda os favoritos de cada usuário no Supabase (PostgreSQL).
 */
@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;

    public FavoriteService(FavoriteRepository favoriteRepository, UserRepository userRepository) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
    }

    /**
     * Busca os favoritos do usuário numa única consulta.
     * LinkedHashSet mantém a ordem do banco (mais recentes primeiro).
     */
    @Transactional(readOnly = true)
    public Set<String> getFavoriteUuids(String email) {
        return new LinkedHashSet<>(favoriteRepository.findStationUuidsByUserEmail(email));
    }

    /**
     * Adiciona ou remove a estação das favoritas.
     * Retorna o novo estado: true = agora é favorita.
     */
    @Transactional
    public boolean toggle(String email, String stationUuid) {
        if (stationUuid == null) return false;

        Optional<FavoriteStation> existing = favoriteRepository.findByUserEmailAndStationUuid(email, stationUuid);

        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false;
        }

        AppUser user = userRepository.findByEmail(email).orElseThrow();
        favoriteRepository.save(new FavoriteStation(user, stationUuid));
        return true;
    }
}
