package com.example.gamestore.services;

import com.example.gamestore.dto.CarrelloDto;
import com.example.gamestore.dto.DettaglioDto;
import com.example.gamestore.entities.*;
import com.example.gamestore.repositories.CarrelloRepository;
import com.example.gamestore.repositories.DettaglioCarrelloRepository;
import com.example.gamestore.repositories.UtenteRepository;
import com.example.gamestore.repositories.VideogiocoRepository;
import com.example.gamestore.support.authentication.Utils;
import com.example.gamestore.support.exceptions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CarrelloService {

    @Autowired
    private CarrelloRepository carrelloRepository;

    @Autowired
    private DettaglioCarrelloRepository dettaglioCarrelloRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private VideogiocoRepository videogiocoRepository;

    @Autowired
    private OrdineService ordineService;

    // mostro tutti i dettagliCarrello di un utente specifico
    @Transactional(readOnly = true)
    public Page<DettaglioCarrello> mostraTutti(int numPagina, int dimPagina, String ordinamento, int idUtente) throws TentativoNonAutorizzato, UtenteNonValidoONonEsistente, CarrelloNonValidoException{

        int idUt = Utils.getIdUtente();
        if(idUtente != idUt){
            throw new TentativoNonAutorizzato();
        }

        Sort.Direction tipoOrdinamento = Sort.Direction.DESC;
        Pageable paging =  PageRequest.of(numPagina, dimPagina, Sort.by(tipoOrdinamento, ordinamento));
        Optional<Utente> opt = utenteRepository.findById(idUtente);

        if(opt.isEmpty() || !utentePresenteNelDb(opt.get())){
            throw new UtenteNonValidoONonEsistente();
        }

        Utente u = opt.get();
        Carrello c = u.getCarrello();

        //il carrello deve essere attivo
        if(c == null || !carrelloRepository.existsByIdAndAttivo(c.getId(), 1))
            throw new CarrelloNonValidoException();

        return dettaglioCarrelloRepository.findByCarrello_Id(u.getCarrello().getId(), paging);

    }

    private boolean utentePresenteNelDb(Utente utente){
        return utenteRepository.existsByNomeIgnoreCaseAndCognomeIgnoreCaseAndEmailIgnoreCase(utente.getNome(), utente.getCognome(), utente.getEmail());
    }

    @Transactional(readOnly = false, rollbackFor = {TentativoNonAutorizzato.class, CarrelloNonValidoException.class, VideogiocoNonValidoException.class, QuantitaVideogiocoNonDisponibile.class})
    public Carrello aggiungiVideogioco(int idUtente, int idVideogioco, int quantita) throws TentativoNonAutorizzato, CarrelloNonValidoException, VideogiocoNonValidoException, QuantitaVideogiocoNonDisponibile {

        int idUt = Utils.getIdUtente();
        if (idUtente != idUt) {
            throw new TentativoNonAutorizzato();
        }

        Optional<Utente> utente = utenteRepository.findById(idUtente);
        Carrello carr = carrelloRepository.findActiveCarrelloByUtenteId(idUtente, 1);
        Optional<Videogioco> videog = videogiocoRepository.findById(idVideogioco);

        if (carr == null) {
            throw new CarrelloNonValidoException();
        }

        /*
         Ridonandante perchè findActiveCarrelloByUtenteId(idUtente, 1) mi ha già restituito un carrello valido
         e legato all’utente.

        if (utente.isPresent()) {
            if (utente.get().getCarrello().getId() != carr.getId()) {
                throw new TentativoNonAutorizzato();
            }
        }
        */

        if (videog.isEmpty()) {
            throw new VideogiocoNonValidoException();
        }

        Videogioco videogioco = videog.get();
        if (quantita < 0 || videogioco.getQuantita() < quantita)
            throw new QuantitaVideogiocoNonDisponibile();

        //Devo gestire il caso in cui un videogioco diventa nascosto dopo che l'utente l'ha messp nel carrello:
        //può capitare che un utenta metta un videogioco nel carrello e rimanga lì per diverso tempo, ma con il videogioco stesso
        //che viene esaurito nel frattempo
        if (quantita != 0 && videogiocoRepository.existsByNomeIgnoreCaseAndNascosto(videogioco.getNome(), 1)) {
            throw new VideogiocoNonValidoException();
        }

        //Se nel carrello attivo del cliente c'è già il videogioco passato aggiorniamo la quantità, in modo tale da usarlo sia
        //per aggiunte che rimozioni di elementi
        //In caso di rimozione elemento (quantita = 0) faccio:
        if (dettaglioCarrelloRepository.existsByCarrello_IdAndVideogioco_Id(carr.getId(), idVideogioco)) {
            DettaglioCarrello dettaglioCarrello = dettaglioCarrelloRepository.findByCarrello_IdAndVideogioco_Id(carr.getId(), idVideogioco);
            if (quantita == 0) {//Se il prodotto è già presente e passo quantità uguale a zero lo rimuovo
                carr.getListaDettagliCarrello().remove(dettaglioCarrello);
                dettaglioCarrelloRepository.delete(dettaglioCarrello);

            } else {
                dettaglioCarrello.setQuantita(quantita);
            }
        } else {//Se il videogioco non era già nel carrello aggiungo solo nel caso in cui il videogioco è disponibile in quantità maggiore di 0
            if (quantita != 0) {
                DettaglioCarrello dettaglioCarrello = new DettaglioCarrello();
                dettaglioCarrello.setCarrello(carr);
                dettaglioCarrello.setVideogioco(videogioco);
                dettaglioCarrello.setQuantita(quantita);
                dettaglioCarrello.setPrezzoUnitario(videogioco.getPrezzo());
                dettaglioCarrelloRepository.save(dettaglioCarrello);
                carr.getListaDettagliCarrello().add(dettaglioCarrello);
            }
        }
        return carr;

    }

    @Transactional(readOnly = false, rollbackFor = {QuantitaVideogiocoNonDisponibile.class, OrdineNonValido.class, UtenteNonValidoONonEsistente.class, VideogiocoNonValidoException.class, CarrelloNonValidoException.class, TentativoNonAutorizzato.class})
    public Ordine acquista(CarrelloDto carrelloDto) throws QuantitaVideogiocoNonDisponibile, OrdineNonValido,
            UtenteNonValidoONonEsistente, VideogiocoNonValidoException, CarrelloNonValidoException, TentativoNonAutorizzato {

        int idUt = Utils.getIdUtente();
        //Ricordiamo che l'utente passatomi nel DTO è != da quello attuale che vedo dal token
        if (idUt != carrelloDto.idUtente()) {
            throw new TentativoNonAutorizzato();
        }

        //Verifica utente non esistente nel DB
        Optional<Utente> u = utenteRepository.findById(idUt);
        if (u.isEmpty()) {
            throw new UtenteNonValidoONonEsistente();
        }

        Utente utente = u.get();

        //Prendo carrello nel DB per verificare se i videogiochi che ho nei due carrelli (lato FE e lato be) sono uguali o meno, altrimenti
        //c'è il problema introdotto a lezione (moglie collana, marito canna da pesca)

        //Se il backend non controllasse che il carrello effettivo (BE) e quello ricevuto dal FE coincidono, rischieremmo che l’ordine
        //non corrisponde al vero carrello.

        Carrello carrelloBE = carrelloRepository.findActiveCarrelloByUtenteId(idUt, 1);
        if (carrelloBE == null) {
            throw new CarrelloNonValidoException();
        }
        //Se nel DB il carrello è vuoto non ha senso andare avanti, l'ordine non è valido
        if (carrelloBE.getListaDettagliCarrello().isEmpty()) {
            throw new OrdineNonValido();
        }

        //Creiamo l'ordine da salvare
        Ordine ordine = new Ordine();
        ordine.setUtente(utente);
        ordine.setListaDettagliOrdine(new LinkedList<>());//li aggiungo uno per volta in seguito
        ordine.setDataOrdine(new Date());
        double totale = 0.0;
        for (DettaglioDto dettaglioFE : carrelloDto.listaDettaglioCarrello()) {

            int idVideogioco = dettaglioFE.idVideogioco();
            if (idVideogioco < 0) {
                throw new VideogiocoNonValidoException();
            }

            //Cerchiamo se esiste il videogioco
            Optional<Videogioco> videog = videogiocoRepository.findById(idVideogioco);
            if (videog.isEmpty()) {
                throw new VideogiocoNonValidoException();
            }

            //Se esiste prendo il videogioco
            Videogioco videogioco = videog.get();

            //Se i dettagliOrdine sono diversi a livello di dimensione sono nel caso in cui la moglie ha messo nel carrello la collana:
            //quindi nel db ho più videogiochi rispetto a quelli che il marito mi sta passando dal FE, ovvero che vede lui

            if (carrelloBE.getListaDettagliCarrello().size() != carrelloDto.listaDettaglioCarrello().size()) {
                throw new OrdineNonValido();
            }

            //Ma questo controllo non basta: i carrelli potrebbero avere stessa dimensione ma di un videogioco potrei avere quantità diverse
            //oppure potrei avere stessa dimensione ma i videogiochi nei due carrelli potrebbero non corrispondere. Quindi, controllo:

            //Se nel carrello lato BE non esiste un DettaglioOrdine che ha lo stesso Videogioco con stessa quantità di quello che sto considerando
            //ovviamente non va bene

            if (!dettaglioCarrelloRepository.existsByCarrello_IdAndVideogioco_IdAndQuantitaAndPrezzoUnitario(carrelloBE.getId(),
                    videogioco.getId(), dettaglioFE.quantita(), dettaglioFE.prezzoUnitario())) {
                throw new OrdineNonValido();
            }
            DettaglioCarrello dettaglioBE = dettaglioCarrelloRepository.findByCarrello_IdAndVideogioco_Id(carrelloBE.getId(), videogioco.getId());

            //Faccio il controllo sulle quantità (lo faccio con il dettaglio lato BE, ma poco cambia dato che ho fatto il check prima per vedere
            //se i due corrispondevano)
            if (dettaglioBE.getQuantita() <= 0 || dettaglioBE.getQuantita() > videogioco.getQuantita()) {
                throw new QuantitaVideogiocoNonDisponibile();
            }

            //Controllo prezzo nel DettaglioCarrello diverso da quello vero del Videogioco
            if (!Objects.equals(dettaglioBE.getPrezzoUnitario(), videogioco.getPrezzo())) {
                throw new VideogiocoNonValidoException();
            }

            DettaglioOrdine dettaglioOrdine = new DettaglioOrdine();
            dettaglioOrdine.setVideogioco(videogioco);
            dettaglioOrdine.setQuantita(dettaglioBE.getQuantita());
            dettaglioOrdine.setPrezzoUnitario(dettaglioBE.getPrezzoUnitario());
            ordine.getListaDettagliOrdine().add(dettaglioOrdine);
            totale += dettaglioBE.getQuantita() * dettaglioBE.getPrezzoUnitario();

            //Devo rimuovere dal Carrello gli elementi inseriti nell'Ordine
            //Non chiamo il metodo realizzato sopra in questo Service (aggiungiProdotto) con quantità = 0
            //perchè potrei avere ConcurrentModificationException
            dettaglioCarrelloRepository.delete(dettaglioBE);
        }
        ordine.setTotale(totale);
        return ordineService.salvaOrdine(ordine);

    }

}
