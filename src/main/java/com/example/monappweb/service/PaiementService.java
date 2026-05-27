package com.example.monappweb.service;

import com.example.monappweb.dto.PaiementRequest;
import com.example.monappweb.dto.PaiementResponse;
import com.example.monappweb.entity.*;
import com.example.monappweb.repository.CommandeRepository;
import com.example.monappweb.repository.PaiementRepository;
import com.example.monappweb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaiementService {

    private final PaiementRepository paiementRepository;
    private final CommandeRepository commandeRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate; // ← WebSocket

    // ─────────────────────────────────────────────
    // NOTIFICATION WebSocket
    // ─────────────────────────────────────────────

    private void notifier(String message) {
        messagingTemplate.convertAndSend("/topic/commandes", message);
    }

    // ─────────────────────────────────────────────
    // ENCAISSEMENT
    // ─────────────────────────────────────────────

    @Transactional
    public PaiementResponse encaisser(PaiementRequest request) {
        Commande commande = commandeRepository.findById(request.getCommandeId())
                .orElseThrow(() -> new RuntimeException("Commande introuvable : " + request.getCommandeId()));

        if (commande.getStatut() != StatutCommande.EN_ATTENTE_PAIEMENT) {
            throw new RuntimeException("La commande n'est pas en attente de paiement.");
        }

        if (paiementRepository.findByCommandeId(commande.getId()).isPresent()) {
            throw new RuntimeException("Cette commande a déjà été encaissée.");
        }

        Utilisateur caissier = userRepository.findById(request.getCaissierId())
                .orElseThrow(() -> new RuntimeException("Caissier introuvable : " + request.getCaissierId()));

        Paiement paiement = Paiement.builder()
                .commande(commande)
                .caissier(caissier)
                .montantTotal(request.getMontantTotal())
                .pourboire(request.getPourboire() != null ? request.getPourboire() : BigDecimal.ZERO)
                .modePaiement(request.getModePaiement())
                .datePaiement(LocalDateTime.now())
                .build();

        paiementRepository.save(paiement);

        // Passer la commande à PAYEE
        commande.setStatut(StatutCommande.PAYEE);
        commandeRepository.save(commande);

        // Si la table existe, la passer en nettoyage automatiquement
        if (commande.getTable() != null) {
            commande.getTable().setStatut(StatutTable.EN_COURS_DE_NETTOYAGE);
        }

        // Notifier tous les clients connectés
        notifier("PAYEE");

        return PaiementResponse.fromEntity(paiement);
    }

    // ─────────────────────────────────────────────
    // LECTURES
    // ─────────────────────────────────────────────

    public PaiementResponse getPaiementParCommande(Long commandeId) {
        Paiement paiement = paiementRepository.findByCommandeId(commandeId)
                .orElseThrow(() -> new RuntimeException("Aucun paiement pour la commande : " + commandeId));
        return PaiementResponse.fromEntity(paiement);
    }

    public List<PaiementResponse> getPaiementsDuJour() {
        LocalDateTime debutJour = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime finJour = debutJour.plusDays(1);
        return paiementRepository.findByDatePaiementBetween(debutJour, finJour)
                .stream()
                .map(PaiementResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public BigDecimal getTotalEncaisseAujourdhui() {
        LocalDateTime debutJour = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime finJour = debutJour.plusDays(1);
        BigDecimal total = paiementRepository.sumMontantBetween(debutJour, finJour);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getTotalPourboiresAujourdhui() {
        LocalDateTime debutJour = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime finJour = debutJour.plusDays(1);
        BigDecimal total = paiementRepository.sumPourboiseBetween(debutJour, finJour);
        return total != null ? total : BigDecimal.ZERO;
    }
}