package indexado;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.Set;
import java.util.TreeSet;

/**
 * 
 * 
 * @author Equipo 3
 */
public class FicheroIndexado {

    private static final int TAMANO_BLOQUE_DATOS = 1024;

    private File ficheroDatos;
    private IndiceDAO indices;

    private RandomAccessFile recorrido;
    private boolean recorridoOrdenado;
    private long siguientePosicionFisica;
    private long siguienteEntrada;
    private long entradasRestantes;

    public FicheroIndexado(File ficheroDatos, File ficheroIndice) {
        this.ficheroDatos = ficheroDatos;
        this.indices = new IndiceDAO(ficheroIndice);

        reconstruirIndice();
    }

    public int reconstruirIndice() {
        int incidencias = 0;
        RandomAccessFile raf = null;

        indices.vaciar();

        if (ficheroDatos != null && ficheroDatos.exists()) {
            try {
                raf = new RandomAccessFile(ficheroDatos, "r");
                long longitudTotal = raf.length();
                long posicionActual = 0;

                if (longitudTotal % TAMANO_BLOQUE_DATOS != 0) {
                    System.err.println("Aviso: la longitud de " + ficheroDatos.getName()
                            + " no es múltiplo de " + TAMANO_BLOQUE_DATOS + " bytes.");
                    incidencias++;
                }

                while (posicionActual + TAMANO_BLOQUE_DATOS <= longitudTotal) {
                    raf.seek(posicionActual);
                    byte[] bloque = new byte[TAMANO_BLOQUE_DATOS];
                    raf.readFully(bloque);

                    if (bloqueActivo(bloque)) {
                        Comparable clave = claveDelBloque(bloque);

                        if (clave == null) {
                            System.err.println("Aviso: clave no válida en la posición " + posicionActual + ".");
                            incidencias++;
                        } else if (desempaquetarRegistro(bloque) == null) {
                            System.err.println("Aviso: registro ilegible en la posición " + posicionActual + ".");
                            incidencias++;
                        } else if (!indices.insertar(clave, posicionActual)) {
                            System.err.println("Aviso: clave repetida o que no cabe en el índice ["
                                    + clave + "] en la posición " + posicionActual + ".");
                            incidencias++;
                        }
                    }

                    posicionActual = posicionActual + TAMANO_BLOQUE_DATOS;
                }

                raf.close();
            } catch (IOException e) {
                System.err.println("Error al reconstruir el índice desde " + ficheroDatos.getName() + ": " + e.getMessage());
                cerrarRaf(raf);
                incidencias++;
            }
        }

        return incidencias;
    }

    public boolean alta(Comparable clave, Object registro) {
        boolean insertado = false;
        RandomAccessFile raf = null;
        long posicionInsercion = -1;

        if (clave != null && registro != null && estructuraDatosValida() && indices.admiteClave(clave)) {
            if (indices.buscar(clave) >= 0) {
                System.err.println("Aviso: Ya existe un registro con la clave [" + clave + "].");
            } else {
                byte[] bloque = empaquetarRegistro(true, clave, registro);

                if (bloque != null) {
                    try {
                        raf = new RandomAccessFile(ficheroDatos, "rw");
                        posicionInsercion = raf.length();

                        raf.seek(posicionInsercion);
                        raf.write(bloque);
                        raf.close();

                        insertado = indices.insertar(clave, posicionInsercion);

                        if (!insertado) {
                            System.err.println("Error: no se pudo guardar el índice en el alta de [" + clave + "].");
                            deshacerAlta(posicionInsercion);
                        }
                    } catch (IOException e) {
                        System.err.println("Error al dar de alta el registro con clave [" + clave + "]: " + e.getMessage());
                        cerrarRaf(raf);

                        if (posicionInsercion >= 0) {
                            deshacerAlta(posicionInsercion);
                        }
                    }
                }
            }
        }

        return insertado;
    }

