package es.iesisidra.datastock.ficheros;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** RF-FIC-01. Devuelve las líneas que hay que mostrar sobre una ruta. */
public class Explorador {

    public static List<String> explorar(String ruta) throws IOException {
        if (ruta == null || ruta.isBlank()) throw new IOException("La ruta está vacía");
        File f = new File(ruta.trim());
        if (!f.exists()) throw new IOException("La ruta no existe: " + ruta.trim());

        List<String> lineas = new ArrayList<>();
        if (f.isFile()) {
            lineas.add("[FICHERO] " + f.getName());
            lineas.add("Ruta absoluta: " + f.getAbsolutePath());
            lineas.add("Tamaño: " + f.length() + " bytes");
            return lineas;
        }

        File[] hijos = f.listFiles();
        if (hijos == null) throw new IOException("No se puede acceder al contenido del directorio");
        // primero los ficheros y luego los directorios, cada grupo por nombre
        Arrays.sort(hijos, Comparator.comparing(File::isDirectory).thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        int ficheros = 0;
        int directorios = 0;
        for (File hijo : hijos) {
            if (hijo.isDirectory()) {
                directorios++;
                lineas.add("[DIR] " + hijo.getName());
            } else {
                ficheros++;
                lineas.add("[FICHERO] " + hijo.getName() + " " + hijo.length() + " bytes");
            }
        }
        if (hijos.length == 0) lineas.add("El directorio está vacío");
        lineas.add("Total ficheros: " + ficheros + " | Directorios: " + directorios);
        return lineas;
    }
}
