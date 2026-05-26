package com.example.monappweb.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "commandes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeCommande typeCommande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCommande statut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServicePeriode servicePeriode;

    // Lien vers la table (nullable si À Emporter)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = true)
    private TableRestaurant table;

    // Serveur qui a pris la commande (nullable si QR code)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "serveur_id", nullable = true)
    private Utilisateur serveur;

    // Pour les commandes à emporter
    @Column(nullable = true)
    private String nomClientRetrait;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    // Temps d'attente estimé en minutes
    @Column(nullable = true)
    private Integer tempsAttenteEstime;

    // Lignes de commande
    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetailCommande> details = new ArrayList<>();

    // --- Bloc Annulation (nullable) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annule_par_id", nullable = true)
    private Utilisateur annulePar;

    @Column(nullable = true)
    private String motifAnnulation;

    @Column(nullable = true)
    private LocalDateTime dateAnnulation;

    // --- Bloc Évaluation (nullable) ---
    @Column(nullable = true)
    private Integer noteSatisfaction; // 1 à 5

    @Column(nullable = true, length = 500)
    private String commentaireClient;

    // --- Lifecycle hook ---
    @PrePersist
    public void prePersist() {
        if (this.dateCreation == null) {
            this.dateCreation = LocalDateTime.now();
        }
        if (this.statut == null) {
            this.statut = StatutCommande.CREEE;
        }
        // Détermine automatiquement le service MIDI ou SOIR
        if (this.servicePeriode == null) {
            int heure = this.dateCreation.getHour();
            this.servicePeriode = (heure >= 6 && heure < 15) ? ServicePeriode.MIDI : ServicePeriode.SOIR;
        }
    }
}