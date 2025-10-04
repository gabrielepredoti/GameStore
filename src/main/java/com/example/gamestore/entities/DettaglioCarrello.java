package com.example.gamestore.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

/*
Appartiene al Carrello di un utente
È temporaneo, cioè finché l’utente non conferma l’acquisto, può cambiare quantità, aggiungere o rimuovere videogiochi.
Contiene le scelte in corso di un utente.
 */

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Entity
@Table(name = "dettaglio_carrello", uniqueConstraints = {
       @UniqueConstraint(columnNames = {"id_carrello", "id_videogioco"})
       //evitiamo che lo stesso videogioco venga inserito due volte nello stesso carrello
})
public class DettaglioCarrello {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dettaglio_carrello", nullable = false)
    private int id;

    @ManyToOne
    @JoinColumn(name = "id_carrello")
    @ToString.Exclude
    @JsonIgnore
    private Carrello carrello;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "id_videogioco")
    private Videogioco videogioco;

    @Basic
    @Column(name = "quantita", nullable = false)
    private int quantita;

    @Basic
    @Column(name = "prezzo_unitario")
    private Double prezzoUnitario;


}
