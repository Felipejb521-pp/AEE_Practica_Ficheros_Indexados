package Empleado;

import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * @author Equipo 3
 */
public class Fecha implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int ANIO_MINIMO = 1900;
    private static final int ANIO_MAXIMO = 9999;

    private byte dia;
    private byte mes;
    private short anio;

    public Fecha(int dia, int mes, int anio) {
        validar(dia, mes, anio);

        this.dia = (byte) dia;
        this.mes = (byte) mes;
        this.anio = (short) anio;
    }

    public Fecha(String texto) {
        if (texto == null
                || !texto.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            throw new IllegalArgumentException(
                    "El formato de fecha debe ser dd/mm/aaaa.");
        }

        String[] partes = texto.split("/");

        int diaLeido = Integer.parseInt(partes[0]);
        int mesLeido = Integer.parseInt(partes[1]);
        int anioLeido = Integer.parseInt(partes[2]);

        validar(diaLeido, mesLeido, anioLeido);

        this.dia = (byte) diaLeido;
        this.mes = (byte) mesLeido;
        this.anio = (short) anioLeido;
    }

    private static void validar(int dia, int mes, int anio) {
        if (anio < ANIO_MINIMO || anio > ANIO_MAXIMO) {
            throw new IllegalArgumentException(
                    "Año fuera de rango.");
        }

        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException(
                    "El mes debe estar entre 1 y 12.");
        }

        if (dia < 1 || dia > diasDelMes(mes, anio)) {
            throw new IllegalArgumentException(
                    "El día no existe en ese mes.");
        }
    }

    public static boolean esBisiesto(int anio) {
        return (anio % 4 == 0 && anio % 100 != 0)
                || anio % 400 == 0;
    }

    public static int diasDelMes(int mes, int anio) {
        int dias;

        switch (mes) {
            case 4:
            case 6:
            case 9:
            case 11:
                dias = 30;
                break;

            case 2:
                if (esBisiesto(anio)) {
                    dias = 29;
                } else {
                    dias = 28;
                }
                break;

            default:
                dias = 31;
                break;
        }

        return dias;
    }

    public static Fecha hoy() {
        GregorianCalendar calendario = new GregorianCalendar();

        return new Fecha(
                calendario.get(Calendar.DAY_OF_MONTH),
                calendario.get(Calendar.MONTH) + 1,
                calendario.get(Calendar.YEAR));
    }

    public boolean esPosteriorA(Fecha otra) {
        if (this.anio != otra.anio) {
            return this.anio > otra.anio;
        }

        if (this.mes != otra.mes) {
            return this.mes > otra.mes;
        }

        return this.dia > otra.dia;
    }

    public byte getDia() {
        return this.dia;
    }

    public byte getMes() {
        return this.mes;
    }

    public short getAnio() {
        return this.anio;
    }

    @Override
    public String toString() {
        return String.format(
                "%02d/%02d/%04d", this.dia, this.mes, this.anio);
    }
}