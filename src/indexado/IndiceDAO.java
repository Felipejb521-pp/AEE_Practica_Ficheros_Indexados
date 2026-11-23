package indexado;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author Equipo 3
 *
 * Fichero de índice ordenado por clave, con entradas de longitud fija:
 *   bytes 0-21  : tipo de clave (char) + valor de la clave, con relleno a 0
 *   bytes 22-29 : posición del registro en el fichero de datos (long)
 *
 * Ningún método declara throws: cada uno captura sus IOException.
 * Los métodos privados que fallan guardan el motivo en el atributo
 * "error" y devuelven un valor especial (null, -1 o false); el método
 * público que los llamó muestra ese error al terminar.
 */
public class IndiceDAO {

    private static final int TAMANO_ENTRADA = 30;
    private static final int OFFSET_POSICION = 22;

    private File fichero;

    // Motivo del último fallo dentro de una operación (null si no hay)
    private String error;

    public IndiceDAO(File fichero) {
        if (fichero == null) {
            throw new IllegalArgumentException("Falta el fichero de índice.");
        }
        this.fichero = fichero;
    }

    // ---------------------------------------------------------------
    // Métodos públicos
    // ---------------------------------------------------------------

    /** Número de entradas del índice, 0 si no existe o -1 si hay error. */
    public long numeroEntradas() {
        long total = 0;
        error = null;

        if (fichero.exists()) {
            RandomAccessFile raf = abrirOrigen();

            if (raf != null) {
                total = comprobarLongitud(raf);
                cerrar(raf);
            }

            if (error != null) {
                mostrarError("contar las entradas");
                total = -1;
            }
        }

        return total;
    }

    /** Indica si la clave se puede guardar en este índice. */
    public boolean admiteClave(Comparable clave) {
        boolean admite = false;
        error = null;

        if (empaquetarEntrada(clave, 0) != null) {
            RandomAccessFile raf = abrirOrigen();

            if (raf != null) {
                long total = comprobarLongitud(raf);

                if (total >= 0) {
                    tipoCompatible(raf, clave, total);
                }
                cerrar(raf);
            }
        }

        if (error == null) {
            admite = true;
        } else {
            mostrarError("comprobar la clave");
        }

        return admite;
    }

    /** Posición del registro con esa clave, o -1 si no está. */
    public long buscar(Comparable clave) {
        long posicion = -1;
        error = null;

        if (empaquetarEntrada(clave, 0) != null) {
            RandomAccessFile raf = abrirOrigen();

            if (raf != null) {
                long total = comprobarLongitud(raf);

                if (total >= 0 && tipoCompatible(raf, clave, total)) {
                    long numero = primeraEntradaDesde(raf, clave, total);

                    if (numero >= 0 && numero < total) {
                        IndiceRegistro entrada = leerEntrada(raf, numero);

                        if (entrada != null && comparar(entrada.getClave(), clave) == 0
                                && error == null) {
                            posicion = entrada.getPosicion();
                        }
                    }
                }
                cerrar(raf);
            }
        }

        if (error != null) {
            mostrarError("buscar la clave");
            posicion = -1;
        }

        return posicion;
    }

    /** Número de la primera entrada con clave mayor o igual (-1 si error). */
    public long primeraEntradaDesde(Comparable clave) {
        long numero = 0;
        error = null;

        if (empaquetarEntrada(clave, 0) != null) {
            RandomAccessFile raf = abrirOrigen();

            if (raf != null) {
                long total = comprobarLongitud(raf);

                if (total >= 0 && tipoCompatible(raf, clave, total)) {
                    numero = primeraEntradaDesde(raf, clave, total);
                }
                cerrar(raf);
            }
        }

        if (error != null) {
            mostrarError("buscar el inicio del recorrido");
            numero = -1;
        }

        return numero;
    }

    /** Entrada número "numero" del índice, o null si no existe. */
    public IndiceRegistro leerEntrada(long numero) {
        IndiceRegistro entrada = null;
        error = null;

        if (numero >= 0) {
            RandomAccessFile raf = abrirOrigen();

            if (raf != null) {
                long total = comprobarLongitud(raf);

                if (numero < total) {
                    entrada = leerEntrada(raf, numero);
                }
                cerrar(raf);
            }

            if (error != null) {
                mostrarError("leer la entrada");
                entrada = null;
            }
        }

        return entrada;
    }

    public boolean insertar(Comparable clave, long posicion) {
        boolean hecho;
        error = null;

        hecho = cambiarEntrada(clave, posicion, false);

        if (error != null) {
            mostrarError("insertar la entrada");
            hecho = false;
        }

        return hecho;
    }

