package com.transmdicas.neumaticos.services;

import com.transmdicas.neumaticos.models.Unidad;
import com.transmdicas.neumaticos.repositories.UnidadRepository;

import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnidadService {

    private final UnidadRepository unidadRepository;

    public UnidadService(UnidadRepository unidadRepository) {
        this.unidadRepository = unidadRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Unidad> buscarPorPlaca(String placa) {

        if (placa == null || placa.isBlank()) {
            return Optional.empty();
        }

        String placaNormalizada =
                placa.strip().toUpperCase(Locale.ROOT);

        return unidadRepository.findByPlaca(placaNormalizada);
    }
}