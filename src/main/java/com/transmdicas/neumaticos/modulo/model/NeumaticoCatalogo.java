package com.transmdicas.neumaticos.modulo.model;

public record NeumaticoCatalogo(
    String codigo,
    String marca,
    String diseno,
    String medida,
    String reencauche1,
    String reencauche2,
    boolean requiereRevision) {}
