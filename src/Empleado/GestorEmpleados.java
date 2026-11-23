package Empleado;

import java.io.File;
import java.util.List;
import java.util.Set;
import indexado.FicheroIndexado;

/**
 * @author Álvaro López De San Román
 *
 * Une la clase Empleado con el motor genérico indexado.FicheroIndexado.
 * La clave de cada registro es el DNI y el registro es el propio objeto
 * Empleado, que el motor guarda serializado en bloques de 512 bytes.
 *
 * El motor no lanza excepciones de fichero: si algo falla lo indica por
 * System.err y devuelve false, null o una lista vacía.
 */
public class GestorEmpleados {

    private FicheroIndexado ficheroIndexado;

    public GestorEmpleados(File datos, File indice) {
        this.ficheroIndexado = new FicheroIndexado(datos, indice);
    }

    /** Devuelve false si el DNI ya existe o no se ha podido grabar. */
    public boolean altaEmpleado(Empleado empleado) {
        return this.ficheroIndexado.alta(empleado.getDni(), empleado);
    }

    public boolean bajaEmpleado(String dni) {
        return this.ficheroIndexado.baja(Empleado.validarDni(dni));
    }

    /**
     * Lee el empleado, cambia su salario (setSalario lo valida) y
     * reescribe su bloque en la misma posición del fichero.
     */
    public boolean modificarSalario(String dni, float salario) {
        boolean modificado = false;
        Empleado empleado = buscarPorDni(dni);

        if (empleado != null) {
            empleado.setSalario(salario);
            modificado = this.ficheroIndexado.modificar(
                    empleado.getDni(), empleado);
        }

        return modificado;
    }

    /** Acceso aleatorio por DNI. Devuelve null si no existe. */
    public Empleado buscarPorDni(String dni) {
        return (Empleado) this.ficheroIndexado.consultarAleatorio(
                Empleado.validarDni(dni));
    }

    /** DNI de los empleados activos, ordenados (claves del índice). */
    public Set getDnis() {
        return this.ficheroIndexado.getClaves();
    }

    /** Empleados en el orden en que están grabados en el fichero. */
    public List listarPorAlmacenamiento() {
        return this.ficheroIndexado.consultarSecuencialFisico();
    }

    /** Hasta "cantidad" empleados ordenados por DNI desde el indicado. */
    public List listarOrdenadoDesdeDni(String dni, int cantidad) {
        return this.ficheroIndexado.consultarDinamico(
                Empleado.validarDni(dni), cantidad);
    }
}
