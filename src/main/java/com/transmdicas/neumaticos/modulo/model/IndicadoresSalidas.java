package com.transmdicas.neumaticos.modulo.model;

import java.time.LocalDate;
import java.util.List;

public record IndicadoresSalidas(
    List<Tramo> periodos, List<PorMarca> marcas, long unidades, long movimientos, long maximo) {
  public record Tramo(
      String etiqueta, LocalDate desde, LocalDate hasta, long unidades, long movimientos) {}

  public record PorMarca(String marca, String modelo, long unidades, long movimientos) {}

  public record PorDia(LocalDate fecha, long unidades, long movimientos) {}
}
