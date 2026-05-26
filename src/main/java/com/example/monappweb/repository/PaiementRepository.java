package com.example.monappweb.repository;

import com.example.monappweb.entity.Paiement;
import com.example.monappweb.entity.ServicePeriode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    Optional<Paiement> findByCommandeId(Long commandeId);

    // Paiements d'un caissier
    List<Paiement> findByCaissierId(Long caissierId);

    // Total encaissé sur une période (rapport)
    @Query("SELECT SUM(p.montantTotal) FROM Paiement p WHERE p.datePaiement BETWEEN :debut AND :fin")
    BigDecimal sumMontantBetween(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    // Total pourboires sur une période
    @Query("SELECT SUM(p.pourboire) FROM Paiement p WHERE p.datePaiement BETWEEN :debut AND :fin")
    BigDecimal sumPourboiseBetween(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    // Paiements pour rapport de service
    List<Paiement> findByDatePaiementBetween(LocalDateTime debut, LocalDateTime fin);
}