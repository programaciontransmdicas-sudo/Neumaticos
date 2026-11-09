# Verificación de esta actualización

Fecha: 2 de noviembre de 2026.

- 20 pruebas automáticas aprobadas en H2, sin fallos ni errores.
- Las mismas 20 pruebas aprobadas con el driver JDBC PostgreSQL contra PostgreSQL 18.6 mediante PGlite.
- Compilación con Java 17 y `-Djava.version=17`. Se mantiene la configuración Java 25 de tu proyecto para el JDK 26 que ya utilizas; el pom no se modifica.
- Comprobación de fronteras del promedio: 0, 6, 6,01, 6,5, 8,99 y 9 mm.
- Verificación de ausencia de OT en Inspección, posición única del técnico, nuevo cargo y creación persistente de disposiciones.
- Altas y ediciones de ambos catálogos, duplicados y conflictos de versión; conservación de los datos históricos.
- Cortes de quincena, mes y semestre, cambio de año, febrero bisiesto, periodos vacíos, filtros y suma de cantidades.
- Importación real del archivo histórico: 2.122 movimientos y 2.122 unidades.
- Reconciliación del filtro Transtone / TT768: 314 unidades, contrastadas con el Excel.
- Prueba HTTP de formularios de alta/edición, alertas, disposiciones, indicadores, registro de salidas y descarga CSV.
- Actualización desde el esquema anterior conservando un documento generado y un catálogo previo.
- Reinicio sin duplicar salidas ni catálogos. Una salida manual de tres unidades incrementó el total a 2.125 y se mantuvo al reiniciar.

Las verificaciones no utilizaron tu Neon ni tus credenciales. La ejecución del instalador y el aspecto de impresión en Windows deben comprobarse en tu equipo. Las vistas se comprobaron funcionalmente mediante HTTP.

El código oficial del Requerimiento permanece por confirmar, como en la versión anterior. Las alertas usan los rangos que confirmaste para el promedio; se actualizan al enviar el formulario a Java.

## Exportación Excel

Fecha del cambio · 4 de noviembre de 2026.

- Se verificaron 28 casos de clasificación, incluidas fronteras, decimales y datos vacíos.
- Se generaron y abrieron tres archivos XLSX de prueba para inspección, rotación y cambio. Se comprobaron el encabezado combinado, las doce posiciones, la posición 22, las mediciones numéricas y la conservación de las observaciones manuales.
- La verificación ejecutó el exportador Java de forma aislada. Las pruebas anteriores corresponden a la actualización del 2 de noviembre.

## Observaciones en la vista imprimible

- Se revisaron las plantillas de vista y de impresión. El encabezado y los tres renglones manuales ocupan doce columnas. La celda del promedio conserva el valor numérico.
- Se ejecutaron los 28 casos de clasificación, la agrupación por posición y las tres exportaciones Excel con la misma lógica que alimenta la vista imprimible.
- La revisión de esta versión no incluyó arrancar Spring Boot ni generar un PDF desde el navegador.

## Búsquedas y formatos del 5 de noviembre de 2026

- Compilación y ejecución con Java 17 para la verificación local. Se conserva el proyecto configurado para Java 25 y su uso con JDK 26.
- 22 pruebas automáticas aprobadas con Spring Boot y H2. Incluyen las consultas HTTP de los tres buscadores, el formulario, las vistas y la descarga de los tres tipos de documento.
- Importación real de los 188 nombres del archivo recibido en H2. Una segunda importación conserva 188 registros.
- Ejecución del JavaScript sobre las páginas servidas por Spring Boot mediante jsdom. Se comprobaron selección con mouse y teclado, actualización inmediata de campos, ambos promedios, ausencia de envío al pulsar Enter, rotaciones y conservación de posiciones al enviar. También se comprobaron búsquedas sin resultados, respuestas atrasadas y reintentos después de un error de red.
- Apertura de cuatro archivos XLSX generados por Java con openpyxl, comprobación de celdas combinadas, imágenes de posiciones, mediciones, observaciones y firmas.
- Comparación visual de la distribución reconstruida con las tres páginas del PDF original. La apariencia final en Microsoft Excel y la impresión física deben comprobarse en Windows.
