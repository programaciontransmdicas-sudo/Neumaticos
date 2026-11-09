package com.transmdicas.neumaticos.modulo.controller;

import com.transmdicas.neumaticos.modulo.model.*;
import com.transmdicas.neumaticos.modulo.repository.SalidaAlmacenRepository;
import com.transmdicas.neumaticos.modulo.service.IndicadorSalidasService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class IndicadorSalidasController {
  private final IndicadorSalidasService servicio;
  private final SalidaAlmacenRepository repo;

  public IndicadorSalidasController(
      IndicadorSalidasService servicio, SalidaAlmacenRepository repo) {
    this.servicio = servicio;
    this.repo = repo;
  }

  @ModelAttribute("salida")
  public SalidaForm form() {
    return new SalidaForm();
  }

  @InitBinder("salida")
  public void bind(WebDataBinder b) {
    b.setAllowedFields("token", "fecha", "codigo", "cantidad", "placa", "observaciones");
  }

  @GetMapping("/reportes/salidas")
  public String ver(
      @RequestParam(required = false) Integer anio,
      @RequestParam(defaultValue = "MENSUAL") PeriodoSalidas periodo,
      @RequestParam(defaultValue = "") String marca,
      @RequestParam(defaultValue = "") String modelo,
      Model m) {
    return preparar(anio, periodo, marca, modelo, m);
  }

  private String preparar(
      Integer anio, PeriodoSalidas periodo, String marca, String modelo, Model m) {
    if (anio == null) anio = LocalDate.now(ZoneId.of("America/Lima")).getYear();
    if (anio < 1900 || anio > 2200)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "Año inválido");
    m.addAttribute("indicadores", servicio.calcular(anio, periodo, marca, modelo));
    m.addAttribute("anio", anio);
    m.addAttribute("periodo", periodo);
    m.addAttribute("periodos", PeriodoSalidas.values());
    m.addAttribute("marca", marca);
    m.addAttribute("modelo", modelo);
    m.addAttribute("marcas", repo.marcas());
    m.addAttribute("modelos", repo.modelos());
    Set<Integer> anios = new TreeSet<>(Comparator.reverseOrder());
    anios.add(anio);
    anios.addAll(repo.anios());
    m.addAttribute("anios", anios);
    m.addAttribute(
        "coberturaExcel",
        repo.coberturaExcel()
            .map(
                f ->
                    f[0].format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + " al "
                        + f[1].format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")))
            .orElse("Sin histórico importado"));
    m.addAttribute("nav", "reportes");
    return "modulo/salidas";
  }

  @PostMapping("/salidas/registrar")
  public String registrar(
      @ModelAttribute("salida") SalidaForm f,
      BindingResult errores,
      Model m,
      RedirectAttributes flash) {
    if (!errores.hasErrors())
      try {
        servicio.registrar(f);
        flash.addFlashAttribute("mensaje", "Salida de almacén registrada.");
        return "redirect:/reportes/salidas?anio=" + LocalDate.parse(f.getFecha()).getYear();
      } catch (IllegalArgumentException ex) {
        errores.reject("salida", ex.getMessage());
      }
    if (errores.hasFieldErrors("cantidad"))
      errores.reject("cantidad", "Ingresa una cantidad entera.");
    m.addAttribute("abrirSalida", true);
    return preparar(null, PeriodoSalidas.MENSUAL, "", "", m);
  }

  @GetMapping("/reportes/salidas.csv")
  public void csv(
      @RequestParam int anio,
      @RequestParam(defaultValue = "MENSUAL") PeriodoSalidas periodo,
      @RequestParam(defaultValue = "") String marca,
      @RequestParam(defaultValue = "") String modelo,
      HttpServletResponse res)
      throws IOException {
    if (anio < 1900 || anio > 2200)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST);
    var datos = servicio.calcular(anio, periodo, marca, modelo);
    res.setContentType("text/csv;charset=UTF-8");
    res.setHeader("Content-Disposition", "attachment; filename=salidas-almacen.csv");
    var w = res.getWriter();
    w.write('\uFEFF');
    linea(
        w,
        "Año",
        "Agrupación",
        "Marca",
        "Modelo",
        "Periodo",
        "Desde",
        "Hasta",
        "Neumáticos",
        "Movimientos");
    for (var f : datos.periodos())
      linea(
          w,
          anio,
          periodo.getTitulo(),
          marca.isBlank() ? "Todas" : marca,
          modelo.isBlank() ? "Todos" : modelo,
          f.etiqueta(),
          f.desde(),
          f.hasta(),
          f.unidades(),
          f.movimientos());
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
