package com.transmdicas.neumaticos.modulo.config;

import jakarta.servlet.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  public static String token(HttpSession s) {
    synchronized (s) {
      String t = (String) s.getAttribute("moduloCsrf");
      if (t == null) {
        t = UUID.randomUUID().toString();
        s.setAttribute("moduloCsrf", t);
      }
      return t;
    }
  }

  @Override
  public void addInterceptors(InterceptorRegistry r) {
    r.addInterceptor(
            new HandlerInterceptor() {
              @Override
              public boolean preHandle(
                  HttpServletRequest req, HttpServletResponse res, Object handler)
                  throws Exception {
                res.setHeader("X-Content-Type-Options", "nosniff");
                res.setHeader("Referrer-Policy", "same-origin");
                res.setHeader("X-Frame-Options", "SAMEORIGIN");
                if ("POST".equals(req.getMethod())) {
                  String esperado = token(req.getSession()), recibido = req.getParameter("_csrf");
                  if (recibido == null
                      || !MessageDigest.isEqual(
                          esperado.getBytes(StandardCharsets.UTF_8),
                          recibido.getBytes(StandardCharsets.UTF_8))) {
                    res.sendError(403);
                    return false;
                  }
                }
                return true;
              }
            })
        .addPathPatterns("/documentos/**", "/salidas/**", "/catalogos/**");
  }
}
