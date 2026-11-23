import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import Empleado.Empleado;
import Empleado.EmpleadoDAO;
import Empleado.Fecha;
import Empleado.GestorEmpleados;
import Empleado.Provincia;
import Empleado.Sexo;
import Empleado.TipoEmpleado;
import FicheIndx.IndiceDAO;

/**
 * @author Sergio García de Baya
 */
public class Practica {

    private static final File DATOS = new File("Ficheros", "FICHENDX.DAT");
    private static final File INDICE = new File("Ficheros", "INDICE.DAT");

    private static BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
    private static GestorEmpleados gestor;

    private static boolean abrirFichero() {
        boolean abierto = false;
        try {
            gestor = new GestorEmpleados(DATOS, INDICE);
            abierto = true;
        } catch (IOException ioe) {
            System.out.println ("No se puede abrir el fichero: " + ioe.getMessage());
        }
        return abierto;
    }

    private static void mostrarMenu() {
        System.out.println("Mantenimiento de FICHENDX.DAT");
        System.out.println("1. Altas");
        System.out.println("2. Bajas");
        System.out.println("3. Modificar salario");
        System.out.println("4. Listado de empleados, ordenado por DNI");
        System.out.println("5. Listado de empleados por orden de almacenamiento");
        System.out.println("6. Salir");
    }

    private static String leerLinea(String mensaje) {
        String linea = "";
        System.out.print(mensaje);
        try {
            linea = teclado.readLine();
            if (linea == null) linea = "";
        } catch (IOException e) {
            System.out.println ("Error al leer del teclado");
        }
        return linea.trim();
    }

    private static int leerOpcion() {
        int opcion;
        try {
            opcion = Integer.parseInt(leerLinea("Opción: "));
        } catch (NumberFormatException nfe) {
            opcion = -1;
        }
        return opcion;
    }

    private static void altas() {
        try {
            String dni = Empleado.validarDni(leerLinea("DNI (8 cifras y letra): "));
            String nombre = leerLinea("Nombre y Apellidos (max 30): ");
            
            System.out.println ("Sexo (H/M): ");
            Sexo sexo = Sexo.fromCodigo(leerLinea("").charAt(0));
            
            System.out.println ("Salario: ");
            float salario = Float.parseFloat(leerLinea(""));
            
            System.out.println ("Fecha de ingreso (dd/mm/aaaa): ");
            Fecha fecha = new Fecha(leerLinea(""));
            
            System.out.println ("Tipo Empleado (C/F/D): ");
            System.out.println ("C (A comisión)");
            System.out.println ("F (Fijos)");
            System.out.println ("D (A domicilio)");
            TipoEmpleado tipo = TipoEmpleado.fromCodigo(leerLinea("").charAt(0));
            
            System.out.println ("Provincia (1-8): ");
            System.out.println ("1. Almería");
            System.out.println ("2. Cádiz");
            System.out.println ("3. Córdoba");
            System.out.println ("4. Granada");
            System.out.println ("5. Huelva");
            System.out.println ("6. Jaén");
            System.out.println ("7. Málaga");
            System.out.println ("8. Sevilla");
            Provincia provincia = Provincia.fromCodigo(Byte.parseByte(leerLinea("")));

            Empleado empleado = new Empleado(dni, nombre, sexo, salario, fecha, tipo, provincia);
            
            gestor.altaEmpleado(empleado);
            System.out.println("Empleado dado de alta correctamente.");
            System.out.println ("");
        } catch (IllegalArgumentException iae) {
            System.out.println ("Error en los datos: " + iae.getMessage());
        } catch (IOException ioe) {
            System.out.println ("Error de fichero: " + ioe.getMessage());
        } catch (Exception e) {
            System.out.println ("Error inesperado: " + e.getMessage());
        }
    }