    public boolean eliminar(Comparable clave) {
        boolean hecho;
        error = null;

        hecho = cambiarEntrada(clave, 0, true);

        if (error != null) {
            mostrarError("eliminar la entrada");
            hecho = false;
        }

        return hecho;
    }

    /** Deja el índice vacío. */
    public void vaciar() {
        error = null;
        File temporal = crearTemporal();

        if (temporal != null) {
            try (RandomAccessFile salida = new RandomAccessFile(temporal, "rw")) {
                salida.setLength(0);
                salida.getFD().sync();
            } catch (IOException e) {
                error = e.getMessage();
            }

            if (error == null) {
                sustituir(temporal);
            }
        }

        if (error != null) {
            mostrarError("vaciar el índice");
        }

        borrarTemporal(temporal);
    }

    /** Sustituye el índice por otro fichero ya ordenado y sin repetidos. */
    boolean reemplazarCon(File temporal) {
        boolean hecho = false;
        error = null;

        try {
            if (temporal == null || !temporal.isFile()
                    || temporal.getCanonicalFile().equals(fichero.getCanonicalFile())) {
                error = "El índice temporal no es válido.";
            }
        } catch (IOException e) {
            error = e.getMessage();
        }

        if (error == null) {
            try (RandomAccessFile entrada = new RandomAccessFile(temporal, "r")) {
                long total = comprobarLongitud(entrada);
                Comparable anterior = null;
                long i = 0;

                while (error == null && i < total) {
                    IndiceRegistro registro = leerEntrada(entrada, i);

                    if (registro != null) {
                        if (anterior != null && comparar(anterior, registro.getClave()) >= 0
                                && error == null) {
                            error = "El índice temporal no está ordenado o contiene duplicados.";
                        }
                        anterior = registro.getClave();
                    }
                    i++;
                }
            } catch (IOException e) {
                error = e.getMessage();
            }
        }

        if (error == null) {
            hecho = sustituir(temporal);
        }

        if (error != null) {
            mostrarError("reemplazar el índice");
        }

        return hecho;
    }

    // ---------------------------------------------------------------
    // Métodos privados: si fallan guardan el motivo en "error"
    // ---------------------------------------------------------------

    /** Abre el índice para leer. Devuelve null si no existe o hay error. */
    private RandomAccessFile abrirOrigen() {
        RandomAccessFile raf = null;

        if (fichero.exists()) {
            if (!fichero.isFile()) {
                error = "La ruta del índice no es un fichero.";
            } else {
                try {
                    raf = new RandomAccessFile(fichero, "r");
                } catch (IOException e) {
                    error = e.getMessage();
                }
            }
        }

        return raf;
    }

    private void cerrar(RandomAccessFile raf) {
        try {
            if (raf != null) {
                raf.close();
            }
        } catch (IOException e) {
            error = e.getMessage();
        }
    }

    /** Número de entradas, o -1 si hay una incompleta o un error. */
    private long comprobarLongitud(RandomAccessFile raf) {
        long total = -1;

        try {
            if (raf.length() % TAMANO_ENTRADA != 0) {
                error = "El índice contiene una entrada incompleta.";
            } else {
                total = raf.length() / TAMANO_ENTRADA;
            }
        } catch (IOException e) {
            error = e.getMessage();
        }

        return total;
    }

    /** Comprueba que la clave es del mismo tipo que las del índice. */
    private boolean tipoCompatible(RandomAccessFile raf, Comparable clave, long total) {
        boolean compatible = true;

        if (total > 0) {
            IndiceRegistro primera = leerEntrada(raf, 0);

            if (primera == null) {
                compatible = false;
            } else {
                comparar(primera.getClave(), clave);
                compatible = (error == null);
            }
        }

        return compatible;
    }

    /** Compara dos claves del mismo tipo. Si no lo son, anota el error. */
    private int comparar(Comparable primera, Comparable segunda) {
        int resultado = 0;

        if (primera == null || segunda == null
                || primera.getClass() != segunda.getClass()) {
            error = "Un índice no puede mezclar tipos de clave.";
        } else {
            resultado = primera.compareTo(segunda);
        }

        return resultado;
    }

    /** Búsqueda binaria de la primera entrada con clave >= clave. */
    private long primeraEntradaDesde(RandomAccessFile raf, Comparable clave, long total) {
        long bajo = 0;
        long alto = total;

        while (bajo < alto && error == null) {
            long medio = bajo + (alto - bajo) / 2;
            IndiceRegistro entrada = leerEntrada(raf, medio);

            if (entrada != null) {
                if (comparar(entrada.getClave(), clave) < 0) {
                    bajo = medio + 1;
                } else {
                    alto = medio;
                }
            }
        }

        if (error != null) {
            bajo = -1;
        }

        return bajo;
    }

