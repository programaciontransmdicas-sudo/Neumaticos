package com.transmdicas.neumaticos.modulo.controller;

import com.transmdicas.neumaticos.modulo.model.*;
import com.transmdicas.neumaticos.modulo.repository.*;
import com.transmdicas.neumaticos.modulo.service.*;
import java.util.*;
import org.springframework.dao.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DocumentoController {
  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(DocumentoController.class);
  private final DocumentoService servicio;
  private final DocumentoRepository documentos;
  private final CatalogoRepository catalogos;
  private final DisposicionRepository disposiciones;

  public DocumentoController(
      DocumentoService servicio,
      DocumentoRepository documentos,
      CatalogoRepository catalogos,
      DisposicionRepository disposiciones) {
    this.servicio = servicio;
    this.documentos = documentos;
    this.catalogos = catalogos;
    this.disposiciones = disposiciones;
  }

  @InitBinder("documento")
  void binding(WebDataBinder b) {
    b.setAutoGrowCollectionLimit(20);
    b.setAllowedFields(
        "id",
        "version",
        "token",
        "tipo",
        "placa",
        "operador",
        "kilometraje",
        "fecha",
        "ot",
        "inicio",
        "fin",
        "rotaciones",
        "observaciones",
        "responsable1",
        "responsable2",
        "responsable3",
        "responsable4",
        "filas[*].posicion",
        "filas[*].codigo",
        "filas[*].rn",
        "filas[*].r1",
        "filas[*].r2",
        "filas[*].presion",
        "filas[*].aro",
        "filas[*].condicion",
        "filas[*].sinTapa",
        "filas[*].disposicion",
        "filas[*].codigoNuevo",
        "filas[*].n1",
        "filas[*].n2",
        "filas[*].cantidad",
        "filas[*].conformidad",
        "despues[*].codigo",
        "despues[*].rn",
        "despues[*].r1",
        "despues[*].r2",
        "despues[*].presion",
        "despues[*].aro",
        "despues[*].condicion",
        "despues[*].sinTapa");
  }

  @ModelAttribute("documento")
  DocumentoForm formulario() {
    return new DocumentoForm();
  }

  @GetMapping("/documentos")
  public String inicio(Model m) {
    m.addAttribute("documento", DocumentoForm.nuevo(TipoDocumento.INSPECCION));
    return editor(m);
  }

  @GetMapping("/documentos/nuevo")
  public String nuevo(@RequestParam(defaultValue = "INSPECCION") TipoDocumento tipo, Model m) {
    m.addAttribute("documento", DocumentoForm.nuevo(tipo));
    return editor(m);
  }

  @GetMapping("/documentos/{id}")
  public String abrir(@PathVariable Long id, Model m) {
    var d = servicio.abrir(id);
    if (d.isGenerado()) return "redirect:/documentos/" + id + "/vista";
    m.addAttribute("documento", d);
    return editor(m);
  }

  @PostMapping("/documentos/procesar")
  public String procesar(
      @ModelAttribute("documento") DocumentoForm d,
      BindingResult errores,
      @RequestParam(defaultValue = "actualizar") String accion,
      @RequestParam(required = false) Integer quitar,
      @RequestParam(required = false) String nuevaDisposicion,
      Model m,
      RedirectAttributes flash) {
    if (d.getTipo() == null) d.setTipo(TipoDocumento.INSPECCION);
    if (d.getFilas().size() > 20
        || d.getDespues().size() > 10
        || d.getFilas().contains(null)
        || d.getDespues().contains(null)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    // Hidden fields are identifiers only. Every update is checked against the stored version.
    if (d.getId() != null) {
      var original = servicio.abrir(d.getId());
      if (original.isGenerado()) return "redirect:/documentos/" + d.getId() + "/vista";
      if (original.getTipo() != d.getTipo() || !original.getToken().equals(d.getToken()))
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
    if (quitar != null
        && d.getTipo() != TipoDocumento.INSPECCION
        && quitar >= 0
        && quitar < d.getFilas().size()
        && d.getFilas().size() > 1) d.getFilas().remove((int) quitar);
    if ("agregar".equals(accion) && d.getTipo() != TipoDocumento.INSPECCION) {
      int max = d.getTipo() == TipoDocumento.CAMBIO ? 12 : 20;
      if (d.getFilas().size() < max) {
        var fila = new FilaDocumento();
        fila.setPosicion(d.getFilas().size() + 1);
        d.getFilas().add(fila);
      }
    }
    if ("agregarDisposicion".equals(accion) && d.getTipo() == TipoDocumento.CAMBIO) {
      try {
        disposiciones.agregar(nuevaDisposicion);
        m.addAttribute(
            "mensaje",
            "Disposición agregada a la lista. Guarda el borrador para conservar el documento.");
      } catch (IllegalArgumentException ex) {
        errores.reject("disposicion", ex.getMessage());
      } catch (DataIntegrityViolationException ex) {
        m.addAttribute("mensaje", "La disposición ya está disponible en la lista.");
      }
    }
    servicio.enriquecer(d);
    boolean generar = "generar".equals(accion), guardar = generar || "guardar".equals(accion);
    servicio.validar(d, errores, generar);
    if (!errores.hasErrors() && guardar) {
      Long idAnterior = d.getId();
      long versionAnterior = d.getVersion();
      try {
        Long id = servicio.guardar(d, generar);
        flash.addFlashAttribute(
            "mensaje", generar ? "Documento generado. Listo para imprimir." : "Borrador guardado.");
        return "redirect:/documentos/" + id + (generar ? "/vista" : "");
      } catch (OptimisticLockingFailureException | IllegalArgumentException ex) {
        errores.reject("guardar", ex.getMessage());
      } catch (DataAccessException ex) {
        d.setId(idAnterior);
        d.setVersion(versionAnterior);
        d.setEstado("BORRADOR");
        log.error("Error guardando documento", ex);
        errores.reject(
            "guardar",
            "No se pudo guardar. Los cambios de esta operación fueron revertidos; revisa la"
                + " terminal.");
      }
    }
    if ("vista".equals(accion) && !errores.hasErrors()) {
      return vistaDocumento(d, m, true);
    }
    if ("actualizar".equals(accion) && !errores.hasErrors())
      m.addAttribute(
          "mensaje",
          "Datos automáticos y promedios actualizados. Todavía no se han guardado estos cambios.");
    return editor(m);
  }

  private String editor(Model m) {
    m.addAttribute("tipos", TipoDocumento.values());
    m.addAttribute("disposiciones", disposiciones.listar());
    m.addAttribute("placas", catalogos.placas());
    m.addAttribute("nav", "documentos");
    m.addAttribute("totalDocumentos", documentos.contar(""));
    return "modulo/editor";
  }

  @GetMapping("/documentos/{id}/vista")
  public String vista(@PathVariable Long id, Model m) {
    return vistaDocumento(servicio.abrir(id), m, false);
  }

  private String vistaDocumento(DocumentoForm documento, Model m, boolean temporal) {
    m.addAttribute("documento", documento);
    m.addAttribute("observacionesNeumaticos", ObservacionesNeumaticos.secciones(documento));
    m.addAttribute("temporal", temporal);
    m.addAttribute("nav", temporal ? "documentos" : "registros");
    return "modulo/vista";
  }

  @PostMapping("/documentos/{id}/duplicar")
  public String duplicar(@PathVariable Long id, Model m) {
    var d = servicio.abrir(id);
    d.setId(null);
    d.setVersion(0);
    d.setToken(UUID.randomUUID().toString());
    d.setEstado("BORRADOR");
    servicio.enriquecer(d);
    m.addAttribute("documento", d);
    m.addAttribute("mensaje", "Copia nueva: pulsa Guardar borrador para conservarla.");
    return editor(m);
  }

  @GetMapping("/documentos/{id}/eliminar")
  public String confirmar(@PathVariable Long id, Model m) {
    m.addAttribute("documento", servicio.abrir(id));
    m.addAttribute("nav", "registros");
    return "modulo/eliminar";
  }

  @PostMapping("/documentos/{id}/eliminar")
  public String eliminar(@PathVariable Long id, @RequestParam long version, RedirectAttributes f) {
    try {
      servicio.eliminar(id, version);
      f.addFlashAttribute("mensaje", "Registro eliminado.");
    } catch (OptimisticLockingFailureException ex) {
      f.addFlashAttribute("mensaje", ex.getMessage());
    }
    return "redirect:/registros";
  }

  @GetMapping("/registros")
  public String registros(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int pagina,
      Model m) {
    pagina = Math.max(0, Math.min(pagina, 100000));
    long total = documentos.contar(q);
    pagina = (int) Math.min(pagina, Math.max(0, (total - 1) / 25));
    m.addAttribute("registros", documentos.listar(q, pagina));
    m.addAttribute("q", q);
    m.addAttribute("pagina", pagina);
    m.addAttribute("total", total);
    m.addAttribute("nav", "registros");
    return "modulo/registros";
  }
}
