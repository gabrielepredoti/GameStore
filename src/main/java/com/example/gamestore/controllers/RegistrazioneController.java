package com.example.gamestore.controllers;

import com.example.gamestore.dto.UtenteRegistrDto;
import com.example.gamestore.entities.Utente;
import com.example.gamestore.services.RegistrazioneService;
import com.example.gamestore.support.ResponseMessage;
import com.example.gamestore.support.exceptions.ErroreLogoutException;
import com.example.gamestore.support.exceptions.ErroreNellaRegistrazioneUtenteException;
import com.example.gamestore.support.exceptions.UtenteNonValidoONonEsistente;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("utenti")
public class RegistrazioneController {

    @Autowired
    private RegistrazioneService registrazioneService;

    @PostMapping("/registrazione")
    private ResponseEntity createUser(@NotNull @RequestBody UtenteRegistrDto utenteRegistrDto) {

        try{

            registrazioneService.registraNuovoUtente(utenteRegistrDto);
            return new ResponseEntity<>(new ResponseMessage("UTENTE CREATO CON SUCCESSO"), HttpStatus.OK);

        } catch (ErroreNellaRegistrazioneUtenteException e) {
            return new ResponseEntity<>(new ResponseMessage("PROBLEMA NELLA REGISTRAZIONE DELL'UTENTE"), HttpStatus.BAD_REQUEST);
        }
        catch (Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(new ResponseMessage("PROBLEMA NELLA REGISTRAZIONE DELL'UTENTE"), HttpStatus.BAD_REQUEST);
        }

    }

    @GetMapping("/trovaUtente")
    private ResponseEntity getUtente() {

        try {

            Utente u=registrazioneService.trovaUtente();

            if(u == null) {
                return new ResponseEntity(new ResponseMessage("NESSUN UTENTE TROVATO"), HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(u, HttpStatus.OK);

        }catch (UtenteNonValidoONonEsistente e) {
            return new ResponseEntity<>(new ResponseMessage("NESSUN UTENTE AUTENTICATO"),HttpStatus.BAD_REQUEST);
        }catch (Exception e){
            return new ResponseEntity<>(new ResponseMessage("ERRORE OTTENIMENTO UTENTE"),HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/logout/{refreshToken}")
    public ResponseEntity logoutUtente(@NotNull @PathVariable String refreshToken) {

        try {

            registrazioneService.logoutUser(refreshToken);
            return new ResponseEntity<>(new ResponseMessage("LOGOUT ESEGUITO CON SUCCESSO"), HttpStatus.OK);
            //String keycloakLoginUrl = "http://localhost:8080/realms/gamestore/protocol/openid-connect/auth";
            //return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(keycloakLoginUrl)).build();
            //La redirezione mi porta a pagina 404 not found


        }catch (ErroreLogoutException e){
            return new ResponseEntity<>(new ResponseMessage("ERRORE DURANTE IL LOGOUT"), HttpStatus.BAD_REQUEST);
        }catch (Exception e){
            return new ResponseEntity<>(new ResponseMessage("ERRORE"), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/test-keycloak")
    public ResponseEntity testKeycloak() {
        registrazioneService.testConnessioneKeycloak();
        return new ResponseEntity<>(new ResponseMessage("TEST COMPLETATO - CONTROLLA I LOG"), HttpStatus.OK);
    }


}