    /**
     * Inserta o elimina una entrada. Copia el índice a un temporal
     * metiendo (o quitando) la entrada en su sitio, y luego lo sustituye.
     * Devuelve false si la clave ya estaba (insertar) o no estaba (eliminar).
     */
    private boolean cambiarEntrada(Comparable clave, long posicion, boolean eliminar) {
        boolean hecho = false;
        byte[] nueva = empaquetarEntrada(clave, posicion);
        File temporal = null;
        boolean continuar = false;
        RandomAccessFile origen = null;
        long total = 0;
        long hueco = 0;

        // 1. Localizar el hueco de la clave y saber si ya existe
        if (nueva != null) {
            origen = abrirOrigen();

            if (origen != null) {
                total = comprobarLongitud(origen);

                if (total >= 0) {
                    tipoCompatible(origen, clave, total);
                }
            }

            if (error == null) {
                hueco = primeraEntradaDesde(origen, clave, total);
                boolean existe = false;

                if (error == null && hueco < total) {
                    IndiceRegistro entrada = leerEntrada(origen, hueco);

                    if (entrada != null) {
                        existe = comparar(entrada.getClave(), clave) == 0;
                    }
                }

                continuar = (error == null) && (eliminar == existe);
            }
        }

        // 2. Copiar el índice al temporal con la entrada añadida o quitada
        if (continuar) {
            temporal = crearTemporal();

            if (temporal != null) {
                try (RandomAccessFile salida = new RandomAccessFile(temporal, "rw")) {
                    Comparable anterior = null;
                    long i = 0;

                    while (error == null && i < total) {
                        byte[] bloque = leerBloque(origen, i);
                        IndiceRegistro entrada = null;

                        if (bloque != null) {
                            entrada = desempaquetarEntrada(bloque);
                        }

                        if (entrada != null) {
                            if (anterior != null && comparar(anterior, entrada.getClave()) >= 0
                                    && error == null) {
                                error = "El índice no está ordenado o tiene claves repetidas.";
                            }
                            anterior = entrada.getClave();
                        }

                        if (error == null) {
                            if (!eliminar && i == hueco) {
                                salida.write(nueva);
                            }
                            if (!eliminar || i != hueco) {
                                salida.write(bloque);
                            }
                        }
                        i++;
                    }

                    if (error == null && !eliminar && hueco == total) {
                        salida.write(nueva);
                    }
                    salida.getFD().sync();
                } catch (IOException e) {
                    error = e.getMessage();
                }
            }
        }

        cerrar(origen);

        // 3. Sustituir el índice por el temporal
        if (continuar && error == null) {
            hecho = sustituir(temporal);
        }

        borrarTemporal(temporal);

        return hecho;
    }

    /** Lee los 30 bytes de la entrada "numero", o null si hay error. */
    private byte[] leerBloque(RandomAccessFile raf, long numero) {
        byte[] bloque = null;
        long total = comprobarLongitud(raf);

        if (total >= 0) {
            if (numero < 0 || numero >= total) {
                error = "Número de entrada fuera del índice.";
            } else {
                try {
                    bloque = new byte[TAMANO_ENTRADA];
                    raf.seek(numero * TAMANO_ENTRADA);
                    raf.readFully(bloque);
                } catch (IOException e) {
                    error = e.getMessage();
                    bloque = null;
                }
            }
        }

        return bloque;
    }

    private IndiceRegistro leerEntrada(RandomAccessFile raf, long numero) {
        IndiceRegistro entrada = null;
        byte[] bloque = leerBloque(raf, numero);

        if (bloque != null) {
            entrada = desempaquetarEntrada(bloque);
        }

        return entrada;
    }

