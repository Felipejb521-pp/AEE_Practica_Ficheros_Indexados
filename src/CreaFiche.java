import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;

import Empleado.Empleado;
import Empleado.EmpleadoDAO;
import Empleado.Fecha;
import Empleado.GestorEmpleados;
import Empleado.Provincia;
import Empleado.Sexo;
import Empleado.TipoEmpleado;

/**
 * Aplicación CreaFiche.
 *
 * @author Felipe Jimenez Brioso
 *
 * Crea y llena el fichero indexado FICHENDX.DAT (y su índice INDICE.DAT)
 * con los datos del fichero directo FICHE.DAT, añadiendo el campo DNI,
 * que es el campo clave y se introduce por teclado.
 *
 * FICHE.DAT es el fichero de empleados de la práctica anterior y debe
 * estar en la carpeta Ficheros. Formato de cada registro (74 bytes):
 *
 *   Campo                 Tipo        Bytes
 *   Activo                boolean       1
 *   Nombre y apellidos    30 char      60
 *   Sexo (H/M)            char          2
 *   Salario               float         4
 *   Año de ingreso        short         2
 *   Mes de ingreso        byte          1
 *   Día de ingreso        byte          1
 *   Tipo (C/F/D)          char          2
 *   Provincia (1-8)       byte          1
 *
 * Es el mismo orden que FICHENDX.DAT (EmpleadoDAO) pero sin el DNI.
 * Si vuestro FICHE.DAT es distinto, solo hay que cambiar LONGITUD_FICHE
 * y el bloque de lecturas de main.
 *
 * No se cargan los empleados en memoria: se lee un registro de FICHE.DAT,
 * se pide su DNI y se graba en FICHENDX.DAT antes de leer el siguiente.
 */
public class CreaFiche {

    private static final int LONGITUD_FICHE = 74;

    private static final File ORIGEN = new File("Ficheros", "FICHE.DAT");
    private static final File DATOS = new File("Ficheros", "FICHENDX.DAT");
    private static final File INDICE = new File("Ficheros", "INDICE.DAT");

    private static BufferedReader teclado
            = new BufferedReader(new InputStreamReader(System.in));

    /** Muestra el mensaje y devuelve la línea tecleada (sin espacios). */
    private static String leerLinea(String mensaje) {
        String linea = "";
        System.out.print(mensaje);

        try {
            linea = teclado.readLine();

            if (linea == null) {
                linea = "";
            }
        } catch (IOException e) {
            System.out.println("Error al leer del teclado.");
        }

        return linea.trim();
    }

    /** Pide una respuesta hasta que sea S o N. */
    private static String leerSiNo(String mensaje) {
        String respuesta;

        do {
            respuesta = leerLinea(mensaje).toUpperCase();
        } while (!respuesta.equals("S") && !respuesta.equals("N"));

        return respuesta;
    }

    /**
     * Comprueba que FICHE.DAT existe y tiene registros completos, y que
     * no se va a mezclar con un FICHENDX.DAT anterior.
     * Devuelve true si se puede empezar a crear el fichero indexado.
     */
    private static boolean comprobarFicheros() {
        boolean correcto = true;

        if (!ORIGEN.exists()) {
            System.out.println("No existe " + ORIGEN.getPath()
                    + ". Copie ahí el FICHE.DAT de la práctica anterior.");
            correcto = false;
        } else if (ORIGEN.length() % LONGITUD_FICHE != 0) {
            System.out.println("FICHE.DAT no tiene registros de "
                    + LONGITUD_FICHE + " bytes. Revise el formato.");
            correcto = false;
        } else if (DATOS.exists()) {
            String respuesta = leerSiNo(
                    "FICHENDX.DAT ya existe. ¿Desea borrarlo y crearlo de nuevo? (S/N): ");

            if (respuesta.equals("S")) {
                DATOS.delete();
                INDICE.delete();
            } else {
                System.out.println("Proceso cancelado.");
                correcto = false;
            }
        }

        return correcto;
    }

    /**
     * Pide el DNI hasta que sea válido (8 cifras y letra correcta)
     * y no esté ya grabado en FICHENDX.DAT.
     */
    private static String leerDni(GestorEmpleados gestor) {
        String dni = null;
        boolean correcto;

        do {
            correcto = false;

            try {
                dni = Empleado.validarDni(leerLinea("DNI (8 cifras y letra): "));

                if (gestor.buscarPorDni(dni) != null) {
                    System.out.println("Ese DNI ya está grabado.");
                } else {
                    correcto = true;
                }
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            } catch (IOException e) {
                System.out.println("Error al buscar el DNI: " + e.getMessage());
            }
        } while (!correcto);

        return dni;
    }

    // ---------------------------------------------------------------
    // MAIN: recorre FICHE.DAT de principio a fin.
    // ---------------------------------------------------------------
    public static void main(String[] args) {
        if (comprobarFicheros()) {
            int grabados = 0;
            int omitidos = 0;

            try (RandomAccessFile fiche = new RandomAccessFile(ORIGEN, "r")) {
                GestorEmpleados gestor = new GestorEmpleados(DATOS, INDICE);
                long numeroRegistros = fiche.length() / LONGITUD_FICHE;

                for (long i = 0; i < numeroRegistros; i++) {
                    fiche.seek(i * LONGITUD_FICHE);

                    boolean activo = fiche.readBoolean();
                    String nombre = EmpleadoDAO.leerTexto(fiche, 30);
                    char sexo = fiche.readChar();
                    float salario = fiche.readFloat();
                    short anio = fiche.readShort();
                    byte mes = fiche.readByte();
                    byte dia = fiche.readByte();
                    char tipo = fiche.readChar();
                    byte provincia = fiche.readByte();

                    if (!activo) {
                        // Los empleados dados de baja en FICHE.DAT no se pasan
                        omitidos++;
                    } else {
                        System.out.println();
                        System.out.println("Empleado " + (i + 1) + " de "
                                + numeroRegistros + ": " + nombre);

                        try {
                            // Se crean primero los datos de FICHE.DAT para
                            // no pedir el DNI de un registro incorrecto.
                            Fecha fecha = new Fecha(dia, mes, anio);
                            Sexo s = Sexo.fromCodigo(sexo);
                            TipoEmpleado t = TipoEmpleado.fromCodigo(tipo);
                            Provincia p = Provincia.fromCodigo(provincia);

                            String dni = leerDni(gestor);

                            Empleado empleado = new Empleado(dni, nombre,
                                    s, salario, fecha, t, p);

                            gestor.altaEmpleado(empleado);
                            grabados++;
                            System.out.println("Grabado: " + empleado);

                        } catch (IllegalArgumentException e) {
                            omitidos++;
                            System.out.println("Registro con datos incorrectos ("
                                    + e.getMessage() + "). No se graba.");
                        }
                    }
                }

            } catch (IOException e) {
                System.out.println("Error de entrada/salida: " + e.getMessage());
            }

            System.out.println();
            System.out.println("Empleados grabados en FICHENDX.DAT: " + grabados);
            System.out.println("Registros no pasados: " + omitidos);
        }
    }
}
