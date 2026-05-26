package com.example.monappweb.dto;

import com.example.monappweb.entity.TypeCommande;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CommandeRequest {

    @NotNull(message = "Le type de commande est obligatoire")
    private TypeCommande typeCommande;

    // Obligatoire si SUR_PLACE_QR ou SUR_PLACE_SERVEUR
    private Long tableId;

    // Obligatoire si A_EMPORTER
    private String nomClientRetrait;

    // Id du serveur (nullable, renseigné par le backend si connecté)
    private Long serveurId;

    // Temps estimé en minutes (optionnel)
    private Integer tempsAttenteEstime;

    @NotEmpty(message = "La commande doit contenir au moins un plat")
    @Valid
    private List<DetailCommandeRequest> details;
}