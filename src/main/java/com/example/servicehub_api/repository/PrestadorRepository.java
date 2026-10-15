package com.example.servicehub_api.repository;

import com.example.servicehub_api.model.Prestador;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrestadorRepository extends JpaRepository<Prestador, Long> {
}