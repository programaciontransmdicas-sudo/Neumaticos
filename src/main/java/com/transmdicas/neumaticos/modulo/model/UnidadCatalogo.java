package com.transmdicas.neumaticos.modulo.model;

public record UnidadCatalogo(
    String placa,
    String marca,
    String operacion,
    String ruta,
    String semirremolque,
    String modelo,
    String tipo,
    boolean requiereRevision) {}