    /** Convierte 30 bytes en una entrada (clave, posición). */
    private IndiceRegistro desempaquetarEntrada(byte[] bloque) {
        IndiceRegistro registro = null;
        Comparable clave = null;
        long posicion = -1;

        try (DataInputStream entrada = new DataInputStream(
                new ByteArrayInputStream(bloque, 0, OFFSET_POSICION));
                DataInputStream entradaPosicion = new DataInputStream(
                new ByteArrayInputStream(bloque, OFFSET_POSICION, 8))) {
            char tipo = entrada.readChar();

            if (tipo == 'S') {
                clave = entrada.readUTF();
            } else if (tipo == 'I') {
                clave = Integer.valueOf(entrada.readInt());
            } else if (tipo == 'L') {
                clave = Long.valueOf(entrada.readLong());
            } else {
                error = "Tipo de clave incorrecto en el índice.";
            }

            // El resto de los 22 bytes de la clave debe ser relleno a 0
            while (error == null && entrada.available() > 0) {
                if (entrada.readUnsignedByte() != 0) {
                    error = "El relleno de la entrada no es válido.";
                }
            }

            if (error == null) {
                posicion = entradaPosicion.readLong();

                if (posicion < 0) {
                    error = "La posición del registro no puede ser negativa.";
                }
            }
        } catch (IOException e) {
            error = e.getMessage();
        }

        if (error == null) {
            registro = new IndiceRegistro(clave, posicion);
        }

        return registro;
    }

    /** Convierte (clave, posición) en 30 bytes, o null si no se puede. */
    private byte[] empaquetarEntrada(Comparable clave, long posicion) {
        byte[] bloque = null;
        byte[] datosClave = null;

        if (posicion < 0) {
            error = "La posición del registro no puede ser negativa.";
        } else if (!(clave instanceof String) && !(clave instanceof Integer)
                && !(clave instanceof Long)) {
            error = "Solo se admiten claves String, Integer o Long.";
        } else {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();

            try (DataOutputStream salida = new DataOutputStream(bytes)) {
                if (clave instanceof String) {
                    salida.writeChar('S');
                    salida.writeUTF((String) clave);
                } else if (clave instanceof Integer) {
                    salida.writeChar('I');
                    salida.writeInt(((Integer) clave).intValue());
                } else {
                    salida.writeChar('L');
                    salida.writeLong(((Long) clave).longValue());
                }
            } catch (IOException e) {
                error = e.getMessage();
            }

            if (error == null) {
                datosClave = bytes.toByteArray();

                if (datosClave.length > OFFSET_POSICION) {
                    error = "La clave no cabe en los 22 bytes reservados.";
                }
            }
        }

        if (error == null) {
            ByteArrayOutputStream bytesPosicion = new ByteArrayOutputStream();

            try (DataOutputStream salida = new DataOutputStream(bytesPosicion)) {
                salida.writeLong(posicion);
            } catch (IOException e) {
                error = e.getMessage();
            }

            if (error == null) {
                bloque = new byte[TAMANO_ENTRADA];
                System.arraycopy(datosClave, 0, bloque, 0, datosClave.length);
                System.arraycopy(bytesPosicion.toByteArray(), 0, bloque, OFFSET_POSICION, 8);
            }
        }

        return bloque;
    }

    /** Crea un fichero temporal junto al índice, o null si hay error. */
    private File crearTemporal() {
        File temporal = null;

        if (fichero.exists() && !fichero.isFile()) {
            error = "La ruta del índice no es un fichero.";
        } else {
            try {
                temporal = File.createTempFile("indice-", ".tmp",
                        fichero.getAbsoluteFile().getParentFile());
            } catch (IOException e) {
                error = e.getMessage();
            }
        }

        return temporal;
    }

    /**
     * Cambia el índice por el temporal guardando antes una copia del
     * anterior, para restaurarlo si el cambio de nombre falla.
     */
    private boolean sustituir(File temporal) {
        boolean hecho = false;
        File copia = null;
        boolean seguir = true;

        if (fichero.exists()) {
            if (!fichero.isFile()) {
                error = "La ruta del índice no es un fichero.";
                seguir = false;
            } else {
                try {
                    copia = File.createTempFile("indice-anterior-", ".tmp",
                            fichero.getAbsoluteFile().getParentFile());
                } catch (IOException e) {
                    error = e.getMessage();
                    seguir = false;
                }

                if (seguir && (!copia.delete() || !fichero.renameTo(copia))) {
                    error = "No se pudo conservar el índice anterior.";
                    seguir = false;
                }
            }
        }

        if (seguir) {
            if (temporal.renameTo(fichero)) {
                hecho = true;
                borrarTemporal(copia);
            } else if (copia != null && !copia.renameTo(fichero)) {
                error = "No se pudo restaurar el índice. Copia conservada en: " + copia;
            } else {
                error = "No se pudo sustituir el índice.";
            }
        }

        return hecho;
    }

    private void borrarTemporal(File temporal) {
        if (temporal != null && temporal.exists() && !temporal.delete()) {
            System.err.println("No se pudo borrar el temporal: " + temporal);
        }
    }

    private void mostrarError(String operacion) {
        System.err.println("Error al " + operacion + " (" + fichero.getName() + "): " + error);
    }
}
