package com.example.gamestore.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "utente")
public class Utente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_utente", nullable = false)
    private int id;

    @Basic
    @Column(name = "nome", length = 50)
    private String nome;

    @Basic
    @Column(name = "cognome",  length = 50)
    private String cognome;

    @Basic
    @Column(name = "email", unique = true, length = 90)
    private String email;

//    @Basic
//    @Column(name = "numero_di_telefono",  length = 20)
//    private String numeroTelefono;

//    @Basic
//    @Column(name = "indirizzo",  length = 150)
//    private String indirizzo;


    @OneToOne(mappedBy = "utente",cascade = CascadeType.MERGE)
    @JsonIgnore
    @ToString.Exclude
    private Carrello carrello;

    @OneToMany(mappedBy = "utente", cascade = CascadeType.MERGE)
    @JsonIgnore //non è di mio interesse vedere ogni volta che prendo un utente i suoi ordini, posso benissimo fare una query
    @ToString.Exclude
    private List<Ordine> ordini;





}

