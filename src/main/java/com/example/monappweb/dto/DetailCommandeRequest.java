package com.example.monappweb.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DetailCommandeRequest {

    @NotNull(message = "L'identifiant du plat est obligatoire")
    private Long platId;

    @NotNull(message = "La quantité est obligatoire")
    @Min(value = 1, message = "La quantité doit être au moins 1")
    private Integer quantite;

    // Ex: "Sans oignon", "Bien cuit"
    private String noteClient;
}