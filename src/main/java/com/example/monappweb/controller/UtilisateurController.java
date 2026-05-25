package com.example.monappweb.controller;

import com.example.monappweb.dto.UtilisateurRequest;
import com.example.monappweb.dto.UtilisateurResponse;
import com.example.monappweb.service.UtilisateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    // Créer un compte — réservé au RESPONSABLE_PERSONNEL
    @PostMapping
    @PreAuthorize("hasRole('RESPONSABLE_PERSONNEL')")
    public ResponseEntity<UtilisateurResponse> creer(@Valid @RequestBody UtilisateurRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(utilisateurService.creerUtilisateur(request));
    }

    // Lister tout le staff
    @GetMapping
    @PreAuthorize("hasRole('RESPONSABLE_PERSONNEL')")
    public ResponseEntity<List<UtilisateurResponse>> lister() {
        return ResponseEntity.ok(utilisateurService.listerUtilisateurs());
    }

    // Activer / Désactiver un compte
    @PatchMapping("/{id}/desactiver")
    @PreAuthorize("hasRole('RESPONSABLE_PERSONNEL')")
    public ResponseEntity<UtilisateurResponse> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.toggleActif(id));
    }
}