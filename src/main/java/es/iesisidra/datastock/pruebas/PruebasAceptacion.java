package es.iesisidra.datastock.pruebas;

import com.google.gson.Gson;
import es.iesisidra.datastock.App;
import es.iesisidra.datastock.complementarias.XPathActividad;
import es.iesisidra.datastock.ficheros.Explorador;
import es.iesisidra.datastock.ficheros.Exportador;
import es.iesisidra.datastock.ficheros.Importador;
import es.iesisidra.datastock.ficheros.Serializacion;
import es.iesisidra.datastock.ficheros.StockBin;
import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.servicio.Inventario;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Pruebas de aceptación del enunciado (RA1-g). Se ejecuta desde la raíz del proyecto y escribe
 * por pantalla la tabla del documento de pruebas en formato Markdown.
 * Las pruebas que dependen del menú (confirmaciones S/N, mensajes) lanzan el programa real
 * en otro proceso con el teclado simulado.
 */
public class PruebasAceptacion {
    private static final String D = "datos/pruebas/";
    private static Path tmp;
    private static int fallos = 0;

    public static void main(String[] args) throws Exception {
        tmp = Files.createTempDirectory("datastock");
        System.out.println("| ID | Datos utilizados | Resultado esperado | Resultado obtenido | Estado |");
        System.out.println("|---|---|---|---|---|");

        // ---- RF-FIC-01
        caso("CP-FIC-01", "Directorio con 2 ficheros y 1 subdirectorio", "3 elementos y totales correctos", () -> {
            File dir = new File(tmp.toFile(), "d1");
            dir.mkdir();
            new File(dir, "catalogo.json").createNewFile();
            new File(dir, "proveedores.xml").createNewFile();
            new File(dir, "antiguos").mkdir();
            List<String> l = Explorador.explorar(dir.getPath());
            exigir(l.size() == 4 && l.get(3).equals("Total ficheros: 2 | Directorios: 1"), l.toString());
            return l.toString();
        });
        caso("CP-FIC-02", "Ruta inexistente (desde el menú)", "Mensaje de error y el programa sigue funcionando", () -> {
            String s = sesion(App.class, "1", "no/existe", "", "0");
            exigir(s.contains("ERROR: La ruta no existe") && s.contains("Hasta pronto."), s);
            return "Muestra \"La ruta no existe\" y vuelve al menú (termina con \"Hasta pronto.\")";
        });
        caso("CP-FIC-03", "Fichero de 123 bytes", "Tamaño y ruta absoluta", () -> {
            File f = new File(tmp.toFile(), "f.txt");
            Files.write(f.toPath(), new byte[123]);
            List<String> l = Explorador.explorar(f.getPath());
            exigir(l.contains("Tamaño: 123 bytes") && l.contains("Ruta absoluta: " + f.getAbsolutePath()), l.toString());
            return l.toString();
        });

        // ---- RF-FIC-02
        caso("CP-JSON-01", "json_3_validos.json", "Se importan los tres", () -> {
            Importador.Resultado r = Importador.importarJson(new File(D + "json_3_validos.json"), new Inventario());
            exigir(r.error == null && r.encontrados == 3 && r.importados == 3, resumen(r));
            return resumen(r);
        });
        caso("CP-JSON-02", "json_3_validos_2_invalidos.json", "Se importan 3 y se explican 2 rechazos", () -> {
            Importador.Resultado r = Importador.importarJson(new File(D + "json_3_validos_2_invalidos.json"), new Inventario());
            exigir(r.encontrados == 5 && r.importados == 3 && r.incidencias.size() == 2, resumen(r));
            return resumen(r) + " " + r.incidencias;
        });
        caso("CP-JSON-03", "json_malformado.json, json_coma_final.json y json_raiz_array.json con 3 productos ya cargados",
                "Los tres se rechazan y no se añade ningún producto", () -> {
            Inventario inv = cargar("json_3_validos.json");
            String texto = "";
            for (String f : List.of("json_malformado.json", "json_coma_final.json", "json_raiz_array.json")) {
                Importador.Resultado r = Importador.importarJson(new File(D + f), inv);
                exigir(r.error != null && inv.getProductos().size() == 3, f + " no se rechazó");
                texto += f + ": " + r.error + " ";
            }
            return texto;
        });
        caso("CP-JSON-04", "json_3_validos.json y luego json_duplicado.json", "Se conserva el producto existente", () -> {
            Inventario inv = cargar("json_3_validos.json");
            Importador.Resultado r = Importador.importarJson(new File(D + "json_duplicado.json"), inv);
            exigir(r.importados == 1 && r.incidencias.size() == 2, resumen(r));
            exigir(inv.buscarPorCodigo("TEC-101").getNombre().equals("Teclado mecánico"), "se sustituyó el original");
            return resumen(r) + " " + r.incidencias + "; TEC-101 conserva su nombre original";
        });

        // ---- RF-FIC-03
        caso("CP-XML-01", "xml_3_validos.xml", "Se importan los tres", () -> {
            Importador.Resultado r = Importador.importarXml(new File(D + "xml_3_validos.xml"), new Inventario());
            exigir(r.error == null && r.importados == 3, resumen(r));
            return resumen(r);
        });
        caso("CP-XML-02", "xml_con_invalidos.xml", "Se rechazan solo los inválidos", () -> {
            Inventario inv = new Inventario();
            Importador.Resultado r = Importador.importarXml(new File(D + "xml_con_invalidos.xml"), inv);
            exigir(r.encontrados == 3 && r.importados == 1 && inv.buscarPorCodigo("XML-501") != null, resumen(r));
            return resumen(r) + " " + r.incidencias;
        });
        caso("CP-XML-03", "xml_con_dtd.xml, xml_malformado.xml y xml_raiz_incorrecta.xml con 3 productos ya cargados",
                "Se rechazan los tres documentos completos", () -> {
            Inventario inv = cargar("json_3_validos.json");
            for (String f : List.of("xml_con_dtd.xml", "xml_malformado.xml", "xml_raiz_incorrecta.xml")) {
                exigir(Importador.importarXml(new File(D + f), inv).error != null && inv.getProductos().size() == 3, f + " no se rechazó");
            }
            return Importador.importarXml(new File(D + "xml_con_dtd.xml"), inv).error;
        });
        caso("CP-XML-04", "json_3_validos.json y luego xml_duplicado_json.xml", "Se rechaza el duplicado", () -> {
            Inventario inv = cargar("json_3_validos.json");
            Importador.Resultado r = Importador.importarXml(new File(D + "xml_duplicado_json.xml"), inv);
            exigir(r.importados == 1 && r.incidencias.size() == 1 && inv.buscarPorCodigo("TEC-101").getPrecio().doubleValue() == 59.9, resumen(r));
            return resumen(r) + " " + r.incidencias;
        });

        // ---- RF-FIC-04
        caso("CP-CON-01", "Listar sin importar nada (desde el menú)", "Mensaje de inventario vacío", () -> {
            exigir(sesion(App.class, "4", "1", "", "0", "0").contains("El inventario está vacío."), "sin mensaje");
            return "Muestra \"El inventario está vacío.\"";
        });
        caso("CP-CON-02", "Buscar \"tec-101\" y \" TeC-101 \"", "Localiza TEC-101", () -> {
            Inventario inv = cargar("json_3_validos.json");
            exigir(inv.buscarPorCodigo("tec-101") != null && inv.buscarPorCodigo(" TeC-101 ") != null && inv.buscarPorCodigo("zzz") == null, "búsqueda");
            return "Localizado " + inv.buscarPorCodigo("tec-101").getCodigo();
        });
        caso("CP-CON-03", "Buscar \"teclado\" con 6 productos cargados", "Coincidencias parciales (2)", () -> {
            Inventario inv = cargar("json_3_validos.json", "json_3_validos_2_invalidos.json");
            List<Producto> l = inv.buscarPorNombre("teclado");
            exigir(l.size() == 2 && inv.buscarPorNombre("TECLADO").size() == 2, codigos(l));
            return codigos(l);
        });
        caso("CP-CON-04", "Importar json_3_validos.json y xml_3_validos.xml y listar", "Productos de ambos formatos ordenados por código", () -> {
            String c = codigos(cargar("json_3_validos.json", "xml_3_validos.xml").ordenados());
            exigir(c.equals("HUB-403, MON-103, MON-401, RAT-102, TEC-101, TEC-402"), c);
            return c;
        });

        // ---- RF-FIC-05
        List<Producto> tres = cargar("json_3_validos.json").ordenados();
        caso("CP-EXP-01", "Exportar 3 productos a JSON y leerlos en un inventario vacío", "JSON válido con 3 productos", () -> {
            File f = new File(tmp.toFile(), "e.json");
            Exportador.aJson(tres, f);
            Importador.Resultado r = Importador.importarJson(f, new Inventario());
            exigir(r.error == null && r.importados == 3, resumen(r));
            return "Fichero de " + f.length() + " bytes que se vuelve a leer con 3 productos";
        });
        caso("CP-EXP-02", "Exportar 3 productos a XML y leerlos en un inventario vacío", "XML válido con 3 productos", () -> {
            File f = new File(tmp.toFile(), "e.xml");
            Exportador.aXml(tres, f);
            Importador.Resultado r = Importador.importarXml(f, new Inventario());
            exigir(r.error == null && r.importados == 3, resumen(r));
            return "Fichero de " + f.length() + " bytes que se vuelve a leer con 3 productos";
        });
        caso("CP-EXP-03", "Destino existente (texto ORIGINAL) y respuesta N a ¿Sobrescribir? (desde el menú)", "Fichero original intacto", () -> {
            File f = new File(tmp.toFile(), "existe.json");
            Files.writeString(f.toPath(), "ORIGINAL");
            String s = sesion(App.class, "2", D + "json_3_validos.json", "", "5", "1", f.getPath(), "N", "", "0", "0");
            exigir(s.contains("el fichero original no se ha modificado") && Files.readString(f.toPath()).equals("ORIGINAL"), s);
            return "Con N el fichero sigue valiendo ORIGINAL";
        });
        caso("CP-EXP-04", "Importar JSON, exportar a XML y volver a importar", "Se conservan los datos", () -> {
            File f = new File(tmp.toFile(), "conv.xml");
            Exportador.aXml(tres, f);
            Inventario b = new Inventario();
            Importador.importarXml(f, b);
            exigir(texto(tres).equals(texto(b.ordenados())), "datos distintos");
            return "Los 3 productos son idénticos campo a campo";
        });
        caso("CP-EXP-05", "Exportar 6 productos (con acentos y &) a JSON y XML e importar en inventario vacío", "Se recuperan los mismos productos", () -> {
            List<Producto> seis = cargar("json_3_validos.json", "xml_3_validos.xml").ordenados();
            File j = new File(tmp.toFile(), "seis.json");
            File x = new File(tmp.toFile(), "seis.xml");
            Exportador.aJson(seis, j);
            Exportador.aXml(seis, x);
            Inventario a = new Inventario();
            Inventario b = new Inventario();
            Importador.importarJson(j, a);
            Importador.importarXml(x, b);
            exigir(texto(seis).equals(texto(a.ordenados())) && texto(seis).equals(texto(b.ordenados())), "datos distintos");
            return "Los 6 productos se recuperan idénticos desde JSON y desde XML";
        });

        // ---- RF-FIC-06
        List<Producto> cinco = new ArrayList<>();
        for (int i = 1; i <= 5; i++) cinco.add(new Producto("P-00" + i, "Producto " + i, new BigDecimal(i + ".50"), i, 1, 1));
        File cincoDat = new File(tmp.toFile(), "cinco.dat");
        Serializacion.guardar(cinco, cincoDat);
        caso("CP-SER-01", "Guardar 5 productos, empezar con un inventario nuevo y recuperar", "Se recuperan los cinco", () -> {
            List<Producto> r = Serializacion.recuperar(cincoDat);
            exigir(r.size() == 5 && texto(r).equals(texto(cinco)), "datos distintos");
            return "Recuperados " + codigos(r);
        });
        caso("CP-SER-02", "3 productos en memoria, recuperar un .dat de 5 y responder N (desde el menú)", "Se conserva el inventario actual", () -> {
            String s = sesion(App.class, "2", D + "json_3_validos.json", "", "6", "2", cincoDat.getPath(), "N", "", "0", "4", "1", "", "0", "0");
            exigir(s.contains("Actualmente hay 3 productos") && s.contains("se conserva el inventario actual"), s);
            String listado = s.substring(s.lastIndexOf("CÓDIGO"));
            exigir(listado.contains("TEC-101") && !listado.contains("P-001"), listado);
            return "Pide confirmación y con N el listado sigue mostrando TEC-101, RAT-102 y MON-103";
        });
        caso("CP-SER-03", "Fichero con texto, fichero truncado y un HashMap serializado", "Se informa y la memoria no cambia", () -> {
            File basura = new File(tmp.toFile(), "basura.dat");
            Files.writeString(basura.toPath(), "esto no es un fichero serializado");
            byte[] bytes = Files.readAllBytes(cincoDat.toPath());
            File truncado = new File(tmp.toFile(), "truncado.dat");
            Files.write(truncado.toPath(), java.util.Arrays.copyOf(bytes, bytes.length / 2));
            File mapa = new File(tmp.toFile(), "mapa.dat");
            try (ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(mapa))) {
                o.writeObject(new HashMap<>(java.util.Map.of("a", "b")));
            }
            String texto = "";
            for (File f : List.of(basura, truncado, mapa)) texto += falla(() -> Serializacion.recuperar(f)) + " / ";
            String s = sesion(App.class, "2", D + "json_3_validos.json", "", "6", "2", basura.getPath(), "", "0", "4", "1", "", "0", "0");
            exigir(s.substring(s.lastIndexOf("CÓDIGO")).contains("TEC-101"), "la memoria cambió");
            return texto + "en el menú la memoria sigue intacta";
        });
        caso("CP-SER-04", "Guardar con el inventario vacío (desde el menú)", "No se crea fichero", () -> {
            String s = sesion(App.class, "6", "1", "", "0", "0");
            exigir(s.contains("no se crea ningún fichero") && !s.contains("Ruta de destino"), s);
            return "Muestra \"no se crea ningún fichero\" y ni siquiera pide la ruta";
        });

