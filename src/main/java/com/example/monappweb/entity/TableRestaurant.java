package com.example.monappweb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tables_restaurant")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TableRestaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Integer numeroTable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutTable statut = StatutTable.LIBRE;

    @Column
    private String qrCodeUrl;

    @Column(nullable = false)
    private Boolean appelServeurActif = false;

    @Column
    private LocalDateTime heureAppel;
}