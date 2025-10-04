package com.example.gamestore.support.authentication;


import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class Utils {

    /**
     * Restituisce l'id utente presente nel token JWT
     * (il claim "idUtente" deve essere configurato su Keycloak)
     */
    public static Integer getIdUtente() {
        // Recupera l'autenticazione corrente dal SecurityContext di Spring
        JwtAuthenticationToken authenticationToken =
                (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        System.out.println(authenticationToken);

        // Se non c'è autenticazione, restituisce null (utente non loggato)
        if (authenticationToken == null) {
            return null;
        }

        // Estrae il token JWT dalle credenziali di autenticazione
        Jwt jwt = (Jwt) authenticationToken.getCredentials();
        System.out.println(jwt);

        // Cerca il claim personalizzato "idUtente" nella mappa dei claims del token
        Object idClaim = jwt.getClaims().get("idUtente");
        System.out.println(idClaim);
        

        // Se il claim non esiste, restituisce null
        if (idClaim == null) {
            return null;
        }

        // Converte il valore in Integer e lo restituisce
        return Integer.valueOf(idClaim.toString());
    }




    /**
     * Restituisce l'username dell'utente loggato (claim "preferred_username")
     */
    public static String getUsername() {
        JwtAuthenticationToken authenticationToken =
                (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        if (authenticationToken == null) {
            return null;
        }

        Jwt jwt = (Jwt) authenticationToken.getCredentials();
        return jwt.getClaim("preferred_username");
    }
}
