package com.example.gamestore.controllers;

import com.example.gamestore.entities.DettaglioOrdine;
import com.example.gamestore.entities.Ordine;
import com.example.gamestore.services.OrdineService;
import com.example.gamestore.support.ResponseMessage;
import com.example.gamestore.support.exceptions.*;
import com.example.gamestore.support.authentication.Utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/ordini")
public class OrdineController {

    @Autowired
    private OrdineService ordineService;

    /** Metodo solo admin per vedere tutti gli ordini */
    @PreAuthorize("hasRole('admin')")
    @GetMapping("/elencoOrdini")
    public ResponseEntity<?> getAll(
            @RequestParam(value = "numPagina", defaultValue = "0") int numPagina,
            @RequestParam(value = "dimPagina", defaultValue = "20") int dimPagina,
            @RequestParam(value = "ordinamento", defaultValue = "dataOrdine") String ordinamento
    ) {
        LinkedList<String> ordinamentoValido = new LinkedList<>(Arrays.asList("dataOrdine", "utente", "totale"));
        if (numPagina < 0 || dimPagina <= 0 || !ordinamentoValido.contains(ordinamento)) {
            return new ResponseEntity<>(new ResponseMessage("Parametri di paginazione/ordinamento non validi"), HttpStatus.BAD_REQUEST);
        }
        List<Ordine> ordini = ordineService.mostraTuttiGliOrdini(numPagina, dimPagina, ordinamento).getContent();
        if (ordini.isEmpty()) {
            return new ResponseEntity<>(new ResponseMessage("Nessun ordine trovato"), HttpStatus.OK);
        }
        return new ResponseEntity<>(ordini, HttpStatus.OK);
    }

    /** Metodo per inserire ordine (admin?) */
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/inserimento")
    public ResponseEntity<?> inserimento(@RequestBody @Valid Ordine ordine) {
        try {
            Date now = new Date();
            if (ordine.getDataOrdine() == null || ordine.getTotale() < 0 || ordine.getDataOrdine().after(now)) {
                return new ResponseEntity<>(new ResponseMessage("Ordine non valido"), HttpStatus.BAD_REQUEST);
            }
            Ordine res = ordineService.salvaOrdine(ordine);
            return new ResponseEntity<>(res, HttpStatus.OK);
        } catch (OrdineNonValido e) {
            return new ResponseEntity<>(new ResponseMessage("Ordine non valido"), HttpStatus.BAD_REQUEST);
        } catch (QuantitaVideogiocoNonDisponibile e) {
            return new ResponseEntity<>(new ResponseMessage("Quantità videogioco non disponibile"), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResponseMessage("Errore nell’inserimento"), HttpStatus.BAD_REQUEST);
        }
    }

    /** Metodi per utente loggato: vedere i propri ordini in un periodo */
    @PreAuthorize("hasRole('utente')")
    @GetMapping("/elencoOrdini/{dataInizio}/{dataFine}")
    public ResponseEntity<?> getOrdiniNelPeriodo(
            @PathVariable("dataInizio") @DateTimeFormat(pattern = "dd-MM-yyyy") Date dataInizio,
            @PathVariable("dataFine") @DateTimeFormat(pattern = "dd-MM-yyyy") Date dataFine,
            @RequestParam(value = "numPagina", defaultValue = "0") int numPagina,
            @RequestParam(value = "dimPagina", defaultValue = "20") int dimPagina,
            @RequestParam(value = "ordinamento", defaultValue = "dataOrdine") String ordinamento
    ) {
        try {
            LinkedList<String> ordinamentoValido = new LinkedList<>(Arrays.asList("dataOrdine", "utente", "totale"));
            if (numPagina < 0 || dimPagina <= 0 || !ordinamentoValido.contains(ordinamento)) {
                return new ResponseEntity<>(new ResponseMessage("Parametri non validi"), HttpStatus.BAD_REQUEST);
            }
            // estendi dataFine a fine giornata, cioè metto dataFine a 23:59 così è compreso, sennò le query lo escludono
            Calendar cal = Calendar.getInstance();
            cal.setTime(dataFine);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            dataFine = cal.getTime();

            List<Ordine> ordini = ordineService.getOrdiniUtenteInPeriodo(dataInizio, dataFine, numPagina, dimPagina, ordinamento).getContent();
            if (ordini.isEmpty()) {
                return new ResponseEntity<>(new ResponseMessage("Nessun ordine trovato in questo periodo"), HttpStatus.OK);
            }
            return new ResponseEntity<>(ordini, HttpStatus.OK);
        } catch (DateORangeDateNonValido e) {
            return new ResponseEntity<>(new ResponseMessage("Intervallo date non accettabile"), HttpStatus.BAD_REQUEST);
        } catch (UtenteNonValidoONonEsistente e) {
            return new ResponseEntity<>(new ResponseMessage("Utente non valido"), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ResponseMessage("Errore nella richiesta"), HttpStatus.BAD_REQUEST);
        }
    }

