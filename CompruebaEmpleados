import java.io.File;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author Álvaro López De San Román
 */
public class CompruebaEmpleados {

    public static void main(String[] args) {
        File fichero = new File("Ficheros", "FICHENDX.DAT");

        if (!fichero.exists()) {
            System.out.println("No existe FICHENDX.DAT.");
            return;
        }

        EmpleadoDAO dao = new EmpleadoDAO(fichero);

        try {
            long numero = dao.getNumeroRegistros();

            for (long i = 0; i < numero; i++) {
                long posicion = i * EmpleadoDAO.LONGITUD_REGISTRO;

                Empleado empleado = dao.leer(posicion);

                if (empleado == null) {
                    System.out.println(
                            "Posición " + posicion + ": BAJA");
                } else {
                    System.out.println(
                            "Posición " + posicion + ": " + empleado);
                }
            }

            System.out.println("Registros físicos: " + numero);
            System.out.println("Bytes: " + fichero.length());

        } catch (FileNotFoundException e) {
            System.out.println("No se puede abrir el fichero.");

        } catch (EOFException e) {
            System.out.println("El fichero está incompleto.");

        } catch (IOException e) {
            System.out.println(
                    "Error al leer: " + e.getMessage());

        } catch (IllegalArgumentException e) {
            System.out.println(
                    "Datos incorrectos: " + e.getMessage());
        }
    }
}
