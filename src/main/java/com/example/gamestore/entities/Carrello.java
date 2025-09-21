package com.example.gamestore.entities;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Entity
@Table(name = "carrello", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_utente", "attivo"})
        //Un utente ha al massimo un carrello attivo
})
public class Carrello {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "carrello_id", nullable = false)
    private int id;

    @OneToOne
    @JoinColumn(name = "id_utente")
    private Utente utente;

    @OneToMany(mappedBy = "carrello", cascade = CascadeType.MERGE)
    private List<DettaglioCarrello> listaDettagliCarrello;

    @Basic
    @Column(name = "attivo", length = 1)
    private int attivo;


}
