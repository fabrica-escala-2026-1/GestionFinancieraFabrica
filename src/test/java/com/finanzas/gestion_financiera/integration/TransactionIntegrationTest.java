package com.finanzas.gestion_financiera.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finanzas.gestion_financiera.entity.Category;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.CategoryRepository;
import com.finanzas.gestion_financiera.repository.UserRepository;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Importante para limpiar la base de datos después de cada test
@DisplayName("Transaction Service Integration Test - Coverage Focus")
class TransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Category testCategory;

   @BeforeEach
    void setUp() {
        // 1. Crear el usuario
        testUser = new User();
        testUser.setPrimer_nombre("QA");
        testUser.setApellido("Transaction");
        testUser.setEmail("test@email.com");
        testUser.setContrasena("123456");
        userRepository.save(testUser);

        // 2. Crear la categoría
        testCategory = new Category();
        testCategory.setNombre("Sueldo");
        testCategory.setTipo(Category.TipoCategoria.INGRESO);
        testCategory.setUsuario(testUser); 
        categoryRepository.save(testCategory);
    }

    @Nested
    @DisplayName("Validaciones de Negocio (Coverage)")
    @WithMockUser(username = "test@email.com")
    class BusinessLogicCoverage {

        @Test
        @DisplayName("Cubre: if (tipo no es INGRESO o GASTO)")
        void debeLanzarErrorSiTipoInvalido() throws Exception {
            String json = """
                {
                    "tipo": "TRANSFERENCIA",
                    "monto": 100.0,
                    "fecha": "2026-05-04",
                    "categoriaId": %d
                }
                """.formatted(testCategory.getId());

            mockMvc.perform(post("/api/v1/transacciones")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest()); // O el error que maneje tu GlobalExceptionHandler
        }

        @Test
        @DisplayName("Cubre: if (monto <= 0)")
        void debeLanzarErrorSiMontoCero() throws Exception {
            String json = """
                {
                    "tipo": "INGRESO",
                    "monto": 0.0,
                    "fecha": "2026-05-04",
                    "categoriaId": %d
                }
                """.formatted(testCategory.getId());

            mockMvc.perform(post("/api/v1/transacciones")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "test@email.com")
        void debeLanzarErrorSiMesInvalido() throws Exception {
            mockMvc.perform(get("/api/v1/transacciones/resumen/2026/13")) // Mes fuera de rango
                    .andExpect(status().isBadRequest()) 
                    .andExpect(jsonPath("$.mensaje").value("El mes debe estar entre 1 y 12"));
        }

        @Test
        @WithMockUser(username = "test@email.com")
        void debeRetornarMensajeCuandoNoHayGastos() throws Exception {
            // Año random (1990) para asegurar que está vacío
            mockMvc.perform(get("/api/v1/transacciones/resumen/1990/1")) 
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensaje").value("No tienes transacciones registradas para este período"));
        }

        @Test
        @WithMockUser(username = "test@email.com")
        @DisplayName("Cobertura Handler: Error de formato en JSON")
        void debeLanzarErrorSiJsonMalFormado() throws Exception {
            String jsonInvalido = "{\"monto\": \"no-soy-un-numero\"}";
            
            mockMvc.perform(post("/api/v1/transacciones")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonInvalido))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje").exists());
        }
    }
}