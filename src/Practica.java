import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

import Empleado.Empleado;
import Empleado.Fecha;
import Empleado.GestorEmpleados;
import Empleado.Provincia;
import Empleado.Sexo;
import Empleado.TipoEmpleado;

/**
 * Aplicación Practica: mantenimiento del fichero indexado FICHENDX.DAT.
 *
 * @author Felipe Jimenez Brioso
 *
 * El menú se repite con un do-while mientras la opción no sea 6.
 * Las operaciones con el fichero se hacen a través de GestorEmpleados,
 * que usa el motor genérico del paquete "indexado".
 */
public class Practica {

    private static final File DATOS = new File("Ficheros", "FICHENDX.DAT");
    private static final File INDICE = new File("Ficheros", "INDICE.DAT");

    private static BufferedReader teclado
            = new BufferedReader(new InputStreamReader(System.in));

    private static GestorEmpleados gestor;

    /**
     * Abre el fichero indexado. Si no existe la carpeta Ficheros se crea,
     * y FICHENDX.DAT se creará con la primera alta.
     */
    private static void abrirFichero() {
        File carpeta = DATOS.getParentFile();

        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }

        gestor = new GestorEmpleados(DATOS, INDICE);
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

    /** Muestra DNI, nombre y sexo de un empleado en una línea. */
    private static void mostrarLinea(Empleado empleado) {
        System.out.println(String.format("%-9s  %-30s  %s",
                empleado.getDni(),
                empleado.getNombreApellidos(),
                empleado.getSexo().getDescripcion()));
    }

    // ---------------------------------------------------------------
    // 1. ALTAS
    // ---------------------------------------------------------------
    /**
     * @author Sergio García de Baya
     */
    private static void altas() {
        try {
            String dni = Empleado.validarDni(leerLinea("DNI (8 cifras y letra): "));
            String nombre = leerLinea("Nombre y Apellidos (max 30): ");

            System.out.println("Sexo (H/M): ");
            Sexo sexo = Sexo.fromCodigo(leerLinea("").charAt(0));

            System.out.println("Salario: ");
            float salario = Float.parseFloat(leerLinea("").replace(',', '.'));

            System.out.println("Fecha de ingreso (dd/mm/aaaa): ");
            Fecha fecha = new Fecha(leerLinea(""));

            System.out.println("Tipo Empleado (C/F/D): ");
            System.out.println("C (A comisión)");
            System.out.println("F (Fijos)");
            System.out.println("D (A domicilio)");
            TipoEmpleado tipo = TipoEmpleado.fromCodigo(leerLinea("").charAt(0));

            System.out.println("Provincia (1-8): ");
            System.out.println("1. Almería");
            System.out.println("2. Cádiz");
            System.out.println("3. Córdoba");
            System.out.println("4. Granada");
            System.out.println("5. Huelva");
            System.out.println("6. Jaén");
            System.out.println("7. Málaga");
            System.out.println("8. Sevilla");
            Provincia provincia = Provincia.fromCodigo(Byte.parseByte(leerLinea("")));

            Empleado empleado = new Empleado(dni, nombre, sexo, salario, fecha, tipo, provincia);

            if (gestor.altaEmpleado(empleado)) {
                System.out.println("Empleado dado de alta correctamente.");
            } else {
                System.out.println("No se ha podido dar de alta (¿DNI repetido?).");
            }
            System.out.println("");
        } catch (IllegalArgumentException iae) {
            System.out.println("Error en los datos: " + iae.getMessage());
        } catch (Exception e) {
            System.out.println("Error inesperado: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // 2. BAJAS
    // ---------------------------------------------------------------
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
        }
    }

    // ---------------------------------------------------------------
    // 3. MODIFICAR SALARIO
    // ---------------------------------------------------------------
    /**
     * @author Sergio García de Baya
     */
    private static void modificarSalario() {
        try {
            String dni = Empleado.validarDni(leerLinea("DNI del empleado: "));
            Empleado empleado = gestor.buscarPorDni(dni);

            if (empleado == null) {
                System.out.println("No existe ningún empleado con DNI " + dni);
            } else {
                System.out.println("Empleado: " + empleado.getNombreApellidos());
                System.out.println("Salario actual: " + empleado.getSalario());
                System.out.print("Nuevo salario: ");
                float nuevoSalario = Float.parseFloat(leerLinea("").replace(',', '.'));

                if (gestor.modificarSalario(dni, nuevoSalario)) {
                    System.out.println("Salario modificado correctamente");
                } else {
                    System.out.println("No se ha podido modificar el salario");
                }
            }
            System.out.println("");
        } catch (IllegalArgumentException iae) {
            System.out.println("Datos incorrectos: " + iae.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // 4. LISTADO ORDENADO POR DNI
    // Se recorren las claves del índice (ya ordenadas, porque es un
    // TreeMap) y se lee cada empleado del fichero de uno en uno con
    // acceso aleatorio. No se carga una lista con todos los empleados.
    // ---------------------------------------------------------------
    private static void listadoPorDni() {
        System.out.println();
        System.out.println("--- LISTADO DE EMPLEADOS POR DNI ---");

        Iterator claves = gestor.getDnis().iterator();

        if (!claves.hasNext()) {
            System.out.println("No hay empleados en el fichero.");
        } else {
            System.out.println("DNI        NOMBRE Y APELLIDOS              SEXO");
            System.out.println("---------  ------------------------------  ------");

            int total = 0;

            while (claves.hasNext()) {
                String dni = (String) claves.next();
                Empleado empleado = gestor.buscarPorDni(dni);

                if (empleado != null) {
                    mostrarLinea(empleado);
                    total++;
                }
            }

            System.out.println("Total de empleados: " + total);
        }
    }

    // ---------------------------------------------------------------
    // 5. LISTADO POR ORDEN DE ALMACENAMIENTO
    // El motor recorre FICHENDX.DAT bloque a bloque, saltando las bajas.
    // ---------------------------------------------------------------
    /**
     * @author Sergio García de Baya
     */
    private static void listadoPorAlmacenamiento() {
        List empleados = gestor.listarPorAlmacenamiento();

        if (empleados.isEmpty()) {
            System.out.println("No hay empleados en el fichero.");
        } else {
            System.out.println("DNI        NOMBRE Y APELLIDOS              SEXO");
            System.out.println("---------  ------------------------------  ------");

            for (int i = 0; i < empleados.size(); i++) {
                mostrarLinea((Empleado) empleados.get(i));
            }
        }
        System.out.println("");
    }

    // ---------------------------------------------------------------
    // MAIN: solo el menú. Va al final de la clase, después de los métodos.
    // ---------------------------------------------------------------
    public static void main(String[] args) {
        int opcion;

        abrirFichero();

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
