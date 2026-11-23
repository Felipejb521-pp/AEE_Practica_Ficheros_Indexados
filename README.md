# Equipo 3 — Funcionamiento e integración (ALVARO)

He preparado el componente genérico y las clases de empleados para unir nuestras partes del trabajo.

El código es compatible con Java 7. Todas las clases van en el `<default package>`, sin líneas `package`.

## 1. Clases del componente genérico

### RegistroConClave<K>

Es una interfaz con el método `getClave()`.

Permite obtener la clave de cualquier registro sin que el componente genérico conozca sus campos.

En `Empleado`, la clave es el DNI:

```java
@Override
public String getClave() {
    return this.dni;
}
```

### IndiceRegistro<K>

Representa una pareja formada por:

- La clave del registro.
- Su posición en bytes dentro del fichero de datos.

Se mantiene porque aparece en el UML. La persistencia actual guarda directamente el `TreeMap`, por lo que no necesita escribir objetos `IndiceRegistro`.

### IndiceDAO<K>

Se encarga exclusivamente del fichero del índice.

- `leer()`: recupera el `TreeMap` con `ObjectInputStream`.
- `guardar()`: escribe el mapa completo con `ObjectOutputStream`.

Si el índice no existe, `leer()` devuelve un mapa vacío. El motor reconstruye el índice leyendo los registros activos del fichero de datos.

### FicheroDirectoDAO<T>

Es una clase abstracta que contiene las operaciones comunes de los ficheros de registros fijos:

- `escribir(posicion, registro)`: escribe un registro en una posición.
- `insertar(registro)`: lo añade al final.
- `borrar(posicion)`: lo marca como inactivo.
- `reescribir(posicion, registro)`: modifica un registro activo.
- `leer(posicion)`: recupera un registro; devuelve `null` si está dado de baja.
- `getNumeroRegistros()`: devuelve el número de registros físicos, incluidas las bajas.

Los métodos protegidos `posicionar()` y `comprobarPosicion()` controlan los desplazamientos.

Cada DAO concreto implementa:

```java
protected void escribirRegistro(RandomAccessFile datos, T registro)
        throws IOException;

protected T leerRegistro(RandomAccessFile datos)
        throws IOException;
```

La superclase guarda y lee la marca de activo. El DAO concreto trabaja solamente con los campos del registro.

### FicheroIndexado<K,T>

Coordina el fichero de datos, el índice persistente y el `TreeMap` en memoria.

- `reconstruirIndice()`: recorre los registros activos y obtiene sus claves y posiciones.
- `alta(clave, registro)`: comprueba duplicados, añade al final y actualiza el índice.
- `baja(clave)`: marca el registro como inactivo y elimina su clave del índice.
- `modificar(clave, registro)`: reescribe los datos conservando la clave.
- `consultarSecuencial()`: devuelve los registros activos en orden físico.
- `posicionar(clave)`: selecciona la posición de una clave; devuelve `-1` si no existe.
- `consultarAleatorio()`: lee el registro seleccionado anteriormente.
- `consultarDinamico(clave, cantidad)`: devuelve hasta esa cantidad de registros en orden ascendente de clave, desde la indicada inclusive.
- `getClaves()`: devuelve una copia de las claves activas ordenadas.
- `getNumeroRegistros()`: devuelve el número de registros físicos.

La consulta aleatoria se utiliza así:

```java
ficheroIndexado.posicionar(dni);
Empleado empleado = ficheroIndexado.consultarAleatorio();
```

Aquí “aleatorio” significa acceso directo, no elegir un empleado al azar.

## 2. Clases de empleados

### Empleado

Contiene:

- DNI.
- Nombre y apellidos.
- Sexo.
- Salario.
- Fecha de ingreso.
- Tipo de empleado.
- Provincia.

Valida los datos al construir el objeto. Implementa `RegistroConClave<String>` y devuelve el DNI como clave.

`setSalario()` permite cambiar el salario del objeto después de validarlo.

### Fecha

Comprueba que la fecha exista, incluyendo los años bisiestos.

También permite obtener la fecha actual y comprobar si una fecha es posterior a otra.

### Sexo, TipoEmpleado y Provincia

Son enumeraciones que limitan los valores permitidos.

- Sexo: H y M.
- Tipo de empleado: C, F y D.
- Provincia: las ocho provincias andaluzas.

