import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.TreeMap;

import Empleado.Empleado;
import Empleado.EmpleadoDAO;
import Empleado.GestorEmpleados;
import FicheIndx.IndiceDAO;

/**
 * Aplicación Practica: mantenimiento del fichero indexado FICHENDX.DAT.
 *
 * El menú se repite con un do-while mientras la opción no sea 6.
 * Opciones implementadas: 2 (Bajas) y 4 (Listado ordenado por DNI).
 * Las opciones 1, 3 y 5 quedan preparadas para el resto del grupo.
 */
public class Practica {

    private static final File DATOS = new File("Ficheros", "FICHENDX.DAT");
    private static final File INDICE = new File("Ficheros", "INDICE.DAT");

    private static BufferedReader teclado
            = new BufferedReader(new InputStreamReader(System.in));

    private static GestorEmpleados gestor;

    /** Abre el fichero indexado. Devuelve true si se ha podido abrir. */
    private static boolean abrirFichero() {
        boolean abierto = false;

        try {
            gestor = new GestorEmpleados(DATOS, INDICE);
            abierto = true;
        } catch (IOException e) {
            System.out.println("No se puede abrir el fichero: " + e.getMessage());
        }

        return abierto;
    }

    private static void mostrarMenu() {
        System.out.println();
        System.out.println("===== MANTENIMIENTO FICHENDX.DAT =====");
        System.out.println("1. Altas.");
        System.out.println("2. Bajas.");
        System.out.println("3. Modificar salario.");
        System.out.println("4. Listado de empleados, ordenado por DNI.");
        System.out.println("5. Listado de empleados por orden de almacenamiento.");
        System.out.println("6. Salir.");
    }

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

    /** Devuelve la opción tecleada, o -1 si no es un número. */
    private static int leerOpcion() {
        int opcion;

        try {
            opcion = Integer.parseInt(leerLinea("Opción: "));
        } catch (NumberFormatException e) {
            opcion = -1;
        }

        return opcion;
    }

    // ---------------------------------------------------------------
    // 1. ALTAS (la hace otro compañero)
    // ---------------------------------------------------------------
    private static void altas() {
        System.out.println("Altas: pendiente de implementar.");
    }

    
    private static void bajas() {
        System.out.println();
        System.out.println("--- BAJA DE EMPLEADO ---");

        try {
            String dni = Empleado.validarDni(leerLinea("DNI del empleado: "));
            Empleado empleado = gestor.buscarPorDni(dni);

            if (empleado == null) {
                System.out.println("No existe ningún empleado con DNI " + dni);
            } else {
                System.out.println("DNI : " + empleado.getDni());
                System.out.println("Nombre : " + empleado.getNombreApellidos());
                System.out.println("Sexo : " + empleado.getSexo().getDescripcion());
                System.out.println("Salario : " + String.format("%.2f", empleado.getSalario()));
                System.out.println("Fecha ingreso : " + empleado.getFechaIngreso());
                System.out.println("Tipo : " + empleado.getTipoEmpleado().getDescripcion());
                System.out.println("Provincia : " + empleado.getProvincia().getNombre());

                String respuesta;
                do {
                    respuesta = leerLinea("¿Confirma la baja? (S/N): ").toUpperCase();
                } while (!respuesta.equals("S") && !respuesta.equals("N"));

                if (respuesta.equals("S")) {
                    if (gestor.bajaEmpleado(dni)) {
                        System.out.println("Empleado dado de baja.");
                    } else {
                        System.out.println("No se ha podido dar de baja.");
                    }
                } else {
                    System.out.println("Baja cancelada.");
                }
            }

        } catch (IllegalArgumentException e) {
            System.out.println("DNI incorrecto: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error de fichero: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // 3. MODIFICAR SALARIO (pendiente)
    // ---------------------------------------------------------------
    private static void modificarSalario() {
        System.out.println("Modificar salario: pendiente de implementar.");
    }

    
    private static void listadoPorDni() {
        System.out.println();
        System.out.println("--- LISTADO DE EMPLEADOS POR DNI ---");

        IndiceDAO<String> indiceDAO = new IndiceDAO<String>(INDICE);
        EmpleadoDAO empleadoDAO = new EmpleadoDAO(DATOS);

        try {
            TreeMap<String, Long> indice = indiceDAO.leer();

            if (indice.isEmpty()) {
                System.out.println("No hay empleados en el fichero.");
            } else {
                System.out.println("DNI        NOMBRE Y APELLIDOS              SEXO");
                System.out.println("---------  ------------------------------  ------");

                int total = 0;
                Iterator<String> claves = indice.keySet().iterator();

                while (claves.hasNext()) {
                    String dni = claves.next();
                    long posicion = indice.get(dni).longValue();

                    Empleado empleado = empleadoDAO.leer(posicion);

                    if (empleado != null) {
                        System.out.println(String.format("%-9s  %-30s  %s",
                                empleado.getDni(),
                                empleado.getNombreApellidos(),
                                empleado.getSexo().getDescripcion()));
                        total++;
                    }
                }

                System.out.println("Total de empleados: " + total);
            }

        } catch (ClassNotFoundException e) {
            System.out.println("No se reconoce el fichero de índice.");
        } catch (IOException e) {
            System.out.println("Error de fichero: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // 5. LISTADO POR ORDEN DE ALMACENAMIENTO (pendiente)
    // ---------------------------------------------------------------
    private static void listadoPorAlmacenamiento() {
        System.out.println("Listado por almacenamiento: pendiente de implementar.");
    }

  
    public static void main(String[] args) {
        if (abrirFichero()) {
            int opcion;

            do {
                mostrarMenu();
                opcion = leerOpcion();

                if (opcion == 1) {
                    altas();
                } else if (opcion == 2) {
                    bajas();
                } else if (opcion == 3) {
                    modificarSalario();
                } else if (opcion == 4) {
                    listadoPorDni();
                } else if (opcion == 5) {
                    listadoPorAlmacenamiento();
                } else if (opcion == 6) {
                    System.out.println("Fin del programa.");
                } else {
                    System.out.println("Opción incorrecta. Elija de 1 a 6.");
                }
            } while (opcion != 6);
        }
    }
}
