package com.example.monappweb.dto;

import com.example.monappweb.entity.ModePaiement;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaiementRequest {

    @NotNull(message = "L'identifiant de la commande est obligatoire")
    private Long commandeId;

    @NotNull(message = "Le mode de paiement est obligatoire")
    private ModePaiement modePaiement;

    @NotNull(message = "Le montant total est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le montant doit être positif")
    private BigDecimal montantTotal;

    // Pourboire optionnel (défaut 0)
    private BigDecimal pourboire;

    // Id du caissier (renseigné par le backend via le token JWT)
    private Long caissierId;
}