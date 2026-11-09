package com.transmdicas.neumaticos.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UnidadController {
  @GetMapping({"/unidades", "/unidades/nueva"})
  public String inicio() {
    return "redirect:/catalogos/flota";
  }
}
