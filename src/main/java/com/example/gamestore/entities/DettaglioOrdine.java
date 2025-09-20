package com.example.gamestore.entities;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;


/*
Appartiene a un Ordine già confermato.
È storico e immutabile, cioè fotografa cosa è stato acquistato in quel momento, a quel prezzo e in quella quantità.
 */

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Entity
@Table(name = "dettaglio_ordine", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_ordine", "id_videogioco"})
        //Ragionamento analogo a DettaglioCarrello:
        //un videogioco può comparire una sola volta in un ordine
})
public class DettaglioOrdine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dettaglio_ordine", nullable = false)
    @ToString.Exclude
    private int id;

    @Basic
    @Column(name = "quantita")
    private int quantita;

    @Basic
    @Column(name = "prezzo_unitario")
    private float prezzoUnitario;

    @ManyToOne
    @JoinColumn(name = "id_ordine")
    @JsonIgnore //mi interessa vedere i dettagli nell'ordine, mica vedere per ogni dettaglio l'ordine di riferimento, in questo modo evitiamo anche i cicli
    @ToString.Exclude
    private Ordine ordine;

    @ManyToOne
    @JoinColumn(name = "id_videogioco")
    private Videogioco videogioco;

}
