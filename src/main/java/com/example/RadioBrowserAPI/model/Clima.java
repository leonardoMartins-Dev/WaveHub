package com.example.RadioBrowserAPI.model;

/**
 * Clima atual de um lugar, vindo da WeatherAPI (weatherapi.com).
 * No Thymeleaf, os campos são lidos como métodos: ${clima.cidade()}.
 */
public record Clima(
        String cidade,
        String regiao,
        String pais,
        int temperatura,
        int sensacao,
        String condicao,
        String icone,
        int umidade,
        int ventoKmh,
        String horaLocal) {
}
