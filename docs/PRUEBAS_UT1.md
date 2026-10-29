# Documento de pruebas · DataStock UT1

Resultados de ejecutar `PruebasAceptacion` (RA1-g) con los ficheros de `datos/pruebas`.
Las pruebas marcadas «desde el menú» lanzan el programa real con el teclado simulado.

| ID | Datos utilizados | Resultado esperado | Resultado obtenido | Estado |
|---|---|---|---|---|
| CP-FIC-01 | Directorio con 2 ficheros y 1 subdirectorio | 3 elementos y totales correctos | [[FICHERO] catalogo.json 0 bytes, [FICHERO] proveedores.xml 0 bytes, [DIR] antiguos, Total ficheros: 2 / Directorios: 1] | OK |
| CP-FIC-02 | Ruta inexistente (desde el menú) | Mensaje de error y el programa sigue funcionando | Muestra "La ruta no existe" y vuelve al menú (termina con "Hasta pronto.") | OK |
| CP-FIC-03 | Fichero de 123 bytes | Tamaño y ruta absoluta | [[FICHERO] f.txt, Ruta absoluta: /tmp/datastock11134418517908747352/f.txt, Tamaño: 123 bytes] | OK |
| CP-JSON-01 | json_3_validos.json | Se importan los tres | encontrados 3, importados 3, rechazados 0 | OK |
| CP-JSON-02 | json_3_validos_2_invalidos.json | Se importan 3 y se explican 2 rechazos | encontrados 5, importados 3, rechazados 2 [Producto 4 (CAB-204): el precio debe ser mayor que 0, Producto 5: el código es obligatorio] | OK |
| CP-JSON-03 | json_malformado.json, json_coma_final.json y json_raiz_array.json con 3 productos ya cargados | Los tres se rechazan y no se añade ningún producto | json_malformado.json: JSON mal formado: End of input at line 2 column 1 path $.productos[1] json_coma_final.json: JSON mal formado: Use JsonReader.setLenient(true) to accept malformed JSON at line 4 column 4 path $.productos[1] json_raiz_array.json: La raíz debe ser un objeto con el array "productos"  | OK |
| CP-JSON-04 | json_3_validos.json y luego json_duplicado.json | Se conserva el producto existente | encontrados 3, importados 1, rechazados 2 [Producto 1 (tec-101): código duplicado, Producto 3 (NEW-001): código duplicado]; TEC-101 conserva su nombre original | OK |
| CP-XML-01 | xml_3_validos.xml | Se importan los tres | encontrados 3, importados 3, rechazados 0 | OK |
| CP-XML-02 | xml_con_invalidos.xml | Se rechazan solo los inválidos | encontrados 3, importados 1, rechazados 2 [Producto 2 (XML-502): el precio no es un número, Producto 3 (XML-503): el stock debe ser un número entero] | OK |
| CP-XML-03 | xml_con_dtd.xml, xml_malformado.xml y xml_raiz_incorrecta.xml con 3 productos ya cargados | Se rechazan los tres documentos completos | XML mal formado o con DTD (no permitido): DOCTYPE is disallowed when the feature "http://apache.org/xml/features/disallow-doctype-decl" set to true. | OK |
| CP-XML-04 | json_3_validos.json y luego xml_duplicado_json.xml | Se rechaza el duplicado | encontrados 2, importados 1, rechazados 1 [Producto 1 (TEC-101): código duplicado] | OK |
| CP-CON-01 | Listar sin importar nada (desde el menú) | Mensaje de inventario vacío | Muestra "El inventario está vacío." | OK |
| CP-CON-02 | Buscar "tec-101" y " TeC-101 " | Localiza TEC-101 | Localizado TEC-101 | OK |
| CP-CON-03 | Buscar "teclado" con 6 productos cargados | Coincidencias parciales (2) | TEC-101, TEC-201 | OK |
| CP-CON-04 | Importar json_3_validos.json y xml_3_validos.xml y listar | Productos de ambos formatos ordenados por código | HUB-403, MON-103, MON-401, RAT-102, TEC-101, TEC-402 | OK |
| CP-EXP-01 | Exportar 3 productos a JSON y leerlos en un inventario vacío | JSON válido con 3 productos | Fichero de 520 bytes que se vuelve a leer con 3 productos | OK |
| CP-EXP-02 | Exportar 3 productos a XML y leerlos en un inventario vacío | XML válido con 3 productos | Fichero de 707 bytes que se vuelve a leer con 3 productos | OK |
| CP-EXP-03 | Destino existente (texto ORIGINAL) y respuesta N a ¿Sobrescribir? (desde el menú) | Fichero original intacto | Con N el fichero sigue valiendo ORIGINAL | OK |
| CP-EXP-04 | Importar JSON, exportar a XML y volver a importar | Se conservan los datos | Los 3 productos son idénticos campo a campo | OK |
| CP-EXP-05 | Exportar 6 productos (con acentos y &) a JSON y XML e importar en inventario vacío | Se recuperan los mismos productos | Los 6 productos se recuperan idénticos desde JSON y desde XML | OK |
| CP-SER-01 | Guardar 5 productos, empezar con un inventario nuevo y recuperar | Se recuperan los cinco | Recuperados P-001, P-002, P-003, P-004, P-005 | OK |
| CP-SER-02 | 3 productos en memoria, recuperar un .dat de 5 y responder N (desde el menú) | Se conserva el inventario actual | Pide confirmación y con N el listado sigue mostrando TEC-101, RAT-102 y MON-103 | OK |
| CP-SER-03 | Fichero con texto, fichero truncado y un HashMap serializado | Se informa y la memoria no cambia | El fichero está dañado o no es un inventario de DataStock / El fichero está dañado o no es un inventario de DataStock / El fichero está dañado o no es un inventario de DataStock / en el menú la memoria sigue intacta | OK |
| CP-SER-04 | Guardar con el inventario vacío (desde el menú) | No se crea fichero | Muestra "no se crea ningún fichero" y ni siquiera pide la ruta | OK |
| CP-XPATH-01 | xpath_catalogo.xml, código tec-001 | Se encuentra el producto | TEC-001 Teclado USB 19.95 € | OK |
| CP-XPATH-02 | xpath_catalogo.xml, categoría 1 | Solo productos de la categoría 1 | TEC-001 y RAT-100 (2 productos) | OK |
| CP-XPATH-03 | xpath_catalogo.xml, precio superior a 100 (RAT-100 cuesta 100.00) | Excluye exactamente 100 | Solo MON-003; RAT-100 queda fuera | OK |
| CP-XPATH-04 | xpath_catalogo.xml y xpath_catalogo_con_errores.xml (3 elementos cada uno) | Resultado 3 | 3 en los dos ficheros (cuenta también los de datos incorrectos) | OK |
| CP-XPATH-05 | xml_con_dtd.xml | Se rechaza | Muestra ERROR y no abre el documento | OK |
| CP-RAF-01 | Generar stock.bin con 4 productos | Fichero de 144 bytes | 144 bytes, 4 registros | OK |
| CP-RAF-02 | Consultar la posición 2 | Acceso directo al tercer registro (MON-003) | MON-003 / 149.99 € / stock 8 | OK |
| CP-RAF-03 | Cambiar el stock de la posición 2 de 8 a 15 | Solo cambia ese campo | Stock 8 → 15; solo han cambiado bytes del campo stock (offsets 104-107); el Producto en memoria sigue en 8 | OK |
| CP-RAF-04 | Posiciones -1 y 4 (hay 4 registros) y stock negativo | Se rechazan | Posición fuera de rango (0-3) / Posición fuera de rango (0-3) / El stock no puede ser negativo | OK |
| CP-RAF-05 | Fichero de 100 bytes | Se informa de fichero inválido | Fichero inválido: su tamaño (100 bytes) no es múltiplo de 36 | OK |

Casos fallidos: 0
