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
        Provincia encontrado = null;
        int i = 0;

        // Se recorre hasta encontrar el código o acabar los valores
        while (encontrado == null && i < values().length) {
            if (values()[i].codigo == codigo) {
                encontrado = values()[i];
            }
            i++;
        }

        if (encontrado == null) {
            throw new IllegalArgumentException(
                    "La provincia debe estar entre 1 y 8.");
        }

        return encontrado;
    }
}