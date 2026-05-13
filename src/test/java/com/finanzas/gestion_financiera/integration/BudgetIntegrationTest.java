package com.finanzas.gestion_financiera.integration; // Ajusta a tu paquete real

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.hamcrest.Matchers.containsString;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

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


import com.finanzas.gestion_financiera.dto.BudgetRequest;
import com.finanzas.gestion_financiera.entity.Budget;
import com.finanzas.gestion_financiera.entity.Category;
import com.finanzas.gestion_financiera.entity.Transaction;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.BudgetRepository;
import com.finanzas.gestion_financiera.repository.CategoryRepository;
import com.finanzas.gestion_financiera.repository.TransactionRepository;
import com.finanzas.gestion_financiera.repository.UserRepository;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Budget Integration Tests for SonarQube Coverage")
class BudgetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired 
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private User testUser;
    private Category testCategory;
    private Long categoryId;
    private Long presupuestoId;
    
    @BeforeEach
    void setUp() {
        // 1. Crear el usuario
        testUser = new User();
        testUser.setPrimer_nombre("QA");
        testUser.setApellido("Tester");
        testUser.setEmail("test@email.com");
        testUser.setContrasena("123456");
        userRepository.save(testUser);

        // 2. Crear la categoría (Usando el Enum interno de Category)
        testCategory = new Category();
        testCategory.setNombre("Alimentación");
        testCategory.setTipo(Category.TipoCategoria.GASTO);
        testCategory.setUsuario(testUser);
        categoryRepository.save(testCategory);

        this.categoryId = testCategory.getId();
    }

    private void setupPresupuestoYGasto(BigDecimal montoPresupuesto, BigDecimal montoGasto) {
        // 1. Limpiar tablas para evitar basura de otros tests
        transactionRepository.deleteAll();
        budgetRepository.deleteAll();

        // 2. Crear presupuesto para el mes actual
        Budget budget = new Budget();
        budget.setCategory(testCategory); // Asume que ya la creaste en el @BeforeEach
        budget.setAmount(montoPresupuesto);
        budget.setStartMonth(LocalDate.now().getMonthValue());
        budget.setStartYear(LocalDate.now().getYear());
        budget.setDurationMonths(1);
        
        Budget savedBudget = budgetRepository.save(budget);
        this.presupuestoId = savedBudget.getId();

        // 3. Crear transacción de gasto para el mes actual
        Transaction t = new Transaction();
        t.setMonto(montoGasto);
        t.setCategoria(testCategory);
        t.setUsuario(testUser);
        t.setFecha(LocalDate.now()); // Para que caiga en el rango del mes actual
        t.setTipo("GASTO");
        transactionRepository.save(t);
    }

    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("POST /api/v1/presupuestos - Crear presupuesto exitoso")
    void crearPresupuesto_Exito() throws Exception {
        String json = """
            {
                "categoryId": %d,
                "amount": 2000.00,
                "durationMonths": 2
            }
            """.formatted(testCategory.getId());

        mockMvc.perform(post("/api/v1/presupuestos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(2000.00));
    }

    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("POST /api/v1/presupuestos - Error por monto inválido")
    void crearPresupuesto_MontoInvalido() throws Exception {
        String json = """
            {
                "categoryId": %d,
                "amount": 0.00,
                "durationMonths": 1
            }
            """.formatted(testCategory.getId());

        mockMvc.perform(post("/api/v1/presupuestos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/presupuestos - Listar presupuestos del usuario")
    void listarPresupuestos_Exito() throws Exception {
        // Crear un presupuesto previo para que la lista no esté vacía y cubra el mapeo
        Budget budget = new Budget();
        budget.setCategory(testCategory);
        budget.setAmount(new BigDecimal("1500"));
        budget.setStartMonth(LocalDate.now().getMonthValue());
        budget.setStartYear(LocalDate.now().getYear());
        budget.setDurationMonths(0);
        budgetRepository.save(budget);

        mockMvc.perform(get("/api/v1/presupuestos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/presupuestos/comparativa - Probar endpoint comparativa")
    void obtenerComparativa_Exito() throws Exception {
        mockMvc.perform(get("/api/v1/presupuestos/comparativa"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("DELETE /api/v1/presupuestos/{id} - Eliminar presupuesto")
    void eliminarPresupuesto_Exito() throws Exception {
        // Primero creamos uno para borrarlo
        Budget budget = new Budget();
        budget.setCategory(testCategory);
        budget.setAmount(new BigDecimal("500"));
        budget.setStartMonth(1);
        budget.setStartYear(2026);
        budget.setDurationMonths(0);
        budgetRepository.save(budget);

        mockMvc.perform(delete("/api/v1/presupuestos/" + budget.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test@email.com")
    void debeLanzarErrorSiYaExistePresupuestoActivo() throws Exception {
        budgetRepository.deleteAll();
        // 1. Primero creamos uno exitosamente
        BudgetRequest request = new BudgetRequest(categoryId, new BigDecimal("500.00"), 3);
        mockMvc.perform(post("/api/v1/presupuestos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 2. Intentamos crear exactamente el mismo presupuesto otra vez
        mockMvc.perform(post("/api/v1/presupuestos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // El Handler lo vuelve 400
                .andExpect(jsonPath("$.mensaje").value("Ya existe un presupuesto activo para esta categoría en ese período"));
    }

    @Test
    @WithMockUser(username = "test@email.com")
    void debeActualizarPresupuestoExistente() throws Exception {
        setupPresupuestoYGasto(new BigDecimal("500.00"), BigDecimal.ZERO);
        BudgetRequest updateRequest = new BudgetRequest(categoryId, new BigDecimal("1000.00"), 6);

        mockMvc.perform(put("/api/v1/presupuestos/{id}", presupuestoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1000.00));
    }

    @Test
    @WithMockUser(username = "test@email.com")
    void debeMostrarMensajeUsoNormalCuandoGastoEsBajo() throws Exception {
        // Escenario: Presupuesto de 1,000,000 y Gasto de 100,000 (10%)
        setupPresupuestoYGasto(new BigDecimal("1000000"), new BigDecimal("100000"));

        mockMvc.perform(get("/api/v1/presupuestos/comparativa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.budgetComparisonResponseList[0].porcentaje").value(10.0))
                .andExpect(jsonPath("$._embedded.budgetComparisonResponseList[0].alerta").value(containsString("Llevas el 10.0% del presupuesto")));
    }

    @Test
    @WithMockUser(username = "test@email.com")
    void debeMostrarAlerta80CuandoSeAcercaAlLimite() throws Exception {
        // Escenario: Presupuesto de 1,000,000 y Gasto de 850,000 (85%)
        setupPresupuestoYGasto(new BigDecimal("1000000"), new BigDecimal("850000"));

        mockMvc.perform(get("/api/v1/presupuestos/comparativa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.budgetComparisonResponseList[0].porcentaje").value(85.0))
                .andExpect(jsonPath("$._embedded.budgetComparisonResponseList[0].alerta").value(containsString("Has superado el 80% del presupuesto")));
    }

    @Test
    @WithMockUser(username = "test@email.com")
    void debeMostrarAlertaDeExcedidoEnComparativa() throws Exception {
        setupPresupuestoYGasto(new BigDecimal("1000.00"), new BigDecimal("1200.00"));
        mockMvc.perform(get("/api/v1/presupuestos/comparativa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.budgetComparisonResponseList[0].alerta").value(containsString("Has excedido el presupuesto")));
    }
}