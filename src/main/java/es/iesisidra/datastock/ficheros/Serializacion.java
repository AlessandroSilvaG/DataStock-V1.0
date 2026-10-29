package es.iesisidra.datastock.ficheros;

import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.servicio.Inventario;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/** RF-FIC-06. Guarda y recupera la lista de productos con serialización de Java. */
public class Serializacion {

    public static void guardar(List<Producto> productos, File destino) throws IOException {
        try (ObjectOutputStream salida = new ObjectOutputStream(new FileOutputStream(destino))) {
            salida.writeObject(new ArrayList<>(productos));
        }
    }

    /** Devuelve los productos del fichero. Si algo falla lanza IOException y no se devuelve nada. */
    public static List<Producto> recuperar(File origen) throws IOException {
        if (!origen.isFile() || !origen.canRead()) throw new IOException("El fichero no existe o no se puede leer: " + origen);
        Object leido;
        try (ObjectInputStream entrada = new ObjectInputStream(new FileInputStream(origen))) {
            // Solo se permiten las clases que DataStock guarda: así un fichero ajeno no puede crear otros objetos
            // (java.lang.Object hace falta porque ArrayList comprueba internamente un Object[])
            entrada.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
                    "java.util.ArrayList;es.iesisidra.datastock.modelo.Producto;java.math.*;java.lang.Number;java.lang.String;java.lang.Object;!*"));
            leido = entrada.readObject();
        } catch (IOException | ClassNotFoundException | RuntimeException e) {
            throw new IOException("El fichero está dañado o no es un inventario de DataStock");
        }

        if (!(leido instanceof List<?> lista)) throw new IOException("El fichero no contiene una lista de productos");
        List<Producto> productos = new ArrayList<>();
        Inventario comprobar = new Inventario(); // sirve para detectar códigos repetidos
        for (Object o : lista) {
            if (!(o instanceof Producto p)) throw new IOException("La lista contiene elementos que no son productos");
            List<String> errores = new ArrayList<>();
            Producto valido = Importador.validar(new String[] {p.getCodigo(), p.getNombre(), String.valueOf(p.getPrecio()),
                    String.valueOf(p.getStock()), String.valueOf(p.getCategoriaId()), String.valueOf(p.getProveedorId())}, errores);
            if (valido == null || !comprobar.agregar(valido)) throw new IOException("El fichero contiene un producto no válido o repetido: " + p.getCodigo());
            productos.add(valido);
        }
        return productos;
    }
}
