package com.example.monappweb.dto;

import com.example.monappweb.entity.Categorie;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class PlatRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    private String description;

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix doit être positif")
    private BigDecimal prix;

    @NotNull(message = "La catégorie est obligatoire")
    private Categorie categorie;

    private String allergenes;

    private String imageUrl;
}