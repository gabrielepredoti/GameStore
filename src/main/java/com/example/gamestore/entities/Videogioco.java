package com.example.gamestore.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Entity
@Table(name = "videogioco", uniqueConstraints =
        {@UniqueConstraint(
                columnNames =
                        {"nome", "piattaforma"})}) //per evitare duplicati dello stesso gioco su una piattaforma
public class Videogioco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_videogioco", nullable = false)
    private int id;

    @Version
    @Column(name = "version", nullable = false)
    @JsonIgnore
    /**
     * TRADUZIONE DA DOCUMENTAZIONE
     * Specifica il campo versione di una classe entity che funge da valore di
     * optimistic lock (blocco ottimistico). La versione viene utilizzata per
     * garantire l'integrità durante le operazioni di merge e per il controllo
     * della concorrenza ottimistica.
     *
     * FUNZIONAMENTO:
     * - Ad ogni modifica del record, la versione viene incrementata automaticamente
     * - Se due transazioni provano a modificare lo stesso record contemporaneamente,
     *   solo la prima avrà successo (basato sul valore della versione)
     * - La seconda transazione fallirà con OptimisticLockException
     */
    private long version;

    @Basic
    @Column(name = "nome")
    private String nome;

    @Basic
    @Column(name = "descrizione", length = 2000) //la descrizione di un videogioco generalmente è più lunga di 255 caratteri (default)
    private String descrizione;

    @Basic
    @Column(name = "piattaforma")
    private String piattaforma;

    @Basic
    @Column(name = "anno_rilascio")
    private int annoRilascio;

    @Basic
    @Column(name = "casa_produttrice")
    private String casaProduttrice;

    @Basic
    @Column(name = "prezzo")
    private float prezzo;

    @Basic
    @Column(name = "quantita", nullable = false)
    private int quantita;


    @OneToMany(mappedBy = "videogioco", cascade = CascadeType.MERGE)
    @JsonIgnore
    @ToString.Exclude
    private List<DettaglioCarrello> listaDettagliCarrello;

    @OneToMany(mappedBy = "videogioco", cascade = CascadeType.MERGE)
    @JsonIgnore
    @ToString.Exclude
    private List<DettaglioOrdine> listaDettagliOrdine;

    /*
    Per gestire prodotti che non mostriamo agli utenti (ad esempio titoli ritirati o non più
    disponibili), senza doverli eliminare dal DB.
    0 = visibile agli utenti
    1 = nascosto (non appare nelle ricerche, ma resta nel DB per eventuale riattivazione
     */
    @Basic
    @Column(name = "nascosto", length = 1)
    private int nascosto;



}
