package com.transmdicas.neumaticos.modulo.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice(basePackages = "com.transmdicas.neumaticos.modulo")
public class ModeloGlobal {
  @ModelAttribute
  public void comunes(HttpSession sesion, Model model) {
    model.addAttribute("csrf", WebConfig.token(sesion));
  }
}
