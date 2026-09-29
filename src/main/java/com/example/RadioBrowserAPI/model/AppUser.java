package com.example.RadioBrowserAPI.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity //IDENTIFICA COMO ENTIDADE PARA O DB
@Table (name = "app_users") //LINKA ESSA ENTIDADE A SUA RESPECTIVA TABELA
public class AppUser {
    
    @Id //atribui ID
    @GeneratedValue(strategy = GenerationType.IDENTITY) //ATRIBUI VALOR
    private Long id;

    @Column (nullable = false) //N pode ser NULL
    private String email;

    @Column (name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;
    // created_at não precisa estar aqui: o banco preenche sozinho (default now())

    //CONSTRUTORES
     public AppUser() {
    }

    public AppUser(String email, String passwordHash, String name) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
    }

    //GETTERS SETTERS

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getName() { return name; }

}