        // ---- RF-FIC-07
        caso("CP-XPATH-01", "xpath_catalogo.xml, código tec-001", "Se encuentra el producto", () -> {
            String s = sesion(XPathActividad.class, D + "xpath_catalogo.xml", "1", "tec-001", "0");
            exigir(s.contains("TEC-001 Teclado USB 19.95"), s);
            return "TEC-001 Teclado USB 19.95 €";
        });
        caso("CP-XPATH-02", "xpath_catalogo.xml, categoría 1", "Solo productos de la categoría 1", () -> {
            String s = sesion(XPathActividad.class, D + "xpath_catalogo.xml", "2", "1", "0");
            exigir(s.contains("TEC-001") && s.contains("RAT-100") && !s.contains("MON-003") && s.contains("encontrados: 2"), s);
            return "TEC-001 y RAT-100 (2 productos)";
        });
        caso("CP-XPATH-03", "xpath_catalogo.xml, precio superior a 100 (RAT-100 cuesta 100.00)", "Excluye exactamente 100", () -> {
            String s = sesion(XPathActividad.class, D + "xpath_catalogo.xml", "3", "100", "0");
            exigir(s.contains("MON-003") && !s.contains("RAT-100") && s.contains("encontrados: 1"), s);
            return "Solo MON-003; RAT-100 queda fuera";
        });
        caso("CP-XPATH-04", "xpath_catalogo.xml y xpath_catalogo_con_errores.xml (3 elementos cada uno)", "Resultado 3", () -> {
            exigir(sesion(XPathActividad.class, D + "xpath_catalogo.xml", "4", "0").contains("Productos: 3"), "primero");
            exigir(sesion(XPathActividad.class, D + "xpath_catalogo_con_errores.xml", "4", "0").contains("Productos: 3"), "segundo");
            return "3 en los dos ficheros (cuenta también los de datos incorrectos)";
        });
        caso("CP-XPATH-05", "xml_con_dtd.xml", "Se rechaza", () -> {
            String s = sesion(XPathActividad.class, D + "xml_con_dtd.xml");
            exigir(s.contains("ERROR") && !s.contains("1. Por código"), s);
            return "Muestra ERROR y no abre el documento";
        });

