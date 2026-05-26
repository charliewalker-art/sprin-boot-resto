package com.example.monappweb.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnnulationRequest {

    @NotBlank(message = "Le motif d'annulation est obligatoire")
    private String motifAnnulation;
}