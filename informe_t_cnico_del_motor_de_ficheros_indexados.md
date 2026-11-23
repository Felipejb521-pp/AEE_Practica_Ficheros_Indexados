# Motor Reutilizable de Ficheros Indexados en Java 7

**Módulo:** Acceso a Datos (2º DAM)  
**Unidad Didáctica:** UD1 – Sistema de archivos y ficheros  
**Paquete desarrollado:** `motor.indexado` (`FicheroIndexado`, `IndiceDAO`, `IndiceRegistro`)

---

## 1. Introducción y Fundamentación Teórica

En el desarrollo de aplicaciones de acceso a datos, la persistencia de la información en almacenamiento secundario se clasifica según la organización física de sus registros y el modo en que la aplicación puede acceder a ellos. Tal y como se estudia en la Unidad Didáctica 1, existen tres organizaciones principales para los ficheros binarios:

* **Organización secuencial:** Los registros se graban uno detrás de otro por estricto orden de llegada. Para localizar un registro concreto es necesario leer todos los anteriores desde el inicio, lo que penaliza gravemente las búsquedas y no permite bajas ni modificaciones directas sin reescribir todo el archivo.
* **Organización directa:** Cada registro ocupa una posición relativa fija calculada matemáticamente a partir de una clave numérica. Ofrece acceso inmediato, pero desperdicia espacio si las claves no son correlativas y no funciona de forma natural con claves alfanuméricas (como un DNI).
* **Organización indexada:** Combina las ventajas de las anteriores. Los registros se almacenan físicamente en un fichero de datos (normalmente por orden de inserción), mientras que un segundo fichero —denominado **índice**— mantiene una tabla ordenada con el valor del **campo clave** de cada registro y su **posición física** exacta en el fichero de datos.

Dado que la plataforma **Java 7** no incluye de forma nativa una clase que gestione automáticamente ficheros con organización indexada, nuestro motor (`motor.indexado`) simula esta arquitectura coordinando dos ficheros físicos binarios independientes y una estructura dinámica en memoria RAM (`TreeMap`), respetando estrictamente las tres clases del diagrama UML (`FicheroIndexado`, `IndiceDAO` e `IndiceRegistro`), prescindiendo de `FicheroDirectoDAO` y evitando el uso de genéricos.

---

## 2. Arquitectura Física y Lógica del Sistema

El motor separa radicalmente el almacenamiento de los datos completos respecto a la estructura de búsqueda, evitando cargar todos los registros en memoria RAM (lo cual sería inviable con volúmenes grandes de datos):

1. **Fichero físico de datos (`*.dat`):** Es un fichero binario compuesto por bloques de **longitud fija** (`TAMANO_BLOQUE_DATOS = 512` bytes). Al tener todos los registros exactamente el mismo tamaño en bytes, es posible calcular la dirección exacta de cualquier registro y saltar directamente hasta él mediante un puntero de acceso aleatorio (`RandomAccessFile`).
2. **Fichero físico de índice (`*.ndx`):** Es un fichero binario secuencial muy ligero que almacena exclusivamente pares `(clave, posicionFisica)`. Al ocupar apenas unos pocos bytes por registro, puede leerse y escribirse por completo de forma prácticamente instantánea mediante flujos binarios de datos (`DataInputStream` y `DataOutputStream`).
3. **Índice en memoria (`TreeMap`):** Actúa como caché de búsqueda rápida durante la ejecución del programa. Internamente implementa un árbol equilibrado (árbol rojo-negro), lo que garantiza dos propiedades fundamentales:
   * Localización de cualquier clave en tiempo logarítmico ($O(\log n)$) sin acceder al disco.
   * Mantenimiento automático de todas las claves ordenadas ascendentemente, permitiendo recorridos secuenciales ordenados y consultas dinámicas.

