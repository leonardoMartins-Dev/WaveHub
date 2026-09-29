package com.example.RadioBrowserAPI.model;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.GenerationType;


/**
 * Estação favorita persistida na tabela "favorites" do Supabase.
 */
@Entity
@Table(name = "favorites")
public class FavoriteStation {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "user_id")
    private AppUser user;

    @Column(name = "station_uuid", nullable = false, length = 64)
    private String stationUuid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public FavoriteStation() {
    }

    public FavoriteStation(AppUser user, String stationUuid) {
        this.user = user;
        this.stationUuid = stationUuid;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public String getStationUuid() { return stationUuid; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