        // ---- RF-FIC-08
        List<Producto> cuatro = List.of(
                new Producto("TEC-001", "Teclado USB", new BigDecimal("19.95"), 12, 1, 1),
                new Producto("CAB-001", "Cable", new BigDecimal("4.50"), 100, 3, 1),
                new Producto("HUB-002", "Hub USB", new BigDecimal("15.75"), 30, 3, 1),
                new Producto("MON-003", "Monitor 24 pulgadas", new BigDecimal("149.99"), 8, 2, 2));
        File bin = new File(tmp.toFile(), "stock.bin");
        caso("CP-RAF-01", "Generar stock.bin con 4 productos", "Fichero de 144 bytes", () -> {
            StockBin.generar(bin, cuatro);
            exigir(bin.length() == 144 && StockBin.contar(bin) == 4, "tamaño " + bin.length());
            return bin.length() + " bytes, 4 registros";
        });
        caso("CP-RAF-02", "Consultar la posición 2", "Acceso directo al tercer registro (MON-003)", () -> {
            String r = StockBin.leer(bin, 2);
            exigir(r.equals("MON-003 | 149.99 € | stock 8"), r);
            return r;
        });
        caso("CP-RAF-03", "Cambiar el stock de la posición 2 de 8 a 15", "Solo cambia ese campo", () -> {
            byte[] antes = Files.readAllBytes(bin.toPath());
            int anterior = StockBin.cambiarStock(bin, 2, 15);
            byte[] despues = Files.readAllBytes(bin.toPath());
            for (int i = 0; i < antes.length; i++) {
                if (antes[i] != despues[i]) {
                    exigir(i >= 2 * 36 + 32 && i < 2 * 36 + 36, "cambió el byte " + i);
                }
            }
            exigir(anterior == 8 && StockBin.leer(bin, 2).endsWith("stock 15") && cuatro.get(3).getStock() == 8, "valores");
            return "Stock 8 → 15; solo han cambiado bytes del campo stock (offsets 104-107); el Producto en memoria sigue en 8";
        });
        caso("CP-RAF-04", "Posiciones -1 y 4 (hay 4 registros) y stock negativo", "Se rechazan", () -> {
            byte[] antes = Files.readAllBytes(bin.toPath());
            String t = falla(() -> StockBin.leer(bin, -1)) + " / " + falla(() -> StockBin.leer(bin, 4)) + " / " + falla(() -> StockBin.cambiarStock(bin, 0, -3));
            exigir(java.util.Arrays.equals(antes, Files.readAllBytes(bin.toPath())), "el fichero cambió");
            return t;
        });
        caso("CP-RAF-05", "Fichero de 100 bytes", "Se informa de fichero inválido", () -> {
            File raro = new File(tmp.toFile(), "raro.bin");
            Files.write(raro.toPath(), new byte[100]);
            String m = falla(() -> StockBin.contar(raro));
            exigir(m.contains("múltiplo de 36"), m);
            return m;
        });

