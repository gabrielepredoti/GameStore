package com.example.gamestore.repositories;

import com.example.gamestore.entities.Videogioco;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideogiocoRepository extends JpaRepository<Videogioco, Integer> {

    // Cerca i videogiochi per nome (e se visibili), non Page perchè non ci sono tantissimi giochi con lo stesso identico nome
    List<Videogioco> findByNomeIgnoreCaseAndNascosto(String nome, int nascosto);

    // Cerca giochi per piattaforma con paginazione (PS5, Xbox, PC, ecc...)
    Page<Videogioco> findByPiattaformaContainingIgnoreCaseAndNascosto(String piattaforma, Pageable paging, int nascosto);

    /**
     * Trova i videogiochi il cui nome inizia con la stringa specificata.
     * Ad esempio cercando "Fi" restituirà "Final Fantasy", "Fifa", ecc...
     */
    Page<Videogioco> findByNomeStartingWithAndNascosto(String prefisso, Pageable paging,  int nascosto);

    // Controlla se esiste già un videogioco con stesso nome e piattaforma (c'è GTA 6 per PS4? Darà false)
    boolean existsByNomeIgnoreCaseAndPiattaformaIgnoreCase(String nome, String piattaforma);

    /*
    Ricerca avanzata con filtri opzionali (prezzo min/max, nome, piattaforma, quantità disponibile).
    I videogiochi "nascosti" (nascosto=1) vengono esclusi dai risultati.
     */
    @Query(" SELECT v "+
           " FROM Videogioco v "+
           " WHERE (v.prezzo >= ?1 OR ?1 IS NULL) AND "+
           "       (v.prezzo <= ?2 OR ?2 IS NULL) AND "+
           "       (v.nome LIKE ?3 OR ?3 IS NULL) AND "+
           "       (v.piattaforma LIKE ?4 OR ?4 IS NULL) AND"+
           "       (v.quantita >= ?5 OR ?5 IS NULL) AND v.nascosto = 0"
    )
    List<Videogioco> ricercaApprofondita(float prezzoMin, float prezzoMax, String nome, String piattaforma, int quantita);


}
