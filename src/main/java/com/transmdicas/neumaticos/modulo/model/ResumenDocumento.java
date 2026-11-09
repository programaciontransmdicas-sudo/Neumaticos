package com.transmdicas.neumaticos.modulo.model;

import java.time.LocalDate;

public record ResumenDocumento(
    Long id,
    TipoDocumento tipo,
    String estado,
    String placa,
    String operador,
    LocalDate fecha,
    long version) {
  public String getNumero() {
    return "DOC-" + String.format("%06d", id);
  }
}
