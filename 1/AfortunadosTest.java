import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AfortunadosTest {

     @Test
    public void testa() {
        assertEquals("No funciona cuando sólo hay un número ->", "2", Afortunados.calcular(3).trim());
        assertEquals("4 6 10", Afortunados.calcular(10).trim());
        assertEquals("10 12 18 22 30", Afortunados.calcular(30).trim());
        assertEquals("30 34 42 48 58 60 78 82", Afortunados.calcular(100).trim());
        //assertEquals("Error", Afortunados.calcular(20), "30 34 42 48 58 60 78 ");

    }
    @Test
    public void test2() {
        //assertEquals("No ha funcionado cuando son distintos", Double.parseDouble(Afortunados.calcular(Integer.MAX_VALUE)) , 3f , 0f);
        //assertEquals("No ha funcionado cuando son distintos", Double.parseDouble(Afortunados.calcular(3)) , 3f , 0f);
    }
}
