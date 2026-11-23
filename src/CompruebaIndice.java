import java.io.File;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;

import FicheIndx.IndiceDAO;

/**
 * @author Álvaro López De San Román
 */
public class CompruebaIndice {

    public static void main(String[] args) {
        File fichero = new File("Ficheros", "INDICE.DAT");

        if (!fichero.exists()) {
            System.out.println("No existe INDICE.DAT.");
            return;
        }

        IndiceDAO<String> dao = new IndiceDAO<String>(fichero);

        try {
            TreeMap<String, Long> indice = dao.leer();

            Iterator<String> claves
                    = indice.keySet().iterator();

            while (claves.hasNext()) {
                String clave = claves.next();

                System.out.println(
                        clave + " -> " + indice.get(clave));
            }

            System.out.println("Entradas: " + indice.size());

        } catch (FileNotFoundException e) {
            System.out.println("No se puede abrir el índice.");

        } catch (EOFException e) {
            System.out.println("El índice está incompleto.");

        } catch (ClassNotFoundException e) {
            System.out.println(
                    "No se reconoce una clase del índice.");

        } catch (IOException e) {
            System.out.println(
                    "Error al leer el índice: " + e.getMessage());
        }
    }
}
