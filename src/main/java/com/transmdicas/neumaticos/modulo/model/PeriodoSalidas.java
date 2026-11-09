package com.transmdicas.neumaticos.modulo.model;

import java.time.LocalDate;

public enum PeriodoSalidas {
  QUINCENAL("Quincenal", 24),
  MENSUAL("Mensual", 12),
  SEMESTRAL("Semestral", 2);
  private final String titulo;
  private final int cantidad;

  PeriodoSalidas(String titulo, int cantidad) {
    this.titulo = titulo;
    this.cantidad = cantidad;
  }

  public String getTitulo() {
    return titulo;
  }

  public int getCantidad() {
    return cantidad;
  }

  public int indice(LocalDate fecha) {
    return switch (this) {
      case QUINCENAL -> (fecha.getMonthValue() - 1) * 2 + (fecha.getDayOfMonth() <= 15 ? 0 : 1);
      case MENSUAL -> fecha.getMonthValue() - 1;
      case SEMESTRAL -> (fecha.getMonthValue() - 1) / 6;
    };
  }

  public LocalDate inicio(int anio, int indice) {
    return switch (this) {
      case QUINCENAL -> LocalDate.of(anio, indice / 2 + 1, indice % 2 == 0 ? 1 : 16);
      case MENSUAL -> LocalDate.of(anio, indice + 1, 1);
      case SEMESTRAL -> LocalDate.of(anio, indice * 6 + 1, 1);
    };
  }

  public LocalDate finExclusivo(int anio, int indice) {
    if (indice == cantidad - 1) return LocalDate.of(anio + 1, 1, 1);
    return inicio(anio, indice + 1);
  }
}
