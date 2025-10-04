package com.example.gamestore.controllers;

import com.example.gamestore.entities.Videogioco;
import com.example.gamestore.services.VideogiocoService;
import com.example.gamestore.support.ResponseMessage;
import com.example.gamestore.support.exceptions.*;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@RestController
@RequestMapping("/videogiochi")
public class VideogiocoController {

    @Autowired
    private VideogiocoService videogiocoService;

    @GetMapping("/elencoDisponibili")
    public ResponseEntity<?> getAll(
            @RequestParam(value = "numPagina", defaultValue = "0") int numPagina,
            @RequestParam(value = "dimPagina", defaultValue = "20") int dimPagina,
            @RequestParam(value = "ordinamento", defaultValue = "prezzo") String ordinamento) {

        try {
            LinkedList<String> ordinamentoValido = new LinkedList<>(
                    Arrays.asList("nome", "piattaforma", "descrizione", "prezzo", "quantita")
            );


            if (numPagina < 0 || dimPagina <= 0 || !ordinamentoValido.contains(ordinamento)) {
                return new ResponseEntity<>(new ResponseMessage("PAGINAZIONE NON VALIDA PER I PARAMETRI PASSATI"), HttpStatus.BAD_REQUEST);
            }

            List<Videogioco> listaVideogiochi = videogiocoService.elencoVideogiochi(numPagina, dimPagina, ordinamento).getContent();

            if (listaVideogiochi.isEmpty()) {
                return new ResponseEntity<>(new ResponseMessage("NESSUN RISULTATO O NUMERO DI PAGINA NON VALIDO"), HttpStatus.OK);
            }
            return new ResponseEntity<>(listaVideogiochi, HttpStatus.OK);

        }catch(Exception e){
            //System.out.println("ERRORE NELLA RICERCA");
            return new ResponseEntity<>(new ResponseMessage("ERRORE NELLA RICERCA"), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/perPiattaforma")
    public ResponseEntity<?> getVideogiochiByPiattaforma(
            @RequestParam(required = false) String piattaforma,
            @RequestParam(value = "numPagina", defaultValue = "0") int numPagina,
            @RequestParam(value = "dimPagina", defaultValue = "20") int dimPagina,
            @RequestParam(value = "ordinamento", defaultValue = "prezzo") String ordinamento) {

        LinkedList<String> ordinamentoValido = new LinkedList<>();
        ordinamentoValido.addAll(Arrays.asList("nome", "piattaforma", "descrizione", "prezzo", "quantita"));
        if (numPagina < 0 || dimPagina <= 0 || !ordinamentoValido.contains(ordinamento)) {
            //System.out.println("PAGINAZIONE NON VALIDA PER I PARAMETRI PASSATI");
            return new ResponseEntity<>(new ResponseMessage("PAGINAZIONE NON VALIDA PER I PARAMETRI PASSATI"), HttpStatus.BAD_REQUEST);
        }
        List<Videogioco> listaProdotti = videogiocoService.elencoVideogiochiPerPiattaforma(piattaforma, numPagina, dimPagina, ordinamento).getContent();
        if (listaProdotti.isEmpty()) {
            //System.out.println("NESSUN RISULTATO O NUMERO DI PAGINA NON VALIDO");
            return new ResponseEntity<>(new ResponseMessage("NESSUN RISULTATO O NUMERO DI PAGINA NON VALIDO"), HttpStatus.OK);
        }
        return new ResponseEntity<>(listaProdotti, HttpStatus.OK);
    }

    @GetMapping("/perFasciaPrezzo")
    public ResponseEntity<?> getVideogiochiByFasciaPrezzo(
            @RequestParam(defaultValue = "0.1") Double minPrezzo,
            @RequestParam(defaultValue = "" + Double.MAX_VALUE) Double maxPrezzo,
            @RequestParam(value = "numPagina", defaultValue = "0") int numPagina,
            @RequestParam(value = "dimPagina", defaultValue = "20") int dimPagina) {

        //Chiamo il metodo ricercaAvanzata di questo controller, in cui farò tutti i controlli del caso
        //Prendere i videogiochi per fascia di prezzo è un caso particolare della ricerca avanzata con i parametri qui sotto
        return this.ricercaAvanzata(minPrezzo, maxPrezzo, null, null, 0, numPagina, dimPagina, "prezzo");
    }

    @GetMapping("/perNome/{nomeVideogioco}")
    public ResponseEntity<?> getVideogiochiByNome(@PathVariable String nomeVideogioco) {

        try{

            List<Videogioco> lista = videogiocoService.trovaVideogiocoByNome(nomeVideogioco);

            if (lista == null || lista.isEmpty()) {
                return new ResponseEntity<>(new ResponseMessage("NESSUN VIDEOGIOCO CON QUESTO NOME"), HttpStatus.OK);
            }

            Videogioco v = lista.get(0); //Nel Service restituiamo una lista di videogiochi con lo stesso nome, perchè possono esserci più
                                         //videogiochi con lo stesso nome ma di diversa piattaforma. Noi decidiamo di restituire il primo videogioco
                                         //con quel nome e che sia visibile.

            if (v == null) {
                return new ResponseEntity<>(new ResponseMessage("NESSUN VIDEOGIOCO CON QUESTO NOME"), HttpStatus.OK);
            }
            return new ResponseEntity<>(v, HttpStatus.OK);

        } catch (Exception e) {

            //System.out.println("ERRORE NELLA RICERCA");
            return new ResponseEntity<>(new ResponseMessage("ERRORE NELLA RICERCA"), HttpStatus.BAD_REQUEST);

        }

    }

    @PreAuthorize("hasRole('admin')")
    @PostMapping
    public ResponseEntity<?> salvaVideogioco(@RequestBody @Valid Videogioco v) {

        try {

            videogiocoService.salvaVideogioco(v);
            return new ResponseEntity<>(new ResponseMessage("VIDEOGIOCO SALVATO CON SUCCESSO"), HttpStatus.OK);

        } catch (VideogiocoGiaEsistenteException e) {
            return new ResponseEntity<>(new ResponseMessage("VIDEOGIOCO GIA' ESISTENTE"), HttpStatus.BAD_REQUEST);

        } catch (VideogiocoNonValidoException | FasciaPrezzoNonValida e) {
            return new ResponseEntity<>(new ResponseMessage("VIDEOGIOCO NON VALIDO O FASCIA PREZZO ERRATA"), HttpStatus.BAD_REQUEST);

        }

        catch (Exception e) {
            return new ResponseEntity<>(new ResponseMessage("ERRORE NELL'AGGIUNTA"), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/ricercaAvanzata")
    public ResponseEntity<?> ricercaAvanzata(
            @RequestParam(required = false, defaultValue = "0.1") Double prezzoMin,
            @RequestParam(required = false, defaultValue = "" + Double.MAX_VALUE) Double prezzoMax,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String piattaforma,
            @RequestParam(required = false, defaultValue = "0") Integer quantita,
            @RequestParam(value = "numPagina", defaultValue = "0") int numPagina,
            @RequestParam(value = "dimPagina", defaultValue = "20") int dimPagina,
            @RequestParam(defaultValue = "prezzo") String ordinamento) {
        try {

            LinkedList<String> ordinamentoValido = new LinkedList<>();
            ordinamentoValido.addAll(Arrays.asList("nome", "piattaforma", "descrizione", "prezzo", "quantita"));
            if (numPagina < 0 || dimPagina <= 0 || !ordinamentoValido.contains(ordinamento)) {
                System.out.println("PAGINAZIONE NON VALIDA PER I PARAMETRI PASSATI");
                return new ResponseEntity<>(new ResponseMessage("PAGINAZIONE NON VALIDA PER I PARAMETRI PASSATI"), HttpStatus.BAD_REQUEST);
            }

            List<Videogioco> listaVideogiochiFiltrati = videogiocoService.ricercaApprofondita(numPagina, dimPagina, prezzoMin, prezzoMax, nome, piattaforma, quantita, ordinamento);
            if (listaVideogiochiFiltrati.isEmpty()) {
                return new ResponseEntity<>(new ResponseMessage("NESSUN RISULTATO"), HttpStatus.OK);
            }
            return new ResponseEntity<>(listaVideogiochiFiltrati, HttpStatus.OK);
        } catch (FasciaPrezzoNonValida e) {
            return new ResponseEntity<>(new ResponseMessage("FASCIA DI PREZZO NON VALIDA"), HttpStatus.BAD_REQUEST);
        }
    }

    @PreAuthorize("hasRole('admin')")
    @DeleteMapping("/{idVideogioco}")
    public ResponseEntity<?> rimuoviVideogioco(@PathVariable int idVideogioco) {

        try {

            videogiocoService.rimuoviVideogioco(idVideogioco);
            return new ResponseEntity<>(new ResponseMessage("RIMOZIONE ANDATA A BUON FINE"), HttpStatus.OK);

        } catch (VideogiocoGiaEliminatoException | VideogiocoNonPresenteNelDBException e) {
            return new ResponseEntity<>(new ResponseMessage("VIDEOGIOCO NON VALIDO O GIA' ELIMINATO"), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResponseMessage("ERRORE NELLA RIMOZIONE"), HttpStatus.BAD_REQUEST);
        }
    }
}