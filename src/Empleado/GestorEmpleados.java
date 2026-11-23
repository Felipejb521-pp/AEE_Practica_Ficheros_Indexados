import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Álvaro López De San Román
 */
public class GestorEmpleados {

    private FicheroIndexado<String, Empleado> ficheroIndexado;
    private EmpleadoDAO empleadoDAO;

    public GestorEmpleados(File datos, File indice)
            throws IOException {

        this.empleadoDAO = new EmpleadoDAO(datos);

        this.ficheroIndexado
                = new FicheroIndexado<String, Empleado>(
                        datos, indice, this.empleadoDAO);
    }

    public void altaEmpleado(Empleado empleado)
            throws IOException {

        this.ficheroIndexado.alta(
                empleado.getDni(), empleado);
    }

    public boolean bajaEmpleado(String dni)
            throws IOException {

        return this.ficheroIndexado.baja(
                Empleado.validarDni(dni));
    }

    public boolean modificarSalario(String dni, float salario)
            throws IOException {

        long posicion = this.ficheroIndexado.posicionar(
                Empleado.validarDni(dni));

        if (posicion < 0) {
            return false;
        }

        this.empleadoDAO.modificarSalario(posicion, salario);

        return true;
    }

    public Empleado buscarPorDni(String dni)
            throws IOException {

        this.ficheroIndexado.posicionar(
                Empleado.validarDni(dni));

        return this.ficheroIndexado.consultarAleatorio();
    }

    public List<Empleado> listarOrdenadoDesdeDni(
            String dni, int cantidad) throws IOException {

        return this.ficheroIndexado.consultarDinamico(
                Empleado.validarDni(dni), cantidad);
    }
}
