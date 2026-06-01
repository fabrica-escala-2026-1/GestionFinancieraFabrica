package com.finanzas.gestion_financiera.integration;

import com.finanzas.gestion_financiera.entity.Category;
import com.finanzas.gestion_financiera.entity.Transaction;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.CategoryRepository;
import com.finanzas.gestion_financiera.repository.TransactionRepository;
import com.finanzas.gestion_financiera.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("SpendingReport Integration Tests for SonarQube Coverage")
class SpendingReportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private User testUser;
    private Category alimentacion;
    private Category transporte;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setPrimer_nombre("QA");
        testUser.setApellido("Tester");
        testUser.setEmail("test@email.com");
        testUser.setContrasena("123456");
        userRepository.save(testUser);

        alimentacion = new Category();
        alimentacion.setNombre("Alimentación");
        alimentacion.setUsuario(testUser);
        categoryRepository.save(alimentacion);

        transporte = new Category();
        transporte.setNombre("Transporte");
        transporte.setUsuario(testUser);
        categoryRepository.save(transporte);
    }

    private void saveGasto(Category categoria, BigDecimal monto, LocalDate fecha) {
        Transaction t = new Transaction();
        t.setTipo("GASTO");
        t.setMonto(monto);
        t.setFecha(fecha);
        t.setUsuario(testUser);
        t.setCategoria(categoria);
        transactionRepository.save(t);
    }

    // SR-01
    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/reportes/current-month - Retorna distribución con datos del mes actual")
    void debeRetornarDistribucionDelMesActual() throws Exception {
        LocalDate hoy = LocalDate.now();
        saveGasto(alimentacion, new BigDecimal("600000"), hoy);
        saveGasto(transporte, new BigDecimal("400000"), hoy);

        mockMvc.perform(get("/api/v1/reportes/current-month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSpent").value(1000000.0))
                .andExpect(jsonPath("$.distribution", hasSize(2)))
                .andExpect(jsonPath("$.highestSpendingCategory.category").value("Alimentación"))
                .andExpect(jsonPath("$.highestSpendingCategory.percentage").value(60.0))
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    // SR-02
    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/reportes/current-month - Sin gastos retorna mensaje informativo")
    void debeRetornarMensajeCuandoNoHayGastos() throws Exception {
        transactionRepository.deleteAll();

        mockMvc.perform(get("/api/v1/reportes/current-month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSpent").value(0))
                .andExpect(jsonPath("$.distribution", hasSize(0)))
                .andExpect(jsonPath("$.message")
                        .value("No hay datos suficientes para generar un reporte en este período"));
    }

    // SR-03
    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/reportes/current-month - No incluye gastos de meses anteriores")
    void noDebeIncluirGastosDeMesesAnteriores() throws Exception {
        transactionRepository.deleteAll();
        saveGasto(alimentacion, new BigDecimal("999999"),
                LocalDate.now().minusMonths(1).withDayOfMonth(1));
        saveGasto(transporte, new BigDecimal("200000"), LocalDate.now());

        mockMvc.perform(get("/api/v1/reportes/current-month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSpent").value(200000.0))
                .andExpect(jsonPath("$.distribution", hasSize(1)));
    }

    // SR-04
    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/reportes/comparative - Reporte comparativo con datos en ambos meses")
    void debeRetornarReporteComparativoConDatos() throws Exception {
        saveGasto(alimentacion, new BigDecimal("500000"), LocalDate.of(2026, 1, 10));
        saveGasto(alimentacion, new BigDecimal("700000"), LocalDate.of(2026, 3, 10));

        mockMvc.perform(get("/api/v1/reportes/comparative")
                        .param("initialYear", "2026")
                        .param("initialMonth", "1")
                        .param("finalYear", "2026")
                        .param("finalMonth", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialYear").value(2026))
                .andExpect(jsonPath("$.initialMonth").value(1))
                .andExpect(jsonPath("$.finalYear").value(2026))
                .andExpect(jsonPath("$.finalMonth").value(3))
                .andExpect(jsonPath("$.rows", hasSize(1)))
                .andExpect(jsonPath("$.rows[0].category").value("Alimentación"))
                .andExpect(jsonPath("$.rows[0].initialMonthTotal").value(500000.0))
                .andExpect(jsonPath("$.rows[0].finalMonthTotal").value(700000.0))
                .andExpect(jsonPath("$.rows[0].differenceCOP").value(200000.0))
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    // SR-05
    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/reportes/comparative - Sin datos en ambos meses retorna mensaje")
    void debeRetornarMensajeCuandoAmbiosMesesSinDatos() throws Exception {
        transactionRepository.deleteAll();

        mockMvc.perform(get("/api/v1/reportes/comparative")
                        .param("initialYear", "2020")
                        .param("initialMonth", "1")
                        .param("finalYear", "2020")
                        .param("finalMonth", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows", hasSize(0)))
                .andExpect(jsonPath("$.message")
                        .value("No hay datos suficientes para generar un reporte en este período"));
    }

    // SR-06
    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/reportes/comparative - Error 400 con mes inválido (0)")
    void debeRetornar400ConMesInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/comparative")
                        .param("initialYear", "2026")
                        .param("initialMonth", "0")
                        .param("finalYear", "2026")
                        .param("finalMonth", "3"))
                .andExpect(status().isBadRequest());
    }

    // SR-07
    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/reportes/comparative - Categoría nueva en mes final tiene initialMonthTotal 0")
    void debeMostrarCategoriaNuevaConInitialMonthTotalCero() throws Exception {
        transactionRepository.deleteAll();
        saveGasto(transporte, new BigDecimal("300000"), LocalDate.of(2026, 3, 5));

        mockMvc.perform(get("/api/v1/reportes/comparative")
                        .param("initialYear", "2026")
                        .param("initialMonth", "1")
                        .param("finalYear", "2026")
                        .param("finalMonth", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].initialMonthTotal").value(0.0))
                .andExpect(jsonPath("$.rows[0].finalMonthTotal").value(300000.0));
    }

    // SR-08
    @Test
    @DisplayName("GET /api/v1/reportes/current-month - Error 403 sin autenticación")
    void debeRetornar403SinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/current-month"))
                .andExpect(status().isForbidden());
    }

    // SR-09
    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/reportes/current-month - Retorna links HATEOAS")
    void debeIncluirLinksHATEOAS() throws Exception {
        saveGasto(alimentacion, new BigDecimal("100000"), LocalDate.now());

        mockMvc.perform(get("/api/v1/reportes/current-month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self").exists())
                .andExpect(jsonPath("$._links.comparative").exists());
    }
}