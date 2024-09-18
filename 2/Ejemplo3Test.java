import static org.junit.jupiter.api.Assertions.*;

class Ejemplo3Test {

    @org.junit.jupiter.api.Test
    void letraDNI() {
        assertAll(
                () -> assertEquals('X', Ejemplo3.letraDNI(95129689)),
                () -> assertEquals('Y', Ejemplo3.letraDNI(12009801)),
                () -> assertEquals('K', Ejemplo3.letraDNI(87213399))
        );
    }
}