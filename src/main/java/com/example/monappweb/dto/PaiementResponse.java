package com.example.monappweb.dto;

import com.example.monappweb.entity.ModePaiement;
import com.example.monappweb.entity.Paiement;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaiementResponse {

    private Long id;
    private Long commandeId;
    private String caissierNomComplet;
    private BigDecimal montantTotal;
    private BigDecimal pourboire;
    private ModePaiement modePaiement;
    private LocalDateTime datePaiement;

    public static PaiementResponse fromEntity(Paiement p) {
        PaiementResponse r = new PaiementResponse();
        r.setId(p.getId());
        r.setCommandeId(p.getCommande().getId());
        r.setCaissierNomComplet(p.getCaissier().getPrenom() + " " + p.getCaissier().getNom());
        r.setMontantTotal(p.getMontantTotal());
        r.setPourboire(p.getPourboire());
        r.setModePaiement(p.getModePaiement());
        r.setDatePaiement(p.getDatePaiement());
        return r;
    }
}