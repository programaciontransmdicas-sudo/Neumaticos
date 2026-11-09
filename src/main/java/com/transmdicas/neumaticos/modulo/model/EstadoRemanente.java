package com.transmdicas.neumaticos.modulo.model;

import java.math.BigDecimal;

public enum EstadoRemanente {
  SIN_DATOS("Sin datos", "sin-datos"),
  CAMBIO("Cambio", "cambio"),
  ADVERTENCIA("Advertencia", "advertencia"),
  CONFORME("Conforme", "conforme");
  private final String etiqueta, clase;

  EstadoRemanente(String etiqueta, String clase) {
    this.etiqueta = etiqueta;
    this.clase = clase;
  }

  public String getEtiqueta() {
    return etiqueta;
  }

  public String getClase() {
    return clase;
  }

  public static EstadoRemanente de(BigDecimal promedio) {
    if (promedio == null || promedio.signum() < 0) return SIN_DATOS;
    if (promedio.compareTo(BigDecimal.valueOf(6)) <= 0) return CAMBIO;
    if (promedio.compareTo(BigDecimal.valueOf(9)) < 0) return ADVERTENCIA;
    return CONFORME;
  }
}
