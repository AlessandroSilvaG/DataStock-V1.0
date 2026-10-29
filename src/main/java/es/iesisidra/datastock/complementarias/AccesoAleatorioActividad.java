package es.iesisidra.datastock.complementarias;

import es.iesisidra.datastock.ficheros.Importador;
import es.iesisidra.datastock.ficheros.StockBin;
import es.iesisidra.datastock.servicio.Inventario;
import es.iesisidra.datastock.util.LectorConsola;
import java.io.File;

/**
 * RF-FIC-08: acceso aleatorio a stock.bin con RandomAccessFile.
 * Como es un programa aparte, empieza con el inventario vacío: primero se cargan productos
 * desde un JSON o XML y después se genera el fichero binario a partir de ellos.
 */
public class AccesoAleatorioActividad {

    public static void main(String[] args) {
        File fichero = new File("datos/salidas/stock.bin");
        Inventario inventario = new Inventario();
        System.out.println("===== ACCESO ALEATORIO =====");
        int opcion;
        do {
            System.out.println("\nFichero: " + fichero + " | Productos en memoria: " + inventario.getProductos().size());
            System.out.println("1. Cargar productos desde JSON");
            System.out.println("2. Cargar productos desde XML");
            System.out.println("3. Generar stock.bin con los productos en memoria");
            System.out.println("4. Consultar un registro por posición");
            System.out.println("5. Modificar el stock de un registro");
            System.out.println("6. Mostrar todos los registros");
            System.out.println("0. Salir");
            opcion = LectorConsola.leerOpcion("Opción: ", 0, 6);
            try {
                if (opcion == 1 || opcion == 2) {
                    File origen = new File(LectorConsola.leerTexto("Ruta: ").trim());
                    Importador.Resultado r = opcion == 1 ? Importador.importarJson(origen, inventario) : Importador.importarXml(origen, inventario);
                    System.out.println(r.error != null ? "ERROR: " + r.error : "Importados: " + r.importados + " | Rechazados: " + (r.encontrados - r.importados));
                } else if (opcion == 3) {
                    if (inventario.getProductos().isEmpty()) System.out.println("No hay productos en memoria: no se genera el fichero.");
                    else if (!fichero.exists() || LectorConsola.confirmar("El fichero ya existe. ¿Sobrescribir?")) {
                        StockBin.generar(fichero, inventario.getProductos());
                        System.out.println("Generado. Registros: " + StockBin.contar(fichero) + " | Tamaño: " + fichero.length() + " bytes");
                    }
                } else if (opcion == 4) {
                    System.out.println("Registros: " + StockBin.contar(fichero) + " | Tamaño: " + fichero.length() + " bytes");
                    System.out.println(StockBin.leer(fichero, LectorConsola.leerEntero("Posición: ")));
                } else if (opcion == 5) {
                    int posicion = LectorConsola.leerEntero("Posición: ");
                    int nuevo = LectorConsola.leerEntero("Nuevo stock: ");
                    int anterior = StockBin.cambiarStock(fichero, posicion, nuevo);
                    System.out.println("Actualización correcta: stock " + anterior + " → " + nuevo);
                    System.out.println("(El inventario en memoria no cambia: stock.bin es una copia independiente.)");
                } else if (opcion == 6) {
                    for (String linea : StockBin.leerTodos(fichero)) System.out.println(linea);
                }
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        } while (opcion != 0);
    }
}
