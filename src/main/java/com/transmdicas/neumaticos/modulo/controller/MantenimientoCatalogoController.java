package com.transmdicas.neumaticos.modulo.controller;

import com.transmdicas.neumaticos.modulo.model.CatalogoForm;
import com.transmdicas.neumaticos.modulo.repository.MantenimientoCatalogoRepository;
import com.transmdicas.neumaticos.modulo.service.MantenimientoCatalogoService;
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
public class MantenimientoCatalogoController {
  private final MantenimientoCatalogoRepository repo;
  private final MantenimientoCatalogoService servicio;

  public MantenimientoCatalogoController(
      MantenimientoCatalogoRepository repo, MantenimientoCatalogoService servicio) {
    this.repo = repo;
    this.servicio = servicio;
  }

  @InitBinder("catalogoForm")
  public void bind(WebDataBinder b) {
    b.setAllowedFields(
        "original",
        "clave",
        "revision",
        "marca",
        "modelo",
        "medida",
        "operacion",
        "ruta",
        "semirremolque",
        "tipoUnidad",
        "reencauche1",
        "reencauche2",
        "requiereRevision");
  }

  private boolean flota(String tipo) {
    if (!tipo.equals("flota") && !tipo.equals("neumaticos"))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return tipo.equals("flota");
  }

  private String vista(String tipo, Model m) {
    m.addAttribute("flota", flota(tipo));
    m.addAttribute("tipoCatalogo", tipo);
    m.addAttribute("nav", tipo);
    return "modulo/catalogo-form";
  }

  @GetMapping("/catalogos/{tipo}/nuevo")
  public String nuevo(@PathVariable String tipo, Model m) {
    m.addAttribute("catalogoForm", new CatalogoForm());
    return vista(tipo, m);
  }

  @GetMapping("/catalogos/{tipo}/editar")
  public String editar(@PathVariable String tipo, @RequestParam String clave, Model m) {
    m.addAttribute(
        "catalogoForm",
        repo.buscar(flota(tipo), clave)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    return vista(tipo, m);
  }

  @PostMapping("/catalogos/{tipo}/guardar")
  public String guardar(
      @PathVariable String tipo,
      @ModelAttribute("catalogoForm") CatalogoForm f,
      BindingResult errores,
      Model m,
      RedirectAttributes flash) {
    boolean esFlota = flota(tipo);
    if (!errores.hasErrors())
      try {
        servicio.guardar(esFlota, f);
        flash.addFlashAttribute(
            "mensaje",
            esFlota ? "Unidad guardada en el catálogo." : "Neumático guardado en el catálogo.");
        flash.addAttribute("q", f.getClave());
        return "redirect:/catalogos/" + tipo;
      } catch (IllegalArgumentException | OptimisticLockingFailureException ex) {
        errores.reject("guardar", ex.getMessage());
      } catch (DataIntegrityViolationException ex) {
        errores.reject(
            "duplicado",
            "Ya existe un registro con ese código o placa. Abre el registro existente para"
                + " editarlo.");
      }
    return vista(tipo, m);
  }
}