| Componente | Tipo de Almacenamiento | Estructura Interna y Responsabilidad |
| :--- | :--- | :--- |
| **Fichero de Datos** (`*.dat`) | Binario de longitud fija (`RandomAccessFile`) | Bloques de 512 bytes: 1 byte booleano de estado (`activo`), indicador y valor de clave, y el objeto serializado. |
| **Fichero de Índice** (`*.ndx`) | Binario secuencial (`DataInputStream` / `DataOutputStream`) | Pares compactos: carácter de tipo (`'S'`, `'I'`, `'L'`), valor de clave y desplazamiento en bytes (`long`). |
| **Índice en Memoria** (`TreeMap`) | Estructura dinámica en RAM | Mapa ordenado que relaciona cada `Comparable` (clave) con su `Long` (posición física). |

### Diagrama UML Adaptado

```text
+---------------------------------------------------------------+
|                       FicheroIndexado                         |
+---------------------------------------------------------------+
| - ficheroDatos: File                                          |
| - indices: IndiceDAO                                          |
| - indice: TreeMap                                             |
+---------------------------------------------------------------+
| + FicheroIndexado(ficheroDatos: File, ficheroIndice: File)    |
| + reconstruirIndice(): void                                   |
| + alta(clave: Comparable, registro: Object): boolean          |
| + baja(clave: Comparable): boolean                            |
| + modificar(clave: Comparable, registro: Object): boolean     |
| + consultarSecuencial(): List                                 |
| + consultarSecuencialFisico(): List                           |
| + consultarAleatorio(clave: Comparable): Object               |
| + consultarDinamico(clave: Comparable, limite: int): List     |
| + posicionar(clave: Comparable): long                         |
| + getClaves(): Set                                            |
| + getNumeroRegistros(): long                                  |
+---------------------------------------------------------------+
                                |
                                | compone (1)
                                v
                  +---------------------------+
                  |         IndiceDAO         |
                  +---------------------------+
                  | - fichero: File           |
                  +---------------------------+
                  | + IndiceDAO(fichero: File)|
                  | + leer(): TreeMap         |
                  | + guardar(indice: TreeMap)|
                  |       : void              |
                  +---------------------------+
                                |
                                | usa
                                v
                  +---------------------------+
                  |      IndiceRegistro       |
                  +---------------------------+
                  | - clave: Comparable       |
                  | - posicion: long          |
                  +---------------------------+
                  | + IndiceRegistro(         |
                  |     clave: Comparable,    |
                  |     posicion: long)       |
                  | + getClave(): Comparable  |
                  | + getPosicion(): long     |
                  +---------------------------+
```

---

## 3. Análisis Detallado de las Clases del Motor

### 3.1. Clase `IndiceRegistro`
Es la unidad mínima de información del sistema de indexación. Representa una fila o entrada individual dentro de la tabla de índices.

* **Atributos:**
  * `private Comparable clave`: Almacena el valor del campo identificador del registro (por ejemplo, un DNI de tipo `String` o un código numérico de tipo `Integer` o `Long`). Se tipa como `Comparable` (interfaz estándar de Java 7) para permitir que el `TreeMap` compare y ordene claves de distintos tipos sin necesidad de utilizar genéricos.
  * `private long posicion`: Almacena el desplazamiento exacto en bytes (*offset*) desde el byte `0` del fichero de datos donde comienza el bloque correspondiente a esa clave.
* **Métodos:** Dispone de su constructor `IndiceRegistro(Comparable clave, long posicion)` y de los métodos observadores `getClave()` y `getPosicion()`.

---

### 3.2. Clase `IndiceDAO`
Encapsula toda la lógica de persistencia del índice en disco, aplicando el patrón DAO (*Data Access Object*) para aislar las operaciones de entrada/salida de la lógica de negocio.

* **Atributo:** `private File fichero`, que apunta al archivo físico del índice en el sistema de archivos.
* **Funcionamiento del método `leer()`:**
  1. Comprueba preventivamente que el objeto `File` no sea nulo, que exista en disco y que su tamaño sea mayor que cero.
  2. Abre un flujo de bytes `FileInputStream` envuelto en un flujo filtrado de tipos primitivos `DataInputStream`.
  3. Mediante una variable bandera `finFichero` y un bloque de control de excepción `EOFException` (sin utilizar `available()` ni bucles infinitos `while (true)`), lee secuencialmente cada entrada del índice:
     * Primero lee un `char` indicador del tipo de dato de la clave (`'S'` para `String`, `'I'` para `Integer`, `'L'` para `Long`). Esta técnica permite que `IndiceDAO` sea completamente reutilizable con diferentes tipos de clave sin usar genéricos ni serialización de objetos.
     * A continuación lee el valor de la clave mediante `readUTF()`, `readInt()` o `readLong()` según corresponda.
     * Seguidamente lee el `long` correspondiente a la posición física en el fichero de datos.
     * Construye el objeto `IndiceRegistro` e inserta el par `(clave, posicion)` en el `TreeMap`.
  4. Al alcanzarse el final del fichero, salta `EOFException`, se activa la bandera de salida, se cierran ordenadamente ambos flujos y se devuelve el `TreeMap` cargado en un único `return`.
