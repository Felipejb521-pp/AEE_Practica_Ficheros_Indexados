package FicheIndx;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author Álvaro López De San Román
 */
public abstract class FicheroDirectoDAO<T> {

    protected File fichero;
    protected int longitudRegistro;

    public FicheroDirectoDAO(File fichero, int longitudRegistro) {
        if (fichero == null || longitudRegistro <= 1) {
            throw new IllegalArgumentException(
                    "Fichero o longitud incorrectos.");
        }

        this.fichero = fichero;
        this.longitudRegistro = longitudRegistro;
    }

    public long escribir(long posicion, T registro)
            throws IOException {

        if (registro == null) {
            throw new IllegalArgumentException(
                    "Falta el registro.");
        }

        File carpeta = this.fichero.getParentFile();

        if (carpeta != null
                && !carpeta.exists()
                && !carpeta.mkdirs()) {
            throw new IOException(
                    "No se puede crear la carpeta de datos.");
        }

        getNumeroRegistros();

        try (RandomAccessFile datos
                = new RandomAccessFile(this.fichero, "rw")) {

            if (posicion < 0
                    || posicion % this.longitudRegistro != 0
                    || posicion > datos.length()) {
                throw new IOException(
                        "Posición de escritura incorrecta.");
            }

            datos.seek(posicion);
            datos.writeBoolean(true);

            escribirRegistro(datos, registro);

            if (datos.getFilePointer()
                    != posicion + this.longitudRegistro) {
                throw new IOException(
                        "El DAO no ha escrito la longitud declarada.");
            }

            return posicion;
        }
    }

    public long insertar(T registro) throws IOException {
        long posicion
                = getNumeroRegistros() * this.longitudRegistro;

        return escribir(posicion, registro);
    }

    public void borrar(long posicion) throws IOException {
        try (RandomAccessFile datos
                = new RandomAccessFile(this.fichero, "rw")) {

            posicionar(datos, posicion);
            datos.writeBoolean(false);
        }
    }

    public void reescribir(long posicion, T registro)
            throws IOException {

        if (leer(posicion) == null) {
            throw new IOException(
                    "El registro está dado de baja.");
        }

        escribir(posicion, registro);
    }

    public T leer(long posicion) throws IOException {
        try (RandomAccessFile datos
                = new RandomAccessFile(this.fichero, "r")) {

            posicionar(datos, posicion);

            if (!datos.readBoolean()) {
                return null;
            }

            T registro = leerRegistro(datos);

            if (datos.getFilePointer()
                    != posicion + this.longitudRegistro) {
                throw new IOException(
                        "El DAO no ha leído la longitud declarada.");
            }

            return registro;
        }
    }

    protected void posicionar(RandomAccessFile datos,
            long posicion) throws IOException {

        comprobarPosicion(datos, posicion);
        datos.seek(posicion);
    }

    public long getNumeroRegistros() throws IOException {
        if (!this.fichero.exists()) {
            return 0;
        }

        try (RandomAccessFile datos
                = new RandomAccessFile(this.fichero, "r")) {

            if (datos.length() % this.longitudRegistro != 0) {
                throw new IOException(
                        "El fichero contiene un registro incompleto.");
            }

            return datos.length() / this.longitudRegistro;
        }
    }

    protected void comprobarPosicion(RandomAccessFile datos,
            long posicion) throws IOException {

        if (datos.length() % this.longitudRegistro != 0
                || posicion < 0
                || posicion % this.longitudRegistro != 0
                || posicion >= datos.length()) {
            throw new IOException(
                    "Posición de registro incorrecta.");
        }
    }

    protected abstract void escribirRegistro(
            RandomAccessFile datos, T registro) throws IOException;

    protected abstract T leerRegistro(
            RandomAccessFile datos) throws IOException;
}
