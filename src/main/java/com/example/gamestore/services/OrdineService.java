package com.example.gamestore.services;

import com.example.gamestore.entities.*;
import com.example.gamestore.repositories.*;
import com.example.gamestore.support.authentication.Utils;
import org.antlr.v4.runtime.misc.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import com.example.gamestore.support.exceptions.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;


@Service
public class OrdineService {

    @Autowired
    private  OrdineRepository ordineRepository;

    @Autowired
    private VideogiocoRepository videogiocoRepository;

    //@Autowired
    //private  CarrelloRepository carrelloRepository;

    @Autowired
    private DettaglioCarrelloRepository dettaglioCarrelloRepository;

    @Autowired
    private  DettaglioOrdineRepository dettaglioOrdineRepository;

    @Autowired
    private UtenteRepository utenteRepository;


    //Prima metodi transazionali con solo lettura dal db

    @Transactional(readOnly = true)
    public Page<Ordine> getOrdiniUtente(int numPagina, int dimPagina, String ordinamento)  throws UtenteNonValidoONonEsistente{

        //L'istanza dell'utente corrente la prendo in automatico dal token
        if(!utenteRepository.existsById(Utils.getIdUtente())) {
            throw new UtenteNonValidoONonEsistente();
        }

        Utente u = utenteRepository.findById(Utils.getIdUtente()).get(); //il controllo precedente mi permette di fare la get direttamente
        Sort.Direction tipoOrdinamento = Sort.Direction.DESC;
        Pageable paging = PageRequest.of(numPagina, dimPagina, Sort.by(tipoOrdinamento, ordinamento)); //ordinamento sarebbe l'attributo per cui vogliamo ordinare(data, totale)
        return ordineRepository.findByUtente(u, paging);

    }

    @Transactional(readOnly = true)
    public Page<Ordine> mostraTuttiGliOrdini(int numPagina, int dimPagina, String ordinamento){
        Sort.Direction tipoOrdinamento = Sort.Direction.DESC;
        Pageable paging = PageRequest.of(numPagina, dimPagina, Sort.by(tipoOrdinamento, ordinamento));
        return ordineRepository.findAll(paging);
    }

    @Transactional(readOnly = true)
    public Page<Ordine> getOrdiniUtenteInPeriodo(Date inizio, Date fine, int numPagina, int dimPagina, String ordinamento) throws DateORangeDateNonValido, UtenteNonValidoONonEsistente{

        if(inizio == null || fine == null || inizio.after(fine)){
            throw new DateORangeDateNonValido();
        }

        if(!utenteRepository.existsById(Utils.getIdUtente())) {
            throw new UtenteNonValidoONonEsistente();
        }

        Utente u = utenteRepository.findById(Utils.getIdUtente()).get();

        Sort.Direction tipoOrdinamento = Sort.Direction.DESC;
        Pageable paging = PageRequest.of(numPagina, dimPagina, Sort.by(tipoOrdinamento, ordinamento));
        return ordineRepository.ricercaPerUtenteInPeriodo(u, inizio, fine, paging);

    }

    @Transactional(readOnly = true)
    public Page<DettaglioOrdine> trovaDettagliOrdine(int idOrdine,int numPagina, int dimPagina,String ordinamento) throws OrdineNonPresenteNelDbException, UtenteNonValidoONonEsistente, TentativoNonAutorizzato {

        Sort.Direction tipoOrdinamento = Sort.Direction.DESC;
        Pageable paging = PageRequest.of(numPagina, dimPagina, Sort.by(tipoOrdinamento, ordinamento));
        Optional<Ordine> ordine = ordineRepository.findById(idOrdine);

        if (ordine.isEmpty()) {
            throw new OrdineNonPresenteNelDbException();
        }

        Utente utenteOrdine=utenteRepository.findById(ordine.get().getUtente().getId()).orElse(null);
        Optional<Utente> u=utenteRepository.findById(Utils.getIdUtente());
        if(u.isEmpty()){
            throw new UtenteNonValidoONonEsistente();
        }
        Utente utente=u.get();

        //Devo capire se l'utente dell'Ordine di id idOrdine esiste nel db e se quest'ultimo corrisponde all'utente dato dal token
        if(utenteOrdine==null||utenteOrdine.getId()!=utente.getId()){
            throw new UtenteNonValidoONonEsistente();
        }

        //se l'utente è lo stesso devo vedere se esso effettivamente ha effettuato l'ordine per cui vogliamo vedere i dettagli
        if(!utente.getOrdini().contains(ordine.get())){
            throw new TentativoNonAutorizzato();
        }

        return dettaglioOrdineRepository.findByOrdine_Id(idOrdine,paging);

        }

    // Ora metodi nel db che comprendono operazioni di write

