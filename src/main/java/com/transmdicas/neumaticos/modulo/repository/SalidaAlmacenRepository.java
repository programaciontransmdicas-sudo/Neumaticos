package com.transmdicas.neumaticos.modulo.repository;

import com.transmdicas.neumaticos.modulo.model.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SalidaAlmacenRepository {
  private final JdbcTemplate jdbc;

  public SalidaAlmacenRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private String filtro() {
    return " FROM neumaticos.salidas_almacen WHERE fecha>=? AND fecha<? AND (?='' OR"
               + " LOWER(TRIM(marca))=LOWER(?)) AND (?='' OR LOWER(TRIM(modelo))=LOWER(?))";
  }

  private Object[] parametros(int anio, String marca, String modelo) {
    return new Object[] {
      LocalDate.of(anio, 1, 1), LocalDate.of(anio + 1, 1, 1), marca, marca, modelo, modelo
    };
  }

  public List<IndicadoresSalidas.PorDia> porDia(int anio, String marca, String modelo) {
    return jdbc.query(
        "SELECT fecha,SUM(cantidad) AS unidades,COUNT(*) AS movimientos"
            + filtro()
            + " GROUP BY fecha ORDER BY fecha",
        (r, n) ->
            new IndicadoresSalidas.PorDia(
                r.getObject("fecha", LocalDate.class),
                r.getLong("unidades"),
                r.getLong("movimientos")),
        parametros(anio, marca, modelo));
  }

  public List<IndicadoresSalidas.PorMarca> porMarca(int anio, String marca, String modelo) {
    return jdbc.query(
        "SELECT MIN(TRIM(marca)) AS marca,MIN(TRIM(modelo)) AS modelo,SUM(cantidad) AS"
            + " unidades,COUNT(*) AS movimientos"
            + filtro()
            + " GROUP BY LOWER(TRIM(marca)),LOWER(TRIM(modelo)) ORDER BY unidades"
            + " DESC,marca,modelo",
        (r, n) ->
            new IndicadoresSalidas.PorMarca(
                r.getString("marca"),
                r.getString("modelo"),
                r.getLong("unidades"),
                r.getLong("movimientos")),
        parametros(anio, marca, modelo));
  }

  public List<String> marcas() {
    return jdbc.queryForList(
        "SELECT MIN(TRIM(marca)) FROM neumaticos.salidas_almacen GROUP BY LOWER(TRIM(marca)) ORDER"
            + " BY MIN(TRIM(marca))",
        String.class);
  }

  public List<String> modelos() {
    return jdbc.queryForList(
        "SELECT MIN(TRIM(modelo)) FROM neumaticos.salidas_almacen GROUP BY LOWER(TRIM(modelo))"
            + " ORDER BY MIN(TRIM(modelo))",
        String.class);
  }

  public List<Integer> anios() {
    return jdbc.query(
        "SELECT DISTINCT EXTRACT(YEAR FROM fecha) AS anio FROM neumaticos.salidas_almacen ORDER BY"
            + " anio DESC",
        (r, n) -> r.getInt(1));
  }

  public Optional<LocalDate[]> coberturaExcel() {
    return jdbc
        .query(
            "SELECT MIN(fecha) AS inicio,MAX(fecha) AS fin FROM neumaticos.salidas_almacen WHERE"
                + " origen='EXCEL'",
            (r, n) ->
                new LocalDate[] {
                  r.getObject("inicio", LocalDate.class), r.getObject("fin", LocalDate.class)
                })
        .stream()
        .filter(f -> f[0] != null)
        .findFirst();
  }

  public boolean existeReferencia(String referencia) {
    return jdbc.queryForObject(
            "SELECT COUNT(*) FROM neumaticos.salidas_almacen WHERE referencia=?",
            Long.class,
            referencia)
        > 0;
  }

  public void insertar(SalidaForm f, LocalDate fecha, NeumaticoCatalogo n) {
    jdbc.update(
        "INSERT INTO"
            + " neumaticos.salidas_almacen(origen,referencia,fecha,codigo,marca,modelo,medida,cantidad,placa,observaciones)"
            + " VALUES ('MANUAL',?,?,?,?,?,?,?,?,?)",
        "MANUAL:" + f.getToken(),
        fecha,
        n.codigo(),
        n.marca(),
        n.diseno(),
        n.medida(),
        f.getCantidad(),
        f.getPlaca().strip().toUpperCase(Locale.ROOT),
        f.getObservaciones().strip());
  }
}
