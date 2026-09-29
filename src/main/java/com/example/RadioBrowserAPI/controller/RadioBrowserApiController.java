package com.example.RadioBrowserAPI.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.RadioBrowserAPI.config.UserConfig;
import com.example.RadioBrowserAPI.model.RadioStation;
import com.example.RadioBrowserAPI.service.ClimaService;
import com.example.RadioBrowserAPI.service.FavoriteService;
import com.example.RadioBrowserAPI.service.PasswordRecoveryService;
import com.example.RadioBrowserAPI.service.RadioBrowserApiService;
import com.example.RadioBrowserAPI.service.SendEmailService;
import com.example.RadioBrowserAPI.service.UserService;

@Controller
public class RadioBrowserApiController {
    private final UserConfig userConfig;
    private final SendEmailService sendEmailService;
    private final UserService userService;
    private final PasswordRecoveryService passwordRecoveryService;
    private final RadioBrowserApiService radioBrowserApiService;
    private final FavoriteService favoriteService;
    private final ClimaService climaService;

    @Value("${app.base-url}")
    private String baseUrl;

    //CONSTRUTOR
    public RadioBrowserApiController(RadioBrowserApiService radioBrowserApiService,
                                      FavoriteService favoriteService, UserConfig userConfig, SendEmailService sendEmailService, UserService userService, PasswordRecoveryService passwordRecoveryService,
                                      ClimaService climaService) {
                                        this.userConfig = userConfig;
                                        this.sendEmailService = sendEmailService;
                                        this.userService = userService;
                                        this.passwordRecoveryService = passwordRecoveryService;
                                        this.radioBrowserApiService = radioBrowserApiService;
                                        this.favoriteService = favoriteService;
                                        this.climaService = climaService;
    }

    // =========================================================
    // HOME
    // =========================================================

     @GetMapping("/home")
        public String home(
                        Authentication authentication,
                        Model model,
                        @RequestParam(defaultValue = "Brazil") String country,
                        @RequestParam(required = false) String state) {

                // Sem o parâmetro (ex.: logo após o login) usa Minas Gerais.
                // Vazio significa "Todos os estados" — por isso não dá pra usar defaultValue,
                // que o Spring também aplica quando o valor vem vazio.
                if (state == null) {
                        state = "Minas Gerais";
                }

                System.out.println(
                                "Usuário logado: " + authentication.getName());

                model.addAttribute(
                                "usuario",
                                authentication.getName());


                     

                List<RadioStation> radioStations = radioBrowserApiService.listRadioStations(country, state);

                // Marca quais estações já são favoritas (uma única consulta ao banco)
                Set<String> favoriteUuids = favoriteService.getFavoriteUuids(authentication.getName());
                radioStations.forEach(station ->
                        station.setFavorite(favoriteUuids.contains(station.getStationuuid())));

                // Sort estável: favoritas primeiro, preservando a ordenação por votos já aplicada
                // dentro de cada grupo (favoritas e não favoritas), já que List.sort é estável.
                radioStations.sort(Comparator.comparing(RadioStation::isFavorite).reversed());

                model.addAttribute("stations", radioStations);
                model.addAttribute("countries", radioBrowserApiService.listCountries());
                model.addAttribute("states", radioBrowserApiService.listStates());
                model.addAttribute("selectedCountry", country);
                model.addAttribute("selectedState", state);
                // Clima do lugar buscado; null se a WeatherAPI falhar (a página abre sem o card)
                model.addAttribute("clima", climaService.climaDoLugar(country, state, radioStations).orElse(null));
                addHeaderAttributes(model, authentication.getName(), favoriteUuids.size(), "explore");

                return "user/home";
        }

    // =========================================================
    // FAVORITAS
    // =========================================================

    @GetMapping("/favorites")
    public String favorites(Authentication authentication, Model model) {
        String email = authentication.getName();
        Set<String> favoriteUuids = favoriteService.getFavoriteUuids(email);

        // A API devolve as estações fora de ordem: reordena pela ordem das favoritas (mais recentes primeiro)
        Map<String, RadioStation> stationsByUuid = new HashMap<>();
        for (RadioStation station : radioBrowserApiService.listStationsByUuids(favoriteUuids)) {
            station.setFavorite(true);
            stationsByUuid.put(station.getStationuuid(), station);
        }

        List<RadioStation> stations = favoriteUuids.stream()
                .map(stationsByUuid::get)
                .filter(Objects::nonNull)
                .toList();

        long countryCount = stations.stream()
                .map(RadioStation::getCountrycode)
                .distinct()
                .count();

        model.addAttribute("stations", stations);
        model.addAttribute("countryCount", countryCount);
        addHeaderAttributes(model, email, favoriteUuids.size(), "favorites");

        return "user/favorites";
    }

