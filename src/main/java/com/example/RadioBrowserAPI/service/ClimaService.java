package com.example.RadioBrowserAPI.service;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.RadioBrowserAPI.model.Clima;
import com.example.RadioBrowserAPI.model.RadioStation;

/**
 * Busca o clima atual na WeatherAPI (weatherapi.com) para o lugar escolhido na busca.
 */
@Service
public class ClimaService {

    // Clima não muda a cada segundo: cada consulta fica guardada por 10 minutos
    private static final Duration VALIDADE_CACHE = Duration.ofMinutes(10);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;
    private final Map<String, ClimaEmCache> cache = new ConcurrentHashMap<>();

    public ClimaService(RestTemplateBuilder restTemplateBuilder,
                        @Value("${weatherapi.api.url}") String baseUrl,
                        @Value("${weatherapi.api.apikey}") String apiKey) {

        // Timeout curto: se a WeatherAPI demorar, a página abre sem o clima em vez de travar
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    /**
     * Clima do país/estado escolhido.
     *
     * 1º Usa as coordenadas da estação mais bem colocada que tenha localização: cai numa
     *    cidade de verdade dentro do estado/país. Buscar só pelo nome do estado erra muito
     *    na WeatherAPI ("Minas Gerais" vira a cidade de Campos Gerais, "Bavaria" cai na Colômbia).
     * 2º Se nenhuma estação tem localização, busca pelo nome e só aceita se o país bater.
     */
    public Optional<Clima> climaDoLugar(String country, String state, List<RadioStation> stations) {
        Optional<RadioStation> comLocalizacao = stations.stream()
                .filter(this::temLocalizacao)
                .findFirst();

        if (comLocalizacao.isPresent()) {
            RadioStation station = comLocalizacao.get();
            // Duas casas decimais (~1 km) bastam para o clima e aproveitam melhor o cache
            String coordenadas = String.format(Locale.ROOT, "%.2f,%.2f", station.getGeoLat(), station.getGeoLong());
            return consultar(coordenadas);
        }

        String lugar = StringUtils.hasText(state) ? state + ", " + country : country;
        return consultar(lugar).filter(clima -> mesmoPais(clima.pais(), country));
    }

    private Optional<Clima> consultar(String lugar) {
        ClimaEmCache emCache = cache.get(lugar);
        if (emCache != null && emCache.validoAte().isAfter(Instant.now())) {
            return Optional.of(emCache.clima());
        }

        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/current.json")
                .queryParam("key", apiKey)
                .queryParam("q", lugar)
                .queryParam("lang", "pt")
                .build()
                .encode()
                .toUri();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resposta = restTemplate.getForObject(uri, Map.class);
            if (resposta == null) {
                return Optional.empty();
            }

            Clima clima = converter(resposta);
            cache.put(lugar, new ClimaEmCache(clima, Instant.now().plus(VALIDADE_CACHE)));
            return Optional.of(clima);

        } catch (Exception e) {
            // Só o tipo do erro: a mensagem completa pode conter a URL com a chave da API
            System.err.println("Erro ao buscar clima para \"" + lugar + "\": " + e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private Clima converter(Map<String, Object> resposta) {
        Map<String, Object> location = (Map<String, Object>) resposta.get("location");
        Map<String, Object> current = (Map<String, Object>) resposta.get("current");
        Map<String, Object> condition = (Map<String, Object>) current.get("condition");

        // "2026-09-29 12:45" -> "12:45"
        String horaLocal = String.valueOf(location.get("localtime"));
        horaLocal = horaLocal.substring(horaLocal.indexOf(' ') + 1);

        // A API manda o ícone sem protocolo: "//cdn.weatherapi.com/..."
        String icone = String.valueOf(condition.get("icon"));
        if (icone.startsWith("//")) {
            icone = "https:" + icone;
        }

        return new Clima(
                (String) location.get("name"),
                (String) location.get("region"),
                (String) location.get("country"),
                arredondar(current.get("temp_c")),
                arredondar(current.get("feelslike_c")),
                (String) condition.get("text"),
                icone,
                arredondar(current.get("humidity")),
                arredondar(current.get("wind_kph")),
                horaLocal);
    }

    private boolean temLocalizacao(RadioStation station) {
        return station.getGeoLat() != null && station.getGeoLong() != null
                && (station.getGeoLat() != 0 || station.getGeoLong() != 0);
    }

    private int arredondar(Object valor) {
        return valor instanceof Number numero ? (int) Math.round(numero.doubleValue()) : 0;
    }

    // Radio Browser: "The United States Of America" / WeatherAPI: "United States of America"
    private boolean mesmoPais(String paisClima, String paisBusca) {
        return normalizarPais(paisClima).equals(normalizarPais(paisBusca));
    }

    private String normalizarPais(String pais) {
        return pais == null ? "" : pais.toLowerCase(Locale.ROOT).replaceFirst("^the ", "").trim();
    }

    private record ClimaEmCache(Clima clima, Instant validoAte) {
    }
}
