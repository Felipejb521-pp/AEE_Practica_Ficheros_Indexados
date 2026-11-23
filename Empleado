import java.util.Locale;

/**
 * @author Álvaro López De San Román
 */
public class Empleado implements RegistroConClave<String> {

    private String dni;
    private String nombreApellidos;
    private Sexo sexo;
    private float salario;
    private Fecha fechaIngreso;
    private TipoEmpleado tipoEmpleado;
    private Provincia provincia;

    public Empleado(String dni, String nombreApellidos,
            Sexo sexo, float salario, Fecha fechaIngreso,
            TipoEmpleado tipoEmpleado, Provincia provincia) {

        this.dni = validarDni(dni);

        if (nombreApellidos == null
                || nombreApellidos.trim().length() == 0
                || nombreApellidos.trim().length() > 30) {
            throw new IllegalArgumentException(
                    "El nombre debe tener de 1 a 30 caracteres.");
        }

        validarSalario(salario);

        if (sexo == null || fechaIngreso == null
                || tipoEmpleado == null || provincia == null) {
            throw new IllegalArgumentException(
                    "Faltan datos del empleado.");
        }

        if (fechaIngreso.esPosteriorA(Fecha.hoy())) {
            throw new IllegalArgumentException(
                    "La fecha de ingreso no puede ser futura.");
        }

        this.nombreApellidos = nombreApellidos.trim();
        this.sexo = sexo;
        this.salario = salario;
        this.fechaIngreso = fechaIngreso;
        this.tipoEmpleado = tipoEmpleado;
        this.provincia = provincia;
    }

    public static String validarDni(String dni) {
        if (dni == null) {
            throw new IllegalArgumentException(
                    "Debe indicar el DNI.");
        }

        dni = dni.trim().toUpperCase(Locale.ROOT);

        if (dni.length() != 9) {
            throw new IllegalArgumentException(
                    "El DNI debe tener 8 cifras y una letra.");
        }

        for (int i = 0; i < 8; i++) {
            if (dni.charAt(i) < '0' || dni.charAt(i) > '9') {
                throw new IllegalArgumentException(
                        "Las primeras 8 posiciones deben ser cifras.");
            }
        }

        String letras = "TRWAGMYFPDXBNJZSQVHLCKE";
        int numero = Integer.parseInt(dni.substring(0, 8));

        if (dni.charAt(8) != letras.charAt(numero % 23)) {
            throw new IllegalArgumentException(
                    "La letra del DNI no es correcta.");
        }

        return dni;
    }

    public static void validarSalario(float salario) {
        if (Float.isNaN(salario)
                || Float.isInfinite(salario)
                || salario < 0
                || salario > 9999.99f) {
            throw new IllegalArgumentException(
                    "El salario debe estar entre 0 y 9999,99.");
        }

        float centimos = salario * 100;

        if (Math.abs(centimos - Math.round(centimos)) > 0.1f) {
            throw new IllegalArgumentException(
                    "El salario admite como máximo dos decimales.");
        }
    }

    @Override
    public String getClave() {
        return this.dni;
    }

    public String getDni() {
        return this.dni;
    }

    public String getNombreApellidos() {
        return this.nombreApellidos;
    }

    public Sexo getSexo() {
        return this.sexo;
    }

    public float getSalario() {
        return this.salario;
    }

    public void setSalario(float salario) {
        validarSalario(salario);
        this.salario = salario;
    }

    public Fecha getFechaIngreso() {
        return this.fechaIngreso;
    }

    public TipoEmpleado getTipoEmpleado() {
        return this.tipoEmpleado;
    }

    public Provincia getProvincia() {
        return this.provincia;
    }

    @Override
    public String toString() {
        return this.dni
                + " | " + this.nombreApellidos
                + " | " + this.sexo.getDescripcion()
                + " | Salario: "
                + String.format(Locale.ROOT, "%.2f", this.salario)
                + " | Ingreso: " + this.fechaIngreso
                + " | " + this.tipoEmpleado.getDescripcion()
                + " | " + this.provincia.getNombre();
    }
}
