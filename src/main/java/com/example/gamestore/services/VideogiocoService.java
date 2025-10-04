package com.example.gamestore.services;

import com.example.gamestore.entities.Videogioco;
import com.example.gamestore.repositories.VideogiocoRepository;
import com.example.gamestore.support.exceptions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class VideogiocoService {

   @Autowired
    private VideogiocoRepository videogiocoRepository;

   @Transactional(readOnly = true)
    public Page<Videogioco> elencoVideogiochi(int numPagina, int dimPagina, String ordinamento){

       Sort.Direction tipoOrdinamento = Sort.Direction.DESC;
       Pageable paging = PageRequest.of(numPagina, dimPagina, Sort.by(tipoOrdinamento, ordinamento));
       return videogiocoRepository.findByQuantitaGreaterThanAndNascosto(paging, 0, 0);

   }

   @Transactional(readOnly = true)
    public Page<Videogioco> elencoVideogiochiPerPiattaforma(String piattaforma, int numPagina, int dimPagina, String ordinamento){

       if(piattaforma == null){

           //Se non mi viene passata la specifica piattaforma del videogioco, mostro tutti i videogiochi ordinati in base alla piattaforma
           //Non vado a mettere un valore di default nel requestparam perchè non so per quale piattaforma l'utente voglia vedere i videogiochi
           //di default

           return this.elencoVideogiochi(numPagina, dimPagina, "piattaforma");

       }

       Sort.Direction tipoOrdinamento = Sort.Direction.DESC;
       Pageable paging = PageRequest.of(numPagina, dimPagina, Sort.by(tipoOrdinamento, ordinamento));
       return videogiocoRepository.findByPiattaformaContainingIgnoreCaseAndNascosto(piattaforma, paging, 0);

   }

   @Transactional(readOnly = true)
    public List<Videogioco> trovaVideogiocoByNome(String nomeVideogioco){

       return videogiocoRepository.findByNomeIgnoreCaseAndNascosto(nomeVideogioco, 0);

   }

   @Transactional(readOnly = true)
    public List<Videogioco> ricercaApprofondita(int numPagina, int dimPagina, double prezzoMin, double prezzoMax, String nome, String piattaforma, int quantita, String ordinamento) throws FasciaPrezzoNonValida{

       if(prezzoMin <= 0 || prezzoMax <= 0 || prezzoMin > prezzoMax){

           throw new FasciaPrezzoNonValida();

       }

       Sort.Direction tipoOrdinamento = Sort.Direction.DESC;
       Pageable paging = PageRequest.of(numPagina, dimPagina, Sort.by(tipoOrdinamento, ordinamento));
       return  videogiocoRepository.ricercaApprofondita(prezzoMin, prezzoMax, nome, piattaforma, quantita);

   }

   @Transactional(readOnly = false, rollbackFor = {VideogiocoGiaEsistenteException.class, FasciaPrezzoNonValida.class, VideogiocoNonValidoException.class})
    public void salvaVideogioco(Videogioco videogioco) throws VideogiocoGiaEsistenteException, FasciaPrezzoNonValida, VideogiocoNonValidoException {


       // 1. Cerca un videogioco esistente con lo stesso nome e piattaforma, Nascosto (1) o Visibile (0)
       Optional<Videogioco> existingHidden = videogiocoRepository.findByNomeIgnoreCaseAndPiattaformaIgnoreCase(videogioco.getNome(), videogioco.getPiattaforma());

       if (existingHidden.isPresent()) {
           Videogioco daAggiornare = existingHidden.get();

           // SCENARIO 1: Il gioco ESISTE ed è GIA VISIBILE (nascosto=0)
           if (daAggiornare.getNascosto() == 0) {
               throw new VideogiocoGiaEsistenteException(); // Errore: stai cercando di creare un duplicato attivo
           }

           // SCENARIO 2: Il gioco ESISTE ma è NASCOSTO (nascosto=1) -> RIATTIVA
           // Copia i nuovi dettagli (prezzo, quantità, descrizione, etc.) nell'oggetto esistente
           daAggiornare.setDescrizione(videogioco.getDescrizione());
           daAggiornare.setPrezzo(videogioco.getPrezzo());
           daAggiornare.setQuantita(videogioco.getQuantita());
           daAggiornare.setAnnoRilascio(videogioco.getAnnoRilascio());
           daAggiornare.setCasaProduttrice(videogioco.getCasaProduttrice());

           daAggiornare.setNascosto(0); // Rendo visibile
           videogiocoRepository.save(daAggiornare);
           return; // Operazione completata
       }


       // 2. Se NON esiste, procedi con i controlli e la creazione di un NUOVO record
       if(videogioco.getPrezzo() <= 0)
           throw new FasciaPrezzoNonValida();

       if(videogioco.getQuantita() <= 0)
           throw new VideogiocoNonValidoException();

       videogioco.setNascosto(0);
       videogiocoRepository.save(videogioco);



/*  VERSIONE SBAGLIATA:
    Così non tengo conto del fatto che se rimuovo un videogioco (setNascosto a 1) per riattivarlo (setNascosto a 0) NON POSSO CREARE UN NUOVO VIDEOGIOCO CON STESSO NOME E STESSA PIATTAFORMA
    CIò CONTRASTA I VINCOLI DI UNIQUE SU VIDEOGIOCO (nome,piattaforma)

    //ESEMPIO: ho GTA 5 per PS4. Lo nascondo tramite rimuoviVideogioco. Bene, ora voglio renderlo visibile. Come faccio?
    //         Richiamo salvaVideogioco con GTA 5 nel body e tutto il resto. Tuttavia con salvaVideogioco con l'implementazione qui sotto
    //         creo un nuovo record con GTA 5 / PS4, cosa vietata da me per come ho impostato i vincoli di unique su entità Videogioco

       if(videogioco.getNome() != null && videogiocoRepository.existsByNomeIgnoreCaseAndNascosto(videogioco.getNome(), 0))
           throw new VideogiocoGiaEsistenteException();

       if(videogioco.getPrezzo() <= 0)
           throw new FasciaPrezzoNonValida();

       if(videogioco.getQuantita() <= 0)
           throw new VideogiocoNonValidoException();

       videogioco.setNascosto(0); //rendo visibile il videogioco, cosa garantita da controlli precedenti

       videogiocoRepository.save(videogioco);

*/

   }

   @Transactional(readOnly = false, rollbackFor = {VideogiocoGiaEliminatoException.class, VideogiocoNonPresenteNelDBException.class})
    public void rimuoviVideogioco(int idVideogioco) throws VideogiocoGiaEliminatoException, VideogiocoNonPresenteNelDBException {

       Optional<Videogioco> videogioco = videogiocoRepository.findById(idVideogioco);
       if(videogioco.isPresent()) {

           Videogioco daEliminare = videogioco.get();
           if (daEliminare.getNascosto() == 1)
               throw new VideogiocoGiaEliminatoException();

           daEliminare.setNascosto(1);
           // Non procedo con videogiocoRepository.delete(daEliminare); perchè abbiamo definito l'attributo nascosto
           // per gestire prodotti che non mostriamo agli utenti (ad esempio titoli ritirati o non più
           // disponibili), senza doverli eliminare dal DB.

           //Rendo persistente la modifica del parametro nascosto
           videogiocoRepository.save(daEliminare);

       }else{

           throw new VideogiocoNonPresenteNelDBException();
       }


   }


}