* **Funcionamiento del método `guardar(TreeMap indice)`:**
  1. Abre un `FileOutputStream` y un `DataOutputStream` sobre el fichero de índice (sobrescribiendo su contenido previo para mantener una imagen exacta del estado actual en memoria).
  2. Recorre el `TreeMap` mediante un `Iterator` sobre su `entrySet()` realizando los *castings* explícitos necesarios en Java 7.
  3. Por cada entrada, instancia un `IndiceRegistro` y escribe en el flujo el carácter identificador de tipo, el valor de la clave y el `long` de la posición física.
  4. Finalmente cierra los flujos. Si se produce algún error de E/S, captura `IOException`, informa descriptivamente por consola (`System.err`) y libera los recursos abiertos mediante los métodos auxiliares `cerrarFlujosLectura` y `cerrarFlujosEscritura`.

---

### 3.3. Clase `FicheroIndexado`
Es el núcleo o motor principal del sistema. Coordina el fichero de datos, el `TreeMap` en memoria y el objeto `IndiceDAO`, ofreciendo hacia el exterior una interfaz limpia y completa para operar sobre registros sin exponer cómo se almacenan los bytes en disco.

* **Atributos:**
  * `private File ficheroDatos`: Referencia al archivo binario de datos.
  * `private IndiceDAO indices`: Componente encargado de leer y guardar el fichero de índice.
  * `private TreeMap indice`: Estructura ordenada en memoria que relaciona cada clave con su posición en bytes.
  * `private static final int TAMANO_BLOQUE_DATOS = 512`: Constante que fija la longitud en bytes de cada registro físico en el fichero de datos.

* **Empaquetado y desempaquetado de registros (`empaquetarRegistro` y `desempaquetarRegistro`):**
  Para conseguir que `FicheroIndexado` funcione con cualquier clase del dominio (`Empleado`, `Producto`, `Cliente`) sin crear clases adicionales fuera del UML y garantizando registros de longitud fija, cada bloque de 512 bytes se construye en memoria usando `ByteArrayOutputStream` y `DataOutputStream` con la siguiente estructura interna:
  1. **Byte 0 (`boolean activo`):** Marca de control que vale `true` si el registro está vigente o `false` si ha sido dado de baja.
  2. **Cabecera de clave (`char tipo` + valor):** Guarda la clave en formato primitivo dentro del propio bloque de datos, permitiendo reconstruir el índice aunque se borre el fichero `.ndx`.
  3. **Cuerpo del registro (`ObjectOutputStream`):** Serializa los atributos del objeto (`Empleado`, etc.) en el resto del array de bytes hasta completar los 512 bytes fijos.

