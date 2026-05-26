package com.example.monappweb.controller;

import com.example.monappweb.dto.PlatRequest;
import com.example.monappweb.dto.PlatResponse;
import com.example.monappweb.service.PlatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plats")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PlatController {

    private final PlatService platService;

    // PUBLIC — menu QR code
    @GetMapping("/menu")
    public ResponseEntity<List<PlatResponse>> menu() {
        return ResponseEntity.ok(platService.listerMenu());
    }

    // MANAGER — tous les plats
    @GetMapping
    public ResponseEntity<List<PlatResponse>> listerTous() {
        return ResponseEntity.ok(platService.listerTous());
    }

    // MANAGER — créer
    @PostMapping
    public ResponseEntity<PlatResponse> creer(@Valid @RequestBody PlatRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(platService.creer(request));
    }

    // MANAGER — modifier
    @PutMapping("/{id}")
    public ResponseEntity<PlatResponse> modifier(@PathVariable Long id,
                                                 @Valid @RequestBody PlatRequest request) {
        return ResponseEntity.ok(platService.modifier(id, request));
    }

    // MANAGER + CUISINIERE — toggle disponible
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<PlatResponse> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(platService.toggleDisponible(id));
    }

    // CUISINIERE — déclarer perte
    @PatchMapping("/{id}/perte/{quantite}")
    public ResponseEntity<PlatResponse> perte(@PathVariable Long id,
                                              @PathVariable Integer quantite) {
        return ResponseEntity.ok(platService.declarerPerte(id, quantite));
    }

    // MANAGER — supprimer
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        platService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}