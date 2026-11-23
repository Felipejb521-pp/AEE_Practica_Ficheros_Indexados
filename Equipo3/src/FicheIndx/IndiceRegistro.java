package FicheIndx;

/**
 * @author Álvaro López De San Román
 */
public class IndiceRegistro<K> {

    private K clave;
    private long posicion;

    public IndiceRegistro(K clave, long posicion) {
        if (clave == null || posicion < 0) {
            throw new IllegalArgumentException(
                    "Clave o posición incorrecta.");
        }

        this.clave = clave;
        this.posicion = posicion;
    }

    public K getClave() {
        return this.clave;
    }

    public long getPosicion() {
        return this.posicion;
    }
}
