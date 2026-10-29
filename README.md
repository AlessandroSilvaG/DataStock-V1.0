# DataStock · UT1 (ficheros)

Acceso a Datos · 2.º DAM · IES Isidra de Guzmán · Java 21 + Maven + Gson. Aplicación de consola para gestionar un inventario
que se importa desde JSON/XML, se consulta en memoria, se exporta y se guarda con serialización.

## Cómo ejecutarlo

Requisitos: JDK 21, Maven (la primera vez descarga Gson) y una consola con UTF-8.

```bash
mvn clean compile
mvn exec:java                                   # programa principal (menú)
mvn exec:java -Dexec.mainClass=es.iesisidra.datastock.complementarias.XPathActividad            # RF-FIC-07
mvn exec:java -Dexec.mainClass=es.iesisidra.datastock.complementarias.AccesoAleatorioActividad  # RF-FIC-08
mvn exec:java -Dexec.mainClass=es.iesisidra.datastock.pruebas.PruebasAceptacion                 # pruebas (desde la raíz)
```

Si salen caracteres raros de colores: `-Ddatastock.color=false`.

## Menú

| Opción | Qué hace | Clase que lo hace |
|---|---|---|
| 1 | Explorar una ruta (fichero o directorio) | `ficheros.Explorador` |
| 2 / 3 | Importar JSON (Gson) / XML (DOM) | `ficheros.Importador` |
| 4 | Listar, buscar por código, buscar por nombre | `servicio.Inventario` |
| 5 | Exportar a JSON / XML | `ficheros.Exportador` |
| 6 | Guardar / recuperar inventario `.dat` | `ficheros.Serializacion` |

`ui.MenuPrincipal` solo pide los datos y llama a esas clases. XPath y `RandomAccessFile` son programas aparte
(`complementarias`), el segundo usa `ficheros.StockBin`.

## Reglas que se aplican al importar

- Si el fichero está mal formado o la raíz no es correcta, no se importa nada.
- Si el fichero es correcto, se añaden los productos válidos y se explica el motivo de cada rechazo.
- Un código ya existente (en memoria o repetido en el mismo fichero) se rechaza sin tocar el original.
- El código se guarda sin espacios y en mayúsculas (`tec-101` y `TEC-101` son el mismo).
- El XML se lee sin permitir DTD ni entidades externas (XXE). El `.dat` solo admite las clases de DataStock.

## Estructura

```text
src/main/java/es/iesisidra/datastock/
├── App.java
├── modelo/            Producto, Categoria, Proveedor, MovimientoStock (facilitadas)
├── servicio/          Inventario (colección compartida)
├── ficheros/          Explorador, Importador, Exportador, Serializacion, StockBin
├── ui/                MenuPrincipal
├── util/              LectorConsola, ColoresConsola (facilitadas)
├── complementarias/   XPathActividad, AccesoAleatorioActividad
└── pruebas/           PruebasAceptacion
datos/ejemplos/        ejemplos facilitados (no se modifican)
datos/pruebas/         ficheros de prueba válidos e inválidos (ver LEEME.txt)
datos/salidas/         exportaciones y stock.bin generados (no se suben a Git)
docs/PRUEBAS_UT1.md    resultados de las pruebas de aceptación
docs/evidencias/       salidas de ejecución del menú, XPath y acceso aleatorio
```

## Notas

- `stock.bin` es una copia independiente del inventario: cambiarlo no modifica los productos en memoria.
  Como `AccesoAleatorioActividad` es un programa aparte y arranca con el inventario vacío, primero se cargan productos
  desde un JSON o un XML y luego se genera el fichero.
- No hay conexión con MySQL en esta fase (UT2).
