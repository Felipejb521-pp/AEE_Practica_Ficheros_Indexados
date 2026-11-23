package FicheIndx;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * @author Álvaro López De San Román
 */
public class IndiceDAO<K> {

    private File fichero;

    public IndiceDAO(File fichero) {
        if (fichero == null) {
            throw new IllegalArgumentException(
                    "Falta el fichero del índice.");
        }

        this.fichero = fichero;
    }

    @SuppressWarnings("unchecked")
    public TreeMap<K, Long> leer()
            throws IOException, ClassNotFoundException {

        if (!this.fichero.exists()) {
            return new TreeMap<K, Long>();
        }

        try (ObjectInputStream entrada = new ObjectInputStream(
                new FileInputStream(this.fichero))) {

            Object objeto = entrada.readObject();

            if (!(objeto instanceof TreeMap)) {
                throw new IOException(
                        "El fichero no contiene un TreeMap.");
            }

            TreeMap<?, ?> comprobacion = (TreeMap<?, ?>) objeto;

            Iterator<?> valores
                    = comprobacion.values().iterator();

            while (valores.hasNext()) {
                Object valor = valores.next();

                if (!(valor instanceof Long)
                        || ((Long) valor).longValue() < 0) {
                    throw new IOException(
                            "El índice contiene posiciones incorrectas.");
                }
            }

            return (TreeMap<K, Long>) objeto;
        }
    }

    public void guardar(TreeMap<K, Long> indice)
            throws IOException {

        if (indice == null) {
            throw new IllegalArgumentException(
                    "Falta el índice.");
        }

        File carpeta = this.fichero.getParentFile();

        if (carpeta != null
                && !carpeta.exists()
                && !carpeta.mkdirs()) {
            throw new IOException(
                    "No se puede crear la carpeta del índice.");
        }

        try (ObjectOutputStream salida = new ObjectOutputStream(
                new FileOutputStream(this.fichero))) {

            salida.writeObject(indice);
        }
    }
}