    // =========================================================
    // BUSCA POR NOME (qualquer país)
    // =========================================================

    @GetMapping("/search")
    public String search(
            Authentication authentication,
            Model model,
            @RequestParam(defaultValue = "") String q) {

        String email = authentication.getName();
        String query = q.trim();
        Set<String> favoriteUuids = favoriteService.getFavoriteUuids(email);

        // Com menos de 2 letras a busca traria milhares de estações sem sentido
        List<RadioStation> stations = query.length() >= 2
                ? radioBrowserApiService.searchByName(query)
                : List.of();
        stations.forEach(station ->
                station.setFavorite(favoriteUuids.contains(station.getStationuuid())));

        // Algumas estações vêm sem país: não entram na contagem
        long countryCount = stations.stream()
                .map(RadioStation::getCountrycode)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .count();

        model.addAttribute("stations", stations);
        model.addAttribute("searchQuery", query);
        model.addAttribute("countryCount", countryCount);
        model.addAttribute("resultLimit", RadioBrowserApiService.SEARCH_LIMIT);
        addHeaderAttributes(model, email, favoriteUuids.size(), "search");

        return "user/search";
    }

    /**
     * Usado pela estrela via JavaScript: troca o favorito sem recarregar a página
     * (recarregar interromperia a rádio que está tocando).
     */
    @PostMapping("/api/favorites/toggle")
    @ResponseBody
    public Map<String, Boolean> toggleFavoriteApi(
            Authentication authentication,
            @RequestParam String stationuuid) {

        boolean favorite = favoriteService.toggle(authentication.getName(), stationuuid);
        return Map.of("favorite", favorite);
    }

    /**
     * Versão sem JavaScript: envia o formulário e volta para a página de onde veio.
     */
    @PostMapping("/favorites/toggle")
    public String toggleFavorite(
                Authentication authentication,
            @RequestParam String stationuuid,
            @RequestParam(defaultValue = "Brazil") String country,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String q) throws UnsupportedEncodingException {

        favoriteService.toggle(authentication.getName(), stationuuid);

        if ("favorites".equals(from)) {
            return "redirect:/favorites";
        }

        if ("search".equals(from)) {
            return "redirect:/search?q=" + URLEncoder.encode(q == null ? "" : q, StandardCharsets.UTF_8);
        }

        // O state vai sempre, mesmo vazio ("Todos os estados"); sem ele a home volta para Minas Gerais.
        String encodedCountry = URLEncoder.encode(country, StandardCharsets.UTF_8);
        String encodedState = URLEncoder.encode(state == null ? "" : state, StandardCharsets.UTF_8);

        return "redirect:/home?country=" + encodedCountry + "&state=" + encodedState;
    }

    // Cabeçalho das páginas logadas: nome do usuário, total de favoritas e aba ativa
    private void addHeaderAttributes(Model model, String email, int favoriteCount, String page) {
        String name = userService.getName(email);

        model.addAttribute("userName", name != null ? name : email);
        model.addAttribute("favoriteCount", favoriteCount);
        model.addAttribute("page", page);
    }
    // =========================================================
    // LOGIN
    // =========================================================

        @GetMapping("/login")
        public String login(Model model) {

                model.addAttribute(
                                "recaptchaSiteKey",
                                userConfig.getRecaptchaSiteKey());

                return "login/login";
        }

    // =========================================================
    // ERROR
    // =========================================================

        @GetMapping("/error")
        public String error() {
                return "error";
        }

    // =========================================================
    // CADASTRO
    // =========================================================

        @GetMapping("/register")
        public String register() {
                return "login/register";
        }

        @PostMapping("/register")
        public String handleRegister(
                        @RequestParam("nome") String nome,
                        @RequestParam("email") String email,
                        @RequestParam("senha") String senha) {

                if (userService.exists(email)) {

                        System.out.println(
                                        "Usuário já cadastrado: " + email);

                        return "redirect:/register?erro=usuario";
                }

                userService.createUser(
                                email,
                                senha,
                                nome);

                System.out.println(
                                "Usuário cadastrado: " + email);

                System.out.println(
                                "Nome cadastrado: " + nome);


                return "redirect:/login?cadastro=sucesso";
        }

    // =========================================================
    // RECUPERAÇÃO DE SENHA
    // =========================================================

        @GetMapping("/recoverpassword")
        public String recoverpassword() {
                return "login/recoverpassword";
        }

