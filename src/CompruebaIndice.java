import java.io.File;
import java.util.Iterator;
import java.util.TreeMap;

import indexado.IndiceDAO;

/**
 * @author Álvaro López De San Román
 *
 * Muestra el contenido de INDICE.DAT: cada DNI con la posición en bytes
 * de su bloque dentro de FICHENDX.DAT.
 */
public class CompruebaIndice {

    public static void main(String[] args) {
        File fichero = new File("Ficheros", "INDICE.DAT");

        if (!fichero.exists()) {
            System.out.println("No existe INDICE.DAT.");
        } else {
            // IndiceDAO (paquete indexado) ya trata sus propios errores
            IndiceDAO dao = new IndiceDAO(fichero);
            TreeMap indice = dao.leer();

            Iterator claves = indice.keySet().iterator();

            while (claves.hasNext()) {
                Object clave = claves.next();
                System.out.println(clave + " -> " + indice.get(clave));
            }

            System.out.println("Entradas: " + indice.size());
        }
    }
}
