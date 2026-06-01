package com.finanzas.gestion_financiera.integration;

import com.finanzas.gestion_financiera.entity.Budget;
import com.finanzas.gestion_financiera.entity.Category;
import com.finanzas.gestion_financiera.entity.Transaction;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.BudgetRepository;
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

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Recommendation Integration Tests for SonarQube Coverage")
class RecommendationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    private User testUser;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setPrimer_nombre("QA");
        testUser.setApellido("Tester");
        testUser.setEmail("test@email.com");
        testUser.setContrasena("123456");
        userRepository.save(testUser);

        testCategory = new Category();
        testCategory.setNombre("Alimentación");
        testCategory.setUsuario(testUser);
        categoryRepository.save(testCategory);
    }

    private Transaction buildTransaction(String tipo, BigDecimal monto, LocalDate fecha) {
        Transaction t = new Transaction();
        t.setTipo(tipo);
        t.setMonto(monto);
        t.setFecha(fecha);
        t.setUsuario(testUser);
        t.setCategoria(testCategory);
        return t;
    }

    private Budget buildBudget(int startMonth, int startYear, BigDecimal amount) {
        Budget b = new Budget();
        b.setCategory(testCategory);
        b.setAmount(amount);
        b.setStartMonth(startMonth);
        b.setStartYear(startYear);
        b.setDurationMonths(1);
        return b;
    }

    // RC-01
    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/recomendaciones - SIN_DATOS cuando usuario no tiene transacciones")
    void debeRetornarSinDatosCuandoNoHayTransacciones() throws Exception {
        transactionRepository.deleteAll();

        mockMvc.perform(get("/api/v1/recomendaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.recommendationResponseList[0].tipo")
                        .value("SIN_DATOS"));
    }

    // RC-02
    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/recomendaciones - SIN_DATOS cuando hay menos de 30 días de uso")
    void debeRetornarSinDatosCuandoMenosDe30Dias() throws Exception {
        transactionRepository.deleteAll();
        transactionRepository.save(
                buildTransaction("GASTO", new BigDecimal("50000"), LocalDate.now().minusDays(5)));

        mockMvc.perform(get("/api/v1/recomendaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.recommendationResponseList[0].tipo")
                        .value("SIN_DATOS"));
    }

    // RC-03
    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/recomendaciones - AHORRO cuando ingresos > gastos 3 meses consecutivos")
    void debeRetornarAhorroConBalancePositivo3Meses() throws Exception {
        transactionRepository.deleteAll();
        budgetRepository.deleteAll();

        // Primera transacción hace más de 30 días
        transactionRepository.save(
                buildTransaction("INGRESO", new BigDecimal("100"), LocalDate.now().minusDays(40)));

        // 3 meses con ingresos > gastos
        for (int i = 1; i <= 3; i++) {
            LocalDate fecha = LocalDate.now().minusMonths(i).withDayOfMonth(15);
            transactionRepository.save(buildTransaction("INGRESO", new BigDecimal("3000000"), fecha));
            transactionRepository.save(buildTransaction("GASTO", new BigDecimal("1000000"), fecha));
        }

        mockMvc.perform(get("/api/v1/recomendaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$._embedded.recommendationResponseList[*].tipo",
                        hasItem("AHORRO")));
    }

    // RC-04
    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/recomendaciones - BALANCE_NEGATIVO cuando gastos > ingresos 2+ meses")
    void debeRetornarBalanceNegativo() throws Exception {
        transactionRepository.deleteAll();
        budgetRepository.deleteAll();

        // Primera transacción hace más de 30 días
        transactionRepository.save(
                buildTransaction("INGRESO", new BigDecimal("100"), LocalDate.now().minusDays(40)));

        // 2 meses con gastos > ingresos
        for (int i = 1; i <= 2; i++) {
            LocalDate fecha = LocalDate.now().minusMonths(i).withDayOfMonth(15);
            transactionRepository.save(buildTransaction("INGRESO", new BigDecimal("500000"), fecha));
            transactionRepository.save(buildTransaction("GASTO", new BigDecimal("1500000"), fecha));
        }
        // Mes 3 positivo para que mesesPositivos no llegue a 3
        LocalDate mes3 = LocalDate.now().minusMonths(3).withDayOfMonth(15);
        transactionRepository.save(buildTransaction("INGRESO", new BigDecimal("2000000"), mes3));
        transactionRepository.save(buildTransaction("GASTO", new BigDecimal("500000"), mes3));

        mockMvc.perform(get("/api/v1/recomendaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$._embedded.recommendationResponseList[*].tipo",
                        hasItem("BALANCE_NEGATIVO")));
    }

    // RC-05
    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/recomendaciones - EXCESO_GASTO cuando presupuesto superado 2 meses seguidos")
    void debeRetornarExcesoGasto2MesesConsecutivos() throws Exception {
        transactionRepository.deleteAll();
        budgetRepository.deleteAll();

        // Primera transacción hace más de 30 días
        transactionRepository.save(
                buildTransaction("GASTO", new BigDecimal("100"), LocalDate.now().minusDays(40)));

        // Presupuestos para los 2 meses anteriores
        for (int i = 1; i <= 2; i++) {
            LocalDate ref = LocalDate.now().minusMonths(i);
            Budget b = buildBudget(ref.getMonthValue(), ref.getYear(), new BigDecimal("100000"));
            budgetRepository.save(b);
            // Gasto que supera el presupuesto
            transactionRepository.save(
                    buildTransaction("GASTO", new BigDecimal("200000"),
                            ref.withDayOfMonth(15)));
        }

        // Balance loop sin impacto
        transactionRepository.save(
                buildTransaction("INGRESO", new BigDecimal("500000"),
                        LocalDate.now().minusMonths(1).withDayOfMonth(10)));

        mockMvc.perform(get("/api/v1/recomendaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$._embedded.recommendationResponseList[*].tipo",
                        hasItem("EXCESO_GASTO")));
    }

    // RC-06
    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/recomendaciones - SIN_PRESUPUESTO cuando hay gastos sin presupuesto")
    void debeRetornarSinPresupuestoCuandoGastosSinPresupuesto() throws Exception {
        transactionRepository.deleteAll();
        budgetRepository.deleteAll();

        // Primera transacción hace más de 30 días
        transactionRepository.save(
                buildTransaction("GASTO", new BigDecimal("100"), LocalDate.now().minusDays(40)));

        // Gasto en el mes actual sin presupuesto
        transactionRepository.save(
                buildTransaction("GASTO", new BigDecimal("150000"), LocalDate.now()));

        mockMvc.perform(get("/api/v1/recomendaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$._embedded.recommendationResponseList[*].tipo",
                        hasItem("SIN_PRESUPUESTO")));
    }

    // RC-07
    @Test
    @DisplayName("GET /api/v1/recomendaciones - Error 403 sin autenticación")
    void debeRetornar403SinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/v1/recomendaciones"))
                .andExpect(status().isForbidden());
    }

    // RC-08
    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/recomendaciones - Respuesta incluye links HATEOAS")
    void debeIncluirLinksHATEOAS() throws Exception {
        transactionRepository.deleteAll();

        mockMvc.perform(get("/api/v1/recomendaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self").exists())
                .andExpect(jsonPath("$._links.presupuestos").exists())
                .andExpect(jsonPath("$._links.categorias").exists())
                .andExpect(jsonPath("$._links.transacciones").exists());
    }
}