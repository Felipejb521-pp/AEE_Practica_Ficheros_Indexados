package Empleado;

/**
 * @author Equipo 3
 */
public enum TipoEmpleado {

    F('F', "Fijos"),
    D('D', "A domicilio"),
    C('C', "A comisión");

    private final char codigo;
    private final String descripcion;

    TipoEmpleado(char codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public char getCodigo() {
        return this.codigo;
    }

    public String getDescripcion() {
        return this.descripcion;
    }

    public static TipoEmpleado fromCodigo(char codigo) {
        char mayuscula = Character.toUpperCase(codigo);

        for (int i = 0; i < values().length; i++) {
            if (values()[i].codigo == mayuscula) {
                return values()[i];
            }
        }

        throw new IllegalArgumentException(
                "El tipo debe ser F, D o C.");
    }
}