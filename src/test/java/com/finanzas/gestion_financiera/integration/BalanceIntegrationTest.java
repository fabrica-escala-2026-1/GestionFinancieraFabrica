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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Balance Integration Tests for SonarQube Coverage")
class BalanceIntegrationTest {

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

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setPrimer_nombre("QA");
        testUser.setApellido("Tester");
        testUser.setEmail("test@email.com");
        testUser.setContrasena("123456");
        userRepository.save(testUser);

        testCategory = new Category();
        testCategory.setNombre("Salario");
        testCategory.setUsuario(testUser);
        categoryRepository.save(testCategory);
    }

    private void saveTransaction(String tipo, BigDecimal monto, LocalDate fecha) {
        Transaction t = new Transaction();
        t.setTipo(tipo);
        t.setMonto(monto);
        t.setFecha(fecha);
        t.setUsuario(testUser);
        t.setCategoria(testCategory);
        transactionRepository.save(t);
    }

    // BL-01
    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/balance/mes-actual - Balance positivo del mes actual")
    void debeRetornarBalancePositivoMesActual() throws Exception {
        saveTransaction("INGRESO", new BigDecimal("3000000"), LocalDate.now());
        saveTransaction("GASTO", new BigDecimal("1000000"), LocalDate.now());

        mockMvc.perform(get("/api/v1/balance/mes-actual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingresosTotales").value(3000000.0))
                .andExpect(jsonPath("$.gastosTotales").value(1000000.0))
                .andExpect(jsonPath("$.balanceNeto").value(2000000.0))
                .andExpect(jsonPath("$.balanceNegativo").value(false))
                .andExpect(jsonPath("$.mensaje").value("Tu balance financiero es positivo"));
    }

    // BL-02
    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/balance/mes-actual - Balance negativo del mes actual")
    void debeRetornarBalanceNegativoMesActual() throws Exception {
        saveTransaction("INGRESO", new BigDecimal("500000"), LocalDate.now());
        saveTransaction("GASTO", new BigDecimal("800000"), LocalDate.now());

        mockMvc.perform(get("/api/v1/balance/mes-actual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceNegativo").value(true))
                .andExpect(jsonPath("$.balanceNeto").value(-300000.0))
                .andExpect(jsonPath("$.mensaje").value(
                        "Tu balance es negativo. Revisa tus movimientos para ajustar el descuadre"));
    }

    // BL-03
    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/balance/mes-actual - Sin transacciones retorna balance cero")
    void debeRetornarBalanceCeroSinTransacciones() throws Exception {
        transactionRepository.deleteAll();

        mockMvc.perform(get("/api/v1/balance/mes-actual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceNeto").value(0.0))
                .andExpect(jsonPath("$.balanceNegativo").value(false));
    }

    // BL-04
    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/balance - Balance por período con datos correctos")
    void debeRetornarBalancePorPeriodo() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fin = LocalDate.of(2026, 1, 31);

        saveTransaction("INGRESO", new BigDecimal("5000000"), LocalDate.of(2026, 1, 15));
        saveTransaction("GASTO", new BigDecimal("2000000"), LocalDate.of(2026, 1, 20));

        mockMvc.perform(get("/api/v1/balance")
                        .param("fechaInicio", inicio.toString())
                        .param("fechaFin", fin.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingresosTotales").value(5000000.0))
                .andExpect(jsonPath("$.gastosTotales").value(2000000.0))
                .andExpect(jsonPath("$.balanceNeto").value(3000000.0))
                .andExpect(jsonPath("$.balanceNegativo").value(false));
    }

    // BL-05
    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/balance - No retorna transacciones fuera del período")
    void noDebeIncluirTransaccionesFueraDelPeriodo() throws Exception {
        // Transaction outside the range
        saveTransaction("INGRESO", new BigDecimal("9000000"), LocalDate.of(2025, 12, 31));
        // Transaction inside the range
        saveTransaction("INGRESO", new BigDecimal("1000000"), LocalDate.of(2026, 1, 5));

        mockMvc.perform(get("/api/v1/balance")
                        .param("fechaInicio", "2026-01-01")
                        .param("fechaFin", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingresosTotales").value(1000000.0));
    }

    // BL-06
    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/balance - Error 400 cuando fechaInicio > fechaFin")
    void debeRetornar400CuandoFechaInicioEsMayorQueFechaFin() throws Exception {
        mockMvc.perform(get("/api/v1/balance")
                        .param("fechaInicio", "2026-06-01")
                        .param("fechaFin", "2026-01-01"))
                .andExpect(status().isBadRequest());
    }

    // BL-07
    @Test
    @WithMockUser(username = "test@email.com")
    @DisplayName("GET /api/v1/balance - Balance con misma fecha inicio y fin")
    void debeRetornarBalanceCuandoFechasIguales() throws Exception {
        LocalDate hoy = LocalDate.now();
        saveTransaction("INGRESO", new BigDecimal("200000"), hoy);

        mockMvc.perform(get("/api/v1/balance")
                        .param("fechaInicio", hoy.toString())
                        .param("fechaFin", hoy.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingresosTotales").value(200000.0));
    }

    // BL-08
    @Test
    @DisplayName("GET /api/v1/balance/mes-actual - Error 403 sin autenticación")
    void debeRetornar403SinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/v1/balance/mes-actual"))
                .andExpect(status().isForbidden());
    }
}