    public boolean baja(Comparable clave) {
        boolean eliminado = false;
        RandomAccessFile raf = null;
        long posicion = posicionar(clave);

        if (posicion >= 0) {
            byte[] bloque = leerBloque(posicion);

            if (bloque != null && bloqueCoincideConClave(bloque, clave)) {
                try {
                    raf = new RandomAccessFile(ficheroDatos, "rw");
                    raf.seek(posicion);
                    raf.writeBoolean(false);
                    raf.close();

                    eliminado = indices.eliminar(clave);

                    if (!eliminado) {
                        System.err.println("Error: no se pudo guardar el índice en la baja de [" + clave + "].");
                        deshacerBaja(posicion);
                    }
                } catch (IOException e) {
                    System.err.println("Error al dar de baja el registro con clave [" + clave + "]: " + e.getMessage());
                    cerrarRaf(raf);
                }
            } else if (bloque != null) {
                System.err.println("Error: la clave del bloque no coincide con la del índice [" + clave + "].");
            }
        }

        return eliminado;
    }

    public boolean modificar(Comparable clave, Object registro) {
        boolean modificado = false;
        RandomAccessFile raf = null;
        long posicion = posicionar(clave);

        if (registro != null && posicion >= 0) {
            byte[] actual = leerBloque(posicion);

            if (actual != null && bloqueCoincideConClave(actual, clave)) {
                try {
                    byte[] bloque = empaquetarRegistro(true, clave, registro);
                    if (bloque != null) {
                        raf = new RandomAccessFile(ficheroDatos, "rw");
                        raf.seek(posicion);
                        raf.write(bloque);
                        raf.close();
                        modificado = true;
                    }
                } catch (IOException e) {
                    System.err.println("Error al modificar el registro con clave [" + clave + "]: " + e.getMessage());
                    cerrarRaf(raf);
                }
            } else if (actual != null) {
                System.err.println("Error: la clave del bloque no coincide con la del índice [" + clave + "].");
            }
        }

        return modificado;
    }

    public Object consultarAleatorio(Comparable clave) {
        Object resultado = null;
        long posicion = posicionar(clave);

        if (posicion >= 0) {
            byte[] bloque = leerBloque(posicion);

            if (bloque != null && bloqueCoincideConClave(bloque, clave)) {
                resultado = desempaquetarRegistro(bloque);
            } else if (bloque != null) {
                System.err.println("Error: la clave del bloque no coincide con la del índice [" + clave + "].");
            }
        }

        return resultado;
    }

    public void iniciarRecorridoFisico() {
        cerrarRecorrido();

        if (ficheroDatos != null && ficheroDatos.exists()) {
            try {
                recorrido = new RandomAccessFile(ficheroDatos, "r");
                recorridoOrdenado = false;
                siguientePosicionFisica = 0;
            } catch (IOException e) {
                System.err.println("Error al iniciar el recorrido físico: " + e.getMessage());
                recorrido = null;
            }
        }
    }

    public Object leerSiguienteFisico() {
        Object registro = null;

        if (recorrido != null && !recorridoOrdenado) {
            try {
                long longitudTotal = recorrido.length();

                while (registro == null
                        && siguientePosicionFisica + TAMANO_BLOQUE_DATOS <= longitudTotal) {
                    recorrido.seek(siguientePosicionFisica);
                    byte[] bloque = new byte[TAMANO_BLOQUE_DATOS];
                    recorrido.readFully(bloque);

                    siguientePosicionFisica = siguientePosicionFisica + TAMANO_BLOQUE_DATOS;
                    registro = desempaquetarRegistro(bloque);
                }
            } catch (IOException e) {
                System.err.println("Error en el recorrido físico: " + e.getMessage());
            }

            if (registro == null) {
                cerrarRecorrido();
            }
        }

        return registro;
    }

    public void iniciarRecorridoOrdenado(Comparable claveInicial, int limite) {
        cerrarRecorrido();

        if (ficheroDatos != null && ficheroDatos.exists()) {
            try {
                recorrido = new RandomAccessFile(ficheroDatos, "r");
                recorridoOrdenado = true;

                if (claveInicial == null) {
                    siguienteEntrada = 0;
                } else {
                    siguienteEntrada = indices.primeraEntradaDesde(claveInicial);
                }

                if (limite > 0) {
                    entradasRestantes = limite;
                } else {
                    entradasRestantes = Long.MAX_VALUE;
                }
            } catch (IOException e) {
                System.err.println("Error al iniciar el recorrido ordenado: " + e.getMessage());
                recorrido = null;
            }
        }
    }

