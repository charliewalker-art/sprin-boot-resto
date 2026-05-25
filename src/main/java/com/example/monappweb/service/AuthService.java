package com.example.monappweb.service;

import com.example.monappweb.dto.LoginRequest;
import com.example.monappweb.dto.LoginResponse;
import com.example.monappweb.entity.Utilisateur;
import com.example.monappweb.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {

        // 1. Spring vérifie username + password via UserDetailsServiceImpl
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // 2. On récupère l'utilisateur authentifié
        Utilisateur utilisateur = (Utilisateur) auth.getPrincipal();

        // 3. On génère le JWT
        String token = jwtUtil.generateToken(
                utilisateur.getUsername(),
                utilisateur.getRole().name()
        );

        // 4. On retourne le token + infos utiles
        return new LoginResponse(
                token,
                utilisateur.getUsername(),
                utilisateur.getRole().name(),
                utilisateur.getNom(),
                utilisateur.getPrenom()
        );
    }
}