* **Operaciones CRUD y de mantenimiento:**
  * **Constructor `FicheroIndexado(File ficheroDatos, File ficheroIndice)`:** Inicializa las referencias y carga el índice desde disco invocando `indices.leer()`. Como medida de tolerancia a fallos, si detecta que el fichero de índice no existe o está vacío pero el fichero de datos ya contiene información, invoca automáticamente a `reconstruirIndice()`.
  * **`alta(Comparable clave, Object registro)`:** Comprueba que la clave no exista previamente en el `TreeMap` (garantizando unicidad de clave primaria). Empaqueta el registro en un bloque de 512 bytes con marca `activo = true`, abre el fichero de datos con `RandomAccessFile` en modo lectura/escritura (`"rw"`), posiciona el puntero al final del archivo (`raf.seek(raf.length())`) y escribe el bloque. Acto seguido registra la nueva pareja `(clave, posicionInsercion)` en el `TreeMap` y sincroniza el fichero de índice en disco mediante `indices.guardar(indice)`.
  * **`baja(Comparable clave)`:** Localiza la posición física en bytes del registro a través del índice. En lugar de eliminar bytes intermedios del archivo (lo que obligaría a desplazar todos los registros posteriores y recalcular todas las posiciones del índice), realiza una **baja lógica**: posiciona el puntero en el byte inicial del registro (`raf.seek(posicion)`) y sobrescribe únicamente el primer byte (`raf.writeBoolean(false)`). Finalmente elimina la clave del `TreeMap` y guarda el índice actualizado en disco.
  * **`modificar(Comparable clave, Object registro)`:** Obtiene la posición física del registro existente, empaqueta el objeto modificado con marca `activo = true`, posiciona el `RandomAccessFile` en esa dirección exacta (`raf.seek(posicion)`) y sobrescribe el bloque de 512 bytes sin alterar el tamaño del archivo ni el índice.
  * **`reconstruirIndice()`:** Limpia el `TreeMap` y recorre secuencialmente el fichero de datos de principio a fin saltando de 512 en 512 bytes. Para cada bloque cuyo primer byte (`activo`) sea `true`, lee su clave de cabecera y la vuelve a insertar en el `TreeMap` junto a su posición física, regenerando al terminar el fichero `.ndx` en disco.

* **Modos de acceso y consulta implementados (UD1):**
  * **Acceso Aleatorio (`consultarAleatorio(Comparable clave)`):** Consulta el desplazamiento en bytes en el `TreeMap`, ejecuta `raf.seek(posicion)` en modo lectura (`"r"`), lee únicamente ese bloque de 512 bytes y devuelve el objeto desempaquetado.
  * **Acceso Secuencial ordenado por clave (`consultarSecuencial()`):** Itera sobre las entradas del `TreeMap` (que mantiene las claves ordenadas ascendentemente de forma natural) y recupera cada registro desde su posición física en el fichero de datos, devolviendo una `List` ordenada por clave.
  * **Acceso Secuencial físico (`consultarSecuencialFisico()`):** Abre el fichero de datos y lo recorre linealmente desde el byte `0` hasta `raf.length()` en incrementos de 512 bytes, ignorando los registros marcados como borrados (`activo == false`) y devolviendo una `List` con los registros en el mismo orden en que fueron grabados en disco.
  * **Acceso Dinámico (`consultarDinamico(Comparable claveInicial, int limite)`):** Utiliza `indice.tailMap(claveInicial)` para posicionarse directamente en la clave solicitada (o en la inmediatamente posterior si no existe) y, a partir de ese punto, avanza secuencialmente recuperando hasta un máximo de `limite` registros ordenados por clave.
  * **Métodos auxiliares de consulta (`posicionar`, `getClaves`, `getNumeroRegistros`):** Permiten conocer el *offset* en bytes de una clave (`-1` si no existe), obtener el `Set` ordenado de claves activas y consultar el total de registros vigentes en el sistema.

---

## 4. Gestión de Recursos y Control de Excepciones (`RA1.a` y `RA1.f`)

Toda la implementación del motor sigue un diseño defensivo y estructurado:

* **Comprobaciones previas:** Antes de abrir cualquier flujo o `RandomAccessFile`, se valida que los parámetros recibidos no sean nulos, que las claves existan o no estén duplicadas según la operación, y que los ficheros existan en disco antes de intentar leerlos.
* **Programación estructurada:** Todos los métodos respetan la norma de una única sentencia `return` al final, sin utilizar bloques `finally`, sin `available()` y sin bucles infinitos `while (true)`.
* **Tratamiento informativo sin interrupción abrupta:** Todos los bloques de entrada/salida capturan `IOException` y `ClassNotFoundException`, emitiendo mensajes claros por `System.err` que detallan la operación fallida y el nombre del fichero afectado, cerrando explícitamente los flujos abiertos mediante métodos auxiliares (`cerrarRaf`, `cerrarFlujosLectura`, `cerrarFlujosEscritura`) y devolviendo valores seguros (`false`, `null` o listas vacías).