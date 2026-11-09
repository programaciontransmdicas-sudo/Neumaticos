package com.transmdicas.neumaticos.modulo.service;

import com.transmdicas.neumaticos.modulo.model.*;
import com.transmdicas.neumaticos.modulo.repository.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentoService {
  private final DocumentoRepository documentos;
  private final CatalogoRepository catalogos;

  public DocumentoService(DocumentoRepository documentos, CatalogoRepository catalogos) {
    this.documentos = documentos;
    this.catalogos = catalogos;
  }

  @Transactional(readOnly = true)
  public DocumentoForm abrir(Long id) {
    return documentos
        .buscar(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento inexistente"));
  }

  public void enriquecer(DocumentoForm d) {
    if (d.getTipo() == TipoDocumento.INSPECCION) d.setOt("");
    d.setPlaca(texto(d.getPlaca()).toUpperCase(Locale.ROOT));
    d.setMarcaUnidad("");
    d.setOperacion("");
    d.setRuta("");
    d.setSemirremolque("");
    catalogos
        .unidad(d.getPlaca())
        .ifPresent(
            u -> {
              d.setMarcaUnidad(u.marca());
              d.setOperacion(u.operacion());
              d.setRuta(u.ruta());
              d.setSemirremolque(u.semirremolque());
            });
    Set<String> codigos = new HashSet<>();
    for (var lista : List.of(d.getFilas(), d.getDespues()))
      for (var r : lista) {
        if (!texto(r.getCodigo()).isBlank()) codigos.add(texto(r.getCodigo()));
        if (!texto(r.getCodigoNuevo()).isBlank()) codigos.add(texto(r.getCodigoNuevo()));
      }
    var mapa = catalogos.porCodigos(codigos);
    for (var lista : List.of(d.getFilas(), d.getDespues()))
      for (var r : lista) {
        r.setCodigo(texto(r.getCodigo()));
        r.setCodigoNuevo(texto(r.getCodigoNuevo()));
        r.setMarca("");
        r.setDiseno("");
        r.setMedida("");
        r.setMarcaNueva("");
        r.setDisenoNuevo("");
        r.setMedidaNueva("");
        Optional.ofNullable(mapa.get(r.getCodigo()))
            .ifPresent(
                t -> {
                  r.setMarca(t.marca());
                  r.setDiseno(t.diseno());
                  r.setMedida(t.medida());
                });
        Optional.ofNullable(mapa.get(r.getCodigoNuevo()))
            .ifPresent(
                t -> {
                  r.setMarcaNueva(t.marca());
                  r.setDisenoNuevo(t.diseno());
                  r.setMedidaNueva(t.medida());
                });
      }
  }

  public void validar(DocumentoForm d, Errors e, boolean generar) {
    if (d.getTipo() == null) {
      e.reject("tipo", "Selecciona un formato válido.");
      return;
    }
    int max =
        d.getTipo() == TipoDocumento.INSPECCION
            ? 10
            : d.getTipo() == TipoDocumento.CAMBIO ? 12 : 20;
    if (d.getFilas().isEmpty()
        || d.getFilas().size() > max
        || (d.getTipo() == TipoDocumento.INSPECCION && d.getFilas().size() != 10)
        || d.getDespues().size() != 10) {
      e.reject("filas", "Revisa la cantidad de posiciones del formulario.");
      return;
    }
    if (d.getToken() == null || !d.getToken().matches("[a-zA-Z0-9:-]{1,100}"))
      e.reject("token", "El formulario perdió su identificador. Abre uno nuevo.");
    if (d.getPlaca().length() > 10) e.reject("placa", "La placa admite hasta 10 caracteres.");
    if (d.getKilometraje() != null && d.getKilometraje() < 0)
      e.reject("km", "El kilometraje no puede ser negativo.");
    if (d.getOperador().length() > 250
        || d.getObservaciones().length() > 10000
        || d.getOt().length() > 100)
      e.reject("texto", "Revisa la longitud del operador, OT u observaciones.");
    for (var nombre : d.getResponsables())
      if (nombre.length() > 250)
        e.reject("responsable", "Los nombres de responsables admiten hasta 250 caracteres.");
    if (generar) {
      if (catalogos.unidad(d.getPlaca()).map(UnidadCatalogo::requiereRevision).orElse(false))
        e.reject(
            "unidadRevision",
            "La placa tiene datos contradictorios en el Excel. Confirma operación, ruta y"
                + " semirremolque antes de generar.");
      if (d.getPlaca().isBlank() || catalogos.unidad(d.getPlaca()).isEmpty())
        e.reject("placa", "Selecciona una placa existente en el catálogo.");
      if (d.getOperador().isBlank() || d.getKilometraje() == null || d.getFecha() == null)
        e.reject("cabecera", "Completa operador, kilometraje y fecha.");
      if (d.getTipo() != TipoDocumento.INSPECCION && d.getOt().isBlank())
        e.reject("ot", "Completa la orden de trabajo.");
      if (d.getTipo() == TipoDocumento.CAMBIO && (d.getInicio() == null || d.getFin() == null))
        e.reject("horas", "Completa las horas de inicio y fin.");
      if (d.getFilas().stream().noneMatch(FilaDocumento::isUsada))
        e.reject("vacio", "Registra al menos un neumático.");
    }
    validarFilas(d, d.getFilas(), "Principal", generar, e);
    if (d.getTipo() == TipoDocumento.INSPECCION && d.isRotaciones())
      validarFilas(d, d.getDespues(), "Rotaciones", generar, e);
  }

  private void validarFilas(
      DocumentoForm d, List<FilaDocumento> filas, String seccion, boolean generar, Errors e) {
    Set<String> codigos = new HashSet<>();
    for (var f : filas) {
      if (!f.getCodigo().isBlank()) codigos.add(f.getCodigo());
      if (!f.getCodigoNuevo().isBlank()) codigos.add(f.getCodigoNuevo());
    }
    var existentes = catalogos.porCodigos(codigos);
    Set<String> usados = new HashSet<>(), montados = new HashSet<>();
    Set<Integer> posiciones = new HashSet<>();
    for (int i = 0; i < filas.size(); i++) {
      var r = filas.get(i);
      String etiqueta = seccion + ", fila " + (i + 1) + ": ";
      if (d.getTipo() == TipoDocumento.INSPECCION) r.setPosicion(i + 1);
      if (!r.isUsada()) continue;
      if (r.getCodigo().length() > 100
          || r.getCodigoNuevo().length() > 100
          || r.getCondicion().length() > 250
          || r.getDisposicion().length() > 250) {
        e.reject("fila", etiqueta + "revisa la longitud de los textos.");
        continue;
      }
      if (!rango(r.getR1(), 99)
          || !rango(r.getR2(), 99)
          || !rango(r.getN1(), 99)
          || !rango(r.getN2(), 99)
          || !rango(r.getPresion(), 250))
        e.reject("medicion", etiqueta + "remanentes entre 0 y 99 mm; presión entre 0 y 250 PSI.");
      if (r.getCantidad() != null && (r.getCantidad() < 1 || r.getCantidad() > 100))
        e.reject("cantidad", etiqueta + "cantidad entre 1 y 100.");
      if (!opcion(r.getRn(), "N", "R")
          || !opcion(r.getAro(), "C", "M")
          || !opcion(r.getSinTapa(), "SI", "NO")
          || !opcion(r.getConformidad(), "Pendiente", "Conforme", "No conforme"))
        e.reject("opcion", etiqueta + "selecciona opciones válidas.");
      if (!generar) continue;
      if (!existentes.containsKey(r.getCodigo()))
        e.reject("codigo", etiqueta + "el código no existe en el catálogo.");
      if (existentes.containsKey(r.getCodigo()) && existentes.get(r.getCodigo()).requiereRevision())
        e.reject(
            "revision",
            etiqueta
                + "el código "
                + r.getCodigo()
                + " tiene datos contradictorios en el Excel. Debe revisarse en el catálogo antes de"
                + " generar.");
      if (!usados.add(r.getCodigo()))
        e.reject("duplicado", etiqueta + "código repetido dentro de la sección.");
      if (d.getTipo() == TipoDocumento.REQUERIMIENTO) {
        if (r.getRn().isBlank() || r.getCantidad() == null)
          e.reject("requerimiento", etiqueta + "completa R/N y cantidad.");
      } else if (d.getTipo() == TipoDocumento.INSPECCION) {
        if (r.getR1() == null
            || r.getR2() == null
            || r.getPresion() == null
            || r.getRn().isBlank()
            || r.getAro().isBlank()
            || r.getCondicion().isBlank()
            || r.getSinTapa().isBlank())
          e.reject(
              "inspeccion", etiqueta + "completa remanentes, PSI, R/N, aro, estado y válvula.");
      } else {
        if (r.getPosicion() == null
            || r.getPosicion() < 1
            || r.getPosicion() > 22
            || !posiciones.add(r.getPosicion()))
          e.reject("posicion", etiqueta + "posición entre 1 y 22, sin repetir.");
        if (!existentes.containsKey(r.getCodigoNuevo()))
          e.reject("montaje", etiqueta + "selecciona un código montado del catálogo.");
        if (existentes.containsKey(r.getCodigoNuevo())
            && existentes.get(r.getCodigoNuevo()).requiereRevision())
          e.reject(
              "revision", etiqueta + "el código montado tiene datos contradictorios en el Excel.");
        if (!montados.add(r.getCodigoNuevo()))
          e.reject("montaje", etiqueta + "código montado repetido.");
        if (r.getCodigo().equals(r.getCodigoNuevo()))
          e.reject("montaje", etiqueta + "el retirado y el montado deben ser distintos.");
        if (r.getR1() == null
            || r.getR2() == null
            || r.getN1() == null
            || r.getN2() == null
            || r.getDisposicion().isBlank())
          e.reject("cambio", etiqueta + "completa ambos remanentes y la disposición.");
      }
    }
  }

  private boolean rango(BigDecimal n, int max) {
    return n == null || (n.signum() >= 0 && n.compareTo(BigDecimal.valueOf(max)) <= 0);
  }

  private boolean opcion(String s, String... opciones) {
    return s.isEmpty() || Arrays.asList(opciones).contains(s);
  }

  private String texto(String s) {
    return s == null ? "" : s.strip();
  }

  @Transactional
  public Long guardar(DocumentoForm d, boolean generar) {
    if (d.getId() == null) {
      var previo = documentos.porToken(d.getToken());
      if (previo.isPresent()) return previo.get();
    } else {
      var previo = abrir(d.getId());
      if (previo.isGenerado())
        throw new IllegalArgumentException(
            "El documento ya fue generado. Puedes duplicarlo para crear otro.");
      if (previo.getTipo() != d.getTipo() || !previo.getToken().equals(d.getToken()))
        throw new IllegalArgumentException("El formulario no coincide con el registro original.");
    }
    enriquecer(d);
    var errores = new org.springframework.validation.BeanPropertyBindingResult(d, "documento");
    validar(d, errores, generar);
    if (errores.hasErrors())
      throw new IllegalArgumentException(errores.getAllErrors().get(0).getDefaultMessage());
    d.setEstado(generar ? "GENERADO" : "BORRADOR");
    return documentos.guardar(d);
  }

  @Transactional
  public void eliminar(Long id, long version) {
    documentos.eliminar(id, version);
  }
}
