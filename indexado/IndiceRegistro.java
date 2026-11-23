package indexado;

/**
 *
 * @author Antonio Muñoz Herrera
 */

public class IndiceRegistro {
    
    private Comparable clave;
    private long posicion;

    public IndiceRegistro(Comparable clave, long posicion) {
        this.clave = clave;
        this.posicion = posicion;
    }

    public Comparable getClave() {
        return clave;
    }

    public long getPosicion() {
        return posicion;
    }
}