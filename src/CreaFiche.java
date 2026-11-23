import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Aplicación CreaFiche.
 *
 * @author Felipe Jimenez Brioso
 * Crea y llena el fichero indexado FICHENDX.DAT (y su índice INDICE.DAT)
 * con los datos de los empleados introducidos por teclado.
 * El DNI es el campo clave: no puede repetirse.
 *
 * No se guarda ningún empleado en memoria: cada uno se pide, se valida
 * y se graba directamente en el fichero antes de pedir el siguiente.
 */
public class CreaFiche {

    private static BufferedReader teclado
            = new BufferedReader(new InputStreamReader(System.in));

    public static void main(String[] args) {
        File datos = new File("Ficheros", "FICHENDX.DAT");
        File indice = new File("Ficheros", "INDICE.DAT");

        // Si el fichero ya existe, se puede empezar de cero o añadir al final
        if (datos.exists()) {
            String respuesta = leerSiNo(
                    "FICHENDX.DAT ya existe. ¿Desea borrarlo y empezar de cero? (S/N): ");

            if (respuesta.equals("S")) {
                datos.delete();
                indice.delete();
                System.out.println("Fichero borrado. Se creará uno nuevo.");
            } else {
                System.out.println("Los nuevos empleados se añadirán al fichero.");
            }
        }

        GestorEmpleados gestor = null;

        try {
            gestor = new GestorEmpleados(datos, indice);
        } catch (IOException e) {
            System.out.println("No se puede abrir el fichero: " + e.getMessage());
        }

        if (gestor != null) {
            int grabados = 0;
            String otro;

            do {
                System.out.println();
                System.out.println("--- NUEVO EMPLEADO ---");

                String dni = leerDni(gestor);
                String nombre = leerNombre();
                Sexo sexo = leerSexo();
                float salario = leerSalario();
                Fecha fechaIngreso = leerFechaIngreso();
                TipoEmpleado tipo = leerTipo();
                Provincia provincia = leerProvincia();

                try {
                    Empleado empleado = new Empleado(dni, nombre, sexo,
                            salario, fechaIngreso, tipo, provincia);

                    gestor.altaEmpleado(empleado);
                    grabados++;
                    System.out.println("Grabado: " + empleado);

                } catch (IllegalArgumentException e) {
                    System.out.println("Datos incorrectos: " + e.getMessage());
                } catch (IOException e) {
                    System.out.println("Error al grabar: " + e.getMessage());
                }

                otro = leerSiNo("¿Desea introducir otro empleado? (S/N): ");
            } while (otro.equals("S"));

            System.out.println();
            System.out.println("Empleados grabados en esta sesión: " + grabados);
        }
    }
  
    
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

    // ---------------------------------------------------------------
    // Lectura y validación de cada campo
    // ---------------------------------------------------------------

    /** DNI: 8 cifras + letra correcta, y que no exista ya en el fichero. */
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

    /**
     * Nombre y apellidos: máximo 30 caracteres y al menos un apellido.
     * Como leerLinea ya quita los espacios del principio y del final,
     * si queda algún espacio es que hay nombre y, como mínimo, un apellido.
     */
    private static String leerNombre() {
        String nombre;
        boolean correcto;

        do {
            correcto = false;
            nombre = leerLinea("Nombre y apellidos (máx. 30): ");

            if (nombre.indexOf(' ') == -1) {
                System.out.println("Debe escribir el nombre y al menos un apellido.");
            } else if (nombre.length() > 30) {
                System.out.println("No puede tener más de 30 caracteres.");
            } else {
                correcto = true;
            }
        } while (!correcto);

        return nombre;
    }

    /** Sexo: H o M. */
    private static Sexo leerSexo() {
        Sexo sexo = null;

        do {
            String texto = leerLinea("Sexo (H/M): ");

            if (texto.length() != 1) {
                System.out.println("Escriba una sola letra: H o M.");
            } else {
                try {
                    sexo = Sexo.fromCodigo(texto.charAt(0));
                } catch (IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                }
            }
        } while (sexo == null);

        return sexo;
    }

    /** Salario: de 0 a 9999,99 con dos decimales como máximo. */
    private static float leerSalario() {
        float salario = 0;
        boolean correcto = false;

        do {
            String texto = leerLinea("Salario (0 - 9999,99): ").replace(',', '.');

            try {
                salario = Float.parseFloat(texto);
                Empleado.validarSalario(salario);
                correcto = true;
            } catch (NumberFormatException e) {
                System.out.println("Debe escribir un número.");
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        } while (!correcto);

        return salario;
    }

    /**
     * Fecha de ingreso en formato dd/mm/aaaa (también vale d/m/aaaa).
     * El constructor Fecha(String) de la clase Fecha ya comprueba el
     * formato y que el día exista en ese mes (años bisiestos incluidos);
     * si algo falla lanza IllegalArgumentException con el motivo.
     * Aquí solo añadimos que no sea una fecha futura, igual que Empleado.
     */
    private static Fecha leerFechaIngreso() {
        Fecha fecha = null;

        do {
            try {
                fecha = new Fecha(leerLinea("Fecha de ingreso (dd/mm/aaaa): "));

                if (fecha.esPosteriorA(Fecha.hoy())) {
                    System.out.println("La fecha de ingreso no puede ser futura.");
                    fecha = null;
                }
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        } while (fecha == null);

        return fecha;
    }

    /** Tipo de empleado: C (a comisión), F (fijo) o D (a domicilio). */
    private static TipoEmpleado leerTipo() {
        TipoEmpleado tipo = null;

        do {
            String texto = leerLinea(
                    "Tipo (C = A comisión, F = Fijos, D = A domicilio): ");

            if (texto.length() != 1) {
                System.out.println("Escriba una sola letra: C, F o D.");
            } else {
                try {
                    tipo = TipoEmpleado.fromCodigo(texto.charAt(0));
                } catch (IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                }
            }
        } while (tipo == null);

        return tipo;
    }

    /** Provincia: número de 1 a 8. */
    private static Provincia leerProvincia() {
        Provincia provincia = null;

        System.out.println("Provincias: 1 Almería, 2 Cádiz, 3 Córdoba, 4 Granada,");
        System.out.println("            5 Huelva, 6 Jaén, 7 Málaga, 8 Sevilla");

        do {
            try {
                byte codigo = Byte.parseByte(leerLinea("Provincia (1-8): "));
                provincia = Provincia.fromCodigo(codigo);
            } catch (NumberFormatException e) {
                System.out.println("Debe escribir un número del 1 al 8.");
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        } while (provincia == null);

        return provincia;
    }
}