        borrar(tmp.toFile());
        System.out.println("\nCasos fallidos: " + fallos);
        System.exit(fallos == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ ayudas

    private interface Accion { void ejecutar() throws Exception; }

    private static void caso(String id, String datos, String esperado, Callable<String> prueba) {
        String obtenido;
        boolean ok = true;
        try {
            obtenido = prueba.call();
        } catch (Throwable e) {
            obtenido = "FALLO: " + e;
            ok = false;
            fallos++;
        }
        System.out.println("| " + id + " | " + datos + " | " + esperado + " | " + obtenido.replace("|", "/").replace("\n", " ") + " | " + (ok ? "OK" : "FALLO") + " |");
    }

    private static void exigir(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }

    /** Ejecuta algo que debe fallar con IOException y devuelve el mensaje. */
    private static String falla(Accion accion) throws Exception {
        try {
            accion.ejecutar();
        } catch (IOException e) {
            return e.getMessage();
        }
        throw new AssertionError("Debía fallar y no falló");
    }

    private static Inventario cargar(String... ficheros) {
        Inventario inv = new Inventario();
        for (String f : ficheros) {
            Importador.Resultado r = f.endsWith(".json") ? Importador.importarJson(new File(D + f), inv) : Importador.importarXml(new File(D + f), inv);
            exigir(r.error == null, f + ": " + r.error);
        }
        return inv;
    }

    private static String resumen(Importador.Resultado r) {
        return r.error != null ? "error: " + r.error : "encontrados " + r.encontrados + ", importados " + r.importados + ", rechazados " + (r.encontrados - r.importados);
    }

    private static String codigos(List<Producto> l) {
        return l.stream().map(Producto::getCodigo).collect(Collectors.joining(", "));
    }

    private static String texto(List<Producto> l) {
        return l.stream().map(Producto::toString).collect(Collectors.joining("\n"));
    }

    /** Lanza una clase con main en otro proceso, le teclea las líneas y devuelve lo que muestra. */
    private static String sesion(Class<?> clase, String... lineas) throws Exception {
        String classpath = ubicacion(App.class) + File.pathSeparator + ubicacion(Gson.class);
        ProcessBuilder pb = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Dstdout.encoding=UTF-8", "-Dfile.encoding=UTF-8", "-Ddatastock.color=false", "-cp", classpath, clase.getName());
        pb.redirectErrorStream(true);
        Process proceso = pb.start();
        proceso.getOutputStream().write((String.join("\n", lineas) + "\n").getBytes("UTF-8"));
        proceso.getOutputStream().close();
        return new String(proceso.getInputStream().readAllBytes(), "UTF-8");
    }

    private static String ubicacion(Class<?> clase) throws Exception {
        return Path.of(clase.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
    }

    private static void borrar(File f) {
        File[] hijos = f.listFiles();
        if (hijos != null) for (File h : hijos) borrar(h);
        f.delete();
    }
}
