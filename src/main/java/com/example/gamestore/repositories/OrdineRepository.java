package com.example.gamestore.repositories;

import com.example.gamestore.entities.Ordine;
import com.example.gamestore.entities.Utente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public interface OrdineRepository extends JpaRepository<Ordine, Integer> {

    Page<Ordine> findByUtente(Utente utente, Pageable pageable);

    @Query( "SELECT o "+
           "FROM Ordine o "+
           "WHERE o.utente = ?1 AND " +
           "o.dataOrdine >= ?2 AND o.dataOrdine <= ?3"
    )
    Page<Ordine> ricercaPerUtenteInPeriodo(Utente u, Date dataI, Date dataF, Pageable paging);

}
