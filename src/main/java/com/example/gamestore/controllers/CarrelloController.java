package com.example.gamestore.controllers;

import com.example.gamestore.entities.Carrello;
import com.example.gamestore.entities.DettaglioCarrello;
import com.example.gamestore.entities.Ordine;
import com.example.gamestore.services.CarrelloService;
import com.example.gamestore.support.ResponseMessage;
import com.example.gamestore.support.exceptions.*;
import com.example.gamestore.dto.CarrelloDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotNull;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@RestController
@RequestMapping("/carrello")
public class CarrelloController {

    @Autowired
    private CarrelloService carrelloService;

    /**
     * Mostra i dettagli del carrello attivo dell’utente corrente
     */
    @PreAuthorize("hasRole('utente')")
    @GetMapping("{idUtente}")
    public ResponseEntity getAll(
            @PathVariable int idUtente,
            @RequestParam(value = "numPagina", defaultValue = "0") int numPagina,
            @RequestParam(value = "dimPagina", defaultValue = "20") int dimPagina,
            @RequestParam(value = "ordinamento", defaultValue = "quantita") String ordinamento
    ) {
        try {

            LinkedList<String> ordinamentoValido = new LinkedList<>();
            ordinamentoValido.addAll(Arrays.asList("quantita", "prezzounitario"));
            if (numPagina < 0 || dimPagina <= 0 || !ordinamentoValido.contains(ordinamento)) {
                return new ResponseEntity<>(new ResponseMessage("Parametri di paginazione o ordinamento non validi"), HttpStatus.BAD_REQUEST);
            }

            List<DettaglioCarrello> dettagli = carrelloService.mostraTutti(numPagina, dimPagina, ordinamento, idUtente).getContent();
            if (dettagli.isEmpty()) {
                return new ResponseEntity<>(new ResponseMessage("Carrello vuoto o nessun risultato"), HttpStatus.OK);
            }
            return new ResponseEntity<>(dettagli, HttpStatus.OK);
        } catch (UtenteNonValidoONonEsistente e) {
            return new ResponseEntity<>(new ResponseMessage("Utente non esistente o non valido"), HttpStatus.UNAUTHORIZED);
        } catch (CarrelloNonValidoException e) {
            return new ResponseEntity<>(new ResponseMessage("Carrello non valido"), HttpStatus.BAD_REQUEST);
        } catch (TentativoNonAutorizzato e){
            return new ResponseEntity<>(new ResponseMessage("Tentativo non autorizzato"), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ResponseMessage("Errore nella richiesta"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Aggiunge videogiochi al carrello dell’utente corrente
     */
    @PreAuthorize("hasRole('utente')")
    @PostMapping("/aggiungi/{idUtente}/{idVideogioco}/{quantita}")
    public ResponseEntity<?> aggiungiVideogiocoACarrello(
            @PathVariable int idUtente,
            @PathVariable int idVideogioco,
            @PathVariable int quantita
    ) {
        try {
            if (idUtente < 0 || idVideogioco < 0) {
                return new ResponseEntity<>(new ResponseMessage("Carrello o Utente non valido!"), HttpStatus.BAD_REQUEST);
            }
            Carrello carrelloAggiornato = carrelloService.aggiungiVideogioco(idUtente, idVideogioco, quantita);
            return new ResponseEntity<>(carrelloAggiornato, HttpStatus.OK);
        } catch (CarrelloNonValidoException e){
            return new ResponseEntity<>(new ResponseMessage("Carrello non valido!"), HttpStatus.BAD_REQUEST);
        } catch (VideogiocoNonValidoException e) {
            return new ResponseEntity<>(new ResponseMessage("Videogioco non valido"), HttpStatus.BAD_REQUEST);
        } catch (QuantitaVideogiocoNonDisponibile e) {
            return new ResponseEntity<>(new ResponseMessage("Quantità non disponibile"), HttpStatus.BAD_REQUEST);
        } catch (TentativoNonAutorizzato e) {
            return new ResponseEntity<>(new ResponseMessage("Tentativo non autorizzato"), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResponseMessage("Errore nella richiesta"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    /**
     * Compra il contenuto del carrello
     */
    @PreAuthorize("hasRole('utente')")
    @PostMapping("/acquista")
    public ResponseEntity<?> acquista(
            @RequestBody @NotNull CarrelloDto carrelloDto
    ) {
        try {
            Ordine ordine = carrelloService.acquista(carrelloDto);
            return new ResponseEntity<>(ordine, HttpStatus.OK);
        } catch (QuantitaVideogiocoNonDisponibile e) {
            return new ResponseEntity<>(new ResponseMessage("Quantità non disponibile"), HttpStatus.BAD_REQUEST);
        } catch (OrdineNonValido e) {
            return new ResponseEntity<>(new ResponseMessage("Ordine non valido"), HttpStatus.BAD_REQUEST);
        } catch(UtenteNonValidoONonEsistente e) {
            return new ResponseEntity<>(new ResponseMessage("Utente non valido o non esistente"), HttpStatus.BAD_REQUEST);
        } catch (VideogiocoNonValidoException e ){
            return new ResponseEntity<>(new ResponseMessage("Videogioco non valido"), HttpStatus.BAD_REQUEST);
        } catch (CarrelloNonValidoException e ){
            return new ResponseEntity<>(new ResponseMessage("Carrello non valido"), HttpStatus.BAD_REQUEST);
        } catch (TentativoNonAutorizzato e) {
            return new ResponseEntity<>(new ResponseMessage("Tentativo non autorizzato"), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResponseMessage("Errore nell'acquisto"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
