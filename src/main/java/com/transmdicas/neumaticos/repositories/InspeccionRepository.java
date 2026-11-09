package com.transmdicas.neumaticos.repositories;

import com.transmdicas.neumaticos.forms.InspeccionForm;
import com.transmdicas.neumaticos.models.Unidad;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InspeccionRepository {

    private final JdbcTemplate jdbc;

    public InspeccionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Long insertarCabecera(InspeccionForm formulario, Unidad unidad) {
        String sql = """
                INSERT INTO neumaticos.inspecciones (
                    placa, marca, operacion, ruta,
                    operador, kilometraje, fecha, observaciones
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        return jdbc.queryForObject(
                sql,
                Long.class,
                unidad.getPlaca(),
                unidad.getMarca(),
                unidad.getOperacion(),
                unidad.getRuta(),
                formulario.getOperador(),
                formulario.getKilometraje(),
                formulario.getFecha(),
                formulario.getObservaciones()
        );
    }

    public void insertarDetalle(
            Long inspeccionId,
            int posicion,
            InspeccionForm.Fila fila) {

        String sql = """
                INSERT INTO neumaticos.inspeccion_detalles (
                    inspeccion_id, posicion, codigo, rn,
                    r1, r2, presion, aro, estado, sin_tapa
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbc.update(
                sql,
                inspeccionId,
                posicion,
                fila.getCodigo(),
                fila.getRn(),
                fila.getR1(),
                fila.getR2(),
                fila.getPresion(),
                fila.getAro(),
                fila.getEstado(),
                fila.isSinTapa()
        );
    }
}