package com.universidad.catalogo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class ApplicationContextTest {

    @Test
    @DisplayName("Verifica que el contexto completo de Spring Boot y H2 levantan correctamente")
    void contextLoads() {
        // Si el contexto de Spring y la base de datos H2 inician sin excepciones, la prueba pasa exitosamente.
    }
}

