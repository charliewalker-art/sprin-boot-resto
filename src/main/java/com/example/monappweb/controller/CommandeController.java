package com.example.monappweb.controller;

import com.example.monappweb.dto.*;
import com.example.monappweb.entity.StatutCommande;
import com.example.monappweb.service.CommandeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/commandes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CommandeController {

    private final CommandeService commandeService;

    // ─────────────────────────────────────────────
    // POST /api/commandes
    // ─────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<CommandeResponse> creerCommande(@Valid @RequestBody CommandeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commandeService.creerCommande(request));
    }

    // ─────────────────────────────────────────────
    // GET /api/commandes
    // ?statut=EN_ATTENTE_CUISINE  → filtrées par statut
    // (sans paramètre)            → toutes les commandes
    // ─────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<CommandeResponse>> getCommandes(
            @RequestParam(required = false) StatutCommande statut) {
        if (statut != null) {
            return ResponseEntity.ok(commandeService.getCommandesParStatut(statut));
        }
        return ResponseEntity.ok(commandeService.getToutesLesCommandes());
    }

    // ─────────────────────────────────────────────
    // GET /api/commandes/{id}
    // ─────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<CommandeResponse> getCommande(@PathVariable Long id) {
        return ResponseEntity.ok(commandeService.getCommande(id));
    }

    // ─────────────────────────────────────────────
    // PATCH /api/commandes/{id}/valider
    // CREEE → EN_ATTENTE_CUISINE
    // ─────────────────────────────────────────────
    @PatchMapping("/{id}/valider")
    public ResponseEntity<CommandeResponse> validerCommande(@PathVariable Long id) {
        return ResponseEntity.ok(commandeService.validerCommande(id));
    }

    // ─────────────────────────────────────────────
    // PATCH /api/commandes/{id}/commencer
    // EN_ATTENTE_CUISINE → EN_PREPARATION
    // ─────────────────────────────────────────────
    @PatchMapping("/{id}/commencer")
    @PreAuthorize("hasRole('CUISINIERE') or hasRole('MANAGER')")
    public ResponseEntity<CommandeResponse> commencerPreparation(@PathVariable Long id) {
        return ResponseEntity.ok(commandeService.commencerPreparation(id));
    }

    // ─────────────────────────────────────────────
    // PATCH /api/commandes/{id}/prete
    // EN_PREPARATION → PRETE
    // ─────────────────────────────────────────────
    @PatchMapping("/{id}/prete")
    @PreAuthorize("hasRole('CUISINIERE') or hasRole('MANAGER')")
    public ResponseEntity<CommandeResponse> marquerPrete(@PathVariable Long id) {
        return ResponseEntity.ok(commandeService.marquerPrete(id));
    }

    // ─────────────────────────────────────────────
    // PATCH /api/commandes/{id}/servie
    // PRETE → SERVIE
    // ─────────────────────────────────────────────
    @PatchMapping("/{id}/servie")
    @PreAuthorize("hasRole('SERVEUR') or hasRole('MANAGER')")
    public ResponseEntity<CommandeResponse> marquerServie(@PathVariable Long id) {
        return ResponseEntity.ok(commandeService.marquerServie(id));
    }

    // ─────────────────────────────────────────────
    // PATCH /api/commandes/{id}/addition
    // SERVIE → EN_ATTENTE_PAIEMENT
    // ─────────────────────────────────────────────
    @PatchMapping("/{id}/addition")
    public ResponseEntity<CommandeResponse> demanderAddition(@PathVariable Long id) {
        return ResponseEntity.ok(commandeService.demanderAddition(id));
    }

    // ─────────────────────────────────────────────
    // PATCH /api/commandes/{id}/annuler?annuleParId=X
    // ─────────────────────────────────────────────
    @PatchMapping("/{id}/annuler")
    public ResponseEntity<CommandeResponse> annulerCommande(
            @PathVariable Long id,
            @RequestParam Long annuleParId,
            @Valid @RequestBody AnnulationRequest request) {
        return ResponseEntity.ok(commandeService.annulerCommande(id, annuleParId, request));
    }

    // ─────────────────────────────────────────────
    // PATCH /api/commandes/{id}/evaluer
    // ─────────────────────────────────────────────
    @PatchMapping("/{id}/evaluer")
    public ResponseEntity<CommandeResponse> evaluerCommande(
            @PathVariable Long id,
            @Valid @RequestBody EvaluationRequest request) {
        return ResponseEntity.ok(commandeService.evaluerCommande(id, request));
    }

    // ─────────────────────────────────────────────
    // GET /api/commandes/retard?seuilMinutes=15
    // ─────────────────────────────────────────────
    @GetMapping("/retard")
    @PreAuthorize("hasRole('CUISINIERE') or hasRole('MANAGER')")
    public ResponseEntity<List<CommandeResponse>> getCommandesEnRetard(
            @RequestParam(defaultValue = "15") int seuilMinutes) {
        return ResponseEntity.ok(commandeService.getCommandesEnRetard(seuilMinutes));
    }

    // ─────────────────────────────────────────────
    // GET /api/commandes/annulations
    // ─────────────────────────────────────────────
    @GetMapping("/annulations")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<CommandeResponse>> getJournalAnnulations() {
        return ResponseEntity.ok(commandeService.getJournalAnnulations());
    }
}