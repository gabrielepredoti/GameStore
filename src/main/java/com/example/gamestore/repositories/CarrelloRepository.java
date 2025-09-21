package com.example.gamestore.repositories;

import com.example.gamestore.entities.Carrello;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CarrelloRepository extends JpaRepository<Carrello, Integer> {

    boolean existsByIdAndAttivo(int id, int attivo);

    @Query(
            "SELECT c " +
            "FROM Carrello c "+
            "WHERE c.utente.id = ?1 " +
            "AND c.attivo = ?2"
    )
    Carrello findActiveCarrelloByUtenteId(int idUtente, int attivo);

}
