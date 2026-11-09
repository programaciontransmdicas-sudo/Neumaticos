package com.transmdicas.neumaticos.modulo.controller;

import com.transmdicas.neumaticos.modulo.repository.CatalogoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class CatalogoController {
  private final CatalogoRepository repo;

  public CatalogoController(CatalogoRepository repo) {
    this.repo = repo;
  }

  @GetMapping("/catalogos/{tipo}")
  public String catalogo(
      @PathVariable String tipo,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int pagina,
      Model m) {
    if (!tipo.equals("flota") && !tipo.equals("neumaticos"))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    boolean flota = tipo.equals("flota");
    long total = repo.contar(flota, q);
    pagina = (int) Math.min(Math.max(0, pagina), Math.max(0, (total - 1) / 25));
    m.addAttribute("datos", flota ? repo.flota(q, pagina) : repo.neumaticos(q, pagina));
    m.addAttribute("total", total);
    m.addAttribute("pagina", pagina);
    m.addAttribute("q", q);
    m.addAttribute("flota", flota);
    m.addAttribute("nav", tipo);
    return "modulo/catalogo";
  }

  @GetMapping("/reglas")
  public String reglas(Model m) {
    m.addAttribute("nav", "reglas");
    return "modulo/reglas";
  }
}