    public Object leerSiguienteOrdenado() {
        Object registro = null;

        if (recorrido != null && recorridoOrdenado) {
            while (registro == null && entradasRestantes > 0) {
                IndiceRegistro entrada = indices.leerEntrada(siguienteEntrada);

                if (entrada == null) {
                    entradasRestantes = 0;
                } else {
                    siguienteEntrada++;

                    if (!posicionValida(entrada.getPosicion())) {
                        System.err.println("Error: la posición [" + entrada.getPosicion()
                                + "] de la clave [" + entrada.getClave() + "] no es válida.");
                    } else {
                        try {
                            recorrido.seek(entrada.getPosicion());
                            byte[] bloque = new byte[TAMANO_BLOQUE_DATOS];
                            recorrido.readFully(bloque);

                            if (bloqueCoincideConClave(bloque, entrada.getClave())) {
                                registro = desempaquetarRegistro(bloque);
                                if (registro != null) {
                                    entradasRestantes--;
                                }
                            } else {
                                System.err.println("Error: la clave del bloque no coincide con la del índice ["
                                        + entrada.getClave() + "].");
                            }
                        } catch (IOException e) {
                            System.err.println("Error en el recorrido ordenado: " + e.getMessage());
                        }
                    }
                }
            }

            if (registro == null) {
                cerrarRecorrido();
            }
        }

        return registro;
    }

    public void cerrarRecorrido() {
        cerrarRaf(recorrido);
        recorrido = null;
    }

    public long posicionar(Comparable clave) {
        long posicion = -1;

        if (clave != null) {
            posicion = indices.buscar(clave);

            if (posicion >= 0 && !posicionValida(posicion)) {
                System.err.println("Error: la posición [" + posicion + "] de la clave ["
                        + clave + "] no es válida en el fichero de datos.");
                posicion = -1;
            }
        }

        return posicion;
    }

    public Set getClaves() {
        TreeSet claves = new TreeSet();
        long total = indices.numeroEntradas();
        long i = 0;

        while (i < total) {
            IndiceRegistro entrada = indices.leerEntrada(i);

            if (entrada != null) {
                claves.add(entrada.getClave());
            }
            i++;
        }

        return claves;
    }

    public long getNumeroRegistros() {
        return indices.numeroEntradas();
    }

    private boolean estructuraDatosValida() {
        boolean valida = ficheroDatos != null && ficheroDatos.length() % TAMANO_BLOQUE_DATOS == 0;

        if (!valida) {
            System.err.println("Error: el fichero de datos no está formado por bloques de "
                    + TAMANO_BLOQUE_DATOS + " bytes.");
        }

        return valida;
    }

    private boolean posicionValida(long posicion) {
        return ficheroDatos != null
                && posicion >= 0
                && posicion % TAMANO_BLOQUE_DATOS == 0
                && posicion + TAMANO_BLOQUE_DATOS <= ficheroDatos.length();
    }

    private byte[] leerBloque(long posicion) {
        byte[] bloque = null;
        RandomAccessFile raf = null;

        try {
            raf = new RandomAccessFile(ficheroDatos, "r");
            raf.seek(posicion);
            bloque = new byte[TAMANO_BLOQUE_DATOS];
            raf.readFully(bloque);
            raf.close();
        } catch (IOException e) {
            System.err.println("Error al leer el bloque de la posición " + posicion + ": " + e.getMessage());
            cerrarRaf(raf);
            bloque = null;
        }

        return bloque;
    }

    private boolean bloqueActivo(byte[] bloque) {
        return bloque[0] != 0;
    }

    private Comparable claveDelBloque(byte[] bloque) {
        Comparable clave = null;

        try {
            DataInputStream dis = new DataInputStream(
                    new ByteArrayInputStream(bloque, 1, bloque.length - 1));
            clave = leerClaveDeFlujo(dis);
            dis.close();
        } catch (IOException e) {
            clave = null;
        }

        return clave;
    }

