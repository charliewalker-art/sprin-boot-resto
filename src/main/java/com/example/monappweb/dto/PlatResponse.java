package com.example.monappweb.dto;

import com.example.monappweb.entity.Categorie;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class PlatResponse {

    private Long id;
    private String nom;
    private String description;
    private BigDecimal prix;
    private Categorie categorie;
    private Boolean disponible;
    private String allergenes;
    private Integer quantitePerdueJour;
    private String imageUrl;
}