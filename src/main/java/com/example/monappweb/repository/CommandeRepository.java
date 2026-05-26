package com.example.monappweb.repository;

import com.example.monappweb.entity.Commande;
import com.example.monappweb.entity.ServicePeriode;
import com.example.monappweb.entity.StatutCommande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {

    // Commandes par statut (ex: écran cuisine)
    List<Commande> findByStatutOrderByDateCreationAsc(StatutCommande statut);

    // Commandes par table
    List<Commande> findByTableIdAndStatutNot(Long tableId, StatutCommande statut);

    // Commandes en attente cuisine depuis plus de N minutes (alerte anti-oubli)
    @Query("SELECT c FROM Commande c WHERE c.statut = 'EN_ATTENTE_CUISINE' AND c.dateCreation < :seuil")
    List<Commande> findCommandesEnRetard(@Param("seuil") LocalDateTime seuil);

    // Commandes pour rapport de fin de service
    List<Commande> findByServicePeriodeAndDateCreationBetween(
            ServicePeriode servicePeriode,
            LocalDateTime debut,
            LocalDateTime fin
    );
    List<Commande> findAllByOrderByDateCreationAsc();
    // Commandes annulées (journal)
    List<Commande> findByStatutOrderByDateAnnulationDesc(StatutCommande statut);

    // Commandes en attente de paiement (écran caissier)
    List<Commande> findByStatutOrderByDateCreationAsc(StatutCommande statut, org.springframework.data.domain.Sort sort);
}

