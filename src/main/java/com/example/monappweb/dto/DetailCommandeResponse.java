package com.example.monappweb.dto;

import com.example.monappweb.entity.DetailCommande;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DetailCommandeResponse {

    private Long id;
    private Long platId;
    private String platNom;
    private BigDecimal platPrix;
    private Integer quantite;
    private String noteClient;
    private BigDecimal sousTotal;

    public static DetailCommandeResponse fromEntity(DetailCommande d) {
        DetailCommandeResponse r = new DetailCommandeResponse();
        r.setId(d.getId());
        r.setPlatId(d.getPlat().getId());
        r.setPlatNom(d.getPlat().getNom());
        r.setPlatPrix(d.getPlat().getPrix());
        r.setQuantite(d.getQuantite());
        r.setNoteClient(d.getNoteClient());
        r.setSousTotal(d.getPlat().getPrix().multiply(BigDecimal.valueOf(d.getQuantite())));
        return r;
    }
}