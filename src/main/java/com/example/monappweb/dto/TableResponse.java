package com.example.monappweb.dto;

import com.example.monappweb.entity.StatutTable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TableResponse {

    private Long id;
    private Integer numeroTable;
    private StatutTable statut;
    private String qrCodeUrl;
    private Boolean appelServeurActif;
    private LocalDateTime heureAppel;
}