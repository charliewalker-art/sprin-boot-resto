package com.example.monappweb.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "plats")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Plat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prix;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categorie categorie;

    @Column(nullable = false)
    private Boolean disponible = true;

    @Column
    private String allergenes;

    @Column(nullable = false)
    private Integer quantitePerdueJour = 0;

    @Column
    private String imageUrl;
}