package com.finanzas.gestion_financiera.integration; // Ajusta a tu paquete real

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.finanzas.gestion_financiera.entity.Budget;
import com.finanzas.gestion_financiera.entity.Category;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.BudgetRepository;
import com.finanzas.gestion_financiera.repository.CategoryRepository;
import com.finanzas.gestion_financiera.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Budget Integration Tests for SonarQube Coverage")
class BudgetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    private User testUser;
    private Category testCategory;
    
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
}