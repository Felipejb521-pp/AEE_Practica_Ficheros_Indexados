package Empleado;

/**
 * @author Equipo 3
 */
public enum Sexo {

    H('H', "Hombre"),
    M('M', "Mujer");

    private final char codigo;
    private final String descripcion;

    Sexo(char codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public char getCodigo() {
        return this.codigo;
    }

    public String getDescripcion() {
        return this.descripcion;
    }

    public static Sexo fromCodigo(char codigo) {
        char mayuscula = Character.toUpperCase(codigo);

        Sexo encontrado = null;
        int i = 0;

        // Se recorre hasta encontrar el código o acabar los valores
        while (encontrado == null && i < values().length) {
            if (values()[i].codigo == mayuscula) {
                encontrado = values()[i];
            }
            i++;
        }

        if (encontrado == null) {
            throw new IllegalArgumentException(
                    "El sexo debe ser H o M.");
        }

        return encontrado;
    }
}