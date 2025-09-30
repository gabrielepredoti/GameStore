package com.example.gamestore.dto;

import com.example.gamestore.entities.DettaglioCarrello;

import java.util.List;

public record CarrelloDto(List<DettaglioDto> listaDettaglioCarrello, int idUtente) {
}
