package es.iesisidra.datastock.ficheros;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.servicio.Inventario;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.helpers.DefaultHandler;

/** Importa catálogos JSON (Gson) y XML (DOM) al inventario. RF-FIC-02 y RF-FIC-03. */
public class Importador {
    private static final String[] CAMPOS = {"codigo", "nombre", "precio", "stock", "categoriaId", "proveedorId"};

    /** Resumen de una importación. Si error != null el fichero se ha rechazado entero. */
    public static class Resultado {
        public int encontrados;
        public int importados;
        public List<String> incidencias = new ArrayList<>();
        public String error;
    }

    public static Resultado importarJson(File fichero, Inventario inventario) {
        Resultado r = new Resultado();
        if (!fichero.isFile() || !fichero.canRead()) {
            r.error = "El fichero no existe o no se puede leer: " + fichero;
            return r;
        }
        List<String[]> filas = new ArrayList<>();
        try (JsonReader lector = new JsonReader(new FileReader(fichero, StandardCharsets.UTF_8))) {
            // JsonReader sin modo permisivo: una coma final o unas comillas simples ya son error
            JsonElement raiz = new Gson().getAdapter(JsonElement.class).read(lector);
            if (lector.peek() != JsonToken.END_DOCUMENT) throw new Exception("hay texto después del documento");
            if (!raiz.isJsonObject() || !raiz.getAsJsonObject().has("productos")
                    || !raiz.getAsJsonObject().get("productos").isJsonArray()) {
                r.error = "La raíz debe ser un objeto con el array \"productos\"";
                return r;
            }
            JsonArray array = raiz.getAsJsonObject().getAsJsonArray("productos");
            for (JsonElement e : array) {
                String[] fila = new String[CAMPOS.length];
                if (e.isJsonObject()) {
                    JsonObject o = e.getAsJsonObject();
                    for (int i = 0; i < CAMPOS.length; i++) {
                        if (o.has(CAMPOS[i]) && o.get(CAMPOS[i]).isJsonPrimitive()) fila[i] = o.get(CAMPOS[i]).getAsString();
                    }
                }
                filas.add(fila);
            }
        } catch (Exception e) {
            r.error = "JSON mal formado: " + e.getMessage();
            return r;
        }
        return procesar(filas, inventario, r);
    }

    public static Resultado importarXml(File fichero, Inventario inventario) {
        Resultado r = new Resultado();
        if (!fichero.isFile() || !fichero.canRead()) {
            r.error = "El fichero no existe o no se puede leer: " + fichero;
            return r;
        }
        List<String[]> filas = new ArrayList<>();
        try (InputStream entrada = new FileInputStream(fichero)) {
            Document doc = leerXml(entrada);
            if (!doc.getDocumentElement().getTagName().equals("catalogo")) {
                r.error = "La raíz del XML debe ser <catalogo>";
                return r;
            }
            NodeList productos = doc.getElementsByTagName("producto");
            for (int i = 0; i < productos.getLength(); i++) {
                Element p = (Element) productos.item(i);
                String[] fila = new String[CAMPOS.length];
                for (int j = 0; j < CAMPOS.length; j++) {
                    NodeList hijos = p.getElementsByTagName(CAMPOS[j]);
                    if (hijos.getLength() > 0) fila[j] = hijos.item(0).getTextContent();
                }
                filas.add(fila);
            }
        } catch (Exception e) {
            r.error = "XML mal formado o con DTD (no permitido): " + e.getMessage();
            return r;
        }
        return procesar(filas, inventario, r);
    }

    /** Lee un XML con DOM rechazando DTD y entidades externas (protección XXE). */
    public static Document leerXml(InputStream entrada) throws Exception {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder constructor = fabrica.newDocumentBuilder();
        constructor.setErrorHandler(new DefaultHandler()); // así no escribe "[Fatal Error]" por consola
        return constructor.parse(entrada);
    }

    /** Parte común a JSON y XML: valida cada fila, descarta duplicados y añade los correctos. */
    private static Resultado procesar(List<String[]> filas, Inventario inventario, Resultado r) {
        List<Producto> validos = new ArrayList<>();
        Set<String> codigosDelFichero = new HashSet<>();
        int numero = 0;
        for (String[] fila : filas) {
            numero++;
            List<String> errores = new ArrayList<>();
            Producto p = validar(fila, errores);
            if (p != null && (inventario.buscarPorCodigo(p.getCodigo()) != null || codigosDelFichero.contains(p.getCodigo()))) {
                errores.add("código duplicado");
            }
            if (errores.isEmpty()) {
                validos.add(p);
                codigosDelFichero.add(p.getCodigo());
            } else {
                String codigo = fila[0] == null || fila[0].isBlank() ? "" : " (" + fila[0].trim() + ")";
                r.incidencias.add("Producto " + numero + codigo + ": " + String.join("; ", errores));
            }
        }
        for (Producto p : validos) inventario.agregar(p);
        r.encontrados = filas.size();
        r.importados = validos.size();
        return r;
    }

    /**
     * Valida los seis campos (codigo, nombre, precio, stock, categoriaId, proveedorId).
     * Devuelve el producto o null, y deja en "errores" los motivos del rechazo.
     */
    public static Producto validar(String[] f, List<String> errores) {
        String codigo = f[0] == null ? "" : f[0].trim().toUpperCase();
        if (codigo.isEmpty()) errores.add("el código es obligatorio");
        else if (codigo.length() > 12 || !codigo.matches("[A-Z0-9-]+")) errores.add("el código no es válido (máximo 12 caracteres: letras, números y guiones)");

        String nombre = f[1] == null ? "" : f[1].trim();
        if (nombre.length() < 3 || nombre.length() > 100) errores.add("el nombre debe tener entre 3 y 100 caracteres");

        BigDecimal precio = null;
        try {
            precio = new BigDecimal(f[2].trim());
            if (precio.signum() <= 0) errores.add("el precio debe ser mayor que 0");
        } catch (Exception e) {
            errores.add("el precio no es un número");
        }

        Integer stock = aEntero(f[3]);
        if (stock == null) errores.add("el stock debe ser un número entero");
        else if (stock < 0) errores.add("el stock no puede ser negativo");

        Integer categoria = aEntero(f[4]);
        if (categoria == null || categoria <= 0) errores.add("categoriaId debe ser un entero positivo");
        Integer proveedor = aEntero(f[5]);
        if (proveedor == null || proveedor <= 0) errores.add("proveedorId debe ser un entero positivo");

        if (!errores.isEmpty()) return null;
        return new Producto(codigo, nombre, precio, stock, categoria, proveedor);
    }

    private static Integer aEntero(String texto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (Exception e) {
            return null; // texto nulo o no numérico
        }
    }
}
