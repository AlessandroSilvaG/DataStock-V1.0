package es.iesisidra.datastock.complementarias;

import es.iesisidra.datastock.ficheros.Importador;
import es.iesisidra.datastock.util.LectorConsola;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** RF-FIC-07: consultas XPath sobre un catálogo XML, sin importarlo al inventario. */
public class XPathActividad {

    public static void main(String[] args) {
        System.out.println("===== CONSULTAS XPATH =====");
        File fichero = new File(LectorConsola.leerTexto("Fichero: ").trim());
        Document doc;
        try (InputStream entrada = new FileInputStream(fichero)) {
            doc = Importador.leerXml(entrada); // lectura segura: rechaza DTD
            if (!doc.getDocumentElement().getTagName().equals("catalogo")) {
                System.out.println("ERROR: la raíz del XML debe ser <catalogo>");
                return;
            }
        } catch (Exception e) {
            System.out.println("ERROR: no se puede abrir el XML (" + e.getMessage() + ")");
            return;
        }

        XPath xpath = XPathFactory.newInstance().newXPath();
        int opcion;
        do {
            System.out.println("\n1. Por código | 2. Por categoría | 3. Precio superior | 4. Contar | 0. Salir");
            opcion = LectorConsola.leerOpcion("Opción: ", 0, 4);
            try {
                if (opcion == 1) {
                    String codigo = LectorConsola.leerTexto("Código: ").trim();
                    // el código del usuario no se mete en la expresión: se seleccionan todos y se compara en Java
                    NodeList todos = (NodeList) xpath.evaluate("/catalogo/producto", doc, XPathConstants.NODESET);
                    int encontrados = 0;
                    for (int i = 0; i < todos.getLength(); i++) {
                        Element p = (Element) todos.item(i);
                        if (!codigo.isEmpty() && texto(p, "codigo").equalsIgnoreCase(codigo)) {
                            mostrar(p);
                            encontrados++;
                        }
                    }
                    System.out.println("Productos encontrados: " + encontrados);
                } else if (opcion == 2) {
                    int categoria = LectorConsola.leerEntero("Categoría: ");
                    mostrarLista((NodeList) xpath.evaluate("/catalogo/producto[categoriaId=" + categoria + "]", doc, XPathConstants.NODESET));
                } else if (opcion == 3) {
                    BigDecimal minimo = new BigDecimal(LectorConsola.leerTexto("Precio mínimo: ").trim());
                    mostrarLista((NodeList) xpath.evaluate("/catalogo/producto[precio > " + minimo.toPlainString() + "]", doc, XPathConstants.NODESET));
                } else if (opcion == 4) {
                    System.out.println("Productos: " + ((Double) xpath.evaluate("count(/catalogo/producto)", doc, XPathConstants.NUMBER)).intValue());
                }
            } catch (NumberFormatException e) {
                System.out.println("ERROR: el precio no es un número válido");
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        } while (opcion != 0);
    }

    private static void mostrarLista(NodeList nodos) {
        for (int i = 0; i < nodos.getLength(); i++) mostrar((Element) nodos.item(i));
        System.out.println("Productos encontrados: " + nodos.getLength());
    }

    private static void mostrar(Element p) {
        System.out.println(texto(p, "codigo") + " " + texto(p, "nombre") + " " + texto(p, "precio") + " €");
    }

    /** Texto de un campo del producto, o "?" si el campo no existe. */
    private static String texto(Element producto, String campo) {
        NodeList hijos = producto.getElementsByTagName(campo);
        return hijos.getLength() == 0 ? "?" : hijos.item(0).getTextContent().trim();
    }
}
