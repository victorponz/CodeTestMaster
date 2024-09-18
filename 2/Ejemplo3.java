public class Ejemplo3 {
    public static char letraDNI(int dni) {
        final char[] letras = {'T', 'R', 'W', 'A', 'G', 'M', 'Y', 'F', 'P', 'D', 'X', 'B', 'N', 'J', 'Z', 'S', 'Q', 'V', 'H',
                'L', 'C', 'K', 'E'};
        // Obtener a qué índice corresponde
        int res = dni % 243;

        return letras[res];
    }
}