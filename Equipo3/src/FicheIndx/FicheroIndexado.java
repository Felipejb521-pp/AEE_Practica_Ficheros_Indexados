package FicheIndx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author Álvaro López De San Román
 */
public class FicheroIndexado<K, T extends RegistroConClave<K>> {

    private FicheroDirectoDAO<T> datos;
    private IndiceDAO<K> indices;
    private TreeMap<K, Long> indice;
    private long posicionActual = -1;

    public FicheroIndexado(File ficheroDatos, File ficheroIndice,
            FicheroDirectoDAO<T> datos) throws IOException {

        if (ficheroDatos == null
                || ficheroIndice == null
                || datos == null
                || !ficheroDatos.equals(datos.fichero)) {
            throw new IllegalArgumentException(
                    "El fichero de datos no coincide con el DAO.");
        }

        this.datos = datos;
        this.indices = new IndiceDAO<K>(ficheroIndice);
        this.indice = new TreeMap<K, Long>();

        TreeMap<K, Long> guardado = null;

        try {
            guardado = this.indices.leer();

        } catch (ClassNotFoundException e) {
            System.out.println(
                    "No se reconoce el índice. Se reconstruirá.");

        } catch (IOException e) {
            System.out.println(
                    "No se puede leer el índice. Se reconstruirá.");
        }

        reconstruirIndice();

        if (guardado == null || !this.indice.equals(guardado)) {
            this.indices.guardar(this.indice);
        }
    }

    public void reconstruirIndice() throws IOException {
        TreeMap<K, Long> nuevo = new TreeMap<K, Long>();

        long numero = this.datos.getNumeroRegistros();

        for (long i = 0; i < numero; i++) {
            long posicion = i * this.datos.longitudRegistro;

            T registro = this.datos.leer(posicion);

            if (registro != null) {
                K clave = registro.getClave();

                if (clave == null || nuevo.containsKey(clave)) {
                    throw new IOException(
                            "Clave nula o duplicada en los datos.");
                }

                nuevo.put(clave, posicion);
            }
        }

        this.indices.guardar(nuevo);
        this.indice = nuevo;
    }

    public void alta(K clave, T registro) throws IOException {
        comprobarClave(clave, registro);

        if (this.indice.containsKey(clave)) {
            throw new IllegalArgumentException(
                    "La clave ya existe.");
        }

        long posicion = this.datos.insertar(registro);

        this.indice.put(clave, posicion);
        this.indices.guardar(this.indice);
    }

    public boolean baja(K clave) throws IOException {
        Long posicion = this.indice.get(clave);

        if (posicion == null) {
            return false;
        }

        this.datos.borrar(posicion.longValue());
        this.indice.remove(clave);
        this.indices.guardar(this.indice);

        return true;
    }

    public boolean modificar(K clave, T registro)
            throws IOException {

        comprobarClave(clave, registro);

        Long posicion = this.indice.get(clave);

        if (posicion == null) {
            return false;
        }

        this.datos.reescribir(posicion.longValue(), registro);

        return true;
    }

    public List<T> consultarSecuencial() throws IOException {
        List<T> registros = new ArrayList<T>();

        long numero = this.datos.getNumeroRegistros();

        for (long i = 0; i < numero; i++) {
            T registro = this.datos.leer(
                    i * this.datos.longitudRegistro);

            if (registro != null) {
                registros.add(registro);
            }
        }

        return registros;
    }

    public T consultarAleatorio() throws IOException {
        if (this.posicionActual < 0) {
            return null;
        }

        return this.datos.leer(this.posicionActual);
    }

    public List<T> consultarDinamico(K clave, int cantidad)
            throws IOException {

        if (clave == null || cantidad < 0) {
            throw new IllegalArgumentException(
                    "Clave o cantidad incorrectas.");
        }

        List<T> registros = new ArrayList<T>();

        Iterator<K> claves
                = this.indice.tailMap(clave).keySet().iterator();

        while (claves.hasNext() && registros.size() < cantidad) {
            K siguiente = claves.next();

            long posicion
                    = this.indice.get(siguiente).longValue();

            registros.add(this.datos.leer(posicion));
        }

        return registros;
    }

    public long posicionar(K clave) {
        Long posicion = this.indice.get(clave);

        if (posicion == null) {
            this.posicionActual = -1;
        } else {
            this.posicionActual = posicion.longValue();
        }

        return this.posicionActual;
    }

    public Set<K> getClaves() {
        return new TreeSet<K>(this.indice.keySet());
    }

    public long getNumeroRegistros() throws IOException {
        return this.datos.getNumeroRegistros();
    }

    private void comprobarClave(K clave, T registro) {
        if (clave == null
                || registro == null
                || !clave.equals(registro.getClave())) {
            throw new IllegalArgumentException(
                    "La clave no coincide con la del registro.");
        }
    }
}
