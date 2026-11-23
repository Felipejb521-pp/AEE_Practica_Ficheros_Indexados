import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author Álvaro López De San Román
 */
public class EmpleadoDAO extends FicheroDirectoDAO<Empleado> {

    public static final int LONGITUD_REGISTRO = 92;
    private static final int POSICION_SALARIO = 81;

    public EmpleadoDAO(File fichero) {
        super(fichero, LONGITUD_REGISTRO);
    }

    @Override
    protected void escribirRegistro(RandomAccessFile datos,
            Empleado empleado) throws IOException {

        escribirTexto(datos, empleado.getDni(), 9);
        escribirTexto(datos, empleado.getNombreApellidos(), 30);

        datos.writeChar(empleado.getSexo().getCodigo());
        datos.writeFloat(empleado.getSalario());

        datos.writeShort(empleado.getFechaIngreso().getAnio());
        datos.writeByte(empleado.getFechaIngreso().getMes());
        datos.writeByte(empleado.getFechaIngreso().getDia());

        datos.writeChar(empleado.getTipoEmpleado().getCodigo());
        datos.writeByte(empleado.getProvincia().getCodigo());
    }

    @Override
    protected Empleado leerRegistro(RandomAccessFile datos)
            throws IOException {

        String dni = leerTexto(datos, 9);
        String nombre = leerTexto(datos, 30);

        Sexo sexo = Sexo.fromCodigo(datos.readChar());
        float salario = datos.readFloat();

        short anio = datos.readShort();
        byte mes = datos.readByte();
        byte dia = datos.readByte();

        TipoEmpleado tipo
                = TipoEmpleado.fromCodigo(datos.readChar());

        Provincia provincia
                = Provincia.fromCodigo(datos.readByte());

        return new Empleado(
                dni, nombre, sexo, salario,
                new Fecha(dia, mes, anio), tipo, provincia);
    }

    public void modificarSalario(long posicion, float salario)
            throws IOException {

        Empleado.validarSalario(salario);

        try (RandomAccessFile datos
                = new RandomAccessFile(this.fichero, "rw")) {

            posicionar(datos, posicion);

            if (!datos.readBoolean()) {
                throw new IOException(
                        "El empleado está dado de baja.");
            }

            datos.seek(posicion + POSICION_SALARIO);
            datos.writeFloat(salario);
        }
    }

    public static void escribirTexto(RandomAccessFile datos,
            String texto, int longitud) throws IOException {

        if (texto == null || texto.length() > longitud) {
            throw new IllegalArgumentException(
                    "Longitud de texto incorrecta.");
        }

        for (int i = 0; i < longitud; i++) {
            if (i < texto.length()) {
                datos.writeChar(texto.charAt(i));
            } else {
                datos.writeChar(' ');
            }
        }
    }

    public static String leerTexto(RandomAccessFile datos,
            int longitud) throws IOException {

        StringBuilder texto = new StringBuilder();

        for (int i = 0; i < longitud; i++) {
            texto.append(datos.readChar());
        }

        return texto.toString().trim();
    }
}
