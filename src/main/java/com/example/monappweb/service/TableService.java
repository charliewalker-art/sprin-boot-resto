package com.example.monappweb.service;

import com.example.monappweb.dto.TableRequest;
import com.example.monappweb.dto.TableResponse;
import com.example.monappweb.entity.StatutTable;
import com.example.monappweb.entity.TableRestaurant;
import com.example.monappweb.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TableService {

    private final TableRepository tableRepository;

    // Créer une table
    public TableResponse creerTable(TableRequest request) {
        if (tableRepository.existsByNumeroTable(request.getNumeroTable())) {
            throw new RuntimeException("La table " + request.getNumeroTable() + " existe déjà");
        }

        TableRestaurant table = TableRestaurant.builder()
                .numeroTable(request.getNumeroTable())
                .statut(StatutTable.LIBRE)
                .appelServeurActif(false)
                .qrCodeUrl("/qr/table/" + request.getNumeroTable())
                .build();

        return toResponse(tableRepository.save(table));
    }

    // Lister toutes les tables
    public List<TableResponse> listerTables() {
        return tableRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Lister par statut
    public List<TableResponse> listerParStatut(StatutTable statut) {
        return tableRepository.findByStatut(statut)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Changer le statut d'une table
    public TableResponse changerStatut(Long id, StatutTable statut) {
        TableRestaurant table = tableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table introuvable : " + id));

        table.setStatut(statut);

        // Si la table redevient libre, on remet l'appel serveur à false
        if (statut == StatutTable.LIBRE) {
            table.setAppelServeurActif(false);
            table.setHeureAppel(null);
        }

        return toResponse(tableRepository.save(table));
    }

    // Client appelle le serveur
    public TableResponse appelServeur(Integer numeroTable) {
        TableRestaurant table = tableRepository.findByNumeroTable(numeroTable)
                .orElseThrow(() -> new RuntimeException("Table introuvable : " + numeroTable));

        table.setAppelServeurActif(true);
        table.setHeureAppel(LocalDateTime.now());

        return toResponse(tableRepository.save(table));
    }

    // Serveur acquitte l'appel
    public TableResponse acquitterAppel(Long id) {
        TableRestaurant table = tableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table introuvable : " + id));

        table.setAppelServeurActif(false);
        table.setHeureAppel(null);

        return toResponse(tableRepository.save(table));
    }

    // Supprimer une table
    public void supprimerTable(Long id) {
        if (!tableRepository.existsById(id)) {
            throw new RuntimeException("Table introuvable : " + id);
        }
        tableRepository.deleteById(id);
    }

    // Mapper entité → DTO

    private TableResponse toResponse(TableRestaurant t) {
        return new TableResponse(
                t.getId(),
                t.getNumeroTable(),
                t.getStatut(),
                t.getQrCodeUrl(),
                t.getAppelServeurActif(),
                t.getHeureAppel()
        );
    }
}