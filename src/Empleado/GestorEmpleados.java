package Empleado;

import java.io.File;
import indexado.FicheroIndexado;

/**
 * @author Equipo 3
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
            modificado = this.ficheroIndexado.modificar(empleado.getDni(), empleado);
        }
        return modificado;
    }

    public Empleado buscarPorDni(String dni) {
        return (Empleado) this.ficheroIndexado.consultarAleatorio(Empleado.validarDni(dni));
    }

    public void iniciarListadoPorAlmacenamiento() {
        this.ficheroIndexado.iniciarRecorridoFisico();
    }

    public Empleado siguientePorAlmacenamiento() {
        return (Empleado) this.ficheroIndexado.leerSiguienteFisico();
    }

    public void iniciarListadoPorDni() {
        this.ficheroIndexado.iniciarRecorridoOrdenado(null, 0);
    }

    public void iniciarListadoPorDniDesde(String dni, int cantidad) {
        this.ficheroIndexado.iniciarRecorridoOrdenado(Empleado.validarDni(dni), cantidad);
    }

    public Empleado siguientePorDni() {
        return (Empleado) this.ficheroIndexado.leerSiguienteOrdenado();
    }

    public void cerrarListado() {
        this.ficheroIndexado.cerrarRecorrido();
    }
}
