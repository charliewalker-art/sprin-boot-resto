package com.example.monappweb.service;

import com.example.monappweb.dto.PlatRequest;
import com.example.monappweb.dto.PlatResponse;
import com.example.monappweb.entity.Plat;
import com.example.monappweb.repository.PlatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlatService {

    private final PlatRepository platRepository;
    private final UploadService uploadService;

    // Tous les plats (MANAGER)
    public List<PlatResponse> listerTous() {
        return platRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Plats disponibles seulement (PUBLIC / QR code)
    public List<PlatResponse> listerMenu() {
        return platRepository.findByDisponibleTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Créer un plat
    public PlatResponse creer(PlatRequest request) {
        Plat plat = Plat.builder()
                .nom(request.getNom())
                .description(request.getDescription())
                .prix(request.getPrix())
                .categorie(request.getCategorie())
                .allergenes(request.getAllergenes())
                .imageUrl(request.getImageUrl())
                .disponible(true)
                .quantitePerdueJour(0)
                .build();

        return toResponse(platRepository.save(plat));
    }

    // Modifier un plat
    public PlatResponse modifier(Long id, PlatRequest request) {
        Plat plat = platRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plat introuvable : " + id));

        // Si nouvelle image → supprime l'ancienne
        if (request.getImageUrl() != null &&
                !request.getImageUrl().equals(plat.getImageUrl())) {
            uploadService.supprimerImage(plat.getImageUrl());
        }

        plat.setNom(request.getNom());
        plat.setDescription(request.getDescription());
        plat.setPrix(request.getPrix());
        plat.setCategorie(request.getCategorie());
        plat.setAllergenes(request.getAllergenes());
        plat.setImageUrl(request.getImageUrl());

        return toResponse(platRepository.save(plat));
    }

    // Toggle disponible/indisponible
    public PlatResponse toggleDisponible(Long id) {
        Plat plat = platRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plat introuvable : " + id));

        plat.setDisponible(!plat.getDisponible());
        return toResponse(platRepository.save(plat));
    }

    // Déclarer une perte
    public PlatResponse declarerPerte(Long id, Integer quantite) {
        Plat plat = platRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plat introuvable : " + id));

        plat.setQuantitePerdueJour(plat.getQuantitePerdueJour() + quantite);
        return toResponse(platRepository.save(plat));
    }

    // Supprimer un plat
    public void supprimer(Long id) {
        Plat plat = platRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plat introuvable : " + id));

        uploadService.supprimerImage(plat.getImageUrl());
        platRepository.deleteById(id);
    }

    // Mapper entité → DTO
    private PlatResponse toResponse(Plat p) {
        return new PlatResponse(
                p.getId(),
                p.getNom(),
                p.getDescription(),
                p.getPrix(),
                p.getCategorie(),
                p.getDisponible(),
                p.getAllergenes(),
                p.getQuantitePerdueJour(),
                p.getImageUrl()
        );
    }
}