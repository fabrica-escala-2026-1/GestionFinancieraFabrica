package com.finanzas.gestion_financiera.integration;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.finanzas.gestion_financiera.entity.Category;
import com.finanzas.gestion_financiera.entity.Transaction;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.CategoryRepository;
import com.finanzas.gestion_financiera.repository.TransactionRepository;
import com.finanzas.gestion_financiera.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Transaction Integration Tests for SonarQube Coverage")
class TransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private User testUser;
    private Category testCategory;
    private Long categoryId;

    @BeforeEach
    void setUp() {
        // 1. Crear el usuario
        testUser = new User();
        testUser.setPrimer_nombre("QA");
        testUser.setApellido("Tester");
        testUser.setEmail("test@email.com");
        testUser.setContrasena("123456");
        userRepository.save(testUser);

        // 2. Crear la categoría
        testCategory = new Category();
        testCategory.setNombre("Alimentación");
        testCategory.setUsuario(testUser);
        categoryRepository.save(testCategory);

        this.categoryId = testCategory.getId();
    }

    @Test // TF-01
    @WithMockUser(username = "test@email.com")
    @DisplayName("POST /api/v1/transacciones - Crear transacción de ingreso exitosa")
    void crearTransaccion_IngresoExitoso() throws Exception {
        // Crear otra categoría
        Category ingresoCategory = new Category();
        ingresoCategory.setNombre("Salario");
        ingresoCategory.setUsuario(testUser);
        categoryRepository.save(ingresoCategory);

        String json = """
                {
                    "categoriaId": %d,
                    "monto": 3000000.00,
                    "tipo": "INGRESO",
                    "fecha": "%s"
                }
                """.formatted(ingresoCategory.getId(), LocalDate.now());

        mockMvc.perform(post("/api/v1/transacciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monto").value(3000000.00))
                .andExpect(jsonPath("$.tipo").value("INGRESO"));
    }

    @Test // TF-02
    @WithMockUser(username = "test@email.com")
    @DisplayName("POST /api/v1/transacciones - Crear transacción de gasto exitosa")
    void crearTransaccion_GastoExitoso() throws Exception {
        String json = """
                {
                    "categoriaId": %d,
                    "monto": 50000.00,
                    "tipo": "GASTO",
                    "fecha": "%s"
                }
                """.formatted(categoryId, LocalDate.now());

        mockMvc.perform(post("/api/v1/transacciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monto").value(50000.00))
                .andExpect(jsonPath("$.tipo").value("GASTO"));
    }

    @Test // TF-03
    @WithMockUser(username = "test@email.com")
    @DisplayName("POST /api/v1/transacciones - Error por monto inválido (cero)")
    void crearTransaccion_MontoInvalido() throws Exception {
        String json = """
                {
                    "categoriaId": %d,
                    "monto": 0.00,
                    "tipo": "GASTO",
                    "fecha": "%s"
                }
                """.formatted(categoryId, LocalDate.now());

        mockMvc.perform(post("/api/v1/transacciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test // TF-04
    @WithMockUser(username = "test@email.com")
    @DisplayName("POST /api/v1/transacciones - Error por categoría inexistente")
    void crearTransaccion_CategoriaInexistente() throws Exception {
        String json = """
                {
                    "categoriaId": 9999,
                    "monto": 50000.00,
                    "tipo": "GASTO",
                    "fecha": "%s"
                }
                """.formatted(LocalDate.now());

        mockMvc.perform(post("/api/v1/transacciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isNotFound());
    }

    @Test // TF-05
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/transacciones - Listar transacciones del usuario autenticado")
    void listarTransacciones_Exito() throws Exception {
        // Crear una transacción previa para que la lista no esté vacía
        Transaction t = new Transaction();
        t.setMonto(new BigDecimal("80000"));
        t.setCategoria(testCategory);
        t.setUsuario(testUser);
        t.setFecha(LocalDate.now());
        t.setTipo("GASTO");
        transactionRepository.save(t);

        mockMvc.perform(get("/api/v1/transacciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test // TF-06
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/transacciones - Listar retorna lista vacía cuando no hay transacciones")
    void listarTransacciones_ListaVacia() throws Exception {
        transactionRepository.deleteAll();

        mockMvc.perform(get("/api/v1/transacciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test // TF-07
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/transacciones/resumen/{anio}/{mes} - Resumen de gastos del mes actual")
    void obtenerResumen_MesActual_Exito() throws Exception {
        int anio = LocalDate.now().getYear();
        int mes = LocalDate.now().getMonthValue();

        // Crear dos transacciones de gasto en el mes actual
        for (BigDecimal monto : List.of(new BigDecimal("200000"), new BigDecimal("150000"))) {
            Transaction t = new Transaction();
            t.setMonto(monto);
            t.setCategoria(testCategory);
            t.setUsuario(testUser);
            t.setFecha(LocalDate.now());
            t.setTipo("GASTO");
            transactionRepository.save(t);
        }

        mockMvc.perform(get("/api/v1/transacciones/resumen/{anio}/{mes}", anio, mes))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorias[0].totalGastado").value(350000.00));
    }

    @Test // TF-08
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/transacciones/resumen/{anio}/{mes} - Resumen vacío cuando no hay gastos en el mes")
    void obtenerResumen_SinGastosEnElMes_Exito() throws Exception {
        transactionRepository.deleteAll();

        int anio = LocalDate.now().getYear();
        int mes = LocalDate.now().getMonthValue();

        mockMvc.perform(get("/api/v1/transacciones/resumen/{anio}/{mes}", anio, mes))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje")
                        .value("No tienes transacciones registradas para este período"));
    }

    @Test // TF-09
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/transacciones/resumen/{anio}/{mes} - Error por mes fuera de rango")
    void obtenerResumen_MesInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/transacciones/resumen/2025/13"))
                .andExpect(status().isBadRequest());
    }

}