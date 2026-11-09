package com.transmdicas.neumaticos.modulo.model;

import java.util.List;

public enum TipoDocumento {
  INSPECCION(
      "Inspección de unidad",
      "MNT-F-37 / MNT-F-60",
      "MNT-F-37",
      "5",
      "REGISTRO DE NEUMÁTICOS DE REMOLCADOR",
      List.of("Supervisor de neumáticos", "Técnico / llantero", "Supervisor de mantenimiento")),
  CAMBIO(
      "Cambio de neumáticos",
      "MNT-F-76",
      "MNT-F-76",
      "02",
      "CAMBIO DE NEUMÁTICOS",
      List.of("Téc. de neumáticos", "Sup. de neumáticos", "Jefe de mantenimiento")),
  REQUERIMIENTO(
      "Requerimiento",
      "Código por confirmar",
      "Por confirmar",
      "Por confirmar",
      "REQUERIMIENTO DE NEUMÁTICOS",
      List.of("Logística", "Téc. de neumáticos", "Inspector de neumáticos", "Autoriza"));
  private final String titulo, codigo, codigoImpreso, version, papel;
  private final List<String> roles;

  TipoDocumento(
      String titulo,
      String codigo,
      String codigoImpreso,
      String version,
      String papel,
      List<String> roles) {
    this.titulo = titulo;
    this.codigo = codigo;
    this.codigoImpreso = codigoImpreso;
    this.version = version;
    this.papel = papel;
    this.roles = roles;
  }

  public String getTitulo() {
    return titulo;
  }

  public String getCodigo() {
    return codigo;
  }

  public String getCodigoImpreso() {
    return codigoImpreso;
  }

  public String getVersion() {
    return version;
  }

  public String getPapel() {
    return papel;
  }

  public List<String> getRoles() {
    return roles;
  }
}
