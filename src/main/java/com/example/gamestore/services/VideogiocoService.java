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
    List<Videogioco> trovaVideogiocoByNome(String nomeVideogioco){

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

       if(videogioco.getNome() != null && videogiocoRepository.existsByNomeIgnoreCaseAndNascosto(videogioco.getNome(), 0))
           throw new VideogiocoGiaEsistenteException();

       if(videogioco.getPrezzo() <= 0)
           throw new FasciaPrezzoNonValida();

       if(videogioco.getQuantita() <= 0)
           throw new VideogiocoNonValidoException();

       videogioco.setNascosto(0); //rendo visibile il videogioco, cosa garantita da controlli precedenti

       videogiocoRepository.save(videogioco);

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
