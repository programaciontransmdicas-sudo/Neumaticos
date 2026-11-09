package com.transmdicas.neumaticos.modulo.service;

import com.transmdicas.neumaticos.modulo.model.CatalogoForm;
import com.transmdicas.neumaticos.modulo.repository.MantenimientoCatalogoRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MantenimientoCatalogoService {
  private final MantenimientoCatalogoRepository repo;

  public MantenimientoCatalogoService(MantenimientoCatalogoRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public void guardar(boolean flota, CatalogoForm f) {
    if (flota) f.setClave(f.getClave().strip().toUpperCase(Locale.ROOT));
    else f.setClave(f.getClave().strip());
    if (f.getClave().isBlank() || f.getClave().length() > (flota ? 10 : 100))
      throw new IllegalArgumentException(
          flota
              ? "La placa es obligatoria y admite hasta 10 caracteres."
              : "El código es obligatorio y admite hasta 100 caracteres.");
    if (!f.getOriginal().isBlank() && !f.getOriginal().equals(f.getClave()))
      throw new IllegalArgumentException(
          "El identificador se conserva al editar. Crea un registro nuevo si necesitas otro código"
              + " o placa.");
    if (f.getMarca().isBlank() || f.getMarca().length() > 100)
      throw new IllegalArgumentException("La marca es obligatoria y admite hasta 100 caracteres.");
    if (f.getModelo().length() > 250
        || f.getMedida().length() > 100
        || f.getOperacion().length() > 150
        || f.getRuta().length() > 200
        || f.getSemirremolque().length() > 250
        || f.getReencauche1().length() > 250
        || f.getReencauche2().length() > 250)
      throw new IllegalArgumentException("Revisa la longitud de las características ingresadas.");
    if (flota && (f.getOperacion().isBlank() || f.getRuta().isBlank()))
      throw new IllegalArgumentException("Completa operación y ruta de la unidad.");
    if (!flota && (f.getModelo().isBlank() || f.getMedida().isBlank()))
      throw new IllegalArgumentException("Completa diseño/modelo y medida del neumático.");
    if (flota && !List.of("", "tracto", "remolque", "otro").contains(f.getTipoUnidad()))
      throw new IllegalArgumentException("Selecciona un tipo de unidad válido.");
    f.setMarca(f.getMarca().strip());
    f.setModelo(f.getModelo().strip());
    f.setMedida(f.getMedida().strip());
    f.setOperacion(f.getOperacion().strip());
    f.setRuta(f.getRuta().strip());
    f.setSemirremolque(f.getSemirremolque().strip());
    f.setReencauche1(f.getReencauche1().strip());
    f.setReencauche2(f.getReencauche2().strip());
    repo.guardar(flota, f);
  }
}
