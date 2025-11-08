package co.edu.uniquindio.application;

import co.edu.uniquindio.application.utils.CsvUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvUtilsTest {

    @Test
    void escape_manejaComasComillasYSaltosDeLinea() {
        assertEquals("simple", CsvUtils.escape("simple"));
        assertEquals("\"a,b\"", CsvUtils.escape("a,b"));                 // coma
        assertEquals("\"a\"\"b\"\"\"", CsvUtils.escape("a\"b\""));       // comillas -> duplicadas y campo entrecomillado
        assertEquals("\"line1\nline2\"", CsvUtils.escape("line1\nline2"));// salto de línea
    }


    @Test
    void joinRow_uneCeldasConComasYAdecuadoEscapado() {
        String row = CsvUtils.joinRow(List.of("1", "A,B", "C\"D", "E\nF"));
        assertEquals("1,\"A,B\",\"C\"\"D\",\"E\nF\"", row);
    }
}