# Preparación de despliegue

Este proyecto usa Spring Boot y Thymeleaf. Render ejecuta la aplicación completa con Docker; LibreOffice convierte documentos a PDF. El archivo original se ha conservado.

El perfil `render` desactiva el SQL automático y las importaciones al arrancar. Es necesario que la base de datos de pruebas ya tenga las tablas y catálogos requeridos. Utiliza una base de pruebas separada, sin datos reales. El proyecto no implementa autenticación y una URL pública permite acceder a sus funcionalidades; no lo publiques con datos reales hasta incorporar control de acceso.

En Render, crea un Web Service Docker apuntando al repositorio y define `DB_URL` como JDBC (`jdbc:postgresql://.../neondb?sslmode=require`), `DB_USERNAME` y `DB_PASSWORD` mediante las variables privadas del servicio. No subas contraseñas al repositorio. Render proporciona `PORT`. `SPRING_PROFILES_ACTIVE=render` y `LIBREOFFICE_PATH=/usr/bin/libreoffice` se definen en el contenedor.

Para usar Vercel como puerta de entrada, crea un proyecto independiente con un archivo `vercel.json` y cambia el dominio del ejemplo por la URL real de Render:

```json
{"rewrites": [{"source": "/:path*", "destination": "https://TU-SERVICIO.onrender.com/:path*"}]}
```

Comprueba formularios, sesiones, cookies y descarga de archivos a través de ambos dominios. Un rewrite no protege la URL directa de Render. No incluyas el dominio de ejemplo literalmente.

La revisión del PDF anterior sigue pendiente. En este proyecto, el auditado de requerimiento todavía devuelve Excel y no tiene botón PDF. Los PDF deben validarse visualmente antes de usarse como formatos oficiales. La conversión entre Excel y LibreOffice puede cambiar páginas o fuentes.

No se ha compilado ni ejecutado este contenedor en este entorno. La imagen Java 25, la instalación de LibreOffice y la disponibilidad de memoria deben verificarse durante el primer despliegue de prueba. La familia de fuentes Carlito/Liberation sustituye Arial en el contenedor, por lo que no garantiza Arial exacta.
