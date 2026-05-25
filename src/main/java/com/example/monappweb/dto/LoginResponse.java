package com.example.monappweb.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String username;
    private String role;
    private String nom;
    private String prenom;
}