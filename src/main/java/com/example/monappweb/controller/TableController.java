package com.example.monappweb.controller;

import com.example.monappweb.dto.TableRequest;
import com.example.monappweb.dto.TableResponse;
import com.example.monappweb.entity.StatutTable;
import com.example.monappweb.service.TableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TableController {

    private final TableService tableService;

    @PostMapping
    public ResponseEntity<TableResponse> creer(@Valid @RequestBody TableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tableService.creerTable(request));
    }

    @GetMapping
    public ResponseEntity<List<TableResponse>> lister() {
        return ResponseEntity.ok(tableService.listerTables());
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<TableResponse>> listerParStatut(@PathVariable StatutTable statut) {
        return ResponseEntity.ok(tableService.listerParStatut(statut));
    }

    @PatchMapping("/{id}/statut/{statut}")
    public ResponseEntity<TableResponse> changerStatut(@PathVariable Long id,
                                                       @PathVariable StatutTable statut) {
        return ResponseEntity.ok(tableService.changerStatut(id, statut));
    }

    @PatchMapping("/appel/{numeroTable}")
    public ResponseEntity<TableResponse> appelServeur(@PathVariable Integer numeroTable) {
        return ResponseEntity.ok(tableService.appelServeur(numeroTable));
    }

    @PatchMapping("/{id}/acquitter")
    public ResponseEntity<TableResponse> acquitter(@PathVariable Long id) {
        return ResponseEntity.ok(tableService.acquitterAppel(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        tableService.supprimerTable(id);
        return ResponseEntity.noContent().build();
    }
}