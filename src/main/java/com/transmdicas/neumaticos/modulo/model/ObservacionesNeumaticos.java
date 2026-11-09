package com.transmdicas.neumaticos.modulo.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Observaciones por posición para la vista imprimible y el Excel. */
public final class ObservacionesNeumaticos {
  private static final int COLUMNAS = 12;
  private static final int MAX_POSICION = 22;

  private ObservacionesNeumaticos() {}

  public record Celda(String encabezado, String texto) {}

  public record Seccion(String titulo, List<List<Celda>> bloques) {}

  public static List<Seccion> secciones(DocumentoForm documento) {
    return switch (documento.getTipo()) {
      case CAMBIO -> List.of(
          new Seccion("Retirados", bloques(documento.getFilas(), false)),
          new Seccion("Montados", bloques(documento.getFilas(), true)));
      case INSPECCION -> documento.isRotaciones()
          ? List.of(
              new Seccion("Inspección inicial", bloques(documento.getFilas(), false)),
              new Seccion("Después de rotaciones / cambios", bloques(documento.getDespues(), false)))
          : List.of(new Seccion("", bloques(documento.getFilas(), false)));
      case REQUERIMIENTO -> List.of();
    };
  }

  private static List<List<Celda>> bloques(List<FilaDocumento> filas, boolean montado) {
    int ultima = filas.stream()
        .map(FilaDocumento::getPosicion)
        .filter(p -> p != null && p >= 1 && p <= MAX_POSICION)
        .mapToInt(Integer::intValue)
        .max().orElse(0);
    List<List<Celda>> bloques = new ArrayList<>();
    for (int primera = 1; primera <= Math.max(COLUMNAS, ultima); primera += COLUMNAS) {
      List<Celda> celdas = new ArrayList<>();
      for (int posicion = primera; posicion < primera + COLUMNAS; posicion++) {
        if (posicion > MAX_POSICION) {
          celdas.add(new Celda("", ""));
          continue;
        }
        List<String> textos = new ArrayList<>();
        for (var fila : filas) {
          if (fila.getPosicion() == null || fila.getPosicion() != posicion) continue;
          String remanente = remanente(montado ? fila.getPromedioNuevo() : fila.getPromedio());
          String presion = montado ? "" : presion(fila.getPresion());
          if (!remanente.isEmpty()) textos.add("Remanente: " + remanente);
          if (!presion.isEmpty()) textos.add(presion);
        }
        celdas.add(new Celda(
            String.format(Locale.ROOT, "Neumático %02d", posicion), String.join("\n", textos)));
      }
      bloques.add(celdas);
    }
    return bloques;
  }

  static String remanente(BigDecimal valor) {
    if (valor == null) return "";
    if (valor.signum() < 0) return "Por revisar";
    if (comparar(valor, 6) < 0) return "Cambio";
    if ((comparar(valor, 6) > 0 && comparar(valor, 8) < 0) || comparar(valor, 9) > 0) {
      return "Normal";
    }
    return "Por revisar";
  }

  static String presion(BigDecimal valor) {
    if (valor == null) return "";
    if (valor.signum() < 0) return "Presión: por revisar";
    if (comparar(valor, 99) < 0) return "Muy baja presión";
    if (comparar(valor, 99) > 0 && comparar(valor, 105) < 0) return "Presión baja";
    if (comparar(valor, 105) > 0 && comparar(valor, 115) < 0) return "Presión normal";
    if (comparar(valor, 116) > 0 && comparar(valor, 120) < 0) return "Presión alta";
    if (comparar(valor, 120) > 0) return "Presión muy alta";
    return "Presión: por revisar";
  }

  private static int comparar(BigDecimal valor, int limite) {
    return valor.compareTo(BigDecimal.valueOf(limite));
  }
}
