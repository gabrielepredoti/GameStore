package com.example.gamestore.entities;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Entity
@Table(name = "ordine")
public class Ordine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ordine", nullable = false)
    @ToString.Exclude
    private int id;

    @Basic
    @CreationTimestamp
    @Column(name = "data_ordine", nullable = false)
    private Date dataOrdine;

    @Basic
    @Column(name = "totale")
    private Double totale;

    @ManyToOne
    @JoinColumn(name = "id_utente")
    private Utente utente;

    @OneToMany(mappedBy = "ordine", cascade = CascadeType.MERGE)
    List<DettaglioOrdine> listaDettagliOrdine;



}
