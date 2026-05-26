package com.example.monappweb.service;

import com.example.monappweb.dto.UpdateProfilRequest;
import com.example.monappweb.dto.UtilisateurRequest;
import com.example.monappweb.dto.UtilisateurResponse;
import com.example.monappweb.entity.Utilisateur;
import com.example.monappweb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Créer un compte staff
    public UtilisateurResponse creerUtilisateur(UtilisateurRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Ce username existe déjà : " + request.getUsername());
        }

        Utilisateur utilisateur = Utilisateur.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .role(request.getRole())
                .actif(true)
                .build();

        Utilisateur saved = userRepository.save(utilisateur);
        return toResponse(saved);
    }

    // Lister tout le staff
    public List<UtilisateurResponse> listerUtilisateurs() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Activer ou désactiver un compte
    public UtilisateurResponse toggleActif(Long id) {
        Utilisateur utilisateur = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + id));

        utilisateur.setActif(!utilisateur.isActif());
        Utilisateur saved = userRepository.save(utilisateur);
        return toResponse(saved);
    }

    // Mapper entité → DTO
    private UtilisateurResponse toResponse(Utilisateur u) {
        return new UtilisateurResponse(
                u.getId(),
                u.getUsername(),
                u.getNom(),
                u.getPrenom(),
                u.getRole(),
                u.isActif()
        );
    }

    // Ajoute cette méthode
    public UtilisateurResponse modifierProfil(UpdateProfilRequest request) {
        // Récupère l'utilisateur connecté
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        Utilisateur utilisateur = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Vérifie si le nouveau username est déjà pris par quelqu'un d'autre
        if (!utilisateur.getUsername().equals(request.getUsername()) &&
                userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Ce username existe déjà : " + request.getUsername());
        }

        utilisateur.setNom(request.getNom());
        utilisateur.setPrenom(request.getPrenom());
        utilisateur.setUsername(request.getUsername());

        // Change le mot de passe seulement si renseigné
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            utilisateur.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return toResponse(userRepository.save(utilisateur));
    }
}