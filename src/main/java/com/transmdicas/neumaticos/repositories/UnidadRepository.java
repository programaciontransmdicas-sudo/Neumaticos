package com.transmdicas.neumaticos.repositories;

import com.transmdicas.neumaticos.models.Unidad;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadRepository extends JpaRepository<Unidad, Long> {

    Optional<Unidad> findByPlaca(String placa);
}