Relacionan los códigos guardados en el fichero con sus descripciones.

### EmpleadoDAO

Extiende `FicheroDirectoDAO<Empleado>`.

Implementa cómo se leen y escriben los campos de un empleado y añade `modificarSalario()`, que modifica únicamente los cuatro bytes del salario.

Los métodos comunes de insertar, borrar, leer y reescribir se heredan de la superclase.

### GestorEmpleados

Es la clase que deben utilizar las aplicaciones para realizar el mantenimiento:

```java
altaEmpleado(empleado);
bajaEmpleado(dni);
modificarSalario(dni, salario);
buscarPorDni(dni);
listarOrdenadoDesdeDni(dni, cantidad);
```

## 3. Creación del gestor

Utilizamos una instancia del gestor durante la ejecución del menú:

```java
File datos = new File("Ficheros", "FICHENDX.DAT");
File indice = new File("Ficheros", "INDICE.DAT");

GestorEmpleados gestor = new GestorEmpleados(datos, indice);
```

Dentro del gestor se crean el DAO de empleados y el componente genérico:

```java
this.empleadoDAO = new EmpleadoDAO(datos);

this.ficheroIndexado =
        new FicheroIndexado<String, Empleado>(
                datos, indice, this.empleadoDAO);
```

## 4. Funcionamiento de los ficheros

### FICHENDX.DAT

Guarda los empleados con registros de longitud fija de 92 bytes.

| Campo | Bytes |
| --- | ---: |
| Activo | 1 |
| DNI | 18 |
| Nombre y apellidos | 60 |
| Sexo | 2 |
| Salario | 4 |
| Año | 2 |
| Mes | 1 |
| Día | 1 |
| Tipo de empleado | 2 |
| Provincia | 1 |
| **Total** | **92** |

Los textos se guardan con `writeChar`, a dos bytes por carácter, completando con espacios.

Las posiciones de inicio son:

- Primer registro: offset 0.
- Segundo registro: offset 92.
- Tercer registro: offset 184.

El salario empieza en el offset 81 dentro del registro.

La superclase guarda el byte de activo. `EmpleadoDAO` guarda los otros 91 bytes. No hay que escribir la marca dos veces.

### INDICE.DAT

Guarda un `TreeMap<String, Long>` serializado:

- La clave es el DNI.
- El valor es el offset del empleado en FICHENDX.DAT.

Por ejemplo:

| DNI | Offset |
| --- | ---: |
| 00000001R | 184 |
| 00000002W | 0 |
| 00000003A | 92 |

El orden por DNI puede ser diferente al orden físico de los empleados.

El índice tiene longitud variable. No utiliza el formato anterior de entradas fijas de 26 bytes.

Aunque se llame `.DAT`, se lee y escribe con flujos de objetos. Cambiar su extensión a `.IND` no cambia su funcionamiento.

## 5. Integración de las partes

### Felipe — CreaFiche, menú, bajas y listado por DNI

Para CreaFiche:

1. Leer cada trabajador del FICHE.DAT original.
2. Pedir su DNI por teclado.
3. Crear el objeto Empleado.
4. Darlo de alta:

```java
gestor.altaEmpleado(empleado);
```

El fichero original no se lee con `EmpleadoDAO`, porque su formato es diferente. La creación debe comprobar que los destinos no existan antes de generar otro fichero.

Para una baja, primero buscar:

```java
Empleado empleado = gestor.buscarPorDni(dni);
```

Si devuelve `null`, informar de que no existe.

Si existe, mostrar sus datos y pedir confirmación. Solo después de confirmar:

```java
boolean realizado = gestor.bajaEmpleado(dni);
```

Para listar todos por DNI:

```java
List<Empleado> empleados = gestor.listarOrdenadoDesdeDni(
        "00000000T", Integer.MAX_VALUE);
```

El DNI inicial se utiliza como límite inferior. En el listado se muestran DNI, nombre y sexo.

### Sergio — altas, salario y listado físico

Para las altas, pedir los datos, crear el objeto y llamar:

```java
gestor.altaEmpleado(empleado);
```

Para modificar salario, buscar primero el empleado y mostrar el importe actual. Después pedir el nuevo:

```java
boolean realizado = gestor.modificarSalario(dni, nuevoSalario);
```

Para el listado físico, el motor ya tiene `consultarSecuencial()`.

Hay que añadir este método de enlace a GestorEmpleados y reflejarlo en el UML:

