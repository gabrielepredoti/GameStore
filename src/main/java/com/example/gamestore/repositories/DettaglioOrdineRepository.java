package com.example.gamestore.repositories;


import com.example.gamestore.entities.DettaglioOrdine;
import com.example.gamestore.entities.Ordine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DettaglioOrdineRepository extends JpaRepository<DettaglioOrdine, Integer> {

    Page<DettaglioOrdine> findByOrdine_Id(int idOrdine, Pageable pageable);

}
