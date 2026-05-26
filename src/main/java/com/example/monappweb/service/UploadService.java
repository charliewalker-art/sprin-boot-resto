package com.example.monappweb.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
public class UploadService {

    @Value("${upload.dir}")
    private String uploadDir;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final List<String> FORMATS_AUTORISES =
            List.of("image/jpeg", "image/png", "image/webp");
    private static final long TAILLE_MAX = 5 * 1024 * 1024; // 5MB

    public String uploadImage(MultipartFile file) throws IOException {

        // Validation format
        if (!FORMATS_AUTORISES.contains(file.getContentType())) {
            throw new RuntimeException("Format non autorisé. JPG, PNG, WEBP seulement.");
        }

        // Validation taille
        if (file.getSize() > TAILLE_MAX) {
            throw new RuntimeException("Fichier trop volumineux. Maximum 5MB.");
        }

        // Crée le dossier si inexistant
        Path dossier = Paths.get(uploadDir);
        if (!Files.exists(dossier)) {
            Files.createDirectories(dossier);
        }

        // Génère un nom unique
        String extension = getExtension(file.getOriginalFilename());
        String nomFichier = UUID.randomUUID().toString() + extension;

        // Sauvegarde le fichier
        Path destination = dossier.resolve(nomFichier);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        // Retourne l'URL publique
        return baseUrl + "/uploads/plats/" + nomFichier;
    }

    public void supprimerImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        try {
            String nomFichier = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            Path fichier = Paths.get(uploadDir).resolve(nomFichier);
            Files.deleteIfExists(fichier);
        } catch (IOException e) {
            System.err.println("Erreur suppression image : " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf("."));
    }
}