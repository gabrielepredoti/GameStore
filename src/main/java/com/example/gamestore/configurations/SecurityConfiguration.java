package com.example.gamestore.configurations;

import com.example.gamestore.support.authentication.JwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationConverter jwtAuthConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // disabilita CSRF (non necessaria per API stateless)
                .authorizeHttpRequests(auth -> auth
                        // Endpoint pubblici
                        .requestMatchers("/videogiochi/elencoDisponibili/**").permitAll()
                        .requestMatchers("/videogiochi/perPiattaforma/**").permitAll()
                        .requestMatchers("/videogiochi/perFasciaPrezzo/**").permitAll()
                        .requestMatchers("/videogiochi/perNome/**").permitAll()
                        .requestMatchers("/videogiochi/ricercaAvanzata/**").permitAll()
                        .requestMatchers("/utenti/**").permitAll()

                        // Tutti gli altri richiedono autenticazione
                        .anyRequest().authenticated()
                )
                // Configurazione per usare JWT come sistema di autenticazione
                // Usiamo un server OAuth2 (Keycloak) per validare i token JWT
                // jwtAuthConverter serve per convertire il token JWT in un oggetto utente che Spring capisce
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)))
                // Niente sessioni lato server → stateless API -> ogni richiesta deve portare con sé il token
                .sessionManagement(sess -> sess.sessionCreationPolicy(STATELESS));

        return http.build();
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowCredentials(true);
        configuration.setAllowedOriginPatterns(List.of("*")); // permette tutte le origini
        configuration.setAllowedHeaders(List.of("*"));        // permette tutti gli header
        configuration.setAllowedMethods(List.of("OPTIONS", "GET", "POST", "PUT", "DELETE"));

        source.registerCorsConfiguration("/**", configuration); //registra configurazione CORS per tutti gli endpoint
        return new CorsFilter(source);
    }
}