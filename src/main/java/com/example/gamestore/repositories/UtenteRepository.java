package com.example.gamestore.repositories;

import com.example.gamestore.entities.Utente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UtenteRepository extends JpaRepository<Utente, Integer> {

    Utente findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    

}
