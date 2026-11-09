(() => {
  "use strict";
  const form = document.querySelector("form.editor");
  if (!form) return;
  const base = form.dataset.catalogos;
  let abierto = null;
  let secuencia = 0;

  function escribir(campo, texto) {
    if (!campo) return;
    if (campo.matches("input")) campo.value = texto || "";
    else campo.textContent = texto || "—";
  }

  function completar(input, opcion) {
    if (input.dataset.busqueda === "flota") {
      for (const propiedad of ["marca", "operacion", "ruta", "semirremolque"]) {
        const nombre = propiedad === "marca" ? "marcaUnidad" : propiedad;
        escribir(form.querySelector('[data-unidad="' + nombre + '"]'), opcion?.[propiedad]);
      }
      escribir(form.querySelector("[data-placa-resumen]"), input.value);
    } else if (input.dataset.busqueda === "neumaticos") {
      const fila = input.closest("tr");
      const montado = input.name.endsWith(".codigoNuevo");
      for (const propiedad of ["marca", "diseno", "medida"]) {
        const nombre = montado
          ? { marca: "marcaNueva", diseno: "disenoNuevo", medida: "medidaNueva" }[propiedad]
          : propiedad;
        escribir(fila.querySelector('[data-auto="' + nombre + '"]'), opcion?.[propiedad]);
      }
    }
  }

  function cerrar() {
    if (abierto) abierto.cerrar();
    abierto = null;
  }

  for (const input of form.querySelectorAll("[data-busqueda]")) {
    const menu = document.createElement("div");
    const estado = document.createElement("small");
    menu.id = "sugerencias-" + (++secuencia);
    menu.className = "catalogo-sugerencias";
    menu.role = "listbox";
    menu.hidden = true;
    document.body.append(menu);
    estado.className = "catalogo-estado";
    estado.setAttribute("aria-live", "polite");
    input.after(estado);
    input.setAttribute("role", "combobox");
    input.setAttribute("aria-autocomplete", "list");
    input.setAttribute("aria-controls", menu.id);
    input.setAttribute("aria-expanded", "false");
    input.autocomplete = "off";
    let controlador = null;
    let temporizador = null;
    let version = 0;
    let opciones = [];
    let activo = -1;

    function cerrarMenu() {
      menu.hidden = true;
      input.setAttribute("aria-expanded", "false");
      input.removeAttribute("aria-activedescendant");
      activo = -1;
    }

    function posicionar() {
      const rect = input.getBoundingClientRect();
      menu.style.left = Math.max(8, Math.min(rect.left, window.innerWidth - 288)) + "px";
      menu.style.width = Math.min(Math.max(rect.width, 280), window.innerWidth - 16) + "px";
      const abajo = window.innerHeight - rect.bottom - 8;
      const altura = Math.min(260, Math.max(abajo, rect.top - 8));
      menu.style.maxHeight = altura + "px";
      menu.style.top = (abajo >= 160 ? rect.bottom + 4 : Math.max(8, rect.top - altura - 4)) + "px";
    }

    function valor(opcion) {
      if (typeof opcion === "string") return opcion;
      return input.dataset.busqueda === "flota" ? opcion.placa : opcion.codigo;
    }

    function seleccionar(opcion) {
      clearTimeout(temporizador);
      version++;
      controlador?.abort();
      input.value = valor(opcion);
      completar(input, opcion);
      actualizarResumen();
      estado.textContent = opcion.requiereRevision
        ? "Revisa los datos de este catálogo antes de generar."
        : "";
      cerrar();
      input.focus({ preventScroll: true });
    }

    async function consultar() {
      const turno = ++version;
      controlador?.abort();
      controlador = new AbortController();
      const consulta = input.value.trim();
      estado.textContent = "Buscando…";
      cerrarMenu();
      try {
        const url = new URL(base + "/" + input.dataset.busqueda, window.location.origin);
        url.searchParams.set("q", consulta);
        const respuesta = await fetch(url, {
          signal: controlador.signal,
          headers: { Accept: "application/json" },
          cache: "no-store"
        });
        if (!respuesta.ok) throw new Error("consulta");
        const datos = await respuesta.json();
        if (turno !== version || input.value.trim() !== consulta || document.activeElement !== input) return;
        opciones = datos;
        estado.textContent = opciones.length ? "" : "Sin coincidencias.";
        const exacto = opciones.find(o => valor(o).toUpperCase() === consulta.toUpperCase());
        if (exacto) completar(input, exacto);
        menu.replaceChildren();
        opciones.forEach((opcion, indice) => {
          const item = document.createElement("div");
          item.id = menu.id + "-" + indice;
          item.role = "option";
          item.setAttribute("aria-selected", "false");
          const titulo = document.createElement("strong");
          titulo.textContent = valor(opcion);
          item.append(titulo);
          if (typeof opcion !== "string") {
            const detalle = document.createElement("small");
            detalle.textContent = input.dataset.busqueda === "flota"
              ? [opcion.marca, opcion.operacion, opcion.ruta].filter(Boolean).join(" · ")
              : [opcion.marca, opcion.diseno, opcion.medida].filter(Boolean).join(" · ");
            item.append(detalle);
          }
          item.addEventListener("pointerdown", e => e.preventDefault());
          item.addEventListener("click", () => seleccionar(opcion));
          menu.append(item);
        });
        if (opciones.length) {
          cerrar();
          abierto = { cerrar: cerrarMenu, posicionar };
          posicionar();
          menu.hidden = false;
          input.setAttribute("aria-expanded", "true");
        }
      } catch (error) {
        if (error.name === "AbortError" || turno !== version) return;
        estado.textContent = "No se pudo consultar. Vuelve a escribir para reintentar.";
      }
    }

    input.addEventListener("input", () => {
      clearTimeout(temporizador);
      version++;
      controlador?.abort();
      completar(input, null);
      cerrarMenu();
      estado.textContent = "";
      temporizador = setTimeout(consultar, 200);
    });
    input.addEventListener("focus", () => {
      cerrar();
      clearTimeout(temporizador);
      temporizador = setTimeout(consultar, 0);
    });
    input.addEventListener("blur", () => {
      clearTimeout(temporizador);
      version++;
      controlador?.abort();
      cerrarMenu();
    });
    input.addEventListener("keydown", event => {
      if (event.isComposing) return;
      if (event.key === "Escape") {
        clearTimeout(temporizador);
        version++;
        controlador?.abort();
        cerrar();
      }
      if (event.key === "Enter") {
        event.preventDefault();
        if (!menu.hidden && activo >= 0) seleccionar(opciones[activo]);
      }
      if ((event.key === "ArrowDown" || event.key === "ArrowUp") && !menu.hidden) {
        event.preventDefault();
        activo = activo < 0
          ? (event.key === "ArrowDown" ? 0 : opciones.length - 1)
          : (activo + (event.key === "ArrowDown" ? 1 : -1) + opciones.length) % opciones.length;
        [...menu.children].forEach((item, i) => item.setAttribute("aria-selected", String(i === activo)));
        const item = menu.children[activo];
        input.setAttribute("aria-activedescendant", item.id);
        item.scrollIntoView({ block: "nearest" });
      }
    });
  }

  document.addEventListener("pointerdown", event => {
    if (!event.target.closest("[data-busqueda], .catalogo-sugerencias")) {
      cerrar();
      if (document.activeElement?.matches("[data-busqueda]")) document.activeElement.blur();
    }
  });
  window.addEventListener("resize", () => abierto?.posicionar());
  window.addEventListener("scroll", () => abierto?.posicionar(), true);
  form.addEventListener("keydown", event => {
    if (event.key === "Enter" && !event.isComposing
        && event.target.matches("input, select")) event.preventDefault();
  });

  function promedios() {
    for (const fila of form.querySelectorAll(".entry-table tbody tr")) {
      for (const par of [["r1", "r2", "promedio"], ["n1", "n2", "promedioNuevo"]]) {
        const a = fila.querySelector('input[name$=".' + par[0] + '"]');
        const b = fila.querySelector('input[name$=".' + par[1] + '"]');
        const salida = fila.querySelector('[data-auto="' + par[2] + '"]');
        if (!salida) continue;
        salida.value = a?.value !== "" && b?.value !== "" && a?.validity.valid && b?.validity.valid
          ? ((Math.round(Number(a.value) * 100) + Math.round(Number(b.value) * 100)) / 200).toFixed(2)
          : "—";
      }
    }
  }
  function actualizarResumen() {
    const filas = [...form.querySelectorAll('tr[id^="fila-principal-"]')];
    const usadas = filas.filter(fila => [...fila.querySelectorAll("input, select")].some(campo =>
      !campo.name.endsWith(".posicion") && campo.value.trim() !== ""
    )).length;
    escribir(form.querySelector("[data-total-neumaticos]"), String(usadas));
  }
  form.addEventListener("input", () => { promedios(); actualizarResumen(); });
  promedios();
  actualizarResumen();

  const rotaciones = form.querySelector('[name="rotaciones"][type="checkbox"]');
  const bloque = form.querySelector("[data-rotaciones]");
  if (rotaciones && bloque) {
    rotaciones.addEventListener("change", () => { bloque.hidden = !rotaciones.checked; });
  }
  for (const boton of form.querySelectorAll("[data-actualizar]")) boton.hidden = true;

  const clave = "neumaticos-posicion-" + form.querySelector('[name="token"]').value;
  form.addEventListener("submit", event => {
    if (event.submitter?.value === "generar" || event.submitter?.value === "vista") return;
    try {
      sessionStorage.setItem(clave, JSON.stringify({
        y: window.scrollY, x: window.scrollX,
        tablas: [...form.querySelectorAll(".table-wrap")].map(tabla => tabla.scrollLeft)
      }));
    } catch {}
  });
  try {
    const posicion = sessionStorage.getItem(clave);
    sessionStorage.removeItem(clave);
    if (posicion) requestAnimationFrame(() => {
      const { x, y, tablas } = JSON.parse(posicion);
      [...form.querySelectorAll(".table-wrap")].forEach((tabla, i) => {
        tabla.scrollLeft = tablas?.[i] || 0;
      });
      window.scrollTo(x, y);
    });
  } catch {}
})();
