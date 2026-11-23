
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
/**
 *
 * @author Antonio Muñoz Herrera
 */
public class FicheroIndexado {
    
// Tamaño fijo reservado por registro en el fichero de datos (en bytes)
    private static final int TAMANO_BLOQUE_DATOS = 512;

    private File ficheroDatos;
    private IndiceDAO indices;
    private TreeMap indice;

    public FicheroIndexado(File ficheroDatos, File ficheroIndice) {
        this.ficheroDatos = ficheroDatos;
        this.indices = new IndiceDAO(ficheroIndice);
        this.indice = this.indices.leer();

        if (this.indice.isEmpty() && ficheroDatos != null && ficheroDatos.exists() && ficheroDatos.length() > 0) {
            reconstruirIndice();
        }
    }

    public void reconstruirIndice() {
        indice.clear();
        RandomAccessFile raf = null;

        if (ficheroDatos != null && ficheroDatos.exists()) {
            try {
                raf = new RandomAccessFile(ficheroDatos, "r");
                long longitudTotal = raf.length();
                long posicionActual = 0;

                while (posicionActual + TAMANO_BLOQUE_DATOS <= longitudTotal) {
                    raf.seek(posicionActual);
                    byte[] bloque = new byte[TAMANO_BLOQUE_DATOS];
                    raf.readFully(bloque);

                    ByteArrayInputStream bais = new ByteArrayInputStream(bloque);
                    DataInputStream dis = new DataInputStream(bais);

                    boolean activo = dis.readBoolean();
                    if (activo) {
                        Comparable clave = leerClaveDeFlujo(dis);
                        if (clave != null) {
                            indice.put(clave, new Long(posicionActual));
                        }
                    }

                    dis.close();
                    bais.close();
                    posicionActual = posicionActual + TAMANO_BLOQUE_DATOS;
                }

                raf.close();
                indices.guardar(indice);
            } catch (IOException e) {
                System.err.println("Error al reconstruir el índice desde " + ficheroDatos.getName() + ": " + e.getMessage());
                cerrarRaf(raf);
            }
        }
    }

    public boolean alta(Comparable clave, Object registro) {
        boolean insertado = false;
        RandomAccessFile raf = null;

        if (clave != null && registro != null && !indice.containsKey(clave)) {
            try {
                byte[] bloque = empaquetarRegistro(true, clave, registro);
                if (bloque != null) {
                    raf = new RandomAccessFile(ficheroDatos, "rw");
                    long posicionInsercion = raf.length();

                    raf.seek(posicionInsercion);
                    raf.write(bloque);
                    raf.close();

                    indice.put(clave, new Long(posicionInsercion));
                    indices.guardar(indice);
                    insertado = true;
                }
            } catch (IOException e) {
                System.err.println("Error al dar de alta el registro con clave [" + clave + "]: " + e.getMessage());
                cerrarRaf(raf);
            }
        } else if (clave != null && indice.containsKey(clave)) {
            System.err.println("Aviso: Ya existe un registro con la clave [" + clave + "].");
        }

        return insertado;
    }

    public boolean baja(Comparable clave) {
        boolean eliminado = false;
        RandomAccessFile raf = null;

        if (clave != null && indice.containsKey(clave)) {
            long posicion = posicionar(clave);
            if (posicion >= 0) {
                try {
                    raf = new RandomAccessFile(ficheroDatos, "rw");
                    raf.seek(posicion);
                    raf.writeBoolean(false);
                    raf.close();

                    indice.remove(clave);
                    indices.guardar(indice);
                    eliminado = true;
                } catch (IOException e) {
                    System.err.println("Error al dar de baja el registro con clave [" + clave + "]: " + e.getMessage());
                    cerrarRaf(raf);
                }
            }
        }

        return eliminado;
    }

    public boolean modificar(Comparable clave, Object registro) {
        boolean modificado = false;
        RandomAccessFile raf = null;

        if (clave != null && registro != null && indice.containsKey(clave)) {
            long posicion = posicionar(clave);
            if (posicion >= 0) {
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
            }
        }

        return modificado;
    }

    public Object consultarAleatorio(Comparable clave) {
        Object resultado = null;
        RandomAccessFile raf = null;

        if (clave != null && indice.containsKey(clave) && ficheroDatos.exists()) {
            long posicion = posicionar(clave);
            if (posicion >= 0) {
                try {
                    raf = new RandomAccessFile(ficheroDatos, "r");
                    raf.seek(posicion);
                    byte[] bloque = new byte[TAMANO_BLOQUE_DATOS];
                    raf.readFully(bloque);
                    raf.close();

                    resultado = desempaquetarRegistro(bloque);
                } catch (IOException e) {
                    System.err.println("Error en consulta aleatoria para clave [" + clave + "]: " + e.getMessage());
                    cerrarRaf(raf);
                }
            }
        }

        return resultado;
    }

