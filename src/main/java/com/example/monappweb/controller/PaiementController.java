package com.example.monappweb.controller;

import com.example.monappweb.dto.PaiementRequest;
import com.example.monappweb.dto.PaiementResponse;
import com.example.monappweb.service.PaiementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/paiements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaiementController {

    private final PaiementService paiementService;

    // ─────────────────────────────────────────────
    // POST /api/paiements
    // Encaisser une commande — Réservé : CAISSIER
    // ─────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('CAISSIER') or hasRole('MANAGER')")
    public ResponseEntity<PaiementResponse> encaisser(@Valid @RequestBody PaiementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paiementService.encaisser(request));
    }

    // ─────────────────────────────────────────────
    // GET /api/paiements/commande/{commandeId}
    // ─────────────────────────────────────────────
    @GetMapping("/commande/{commandeId}")
    @PreAuthorize("hasRole('CAISSIER') or hasRole('MANAGER')")
    public ResponseEntity<PaiementResponse> getPaiementParCommande(@PathVariable Long commandeId) {
        return ResponseEntity.ok(paiementService.getPaiementParCommande(commandeId));
    }

    // ─────────────────────────────────────────────
    // GET /api/paiements/aujourdhui
    // Liste des paiements du jour — Réservé : MANAGER, CAISSIER
    // ─────────────────────────────────────────────
    @GetMapping("/aujourdhui")
    @PreAuthorize("hasRole('CAISSIER') or hasRole('MANAGER')")
    public ResponseEntity<List<PaiementResponse>> getPaiementsDuJour() {
        return ResponseEntity.ok(paiementService.getPaiementsDuJour());
    }

    // ─────────────────────────────────────────────
    // GET /api/paiements/stats
    // Statistiques du jour (CA + pourboires) — Réservé : MANAGER
    // ─────────────────────────────────────────────
    @GetMapping("/stats")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Map<String, BigDecimal>> getStatsDuJour() {
        return ResponseEntity.ok(Map.of(
                "totalEncaisse", paiementService.getTotalEncaisseAujourdhui(),
                "totalPourboires", paiementService.getTotalPourboiresAujourdhui()
        ));
    }
}