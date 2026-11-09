package com.transmdicas.neumaticos.modulo.controller;

import com.transmdicas.neumaticos.modulo.exportacion.DocumentoExcel;
import com.transmdicas.neumaticos.modulo.model.*;
import com.transmdicas.neumaticos.modulo.repository.*;
import com.transmdicas.neumaticos.modulo.service.DocumentoService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.transmdicas.neumaticos.util.ConversorPdf;

@Controller
public class ReporteController {
  private final DocumentoRepository repo;
  private final DocumentoService servicio;
  private final CatalogoRepository catalogos;

  public ReporteController(
      DocumentoRepository repo, DocumentoService servicio, CatalogoRepository catalogos) {
    this.repo = repo;
    this.servicio = servicio;
    this.catalogos = catalogos;
  }

  @GetMapping("/reportes")
  public String reportes(Model m) {
    var resumen = repo.resumen();
    long total = 0, generados = 0;
    Map<String, Long> porTipo = new LinkedHashMap<>();
    for (var t : TipoDocumento.values()) porTipo.put(t.getTitulo(), 0L);
    for (var fila : resumen) {
      long n = ((Number) fila.get("cantidad")).longValue();
      total += n;
      if ("GENERADO".equals(fila.get("estado"))) generados += n;
      var t = TipoDocumento.valueOf(fila.get("tipo").toString());
      porTipo.merge(t.getTitulo(), n, Long::sum);
    }
    m.addAttribute("total", total);
    m.addAttribute("generados", generados);
    m.addAttribute("borradores", total - generados);
    m.addAttribute("porTipo", porTipo);
    m.addAttribute("unidades", catalogos.contar(true, ""));
    m.addAttribute("neumaticos", catalogos.contar(false, ""));
    m.addAttribute("nav", "reportes");
    return "modulo/reportes";
  }
  @GetMapping("/documentos/{id}/pdf")
public void descargarPdf(
        @PathVariable("id") Long id,
        HttpServletResponse respuesta
) throws IOException {

    // Buscar el documento guardado.
    var documento = servicio.abrir(id);

    if (!DocumentoExcel.admite(documento.getTipo())) {
        respuesta.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
    }

    // Crear el formato anterior en memoria.
    ByteArrayOutputStream excel = new ByteArrayOutputStream();

    DocumentoExcel.escribir(documento, excel);

    // Convertir el formato a PDF usando LibreOffice.
    byte[] pdf = ConversorPdf.convertir(
            excel.toByteArray(),
            "xlsx"
    );

    // Enviar el PDF al navegador para su descarga.
    respuesta.setContentType("application/pdf");

    respuesta.setHeader(
            "Content-Disposition",
            "attachment; filename=neumaticos-" + id + ".pdf"
    );

    respuesta.getOutputStream().write(pdf);
}
  @GetMapping("/documentos/{id}/excel")
  public void excel(@PathVariable Long id, HttpServletResponse res) throws IOException {
    var documento = servicio.abrir(id);
    if (!DocumentoExcel.admite(documento.getTipo())) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    res.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    res.setHeader("Content-Disposition", "attachment; filename=neumaticos-" + id + ".xlsx");
    DocumentoExcel.escribir(documento, res.getOutputStream());
  }

  @GetMapping("/registros/exportar.csv")
  @Transactional(readOnly = true)
  public void csv(HttpServletResponse res) throws IOException {
    res.setContentType("text/csv;charset=UTF-8");
    res.setHeader("Content-Disposition", "attachment; filename=registros-neumaticos.csv");
    var w = res.getWriter();
    w.write('﻿');
    linea(
        w,
        "Registro",
        "Tipo",
        "Estado",
        "Placa",
        "Operador",
        "Fecha",
        "Km",
        "OT",
        "Operación",
        "Ruta",
        "Sección",
        "Posición",
        "Código",
        "Marca",
        "Diseño",
        "Medida",
        "R/N",
        "R1",
        "R2",
        "Promedio",
        "Evaluación de remanente",
        "PSI",
        "Aro",
        "Estado neumático",
        "Sin tapa",
        "Disposición",
        "Código montado",
        "Marca montado",
        "Diseño montado",
        "Medida montado",
        "R3",
        "R4",
        "Promedio montado",
        "Evaluación montado",
        "Cantidad",
        "Conformidad",
        "Observaciones",
        "Responsable 1",
        "Responsable 2",
        "Responsable 3",
        "Responsable 4");
    for (Long id : repo.todosIds()) {
      var d = servicio.abrir(id);
      for (String seccion :
          d.isRotaciones() ? List.of("Principal", "Rotaciones") : List.of("Principal")) {
        var filas =
            (seccion.equals("Principal") ? d.getFilas() : d.getDespues())
                .stream().filter(FilaDocumento::isUsada).toList();
        if (filas.isEmpty()) filas = List.of(new FilaDocumento());
        for (var f : filas)
          linea(
              w,
              d.getNumero(),
              d.getTipo().getTitulo(),
              d.getEstado(),
              d.getPlaca(),
              d.getOperador(),
              d.getFecha(),
              d.getKilometraje(),
              d.getTipo() == TipoDocumento.INSPECCION ? "" : d.getOt(),
              d.getOperacion(),
              d.getRuta(),
              seccion,
              f.getPosicion(),
              f.getCodigo(),
              f.getMarca(),
              f.getDiseno(),
              f.getMedida(),
              f.getRn(),
              f.getR1(),
              f.getR2(),
              f.getPromedio(),
              f.getPromedio() == null ? "" : f.getAlerta().getEtiqueta(),
              f.getPresion(),
              f.getAro(),
              f.getCondicion(),
              f.getSinTapa(),
              f.getDisposicion(),
              f.getCodigoNuevo(),
              f.getMarcaNueva(),
              f.getDisenoNuevo(),
              f.getMedidaNueva(),
              f.getN1(),
              f.getN2(),
              f.getPromedioNuevo(),
              f.getPromedioNuevo() == null ? "" : f.getAlertaNuevo().getEtiqueta(),
              f.getCantidad(),
              f.getConformidad(),
              d.getObservaciones(),
              d.getResponsable1(),
              d.getResponsable2(),
              d.getResponsable3(),
              d.getResponsable4());
      }
    }
  }

  private void linea(Writer w, Object... valores) throws IOException {
    List<String> celdas = new ArrayList<>();
    for (Object v : valores) {
      String s = v == null ? "" : v.toString();
      String inicio = s.stripLeading();
      if (!inicio.isEmpty() && "=+-@".indexOf(inicio.charAt(0)) >= 0) s = "'" + s;
      celdas.add("\"" + s.replace("\"", "\"\"") + "\"");
    }
    w.write(String.join(";", celdas) + "\r\n");
  }
}
