/**
 * @author Álvaro López De San Román
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

        for (int i = 0; i < values().length; i++) {
            if (values()[i].codigo == mayuscula) {
                return values()[i];
            }
        }

        throw new IllegalArgumentException(
                "El sexo debe ser H o M.");
    }
}