    private boolean bloqueCoincideConClave(byte[] bloque, Comparable clave) {
        boolean coincide = false;

        if (clave != null && bloqueActivo(bloque)) {
            Comparable claveBloque = claveDelBloque(bloque);

            if (claveBloque != null && claveBloque.getClass() == clave.getClass()) {
                coincide = claveBloque.compareTo(clave) == 0;
            }
        }

        return coincide;
    }

    private void deshacerAlta(long posicion) {
        RandomAccessFile raf = null;

        System.err.println("Se deshace el alta: se recorta el fichero de datos y se reconstruye el índice.");

        try {
            raf = new RandomAccessFile(ficheroDatos, "rw");
            raf.setLength(posicion);
            raf.close();
        } catch (IOException e) {
            System.err.println("No se pudo deshacer el alta en el fichero de datos: " + e.getMessage());
            cerrarRaf(raf);
        }

        reconstruirIndice();
    }

    private void deshacerBaja(long posicion) {
        RandomAccessFile raf = null;

        System.err.println("Se deshace la baja: se reactiva el registro y se reconstruye el índice.");

        try {
            raf = new RandomAccessFile(ficheroDatos, "rw");
            raf.seek(posicion);
            raf.writeBoolean(true);
            raf.close();
        } catch (IOException e) {
            System.err.println("No se pudo deshacer la baja en el fichero de datos: " + e.getMessage());
            cerrarRaf(raf);
        }

        reconstruirIndice();
    }

    private byte[] empaquetarRegistro(boolean activo, Comparable clave, Object registro) {
        byte[] bloqueFijo = null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeBoolean(activo);

            if (clave instanceof String) {
                dos.writeChar('S');
                dos.writeUTF((String) clave);
            } else if (clave instanceof Integer) {
                dos.writeChar('I');
                dos.writeInt(((Integer) clave).intValue());
            } else if (clave instanceof Long) {
                dos.writeChar('L');
                dos.writeLong(((Long) clave).longValue());
            }

            ObjectOutputStream oos = new ObjectOutputStream(dos);
            oos.writeObject(registro);
            oos.flush();

            byte[] datosCrudos = baos.toByteArray();

            oos.close();
            dos.close();
            baos.close();

            if (datosCrudos.length <= TAMANO_BLOQUE_DATOS) {
                bloqueFijo = new byte[TAMANO_BLOQUE_DATOS];
                System.arraycopy(datosCrudos, 0, bloqueFijo, 0, datosCrudos.length);
            } else {
                System.err.println("Error: El registro excede el tamaño máximo de bloque (" + TAMANO_BLOQUE_DATOS + " bytes).");
            }
        } catch (IOException e) {
            System.err.println("Error al empaquetar el registro en bytes: " + e.getMessage());
        }

        return bloqueFijo;
    }

    private Object desempaquetarRegistro(byte[] bloque) {
        Object registro = null;

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bloque);
            DataInputStream dis = new DataInputStream(bais);

            boolean activo = dis.readBoolean();
            if (activo) {

                // Solo se lee el objeto si la clave de cabecera es correcta
                if (leerClaveDeFlujo(dis) != null) {
                    ObjectInputStream ois = new ObjectInputStream(dis);
                    registro = ois.readObject();
                    ois.close();
                }
            }

            dis.close();
            bais.close();
        } catch (IOException e) {
            System.err.println("Error de lectura al desempaquetar registro: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Error: Clase de registro no encontrada al deserializar: " + e.getMessage());
        }

        return registro;
    }

    /**
     * Lee la clave (tipo + valor) del flujo. Devuelve null si el tipo no
     * es válido o si no se puede leer.
     */
    private Comparable leerClaveDeFlujo(DataInputStream dis) {
        Comparable clave = null;

        try {
            char tipoClave = dis.readChar();

            if (tipoClave == 'S') {
                clave = dis.readUTF();
            } else if (tipoClave == 'I') {
                clave = new Integer(dis.readInt());
            } else if (tipoClave == 'L') {
                clave = new Long(dis.readLong());
            }
        } catch (IOException e) {
            clave = null;
        }

        return clave;
    }

    private void cerrarRaf(RandomAccessFile raf) {
        try {
            if (raf != null) {
                raf.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar el fichero de datos: " + e.getMessage());
        }
    }
}