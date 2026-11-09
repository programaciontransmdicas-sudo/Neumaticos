package com.transmdicas.neumaticos.modulo.service;

import com.transmdicas.neumaticos.modulo.model.*;
import com.transmdicas.neumaticos.modulo.repository.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IndicadorSalidasService {
  private final SalidaAlmacenRepository salidas;
  private final CatalogoRepository catalogos;

  public IndicadorSalidasService(SalidaAlmacenRepository salidas, CatalogoRepository catalogos) {
    this.salidas = salidas;
    this.catalogos = catalogos;
  }

  @Transactional(readOnly = true)
  public IndicadoresSalidas calcular(
      int anio, PeriodoSalidas periodo, String marca, String modelo) {
    if (anio < 1900 || anio > 2200)
      throw new IllegalArgumentException("Selecciona un año entre 1900 y 2200.");
    long[] unidades = new long[periodo.getCantidad()],
        movimientos = new long[periodo.getCantidad()];
    for (var dia : salidas.porDia(anio, marca.strip(), modelo.strip())) {
      int i = periodo.indice(dia.fecha());
      unidades[i] += dia.unidades();
      movimientos[i] += dia.movimientos();
    }
    List<IndicadoresSalidas.Tramo> tramos = new ArrayList<>();
    long total = 0, registros = 0, max = 1;
    var formato = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    for (int i = 0; i < unidades.length; i++) {
      LocalDate desde = periodo.inicio(anio, i), hasta = periodo.finExclusivo(anio, i).minusDays(1);
      String mes =
          desde
              .getMonth()
              .getDisplayName(java.time.format.TextStyle.FULL, Locale.forLanguageTag("es-PE"));
      String etiqueta =
          switch (periodo) {
            case QUINCENAL -> desde.getDayOfMonth() + "–" + hasta.getDayOfMonth() + " de " + mes;
            case MENSUAL -> mes;
            case SEMESTRAL -> (i == 0 ? "Enero–junio" : "Julio–diciembre");
          };
      tramos.add(new IndicadoresSalidas.Tramo(etiqueta, desde, hasta, unidades[i], movimientos[i]));
      total += unidades[i];
      registros += movimientos[i];
      max = Math.max(max, unidades[i]);
    }
    return new IndicadoresSalidas(
        tramos, salidas.porMarca(anio, marca.strip(), modelo.strip()), total, registros, max);
  }

  @Transactional
  public void registrar(SalidaForm f) {
    if (f.getToken() == null || !f.getToken().matches("[a-zA-Z0-9-]{1,100}"))
      throw new IllegalArgumentException("Abre nuevamente el formulario de salida.");
    if (salidas.existeReferencia("MANUAL:" + f.getToken())) return;
    LocalDate fecha;
    try {
      fecha = LocalDate.parse(f.getFecha());
    } catch (Exception ex) {
      throw new IllegalArgumentException("Selecciona una fecha válida.");
    }
    if (f.getCantidad() == null || f.getCantidad() < 1 || f.getCantidad() > 1000)
      throw new IllegalArgumentException("La cantidad debe ser un entero entre 1 y 1000.");
    if (f.getPlaca().length() > 100 || f.getObservaciones().length() > 10000)
      throw new IllegalArgumentException("Revisa la longitud de placa y observaciones.");
    var n =
        catalogos
            .neumatico(f.getCodigo().strip())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "El código de neumático no existe en el catálogo."));
    if (n.requiereRevision())
      throw new IllegalArgumentException(
          "Confirma primero las características de este código en el catálogo de neumáticos.");
    salidas.insertar(f, fecha, n);
  }
}