        @PostMapping("/recoverpassword")
        public String handleRecoverPassword(
                        @RequestParam("email") String email) {

                if (!userService.exists(email)) {

                        System.out.println(
                                        "E-mail não encontrado: " + email);

                        return "redirect:/recoverpassword?erro=email";
                }

                String nome = userService.getName(email);

                if (nome == null || nome.isBlank()) {
                        nome = email;
                }

                String token = passwordRecoveryService.generateToken(email);

                String link = baseUrl + "/resetpassword?token="
                                + token;

                sendEmailService.sendEmail(
                                email,
                                "Recuperação de Senha - Tela Login",
                                "<!DOCTYPE html><html lang='pt-BR'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head><body style='margin:0;padding:0;background-color:#f8fafc;font-family:Arial,Helvetica,sans-serif;color:#171717;'><div style='width:100%;padding:40px 20px;box-sizing:border-box;'><div style='max-width:600px;margin:0 auto;background:#ffffff;border:1px solid rgba(0,51,79,0.09);border-radius:20px;overflow:hidden;box-shadow:0 2px 4px rgba(0,51,79,0.08),0 24px 56px -12px rgba(0,51,79,0.20);'><div style='padding:40px 30px;text-align:center;background:linear-gradient(145deg,#00334f,#005380);'><div style='color:#ffffff;font-size:13px;font-weight:500;letter-spacing:2px;margin-bottom:12px;'>TELA LOGIN</div><div style='color:#ffffff;font-size:28px;font-weight:500;line-height:1.2;'>Recuperação de Senha</div></div><div style='padding:40px 45px;text-align:center;'><p style='margin:0 0 18px 0;color:#005380;font-size:18px;font-weight:500;'>Olá, "
                                                + nome
                                                + "!</p><div style='width:60px;height:3px;margin:0 auto 25px auto;background-color:#005380;border-radius:999px;'></div><p style='margin:0 0 18px 0;color:#64748b;font-size:15px;line-height:1.7;'>Recebemos uma solicitação para redefinir a senha da sua conta no sistema.</p><p style='margin:0 0 30px 0;color:#64748b;font-size:15px;line-height:1.7;'>Clique no botão abaixo para criar uma nova senha.</p><a href='"
                                                + link
                                                + "' style='display:inline-block;padding:14px 28px;background-color:#005380;color:#ffffff;text-decoration:none;border-radius:8px;font-size:15px;font-weight:500;'>Redefinir minha senha</a><p style='margin:30px 0 0 0;padding:15px;background-color:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;color:#64748b;font-size:13px;line-height:1.6;'>Este link é válido por <strong style='color:#171717;'>15 minutos</strong>.</p><p style='margin:25px 0 0 0;color:#64748b;font-size:13px;line-height:1.6;'>Se você não solicitou a recuperação da senha, ignore este e-mail.</p></div><div style='padding:20px 30px;background-color:#f8fafc;border-top:1px solid #e2e8f0;text-align:center;'><p style='margin:0;color:#64748b;font-size:12px;line-height:1.5;'>Tela Login<br>Sistema de Autenticação</p></div></div></div></body></html>");

                System.out.println(
                                "Link de recuperação enviado para: " + email);

                return "redirect:/recoverpassword?sucesso=email";
        }

    // =========================================================
    // RESET DE SENHA
    // =========================================================

        @GetMapping("/resetpassword")
        public String resetPassword(
                        @RequestParam("token") String token,
                        Model model) {

                String email = passwordRecoveryService
                                .getEmailFromToken(token);

                if (email == null) {

                        model.addAttribute(
                                        "erro",
                                        "O link de recuperação é inválido ou expirou.");

                        return "error";
                }

                model.addAttribute(
                                "token",
                                token);

                return "login/resetpassword";
        }

        @PostMapping("/resetpassword")
        public String handleResetPassword(
                        @RequestParam("token") String token,
                        @RequestParam("senha") String senha,
                        @RequestParam("confirmarSenha") String confirmarSenha) {

                if (!senha.equals(confirmarSenha)) {

                        return "redirect:/resetpassword?token="
                                        + token
                                        + "&erro=senhas";
                }

                String email = passwordRecoveryService
                                .getEmailFromToken(token);

                if (email == null) {

                        return "redirect:/login?erro=token";
                }

                userService.updatePassword(
                                email,
                                senha);

                passwordRecoveryService.invalidateToken(token);

                System.out.println(
                                "Senha alterada com sucesso para: "
                                                + email);

                return "redirect:/login?senha=alterada";
        }



}