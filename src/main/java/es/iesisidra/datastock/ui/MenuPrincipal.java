package es.iesisidra.datastock.ui;

import es.iesisidra.datastock.ficheros.Explorador;
import es.iesisidra.datastock.ficheros.Exportador;
import es.iesisidra.datastock.ficheros.Importador;
import es.iesisidra.datastock.ficheros.Serializacion;
import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.servicio.Inventario;
import es.iesisidra.datastock.util.ColoresConsola;
import es.iesisidra.datastock.util.LectorConsola;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Presentación de menús y navegación de la primera fase de DataStock.
 * <p>El menú pide los datos al usuario y llama a las clases del paquete {@code ficheros},
 * que son las que leen, escriben y validan.</p>
 */
public class MenuPrincipal {
    private final Inventario inventario;

    /** @param inventario colección común de productos de esta ejecución. */
    public MenuPrincipal(Inventario inventario) { this.inventario = inventario; }

    /** Ejecuta el menú principal hasta que el usuario selecciona salir. */
    public void iniciar() {
        int opcion;
        do {
            mostrarCabecera();
            System.out.println("1. Explorar ficheros y directorios");
            System.out.println("2. Importar productos desde JSON");
            System.out.println("3. Importar productos desde XML");
            System.out.println("4. Consultar productos");
            System.out.println("5. Exportar productos");
            System.out.println("6. Guardar y recuperar inventario");
            System.out.println("0. Salir");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 6);
            switch (opcion) {
                case 1 -> ejecutar(this::explorar);
                case 2 -> ejecutar(() -> importar(true));
                case 3 -> ejecutar(() -> importar(false));
                case 4 -> menuConsultas();
                case 5 -> menuExportaciones();
                case 6 -> menuSerializacion();
                case 0 -> System.out.println(ColoresConsola.pintar("Hasta pronto.", ColoresConsola.VERDE));
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    private void mostrarCabecera() {
        System.out.println(ColoresConsola.pintar("\n================================================", ColoresConsola.CIAN));
        System.out.println(ColoresConsola.pintar("         DATASTOCK - GESTIÓN DE INVENTARIO", ColoresConsola.NEGRITA));
        System.out.println(ColoresConsola.pintar("                 FASE 1 - UT1", ColoresConsola.CIAN));
        System.out.println(ColoresConsola.pintar("================================================", ColoresConsola.CIAN));
        System.out.println(ColoresConsola.pintar("Productos en memoria: " + inventario.getProductos().size(), ColoresConsola.GRIS));
    }

    private void menuConsultas() {
        int opcion;
        do {
            System.out.println("\n---------- CONSULTA DE PRODUCTOS ----------");
            System.out.println("1. Listar todos los productos");
            System.out.println("2. Buscar producto por código");
            System.out.println("3. Buscar productos por nombre");
            System.out.println("0. Volver al menú principal");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 3);
            switch (opcion) {
                case 1 -> ejecutar(this::listar);
                case 2 -> ejecutar(this::buscarPorCodigo);
                case 3 -> ejecutar(this::buscarPorNombre);
                case 0 -> { }
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    private void menuExportaciones() {
        int opcion;
        do {
            System.out.println("\n---------- EXPORTAR PRODUCTOS ----------");
            System.out.println("1. Exportar productos a JSON");
            System.out.println("2. Exportar productos a XML");
            System.out.println("0. Volver al menú principal");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 2);
            switch (opcion) {
                case 1 -> ejecutar(() -> exportar(true));
                case 2 -> ejecutar(() -> exportar(false));
                case 0 -> { }
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    private void menuSerializacion() {
        int opcion;
        do {
            System.out.println("\n---------- GUARDAR Y RECUPERAR INVENTARIO ----------");
            System.out.println("1. Guardar inventario en fichero .dat");
            System.out.println("2. Recuperar inventario desde fichero .dat");
            System.out.println("0. Volver al menú principal");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 2);
            switch (opcion) {
                case 1 -> ejecutar(this::guardar);
                case 2 -> ejecutar(this::recuperar);
                case 0 -> { }
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    /** Una operación del menú que puede fallar con cualquier excepción. */
    private interface Accion { void ejecutar() throws Exception; }

    /** Ejecuta la operación; si falla muestra el motivo y vuelve al menú en lugar de cerrar el programa. */
    private void ejecutar(Accion accion) {
        try {
            accion.ejecutar();
        } catch (Exception e) {
            System.out.println(ColoresConsola.pintar("ERROR: " + e.getMessage(), ColoresConsola.ROJO));
        }
        LectorConsola.pausar();
    }

    // ---------- Opción 1: explorar ----------
    private void explorar() throws IOException {
        System.out.println("\n===== EXPLORAR RUTA =====");
        for (String linea : Explorador.explorar(LectorConsola.leerTexto("Ruta: "))) System.out.println(linea);
    }

    // ---------- Opciones 2 y 3: importar ----------
    private void importar(boolean json) {
        System.out.println("\n===== IMPORTAR " + (json ? "JSON" : "XML") + " =====");
        File fichero = new File(LectorConsola.leerTexto("Ruta: ").trim());
        Importador.Resultado r = json ? Importador.importarJson(fichero, inventario) : Importador.importarXml(fichero, inventario);
        if (r.error != null) {
            System.out.println(ColoresConsola.pintar("ERROR: " + r.error, ColoresConsola.ROJO));
            System.out.println("Importación cancelada: el inventario no ha cambiado.");
            return;
        }
        System.out.println("Productos encontrados: " + r.encontrados);
        System.out.println("Productos importados: " + r.importados);
        System.out.println("Productos rechazados: " + (r.encontrados - r.importados));
        if (!r.incidencias.isEmpty()) System.out.println("Incidencias:");
        for (String incidencia : r.incidencias) System.out.println("- " + incidencia);
        System.out.println(r.incidencias.isEmpty() ? "Importación finalizada." : "Importación finalizada con incidencias.");
    }

    // ---------- Opción 4: consultas ----------
    private void listar() {
        if (inventario.getProductos().isEmpty()) {
            System.out.println("El inventario está vacío.");
            return;
        }
        System.out.printf("%-12s %-30s %10s %6s %5s %5s%n", "CÓDIGO", "NOMBRE", "PRECIO", "STOCK", "CAT.", "PROV.");
        for (Producto p : inventario.ordenados()) {
            System.out.printf("%-12s %-30s %10s %6d %5d %5d%n", p.getCodigo(), p.getNombre(),
                    p.getPrecio().toPlainString() + " €", p.getStock(), p.getCategoriaId(), p.getProveedorId());
        }
    }

    private void buscarPorCodigo() {
        String codigo = LectorConsola.leerTexto("Código: ");
        if (codigo.isBlank()) {
            System.out.println("Escribe un código.");
            return;
        }
        Producto p = inventario.buscarPorCodigo(codigo);
        if (p == null) System.out.println("No existe ningún producto con ese código.");
        else mostrar(p);
    }

    private void buscarPorNombre() {
        String texto = LectorConsola.leerTexto("Nombre (o parte): ");
        if (texto.isBlank()) {
            System.out.println("Escribe un texto.");
            return;
        }
        List<Producto> encontrados = inventario.buscarPorNombre(texto);
        if (encontrados.isEmpty()) System.out.println("No hay ningún producto con ese nombre.");
        for (Producto p : encontrados) {
            mostrar(p);
            System.out.println();
        }
    }

    private void mostrar(Producto p) {
        System.out.println("Código: " + p.getCodigo());
        System.out.println("Nombre: " + p.getNombre());
        System.out.println("Precio: " + p.getPrecio().toPlainString() + " € | Stock: " + p.getStock());
        System.out.println("Categoría: " + p.getCategoriaId() + " | Proveedor: " + p.getProveedorId());
    }

    // ---------- Opción 5: exportar ----------
    private void exportar(boolean json) throws Exception {
        if (inventario.getProductos().isEmpty()) {
            System.out.println("No hay productos que exportar.");
            return;
        }
        File destino = pedirDestino(LectorConsola.leerTexto("Ruta de destino: "));
        if (destino == null) return;
        if (json) Exportador.aJson(inventario.ordenados(), destino);
        else Exportador.aXml(inventario.ordenados(), destino);
        System.out.println("Fichero generado correctamente: " + destino.getAbsolutePath());
        System.out.println("Formato: " + (json ? "JSON" : "XML") + " | Productos exportados: " + inventario.getProductos().size());
    }

    /** Comprueba que el directorio existe y pide confirmación si el fichero ya existe. Devuelve null si se cancela. */
    private File pedirDestino(String ruta) throws IOException {
        if (ruta.isBlank()) throw new IOException("La ruta está vacía");
        File destino = new File(ruta.trim());
        File directorio = destino.getAbsoluteFile().getParentFile();
        if (destino.isDirectory() || directorio == null || !directorio.isDirectory()) {
            throw new IOException("El directorio de destino no existe o la ruta es un directorio");
        }
        if (destino.exists() && !LectorConsola.confirmar("El fichero ya existe. ¿Sobrescribir?")) {
            System.out.println("Operación cancelada: el fichero original no se ha modificado.");
            return null;
        }
        return destino;
    }

    // ---------- Opción 6: serialización ----------
    private void guardar() throws IOException {
        if (inventario.getProductos().isEmpty()) {
            System.out.println("No hay productos que guardar: no se crea ningún fichero.");
            return;
        }
        String ruta = LectorConsola.leerTexto("Ruta de destino (.dat): ").trim();
        if (!ruta.isEmpty() && !ruta.endsWith(".dat")) ruta += ".dat";
        File destino = pedirDestino(ruta);
        if (destino == null) return;
        Serializacion.guardar(inventario.ordenados(), destino);
        System.out.println("Inventario guardado. Productos guardados: " + inventario.getProductos().size());
    }

    private void recuperar() throws IOException {
        System.out.println("Recupera solo ficheros .dat generados por DataStock.");
        List<Producto> recuperados = Serializacion.recuperar(new File(LectorConsola.leerTexto("Ruta: ").trim()));
        if (!inventario.getProductos().isEmpty()) {
            System.out.println("Actualmente hay " + inventario.getProductos().size() + " productos en memoria.");
            System.out.println("La recuperación sustituirá el inventario actual.");
            if (!LectorConsola.confirmar("¿Continuar?")) {
                System.out.println("Operación cancelada: se conserva el inventario actual.");
                return;
            }
        }
        inventario.sustituir(recuperados);
        System.out.println("Inventario recuperado correctamente. Productos disponibles: " + recuperados.size());
    }
}
