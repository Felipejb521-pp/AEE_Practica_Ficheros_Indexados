package Empleado;

import java.io.File;
import java.util.List;
import java.util.Set;
import indexado.FicheroIndexado;

/**
 * @author Equipo 3
 *
 * 
 */
public class GestorEmpleados {

    private FicheroIndexado ficheroIndexado;

    public GestorEmpleados(File datos, File indice) {
        this.ficheroIndexado = new FicheroIndexado(datos, indice);
    }

    public boolean altaEmpleado(Empleado empleado) {
        return this.ficheroIndexado.alta(empleado.getDni(), empleado);
    }

    public boolean bajaEmpleado(String dni) {
        return this.ficheroIndexado.baja(Empleado.validarDni(dni));
    }

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

    public Empleado buscarPorDni(String dni) {
        return (Empleado) this.ficheroIndexado.consultarAleatorio(
                Empleado.validarDni(dni));
    }

    public Set getDnis() {
        return this.ficheroIndexado.getClaves();
    }

    public List listarPorAlmacenamiento() {
        return this.ficheroIndexado.consultarSecuencialFisico();
    }

    public List listarOrdenadoDesdeDni(String dni, int cantidad) {
        return this.ficheroIndexado.consultarDinamico(
                Empleado.validarDni(dni), cantidad);
    }
}