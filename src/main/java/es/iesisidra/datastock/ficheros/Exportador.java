package es.iesisidra.datastock.ficheros;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import es.iesisidra.datastock.modelo.Producto;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Exporta una lista de productos (ya ordenada) a JSON con Gson o a XML con DOM. RF-FIC-05. */
public class Exportador {

    public static void aJson(List<Producto> productos, File destino) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        JsonObject raiz = new JsonObject();
        raiz.add("productos", gson.toJsonTree(productos));
        try (Writer escritor = new FileWriter(destino, StandardCharsets.UTF_8)) {
            gson.toJson(raiz, escritor);
        }
    }

    public static void aXml(List<Producto> productos, File destino) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element raiz = doc.createElement("catalogo");
        doc.appendChild(raiz);
        for (Producto p : productos) {
            Element producto = doc.createElement("producto");
            raiz.appendChild(producto);
            campo(doc, producto, "codigo", p.getCodigo());
            campo(doc, producto, "nombre", p.getNombre());
            campo(doc, producto, "precio", p.getPrecio().toPlainString());
            campo(doc, producto, "stock", String.valueOf(p.getStock()));
            campo(doc, producto, "categoriaId", String.valueOf(p.getCategoriaId()));
            campo(doc, producto, "proveedorId", String.valueOf(p.getProveedorId()));
        }
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        t.transform(new DOMSource(doc), new StreamResult(destino));
    }

    private static void campo(Document doc, Element padre, String nombre, String texto) {
        Element e = doc.createElement(nombre);
        e.setTextContent(texto);
        padre.appendChild(e);
    }
}
