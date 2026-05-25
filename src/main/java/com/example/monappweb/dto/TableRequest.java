package com.example.monappweb.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TableRequest {

    @NotNull(message = "Le numéro de table est obligatoire")
    @Positive(message = "Le numéro de table doit être positif")
    private Integer numeroTable;
}