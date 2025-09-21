package com.example.gamestore.repositories;

import com.example.gamestore.entities.DettaglioCarrello;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DettaglioCarrelloRepository extends JpaRepository<DettaglioCarrello, Integer> {

    Page<DettaglioCarrello> findByCarrello_Id(int idCarrello, Pageable paging);

    boolean existsByCarrello_IdAndVideogioco_Id(int idCarrello, int videogiocoId);

    DettaglioCarrello findByCarrello_IdAndVideogioco_Id(int idCarrello, int videogiocoId);

    boolean existsByCarrello_IdAndVideogioco_IdAndQuantitaAndPrezzoUnitario(int idCarrello, int videogiocoId, int quantita, float prezzo);

}
