
package indexado;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Equipo 3
 */
public class IndiceDAO {
    
    private File fichero;

    public IndiceDAO(File fichero) {
        this.fichero = fichero;
    }
    
    public TreeMap leer() {
        TreeMap indice = new TreeMap();
        FileInputStream fis = null;
        DataInputStream dis = null;
        boolean finFichero = false;

        if (fichero != null && fichero.exists() && fichero.length() > 0) {
            try {
                fis = new FileInputStream(fichero);
                dis = new DataInputStream(fis);

                while (!finFichero) {
                    try {
                        char tipoClave = dis.readChar();
                        Comparable clave = null;

                        if (tipoClave == 'S') {
                            clave = dis.readUTF();
                        } else if (tipoClave == 'I') {
                            clave = new Integer(dis.readInt());
                        } else if (tipoClave == 'L') {
                            clave = new Long(dis.readLong());
                        }

                        long posicion = dis.readLong();

                        if (clave != null) {
                            IndiceRegistro entrada = new IndiceRegistro(clave, posicion);
                            indice.put(entrada.getClave(), new Long(entrada.getPosicion()));
                        }
                    } catch (EOFException e) {
                        finFichero = true;
                    }
                }

                dis.close();
                fis.close();
            } catch (IOException e) {
                System.err.println("Error de lectura en el fichero de índice (" + fichero.getName() + "): " + e.getMessage());
                cerrarFlujosLectura(dis, fis);
            }
        }

        return indice;
    }

    public void guardar(TreeMap indice) {
        FileOutputStream fos = null;
        DataOutputStream dos = null;

        if (fichero != null && indice != null) {
            try {
                fos = new FileOutputStream(fichero);
                dos = new DataOutputStream(fos);

                Iterator it = indice.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry entradaMapa = (Map.Entry) it.next();
                    Comparable clave = (Comparable) entradaMapa.getKey();
                    Long posicion = (Long) entradaMapa.getValue();

                    IndiceRegistro registro = new IndiceRegistro(clave, posicion.longValue());

                    if (registro.getClave() instanceof String) {
                        dos.writeChar('S');
                        dos.writeUTF((String) registro.getClave());
                    } else if (registro.getClave() instanceof Integer) {
                        dos.writeChar('I');
                        dos.writeInt(((Integer) registro.getClave()).intValue());
                    } else if (registro.getClave() instanceof Long) {
                        dos.writeChar('L');
                        dos.writeLong(((Long) registro.getClave()).longValue());
                    }

                    dos.writeLong(registro.getPosicion());
                }

                dos.close();
                fos.close();
            } catch (IOException e) {
                System.err.println("Error al guardar el fichero de índice (" + fichero.getName() + "): " + e.getMessage());
                cerrarFlujosEscritura(dos, fos);
            }
        }
    }

    private void cerrarFlujosLectura(DataInputStream dis, FileInputStream fis) {
        try {
            if (dis != null) {
                dis.close();
            }
            if (fis != null) {
                fis.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar los flujos de lectura del índice: " + e.getMessage());
        }
    }

    private void cerrarFlujosEscritura(DataOutputStream dos, FileOutputStream fos) {
        try {
            if (dos != null) {
                dos.close();
            }
            if (fos != null) {
                fos.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar los flujos de escritura del índice: " + e.getMessage());
        }
    }
}
