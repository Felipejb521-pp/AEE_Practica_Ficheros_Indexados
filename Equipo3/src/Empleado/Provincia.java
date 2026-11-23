package Empleado;

/**
 * @author Equipo 3
 */
public enum Provincia {

    ALMERIA((byte) 1, "Almería"),
    CADIZ((byte) 2, "Cádiz"),
    CORDOBA((byte) 3, "Córdoba"),
    GRANADA((byte) 4, "Granada"),
    HUELVA((byte) 5, "Huelva"),
    JAEN((byte) 6, "Jaén"),
    MALAGA((byte) 7, "Málaga"),
    SEVILLA((byte) 8, "Sevilla");

    private final byte codigo;
    private final String nombre;

    Provincia(byte codigo, String nombre) {
        this.codigo = codigo;
        this.nombre = nombre;
    }

    public byte getCodigo() {
        return this.codigo;
    }

    public String getNombre() {
        return this.nombre;
    }

    public static Provincia fromCodigo(byte codigo) {
        for (int i = 0; i < values().length; i++) {
            if (values()[i].codigo == codigo) {
                return values()[i];
            }
        }

        throw new IllegalArgumentException(
                "La provincia debe estar entre 1 y 8.");
    }
}