package com.transmdicas.neumaticos.modulo.config;

import com.transmdicas.neumaticos.modulo.repository.OperadorRepository;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@org.springframework.core.annotation.Order(12)
@ConditionalOnProperty(name = "modulo.catalogos.importar", havingValue = "true", matchIfMissing = true)
public class OperadoresInitializer implements ApplicationRunner {
  private final JdbcTemplate jdbc;

  public OperadoresInitializer(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void run(ApplicationArguments args) throws Exception {
    try (var lector = new BufferedReader(new InputStreamReader(
        new ClassPathResource("catalogos/operadores.txt").getInputStream(), StandardCharsets.UTF_8))) {
      for (String nombre : lector.lines().filter(n -> !n.isBlank()).toList()) {
        jdbc.update(
            "INSERT INTO neumaticos.operadores(busqueda,nombre) VALUES (?,?)"
                + " ON CONFLICT DO NOTHING",
            OperadorRepository.normalizar(nombre), nombre.strip());
      }
    }
  }
}
