package com.transmdicas.neumaticos.modulo.controller;

import com.transmdicas.neumaticos.modulo.model.*;
import com.transmdicas.neumaticos.modulo.repository.CatalogoRepository;
import com.transmdicas.neumaticos.modulo.repository.OperadorRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalogos")
public class BusquedaCatalogosController {
  private final CatalogoRepository catalogos;
  private final OperadorRepository operadores;

  public BusquedaCatalogosController(CatalogoRepository catalogos, OperadorRepository operadores) {
    this.catalogos = catalogos;
    this.operadores = operadores;
  }

  @GetMapping("/flota")
  public ResponseEntity<List<UnidadCatalogo>> flota(@RequestParam(defaultValue = "") String q) {
    String busqueda = termino(q);
    var opciones = new ArrayList<UnidadCatalogo>();
    catalogos.unidad(busqueda.toUpperCase(java.util.Locale.ROOT)).ifPresent(opciones::add);
    for (var unidad : catalogos.flota(busqueda, 0)) {
      if (opciones.stream().noneMatch(u -> Objects.equals(u.placa(), unidad.placa()))) {
        opciones.add(unidad);
      }
    }
    return respuesta(opciones);
  }

  @GetMapping("/neumaticos")
  public ResponseEntity<List<NeumaticoCatalogo>> neumaticos(
      @RequestParam(defaultValue = "") String q) {
    String busqueda = termino(q);
    var opciones = new ArrayList<NeumaticoCatalogo>();
    catalogos.neumatico(busqueda).ifPresent(opciones::add);
    for (var neumatico : catalogos.neumaticos(busqueda, 0)) {
      if (opciones.stream().noneMatch(n -> Objects.equals(n.codigo(), neumatico.codigo()))) {
        opciones.add(neumatico);
      }
    }
    return respuesta(opciones);
  }

  @GetMapping("/operadores")
  public ResponseEntity<List<String>> operadores(@RequestParam(defaultValue = "") String q) {
    return respuesta(operadores.buscar(termino(q)));
  }

  private static String termino(String texto) {
    String valor = texto.strip();
    return valor.substring(0, Math.min(valor.length(), 250));
  }

  private static <T> ResponseEntity<List<T>> respuesta(List<T> opciones) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(opciones);
  }
}