    public List consultarSecuencial() {
        List lista = new ArrayList();
        RandomAccessFile raf = null;

        if (!indice.isEmpty() && ficheroDatos.exists()) {
            try {
                raf = new RandomAccessFile(ficheroDatos, "r");
                Iterator it = indice.entrySet().iterator();

                while (it.hasNext()) {
                    Map.Entry entrada = (Map.Entry) it.next();
                    Long posicion = (Long) entrada.getValue();

                    raf.seek(posicion.longValue());
                    byte[] bloque = new byte[TAMANO_BLOQUE_DATOS];
                    raf.readFully(bloque);

                    Object registro = desempaquetarRegistro(bloque);
                    if (registro != null) {
                        lista.add(registro);
                    }
                }

                raf.close();
            } catch (IOException e) {
                System.err.println("Error en consulta secuencial por clave: " + e.getMessage());
                cerrarRaf(raf);
            }
        }

        return lista;
    }

    public List consultarSecuencialFisico() {
        List lista = new ArrayList();
        RandomAccessFile raf = null;

        if (ficheroDatos != null && ficheroDatos.exists() && ficheroDatos.length() > 0) {
            try {
                raf = new RandomAccessFile(ficheroDatos, "r");
                long longitudTotal = raf.length();
                long posicionActual = 0;

                while (posicionActual + TAMANO_BLOQUE_DATOS <= longitudTotal) {
                    raf.seek(posicionActual);
                    byte[] bloque = new byte[TAMANO_BLOQUE_DATOS];
                    raf.readFully(bloque);

                    Object registro = desempaquetarRegistro(bloque);
                    if (registro != null) {
                        lista.add(registro);
                    }
                    posicionActual = posicionActual + TAMANO_BLOQUE_DATOS;
                }

                raf.close();
            } catch (IOException e) {
                System.err.println("Error en consulta secuencial física: " + e.getMessage());
                cerrarRaf(raf);
            }
        }

        return lista;
    }

    public List consultarDinamico(Comparable claveInicial, int limite) {
        List lista = new ArrayList();
        RandomAccessFile raf = null;

        if (claveInicial != null && limite > 0 && !indice.isEmpty() && ficheroDatos.exists()) {
            try {
                SortedMap subMapa = indice.tailMap(claveInicial);
                raf = new RandomAccessFile(ficheroDatos, "r");
                Iterator it = subMapa.entrySet().iterator();
                int contador = 0;

                while (it.hasNext() && contador < limite) {
                    Map.Entry entrada = (Map.Entry) it.next();
                    Long posicion = (Long) entrada.getValue();

                    raf.seek(posicion.longValue());
                    byte[] bloque = new byte[TAMANO_BLOQUE_DATOS];
                    raf.readFully(bloque);

                    Object registro = desempaquetarRegistro(bloque);
                    if (registro != null) {
                        lista.add(registro);
                        contador++;
                    }
                }

                raf.close();
            } catch (IOException e) {
                System.err.println("Error en consulta dinámica desde clave [" + claveInicial + "]: " + e.getMessage());
                cerrarRaf(raf);
            }
        }

        return lista;
    }

    public long posicionar(Comparable clave) {
        long posicion = -1;

        if (clave != null && indice.containsKey(clave)) {
            Long posObj = (Long) indice.get(clave);
            if (posObj != null) {
                posicion = posObj.longValue();
            }
        }

        return posicion;
    }

    public Set getClaves() {
        return indice.keySet();
    }

    public long getNumeroRegistros() {
        return (long) indice.size();
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

                leerClaveDeFlujo(dis);

                ObjectInputStream ois = new ObjectInputStream(dis);
                registro = ois.readObject();
                ois.close();
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

    private Comparable leerClaveDeFlujo(DataInputStream dis) throws IOException {
        Comparable clave = null;
        char tipoClave = dis.readChar();

        if (tipoClave == 'S') {
            clave = dis.readUTF();
        } else if (tipoClave == 'I') {
            clave = new Integer(dis.readInt());
        } else if (tipoClave == 'L') {
            clave = new Long(dis.readLong());
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