    /** Visualizza i propri ordini */
    @PreAuthorize("hasRole('utente')")
    @GetMapping("/elencoOrdini/perUtente")
    public ResponseEntity<?> getOrdiniUtente(
            @RequestParam(value = "numPagina", defaultValue = "0") int numPagina,
            @RequestParam(value = "dimPagina", defaultValue = "20") int dimPagina,
            @RequestParam(value = "ordinamento", defaultValue = "dataOrdine") String ordinamento
    ) {
        try {
            LinkedList<String> ordinamentoValido = new LinkedList<>(Arrays.asList("dataOrdine", "utente", "totale"));
            if (numPagina < 0 || dimPagina <= 0 || !ordinamentoValido.contains(ordinamento)) {
                return new ResponseEntity<>(new ResponseMessage("Parametri non validi"), HttpStatus.BAD_REQUEST);
            }
            List<Ordine> ordini = ordineService.getOrdiniUtente(numPagina, dimPagina, ordinamento).getContent();
            if (ordini.isEmpty()) {
                return new ResponseEntity<>(new ResponseMessage("Nessun ordine trovato per l'utente"), HttpStatus.OK);
            }
            return new ResponseEntity<>(ordini, HttpStatus.OK);
        } catch (UtenteNonValidoONonEsistente e) {
            return new ResponseEntity<>(new ResponseMessage("Utente non valido"), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ResponseMessage("Errore nella richiesta"), HttpStatus.BAD_REQUEST);
        }
    }

    /** Annulla un ordine (se ancora possibile) */
    @PreAuthorize("hasRole('utente')")
    @DeleteMapping("/{idOrdine}")
    public ResponseEntity<?> rimuoviOrdine(@PathVariable int idOrdine) {
        try {
            ordineService.rimuoviOrdine(idOrdine);
            return new ResponseEntity<>(new ResponseMessage("Ordine rimosso"), HttpStatus.OK);
        } catch (OrdineNonPiuAnnullabileException e) {
            return new ResponseEntity<>(new ResponseMessage("Ordine non più annullabile, è trascorsa più di un'ora dalla sua accettazione"), HttpStatus.BAD_REQUEST);
        } catch (UtenteNonValidoONonEsistente e) {
            return new ResponseEntity<>(new ResponseMessage("Utente non valido o non esistente"), HttpStatus.UNAUTHORIZED);
        } catch (DettaglioOrdineNonValido e) {
            return new ResponseEntity<>(new ResponseMessage("Dettaglio ordine non valido"), HttpStatus.BAD_REQUEST);
        } catch (OrdineNonPresenteNelDbException e) {
            return new ResponseEntity<>(new ResponseMessage("Ordine non presente"), HttpStatus.BAD_REQUEST);
        } catch (VideogiocoNonValidoException e){
            return new ResponseEntity<>(new ResponseMessage("Videogioco non valido"), HttpStatus.BAD_REQUEST);
        } catch (TentativoNonAutorizzato e) {
            return new ResponseEntity<>(new ResponseMessage("Tentativo non autorizzato"), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResponseMessage("Errore nella rimozione"), HttpStatus.BAD_REQUEST);
        }
    }

    /** Vedi i dettagli di un ordine specifico */
    @PreAuthorize("hasRole('utente')")
    @GetMapping("dettagliOrdine/{idOrdine}")
    public ResponseEntity<?> getDettaglioOrdine(
            @PathVariable int idOrdine,
            @RequestParam(value = "numPagina", defaultValue = "0") int numPagina,
            @RequestParam(value = "dimPagina", defaultValue = "20") int dimPagina,
            @RequestParam(value = "ordinamento", defaultValue = "prezzoUnitario") String ordinamento
    ) {
        try {
            LinkedList<String> ordinamentoValido = new LinkedList<>(Arrays.asList("quantita", "prezzoUnitario"));
            if (numPagina < 0 || dimPagina <= 0 || !ordinamentoValido.contains(ordinamento)) {
                return new ResponseEntity<>(new ResponseMessage("Parametri non validi"), HttpStatus.BAD_REQUEST);
            }
            List<DettaglioOrdine> dettagli = ordineService.trovaDettagliOrdine(idOrdine, numPagina, dimPagina, ordinamento).getContent();
            if (dettagli.isEmpty()) {
                return new ResponseEntity<>(new ResponseMessage("Nessun dettaglio per questo ordine"), HttpStatus.OK);
            }
            return new ResponseEntity<>(dettagli, HttpStatus.OK);
        } catch (OrdineNonPresenteNelDbException e) {
            return new ResponseEntity<>(new ResponseMessage("Ordine non presente"), HttpStatus.BAD_REQUEST);
        } catch (UtenteNonValidoONonEsistente e) {
            return new ResponseEntity<>(new ResponseMessage("Utente non valido o non esistente"), HttpStatus.UNAUTHORIZED);
        } catch (TentativoNonAutorizzato e) {
            return new ResponseEntity<>(new ResponseMessage("Tentativo non autorizzato"), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResponseMessage("Errore nella richiesta"), HttpStatus.BAD_REQUEST);
        }
    }
}
