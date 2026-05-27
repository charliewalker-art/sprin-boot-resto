package com.example.monappweb.service;

import com.example.monappweb.dto.*;
import com.example.monappweb.entity.*;
import com.example.monappweb.repository.CommandeRepository;
import com.example.monappweb.repository.PlatRepository;
import com.example.monappweb.repository.TableRepository;
import com.example.monappweb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final PlatRepository platRepository;
    private final TableRepository tableRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate; // ← WebSocket

    // ─────────────────────────────────────────────
    // NOTIFICATION WebSocket
    // ─────────────────────────────────────────────

    private void notifier(CommandeResponse response) {
        messagingTemplate.convertAndSend("/topic/commandes", response);
    }

    // ─────────────────────────────────────────────
    // CRÉATION (Client QR ou Serveur)
    // ─────────────────────────────────────────────

    @Transactional
    public CommandeResponse creerCommande(CommandeRequest request) {
        Commande commande = new Commande();
        commande.setTypeCommande(request.getTypeCommande());
        commande.setStatut(StatutCommande.EN_ATTENTE_CUISINE);
        commande.setTempsAttenteEstime(request.getTempsAttenteEstime());
        commande.setNomClientRetrait(request.getNomClientRetrait());

        if (request.getTableId() != null) {
            TableRestaurant table = tableRepository.findById(request.getTableId())
                    .orElseThrow(() -> new RuntimeException("Table introuvable : " + request.getTableId()));
            commande.setTable(table);
        }

        if (request.getServeurId() != null && request.getServeurId() != 0) {
            Utilisateur serveur = userRepository.findById(request.getServeurId())
                    .orElseThrow(() -> new RuntimeException("Serveur introuvable : " + request.getServeurId()));
            commande.setServeur(serveur);
        }

        List<DetailCommande> details = request.getDetails().stream().map(d -> {
            Plat plat = platRepository.findById(d.getPlatId())
                    .orElseThrow(() -> new RuntimeException("Plat introuvable : " + d.getPlatId()));
            if (!plat.getDisponible()) {
                throw new RuntimeException("Le plat '" + plat.getNom() + "' n'est plus disponible.");
            }
            return DetailCommande.builder()
                    .commande(commande)
                    .plat(plat)
                    .quantite(d.getQuantite())
                    .noteClient(d.getNoteClient())
                    .build();
        }).collect(Collectors.toList());

        commande.setDetails(details);

        Commande saved = commandeRepository.save(commande);
        CommandeResponse response = CommandeResponse.fromEntity(saved);
        notifier(response); // ← notifie la cuisine immédiatement
        return response;
    }

    // ─────────────────────────────────────────────
    // VALIDATION → passe à EN_ATTENTE_CUISINE
    // ─────────────────────────────────────────────

    @Transactional
    public CommandeResponse validerCommande(Long id) {
        Commande commande = getCommandeOuErreur(id);
        verifierStatutAutorise(commande, StatutCommande.CREEE);
        commande.setStatut(StatutCommande.EN_ATTENTE_CUISINE);
        CommandeResponse response = CommandeResponse.fromEntity(commandeRepository.save(commande));
        notifier(response);
        return response;
    }

    // ─────────────────────────────────────────────
    // CUISINIÈRE — Commencer → EN_PREPARATION
    // ─────────────────────────────────────────────

    @Transactional
    public CommandeResponse commencerPreparation(Long id) {
        Commande commande = getCommandeOuErreur(id);
        verifierStatutAutorise(commande, StatutCommande.EN_ATTENTE_CUISINE);
        commande.setStatut(StatutCommande.EN_PREPARATION);
        CommandeResponse response = CommandeResponse.fromEntity(commandeRepository.save(commande));
        notifier(response);
        return response;
    }

    // ─────────────────────────────────────────────
    // CUISINIÈRE — Terminer → PRETE
    // ─────────────────────────────────────────────

    @Transactional
    public CommandeResponse marquerPrete(Long id) {
        Commande commande = getCommandeOuErreur(id);
        verifierStatutAutorise(commande, StatutCommande.EN_PREPARATION);
        commande.setStatut(StatutCommande.PRETE);
        CommandeResponse response = CommandeResponse.fromEntity(commandeRepository.save(commande));
        notifier(response);
        return response;
    }

    // ─────────────────────────────────────────────
    // SERVEUR — Déposer → SERVIE
    // ─────────────────────────────────────────────

    @Transactional
    public CommandeResponse marquerServie(Long id) {
        Commande commande = getCommandeOuErreur(id);
        verifierStatutAutorise(commande, StatutCommande.PRETE);
        commande.setStatut(StatutCommande.SERVIE);
        CommandeResponse response = CommandeResponse.fromEntity(commandeRepository.save(commande));
        notifier(response);
        return response;
    }

    // ─────────────────────────────────────────────
    // CLIENT — Demander l'addition → EN_ATTENTE_PAIEMENT
    // ─────────────────────────────────────────────

    @Transactional
    public CommandeResponse demanderAddition(Long id) {
        Commande commande = getCommandeOuErreur(id);
        verifierStatutAutorise(commande, StatutCommande.SERVIE);
        commande.setStatut(StatutCommande.EN_ATTENTE_PAIEMENT);
        CommandeResponse response = CommandeResponse.fromEntity(commandeRepository.save(commande));
        notifier(response);
        return response;
    }

    // ─────────────────────────────────────────────
    // ANNULATION
    // ─────────────────────────────────────────────

    @Transactional
    public CommandeResponse annulerCommande(Long id, Long annuleParId, AnnulationRequest request) {
        Commande commande = getCommandeOuErreur(id);

        if (commande.getStatut() == StatutCommande.EN_PREPARATION
                || commande.getStatut() == StatutCommande.PRETE
                || commande.getStatut() == StatutCommande.SERVIE
                || commande.getStatut() == StatutCommande.PAYEE) {
            throw new RuntimeException("Impossible d'annuler une commande au statut : " + commande.getStatut());
        }

        Utilisateur annulePar = userRepository.findById(annuleParId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + annuleParId));

        commande.setStatut(StatutCommande.ANNULEE);
        commande.setAnnulePar(annulePar);
        commande.setMotifAnnulation(request.getMotifAnnulation());
        commande.setDateAnnulation(LocalDateTime.now());

        CommandeResponse response = CommandeResponse.fromEntity(commandeRepository.save(commande));
        notifier(response);
        return response;
    }

    // ─────────────────────────────────────────────
    // ÉVALUATION CLIENT
    // ─────────────────────────────────────────────

    @Transactional
    public CommandeResponse evaluerCommande(Long id, EvaluationRequest request) {
        Commande commande = getCommandeOuErreur(id);
        if (commande.getStatut() != StatutCommande.PAYEE) {
            throw new RuntimeException("L'évaluation n'est possible que pour une commande PAYEE.");
        }
        commande.setNoteSatisfaction(request.getNoteSatisfaction());
        commande.setCommentaireClient(request.getCommentaireClient());
        CommandeResponse response = CommandeResponse.fromEntity(commandeRepository.save(commande));
        notifier(response);
        return response;
    }

    // ─────────────────────────────────────────────
    // LECTURES
    // ─────────────────────────────────────────────

    public CommandeResponse getCommande(Long id) {
        return CommandeResponse.fromEntity(getCommandeOuErreur(id));
    }

    public List<CommandeResponse> getCommandesParStatut(StatutCommande statut) {
        return commandeRepository.findByStatutOrderByDateCreationAsc(statut)
                .stream()
                .map(CommandeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<CommandeResponse> getToutesLesCommandes() {
        return commandeRepository.findAllByOrderByDateCreationAsc()
                .stream()
                .map(CommandeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<CommandeResponse> getCommandesEnRetard(int seuilMinutes) {
        LocalDateTime seuil = LocalDateTime.now().minusMinutes(seuilMinutes);
        return commandeRepository.findCommandesEnRetard(seuil)
                .stream()
                .map(CommandeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<CommandeResponse> getJournalAnnulations() {
        return commandeRepository.findByStatutOrderByDateAnnulationDesc(StatutCommande.ANNULEE)
                .stream()
                .map(CommandeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // UTILITAIRES PRIVÉS
    // ─────────────────────────────────────────────

    private Commande getCommandeOuErreur(Long id) {
        return commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande introuvable : " + id));
    }

    private void verifierStatutAutorise(Commande commande, StatutCommande statutAttendu) {
        if (commande.getStatut() != statutAttendu) {
            throw new RuntimeException(
                    "Action impossible : statut actuel = " + commande.getStatut()
                            + ", statut requis = " + statutAttendu
            );
        }
    }
}