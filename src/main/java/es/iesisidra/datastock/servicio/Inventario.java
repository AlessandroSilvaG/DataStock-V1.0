package es.iesisidra.datastock.servicio;

import es.iesisidra.datastock.modelo.Producto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Colección de productos compartida por todas las opciones del menú. */
public class Inventario {
    private final List<Producto> productos = new ArrayList<>();

    public List<Producto> getProductos() { return productos; }

    /** Busca por código sin distinguir mayúsculas ni espacios extremos. Devuelve null si no existe. */
    public Producto buscarPorCodigo(String codigo) {
        for (Producto p : productos) {
            if (p.getCodigo().equalsIgnoreCase(codigo.trim())) return p;
        }
        return null;
    }

    /** Añade el producto si su código no existe todavía. */
    public boolean agregar(Producto p) {
        if (buscarPorCodigo(p.getCodigo()) != null) return false;
        productos.add(p);
        return true;
    }

    /** Copia de la lista ordenada por código (la colección original no se toca). */
    public List<Producto> ordenados() {
        List<Producto> copia = new ArrayList<>(productos);
        copia.sort(Comparator.comparing(Producto::getCodigo));
        return copia;
    }

    /** Productos cuyo nombre contiene el texto, sin distinguir mayúsculas. */
    public List<Producto> buscarPorNombre(String texto) {
        List<Producto> encontrados = new ArrayList<>();
        for (Producto p : ordenados()) {
            if (p.getNombre().toLowerCase().contains(texto.trim().toLowerCase())) encontrados.add(p);
        }
        return encontrados;
    }

    /** Sustituye todo el inventario por la lista recibida. */
    public void sustituir(List<Producto> nuevos) {
        productos.clear();
        productos.addAll(nuevos);
    }
}
