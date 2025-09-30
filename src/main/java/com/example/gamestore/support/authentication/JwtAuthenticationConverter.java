package com.example.gamestore.support.authentication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component // Rende questa classe un bean Spring, così può essere iniettata in SecurityConfig
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    /**
     * Converter standard di Spring che legge i "scope" dal JWT (es. scope=read,write)
     */
    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    /**
     * Indica quale claim usare come "username" principale.
     * Di default prende "preferred_username" (che Keycloak mette nel token).
     */
    @Value("${jwt.auth.converter.principle-attribute:preferred_username}")
    private String principleAttribute;

    /**
     * Indica quale clientId usare per estrarre i ruoli dall'oggetto "resource_access" nel token.
     * Di default "gamestore-client" (cioè il tuo client registrato su Keycloak).
     */
    @Value("${jwt.auth.converter.resource-id:gamestore-client}")
    private String resourceId;

    /**
     * Converte un oggetto Jwt (token JWT decodificato) in un'Authentication di Spring Security.
     */
    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        // Combina le authorities standard di Spring (scope=...) con i ruoli custom di Keycloak
        Collection<GrantedAuthority> authorities = Stream.concat(
                jwtGrantedAuthoritiesConverter.convert(jwt).stream(),
                extractResourceRoles(jwt).stream()
        ).collect(Collectors.toSet());

        // Crea un token di autenticazione Spring Security contenente:
        // - il JWT originale
        // - i ruoli/authorities
        // - l’attributo principale che useremo come "username"
        return new JwtAuthenticationToken(jwt, authorities, getPrincipleClaimName(jwt));
    }

    /**
     * Determina quale attributo usare come "username" dell’utente autenticato.
     * Di default usa "preferred_username", se non c’è usa "sub" (l’ID univoco del token).
     */
    private String getPrincipleClaimName(Jwt jwt) {
        return jwt.getClaim(principleAttribute) != null ?
                jwt.getClaim(principleAttribute) :
                jwt.getClaim(JwtClaimNames.SUB);
    }

    /**
     * Estrae i ruoli assegnati all’utente da Keycloak dal claim "resource_access".
     * Questo claim contiene le autorizzazioni per ciascun client registrato in Keycloak.
     * Ad esempio:
     * {
     *   "resource_access": {
     *     "gamestore-client": {
     *       "roles": ["USER", "ADMIN"]
     *     }
     *   }
     * }
     */
    private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
        if (jwt.getClaim("resource_access") == null) {
            return Set.of();
        }
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");

        if (resourceAccess.get(resourceId) == null) {
            return Set.of();
        }

        // Estraggo i ruoli del client specifico
        Map<String, Object> resource = (Map<String, Object>) resourceAccess.get(resourceId);
        Collection<String> resourceRoles = (Collection<String>) resource.get("roles");

        // Converto i ruoli in authorities di Spring Security, prefissati con "ROLE_"
        return resourceRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toSet());
    }
}