    private static void bajas() {
        try {
            String dni = Empleado.validarDni(leerLinea("DNI del empleado: "));
            Empleado empleado = gestor.buscarPorDni(dni);

            if (empleado == null) {
                System.out.println ("No existe ningún empleado con DNI " + dni);
            } else {
                System.out.println ("DNI : " + empleado.getDni());
                System.out.println ("Nombre : " + empleado.getNombreApellidos());
                System.out.println ("Sexo : " + empleado.getSexo().getDescripcion());
                System.out.println ("Salario : " + String.format("%.2f", empleado.getSalario()));
                System.out.println ("Fecha ingreso : " + empleado.getFechaIngreso());
                System.out.println ("Tipo : " + empleado.getTipoEmpleado().getDescripcion());
                System.out.println ("Provincia : " + empleado.getProvincia().getNombre());

                String respuesta;
                do {
                    respuesta = leerLinea("¿Confirma la baja? (S/N): ").toUpperCase();
                } while (!respuesta.equals("S") && !respuesta.equals("N"));

                if (respuesta.equals("S")) {
                    if (gestor.bajaEmpleado(dni)) {
                        System.out.println ("Empleado dado de baja");
                    } else {
                        System.out.println ("No se ha podido dar de baja");
                    }
                } else {
                    System.out.println ( "Baja cancelada");
                }
            }
            System.out.println ("");
        } catch (IllegalArgumentException iae) {
            System.out.println ("DNI incorrecto: " + iae.getMessage());
        } catch (IOException ioe) {
            System.out.println ("Error de fichero: " + ioe.getMessage());
        }
    }

    private static void modificarSalario() {
        try {
            String dni = Empleado.validarDni(leerLinea("DNI del empleado: "));
            Empleado empleado = gestor.buscarPorDni(dni);

            if (empleado == null) {
                System.out.println ("No existe ningún empleado con DNI " + dni);
            } else {
                System.out.println ("Empleado: " + empleado.getNombreApellidos());
                System.out.println ("Salario actual: " + empleado.getSalario());
                System.out.print("Nuevo salario: ");
                float nuevoSalario = Float.parseFloat(leerLinea(""));
                
                if (gestor.modificarSalario(dni, nuevoSalario)) {
                    System.out.println ("Salario modificado correctamente");
                } else {
                    System.out.println ("No se ha podido modificar el salario");
                }
            }
            System.out.println ("");
        } catch (IllegalArgumentException iae) {
            System.out.println ("Datos incorrectos: " + iae.getMessage());
        } catch (IOException ioe) {
            System.out.println ("Error de fichero: " + ioe.getMessage());
        }
    }

    private static void listadoPorDni() {
        IndiceDAO<String> indiceDAO = new IndiceDAO<String>(INDICE);
        EmpleadoDAO empleadoDAO = new EmpleadoDAO(DATOS);

        try {
            TreeMap<String, Long> indice = indiceDAO.leer();
            if (indice.isEmpty()) {
                System.out.println ("No hay empleados en el fichero.");
            } else {
                System.out.println ("DNI        NOMBRE Y APELLIDOS              SEXO");
                System.out.println ("---------  ------------------------------  ------");

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
                System.out.println ("Total de empleados: " + total);
            }
            System.out.println ("");
        } catch (ClassNotFoundException cnfe) {
            System.out.println ("No se reconoce el fichero de índice.");
        } catch (IOException ioe) {
            System.out.println ("Error de fichero: " + ioe.getMessage());
        }
    }

    private static void listadoPorAlmacenamiento() {
        EmpleadoDAO empleadoDAO = new EmpleadoDAO(DATOS);
        
        try {
            long numeroRegistros = empleadoDAO.getNumeroRegistros();
            
            if (numeroRegistros == 0) {
                System.out.println ("No hay empleados en el fichero.");
            } else {
                System.out.println ("DNI        NOMBRE Y APELLIDOS              SEXO");
                System.out.println ("---------  ------------------------------  ------");
                
                for (long i = 0; i < numeroRegistros; i++) {
                    long posicion = i * EmpleadoDAO.LONGITUD_REGISTRO;
                    
                    Empleado empleado = empleadoDAO.leer(posicion);
                    
                    if (empleado != null) {
                        System.out.println(String.format("%-9s  %-30s  %s",
                                empleado.getDni(),
                                empleado.getNombreApellidos(),
                                empleado.getSexo().getDescripcion()));
                    }
                }
            }
            System.out.println ("");
        } catch (IOException ioe) {
            System.out.println ("Error de fichero: " + ioe.getMessage());
        }
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
                } else {
                    System.out.println ("Opción incorrecta, Elija de 1 a 6");
                }
            } while (opcion != 6);
        }
    }
}