    @Transactional(readOnly = false, rollbackFor = {OrdineNonValido.class, QuantitaVideogiocoNonDisponibile.class})
    public Ordine salvaOrdine(@NotNull Ordine ordine) throws OrdineNonValido, QuantitaVideogiocoNonDisponibile{

        if(ordine.getListaDettagliOrdine().isEmpty())
            throw new OrdineNonValido();

        double totaleOrdine = 0.0;

        for(DettaglioOrdine d: ordine.getListaDettagliOrdine()){

            Videogioco videogiocoDaAcquistare = videogiocoRepository.findById(d.getVideogioco().getId()).orElse(null);


            //Devo controllare che esista il videogioco, che le quantità siano valide e che il prezzo nel dettaglio ordine sia quello del videogioco nello stesso dettaglio ordine
            if(videogiocoDaAcquistare==null || d.getQuantita() <= 0 || d.getPrezzoUnitario() == null ||
                d.getPrezzoUnitario() <= 0 || !d.getPrezzoUnitario().equals(videogiocoDaAcquistare.getPrezzo())){
                throw new OrdineNonValido();
            }

            if(videogiocoDaAcquistare.getQuantita() < d.getQuantita()){
                throw new QuantitaVideogiocoNonDisponibile();
            }

            //Il videogioco a questo punto sicuramente non sarà null, perciò
            int nuovaQuantita = videogiocoDaAcquistare.getQuantita() - d.getQuantita();
            videogiocoDaAcquistare.setQuantita(nuovaQuantita);

            // Salvo subito il videogioco aggiornato (nuova quantità disponibile)
            videogiocoRepository.save(videogiocoDaAcquistare);

            //Devo gestire anche persistenza di dettaglioOrdine
            d.setOrdine(ordine);

            //Totale da mettere su Ordine
            totaleOrdine += d.getPrezzoUnitario()*d.getQuantita();

        }

        ordine.setTotale(totaleOrdine);

        Ordine ordineSalvato = ordineRepository.save(ordine);
        dettaglioOrdineRepository.saveAll(ordine.getListaDettagliOrdine());

        return ordineSalvato;

    }


    @Transactional(readOnly = false, rollbackFor = {OrdineNonPresenteNelDbException.class, OrdineNonPiuAnnullabileException.class, UtenteNonValidoONonEsistente.class, TentativoNonAutorizzato.class, DettaglioOrdineNonValido.class, VideogiocoNonValidoException.class})
    public void rimuoviOrdine(int idOrdine) throws OrdineNonPresenteNelDbException, OrdineNonPiuAnnullabileException, UtenteNonValidoONonEsistente, TentativoNonAutorizzato, DettaglioOrdineNonValido, VideogiocoNonValidoException
    {

        Optional<Ordine> ordine = ordineRepository.findById(idOrdine);
        if(ordine.isPresent()){

            Ordine daEliminare =  ordine.get();
            Date adesso = new Date();
            Date dataOrdine = daEliminare.getDataOrdine();

            //Permetto rimozione ordine solo entro un certo tempo (un'ora)
            long millisecondiTrascorsi=Math.abs(dataOrdine.getTime()-adesso.getTime());

            if(millisecondiTrascorsi > 3600*1000){ //è passata piu di un'ora dall'ordine
                throw new OrdineNonPiuAnnullabileException();
            }

            Utente utenteOrdineDaEliminare=utenteRepository.findById(daEliminare.getUtente().getId()).orElse(null);

            Optional<Utente> u = utenteRepository.findById(Utils.getIdUtente());
            if(u.isEmpty()){
                throw new UtenteNonValidoONonEsistente();
            }
            Utente utente=u.get();
            //Verifico se ricevo null o se l'utente presente nell'ordine non è quello del token
            if(utenteOrdineDaEliminare == null || utenteOrdineDaEliminare.getId() != utente.getId()){
                throw new UtenteNonValidoONonEsistente();
            }

            if(!utente.getOrdini().contains(ordine.get())){
                throw new TentativoNonAutorizzato();
            }

            utenteOrdineDaEliminare.getOrdini().remove(daEliminare); //elimino questo tra i suoi ordini


            for(DettaglioOrdine d: daEliminare.getListaDettagliOrdine()){

                DettaglioOrdine dettaglioDaEliminare = dettaglioOrdineRepository.findById(d.getId()).orElse(null);

                if(dettaglioDaEliminare == null){

                    throw new DettaglioOrdineNonValido();

                }

                Videogioco videogioco = videogiocoRepository.findById(d.getVideogioco().getId()).orElse(null);

                if(videogioco == null){
                    throw new VideogiocoNonValidoException();
                }


                int quantitaAggiornata = videogioco.getQuantita() + d.getQuantita();
                videogioco.setQuantita(quantitaAggiornata);//reimposto quantita disponibile prodotto
                videogioco.getListaDettagliOrdine().remove(dettaglioDaEliminare);

                // Se nel carrello c'era già uno dei prodotti presente nell'ordine annullato
                // vado ad aumentare la quantità senza creare un nuovo DettaglioCarrello o violo i vincoli unique id_prodotto-id_carrello
                if(dettaglioCarrelloRepository.existsByCarrello_IdAndVideogioco_Id(utente.getCarrello().getId(), videogioco.getId())){
                    DettaglioCarrello dettaglioCarrello = dettaglioCarrelloRepository.findByCarrello_IdAndVideogioco_Id(utente.getCarrello().getId(), videogioco.getId());
                    dettaglioCarrello.setQuantita(dettaglioCarrello.getQuantita() + d.getQuantita());
                }
                //e se non c'è aggiungo
                else {
                    DettaglioCarrello dettcarr = new DettaglioCarrello();
                    dettcarr.setVideogioco(d.getVideogioco());
                    dettcarr.setQuantita(d.getQuantita());
                    dettcarr.setPrezzoUnitario(videogioco.getPrezzo());
                    dettcarr.setCarrello(utenteOrdineDaEliminare.getCarrello());
                    dettaglioCarrelloRepository.save(dettcarr);
                }
                dettaglioOrdineRepository.delete(d);
            }

            //Elimino l'ordine alla fine
            ordineRepository.delete(daEliminare);
        }else{
            throw new OrdineNonPresenteNelDbException();
        }

        }

    }









