package com.example.monappweb.dto;

import com.example.monappweb.entity.Commande;
import com.example.monappweb.entity.ServicePeriode;
import com.example.monappweb.entity.StatutCommande;
import com.example.monappweb.entity.TypeCommande;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class CommandeResponse {

    private Long id;
    private TypeCommande typeCommande;
    private StatutCommande statut;
    private ServicePeriode servicePeriode;

    // Table (nullable)
    private Long tableId;
    private Integer numeroTable;

    // Serveur (nullable)
    private Long serveurId;
    private String serveurNomComplet;

    // À emporter
    private String nomClientRetrait;

    private LocalDateTime dateCreation;
    private Integer tempsAttenteEstime;

    // Lignes
    private List<DetailCommandeResponse> details;
    private BigDecimal montantTotal;

    // Annulation (nullable)
    private String annuleParNomComplet;
    private String motifAnnulation;
    private LocalDateTime dateAnnulation;

    // Évaluation (nullable)
    private Integer noteSatisfaction;
    private String commentaireClient;

    public static CommandeResponse fromEntity(Commande c) {
        CommandeResponse r = new CommandeResponse();
        r.setId(c.getId());
        r.setTypeCommande(c.getTypeCommande());
        r.setStatut(c.getStatut());
        r.setServicePeriode(c.getServicePeriode());

        if (c.getTable() != null) {
            r.setTableId(c.getTable().getId());
            r.setNumeroTable(c.getTable().getNumeroTable());
        }

        if (c.getServeur() != null) {
            r.setServeurId(c.getServeur().getId());
            r.setServeurNomComplet(c.getServeur().getPrenom() + " " + c.getServeur().getNom());
        }

        r.setNomClientRetrait(c.getNomClientRetrait());
        r.setDateCreation(c.getDateCreation());
        r.setTempsAttenteEstime(c.getTempsAttenteEstime());

        if (c.getDetails() != null) {
            List<DetailCommandeResponse> details = c.getDetails()
                    .stream()
                    .map(DetailCommandeResponse::fromEntity)
                    .collect(Collectors.toList());
            r.setDetails(details);

            BigDecimal total = details.stream()
                    .map(DetailCommandeResponse::getSousTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            r.setMontantTotal(total);
        }

        if (c.getAnnulePar() != null) {
            r.setAnnuleParNomComplet(c.getAnnulePar().getPrenom() + " " + c.getAnnulePar().getNom());
            r.setMotifAnnulation(c.getMotifAnnulation());
            r.setDateAnnulation(c.getDateAnnulation());
        }

        r.setNoteSatisfaction(c.getNoteSatisfaction());
        r.setCommentaireClient(c.getCommentaireClient());

        return r;
    }
}