```java
public List<Empleado> listarSecuencial() throws IOException {
    return this.ficheroIndexado.consultarSecuencial();
}
```

Desde el menú:

```java
List<Empleado> empleados = gestor.listarSecuencial();
```

El listado muestra DNI, nombre y sexo en orden de almacenamiento, omitiendo las bajas.

### Antonio y Álvaro — paquete genérico

Tenemos que mantener coordinadas estas clases:

- RegistroConClave.
- IndiceRegistro.
- IndiceDAO.
- FicheroDirectoDAO.
- FicheroIndexado.

Al unir el proyecto, debemos utilizar una única versión de cada clase y mantener las mismas firmas.

## 6. Ajustes respecto al UML

Estos ajustes deben reflejarse en el diagrama final:

1. EmpleadoDAO utiliza `extends FicheroDirectoDAO<Empleado>`, no `implements`, porque la superclase es abstracta y tiene código común.
2. El constructor de FicheroIndexado recibe también el DAO concreto. Con dos rutas no puede conocer cómo leer y escribir un tipo T.
3. Empleado implementa RegistroConClave<String> y añade getClave().
4. FicheroIndexado utiliza posicionActual para conservar la selección realizada con posicionar().
5. FicheroDirectoDAO añade los métodos protegidos abstractos leerRegistro() y escribirRegistro().
6. GestorEmpleados necesita el método de enlace para el listado físico.

La consulta dinámica se ha definido como recorrido ascendente desde una clave. Debemos mantener esta interpretación al unir el código.

## 7. Bajas y modificaciones

La baja es lógica:

- El empleado queda marcado como inactivo.
- Su clave desaparece del índice.
- Los demás registros conservan sus posiciones.
- El tamaño del fichero de datos no disminuye.

Si se vuelve a dar de alta el mismo DNI, el nuevo registro se añade al final.

La modificación de salario mantiene el DNI y la posición física.

## 8. Programas de comprobación

### CompruebaEmpleados

Lee FICHENDX.DAT y muestra:

- Posición de cada registro.
- Datos del empleado.
- Indicación de BAJA cuando corresponda.
- Número de registros físicos.
- Tamaño del fichero.

### CompruebaIndice

Lee INDICE.DAT y muestra:

- DNI.
- Offset asociado.
- Número de entradas activas.

Estos programas permiten comprobar los ficheros sin ejecutar todo el menú.

## 9. Pruebas antes de entregar

1. Dar de alta empleados en orden inverso de DNI.
2. Comprobar que el listado por DNI queda ordenado.
3. Comprobar que el listado físico conserva el orden de las altas.
4. Intentar repetir un DNI.
5. Modificar un salario y comprobarlo al reabrir.
6. Cancelar una baja y comprobar que el empleado sigue existiendo.
7. Confirmar la baja y comprobar que desaparece de ambos listados.
8. Dar de alta otra vez el DNI borrado y comprobar que queda al final.
9. Ejecutar los dos programas de comprobación.
10. En una copia, retirar el índice con el programa cerrado y comprobar su reconstrucción.

La compilación y una prueba de integración del gestor se han comprobado con compatibilidad Java 7 utilizando JDK 17. Falta comprobar el menú completo cuando estén unidas todas las partes.

## 10. Excepciones y recursos

Las aplicaciones deben capturar las excepciones específicas que correspondan:

- FileNotFoundException.
- EOFException.
- IOException.
- NumberFormatException.
- IllegalArgumentException.
- ClassNotFoundException cuando se lee directamente el índice.

La entrada por teclado utiliza BufferedReader con InputStreamReader(System.in).

Los DAO cierran sus ficheros con try-with-resources. No mantienen un fichero abierto entre operaciones.

Se trabaja con una sola ejecución del programa a la vez. No hay transacciones ni compactación; si falla una escritura, hay que comprobar los datos antes de repetirla.

## 11. Cambios pendientes en la documentación

- Retirar el párrafo que indica que se ha descartado TreeMap.
- Explicar que el índice está en memoria y se guarda como objeto.
- Eliminar la descripción de entradas fijas de 26 bytes para el índice.
- Unificar el nombre FICHENDX.DAT.
- Indicar 92 bytes por registro y corregir las posiciones.
- Actualizar las firmas del UML.
- Anotar el tiempo real invertido por cada integrante.
