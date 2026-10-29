package es.iesisidra.datastock.ficheros;

import es.iesisidra.datastock.modelo.Producto;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * RF-FIC-08. Fichero de registros de 36 bytes: código (12 chars = 24 bytes), precio (double = 8) y stock (int = 4).
 * El registro n empieza en n*36 y su stock en n*36+32.
 */
public class StockBin {
    private static final int TAM = 36;
    private static final int POS_STOCK = 32;

    public static void generar(File fichero, List<Producto> productos) throws IOException {
        if (productos.isEmpty()) throw new IOException("No hay productos en memoria: no se genera el fichero");
        List<Producto> orden = new ArrayList<>(productos); // copia, la lista original no cambia
        orden.sort(Comparator.comparing(Producto::getCodigo));
        try (RandomAccessFile raf = new RandomAccessFile(fichero, "rw")) {
            raf.setLength(0); // se vacía por si el fichero ya existía
            for (Producto p : orden) {
                raf.writeChars(String.format("%-12s", p.getCodigo())); // 12 caracteres con relleno de espacios
                raf.writeDouble(p.getPrecio().doubleValue());
                raf.writeInt(p.getStock());
            }
        }
    }

    /** Número de registros. Falla si el fichero no existe o su tamaño no es múltiplo de 36. */
    public static long contar(File fichero) throws IOException {
        if (!fichero.isFile()) throw new IOException("El fichero no existe: " + fichero);
        if (fichero.length() % TAM != 0) throw new IOException("Fichero inválido: su tamaño (" + fichero.length() + " bytes) no es múltiplo de 36");
        return fichero.length() / TAM;
    }

    private static void comprobarPosicion(File fichero, int posicion) throws IOException {
        long total = contar(fichero);
        if (posicion < 0 || posicion >= total) throw new IOException("Posición fuera de rango (0-" + (total - 1) + ")");
    }

    /** Lee un solo registro saltando directamente a su posición con seek. */
    public static String leer(File fichero, int posicion) throws IOException {
        comprobarPosicion(fichero, posicion);
        try (RandomAccessFile raf = new RandomAccessFile(fichero, "r")) {
            raf.seek((long) posicion * TAM);
            return leerRegistro(raf);
        }
    }

    /** Cambia solo el stock del registro (4 bytes) y devuelve el valor anterior. */
    public static int cambiarStock(File fichero, int posicion, int nuevoStock) throws IOException {
        if (nuevoStock < 0) throw new IOException("El stock no puede ser negativo");
        comprobarPosicion(fichero, posicion);
        try (RandomAccessFile raf = new RandomAccessFile(fichero, "rw")) {
            raf.seek((long) posicion * TAM + POS_STOCK);
            int anterior = raf.readInt();
            raf.seek((long) posicion * TAM + POS_STOCK); // readInt ha movido el puntero, hay que volver
            raf.writeInt(nuevoStock);
            return anterior;
        }
    }

    /** Lectura secuencial: se leen los registros uno tras otro desde el principio. */
    public static List<String> leerTodos(File fichero) throws IOException {
        long total = contar(fichero);
        List<String> lineas = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(fichero, "r")) {
            for (int i = 0; i < total; i++) lineas.add(i + ": " + leerRegistro(raf));
        }
        return lineas;
    }

    private static String leerRegistro(RandomAccessFile raf) throws IOException {
        StringBuilder codigo = new StringBuilder();
        for (int i = 0; i < 12; i++) codigo.append(raf.readChar());
        double precio = raf.readDouble();
        int stock = raf.readInt();
        return codigo.toString().trim() + " | " + precio + " € | stock " + stock;
    }
}
