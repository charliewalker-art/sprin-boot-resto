package com.example.monappweb.repository;

import com.example.monappweb.entity.Categorie;
import com.example.monappweb.entity.Plat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlatRepository extends JpaRepository<Plat, Long> {
    List<Plat> findByDisponibleTrue();
    List<Plat> findByCategorie(Categorie categorie);
    List<Plat> findByDisponibleTrueAndCategorie(Categorie categorie);
}