package com.example.monappweb.repository;

import com.example.monappweb.entity.TableRestaurant;
import com.example.monappweb.entity.StatutTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TableRepository extends JpaRepository<TableRestaurant, Long> {
    boolean existsByNumeroTable(Integer numeroTable);
    Optional<TableRestaurant> findByNumeroTable(Integer numeroTable);
    List<TableRestaurant> findByStatut(StatutTable statut);
}