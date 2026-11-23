import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author Álvaro López De San Román
 *
 * Recorre FICHENDX.DAT bloque a bloque y muestra la posición de cada
 * registro, si está activo o de baja y su DNI.
 *
 * El paquete indexado guarda cada registro en un bloque de 512 bytes:
 *   boolean activo | char tipo de clave ('S') | DNI (writeUTF) |
 *   objeto Empleado serializado | relleno hasta 512 bytes
 */
public class CompruebaEmpleados {

    private static final int TAMANO_BLOQUE = 512;

    public static void main(String[] args) {
        File fichero = new File("Ficheros", "FICHENDX.DAT");

        if (!fichero.exists()) {
            System.out.println("No existe FICHENDX.DAT.");
        } else {
            try (RandomAccessFile datos = new RandomAccessFile(fichero, "r")) {
                long numero = datos.length() / TAMANO_BLOQUE;

                for (long i = 0; i < numero; i++) {
                    long posicion = i * TAMANO_BLOQUE;
                    datos.seek(posicion);

                    boolean activo = datos.readBoolean();
                    char tipoClave = datos.readChar();
                    String clave = "?";

                    if (tipoClave == 'S') {
                        clave = datos.readUTF();
                    }

                    if (activo) {
                        System.out.println("Posición " + posicion + ": " + clave);
                    } else {
                        System.out.println("Posición " + posicion + ": BAJA (" + clave + ")");
                    }
                }

                System.out.println("Registros físicos: " + numero);
                System.out.println("Bytes: " + fichero.length());

                if (fichero.length() % TAMANO_BLOQUE != 0) {
                    System.out.println("Aviso: el último bloque está incompleto.");
                }

            } catch (FileNotFoundException e) {
                System.out.println("No se puede abrir el fichero.");
            } catch (EOFException e) {
                System.out.println("El fichero está incompleto.");
            } catch (IOException e) {
                System.out.println("Error al leer: " + e.getMessage());
            }
        }
    }
}
