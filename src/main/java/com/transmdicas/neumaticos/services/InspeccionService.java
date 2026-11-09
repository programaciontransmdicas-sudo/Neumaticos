package com.transmdicas.neumaticos.services;

import java.math.BigDecimal;

import com.transmdicas.neumaticos.forms.InspeccionForm;
import com.transmdicas.neumaticos.repositories.InspeccionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InspeccionService {

    private final InspeccionRepository inspeccionRepository;
    private final UnidadService unidadService;

    public InspeccionService(
            InspeccionRepository inspeccionRepository,
            UnidadService unidadService) {

        this.inspeccionRepository = inspeccionRepository;
        this.unidadService = unidadService;
    }

    @Transactional
    public Long guardarBorrador(InspeccionForm formulario) {

        var unidad = unidadService.buscarPorPlaca(formulario.getPlaca())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "La placa no existe en el catálogo."
                        )
                );

        if (formulario.getKilometraje() != null
                && formulario.getKilometraje() < 0) {
            throw new IllegalArgumentException(
                    "El kilometraje no puede ser negativo."
            );
        }

        if (formulario.getFilas() == null
                || formulario.getFilas().size() != 10) {
            throw new IllegalArgumentException(
                    "La inspección debe contener 10 posiciones."
            );
        }

        for (var fila : formulario.getFilas()) {
            if (fila == null
                    || esNegativo(fila.getR1())
                    || esNegativo(fila.getR2())
                    || esNegativo(fila.getPresion())) {
                throw new IllegalArgumentException(
                        "Revisa las mediciones: no pueden ser negativas."
                );
            }
        }

        // Los datos automáticos se toman de Neon.
        Long id = inspeccionRepository.insertarCabecera(formulario, unidad);

        for (int i = 0; i < formulario.getFilas().size(); i++) {
            inspeccionRepository.insertarDetalle(
                    id,
                    i + 1,
                    formulario.getFilas().get(i)
            );
        }

        return id;
    }

    private boolean esNegativo(BigDecimal valor) {
        return valor != null && valor.signum() < 0;
    }
}