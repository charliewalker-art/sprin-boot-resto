package com.example.monappweb.dto;

import com.example.monappweb.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UtilisateurResponse {

    private Long id;
    private String username;
    private String nom;
    private String prenom;
    private Role role;
    private boolean actif;
}