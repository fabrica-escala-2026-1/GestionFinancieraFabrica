package com.finanzas.gestion_financiera.unit.service;

import com.finanzas.gestion_financiera.dto.RecommendationResponse;
import com.finanzas.gestion_financiera.entity.Budget;
import com.finanzas.gestion_financiera.entity.Category;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.BudgetRepository;
import com.finanzas.gestion_financiera.repository.CategoryRepository;
import com.finanzas.gestion_financiera.repository.TransactionRepository;
import com.finanzas.gestion_financiera.repository.UserRepository;
import com.finanzas.gestion_financiera.service.RecommendationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService - Unit Tests")
class RecommendationServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RecommendationService recommendationService;

    private User testUser;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@email.com");

        testCategory = new Category();
        testCategory.setId(10L);
        testCategory.setNombre("Alimentación");
        testCategory.setUsuario(testUser);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("test@email.com")
                .password("{noop}")
                .authorities(List.of())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities())
        );

        lenient().when(userRepository.findByEmail("test@email.com"))
                .thenReturn(Optional.of(testUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // Escenario 5 — Sin datos suficientes
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Escenario 5 — Sin datos suficientes")
    class SinDatos {

        @Test
        @DisplayName("Should return SIN_DATOS when the user has no transactions at all")
        void shouldReturnSinDatosWhenNoTransactions() {
            when(transactionRepository.findFechaFirstTransaction(1L)).thenReturn(null);

            List<RecommendationResponse> result = recommendationService.getRecommendations();

            assertEquals(1, result.size());
            assertEquals("SIN_DATOS", result.get(0).getTipo());
            assertEquals(
                    "Sigue registrando tus movimientos para recibir recomendaciones personalizadas",
                    result.get(0).getMensaje());
        }

        @Test
        @DisplayName("Should return SIN_DATOS when the user has less than 30 days of usage")
        void shouldReturnSinDatosWhenLessThan30Days() {
            LocalDate recentDate = LocalDate.now().minusDays(10);
            when(transactionRepository.findFechaFirstTransaction(1L)).thenReturn(recentDate);

            List<RecommendationResponse> result = recommendationService.getRecommendations();

            assertEquals(1, result.size());
            assertEquals("SIN_DATOS", result.get(0).getTipo());
        }

        @Test
        @DisplayName("Should not return SIN_DATOS when the user has exactly 30 days of usage")
        void shouldNotReturnSinDatosWhenExactly30Days() {
            LocalDate firstTx = LocalDate.now().minusDays(30);
            when(transactionRepository.findFechaFirstTransaction(1L)).thenReturn(firstTx);
            when(transactionRepository.findLastTransactionDate(1L)).thenReturn(LocalDate.now());
            when(categoryRepository.findByUsuarioId(1L)).thenReturn(Collections.emptyList());
            // Stub the 3-month balance loop with zero income and zero expenses
            when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                    eq(1L), anyString(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ZERO);

            List<RecommendationResponse> result = recommendationService.getRecommendations();

            assertTrue(result.stream().noneMatch(r -> "SIN_DATOS".equals(r.getTipo())));
        }
    }

    // -------------------------------------------------------------------------
    // Escenario 1 — Exceso de gasto 2 meses consecutivos
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Escenario 1 — Exceso de gasto en 2 meses consecutivos")
    class ExcesoGasto {

        @Test
        @DisplayName("Should add EXCESO_GASTO recommendation when budget exceeded 2 months in a row")
        void shouldAddExcesoGastoWhenExceeded2ConsecutiveMonths() {
            LocalDate firstTx = LocalDate.now().minusDays(60);
            when(transactionRepository.findFechaFirstTransaction(1L)).thenReturn(firstTx);
            when(transactionRepository.findLastTransactionDate(1L)).thenReturn(LocalDate.now());
            when(categoryRepository.findByUsuarioId(1L)).thenReturn(List.of(testCategory));

            Budget budget = buildBudget(new BigDecimal("100000"));
            when(budgetRepository.findActiveBudgetForCategoryAndMonth(
                    eq(10L), anyInt(), anyInt()))
                    .thenReturn(Optional.of(budget));
            // Exceeded both months (month -1 and month -2)
            when(transactionRepository.sumGastosByCategoryAndPeriod(
                    eq(10L), eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new BigDecimal("150000"));

            // Balance loop — break-even so no other recommendations trigger
            when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                    eq(1L), anyString(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ZERO);
            // Escenario 4 — no categories without budget
            when(budgetRepository.existsActiveBudgetForCategory(
                    eq(10L), anyInt(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(true);

            List<RecommendationResponse> result = recommendationService.getRecommendations();

            assertTrue(result.stream().anyMatch(r -> "EXCESO_GASTO".equals(r.getTipo())));
            RecommendationResponse rec = result.stream()
                    .filter(r -> "EXCESO_GASTO".equals(r.getTipo()))
                    .findFirst().orElseThrow();
            assertTrue(rec.getMensaje().contains("Alimentación"));
        }

        @Test
        @DisplayName("Should NOT add EXCESO_GASTO when budget exceeded only 1 month")
        void shouldNotAddExcesoGastoWhenExceededOnly1Month() {
            LocalDate firstTx = LocalDate.now().minusDays(60);
            when(transactionRepository.findFechaFirstTransaction(1L)).thenReturn(firstTx);
            when(transactionRepository.findLastTransactionDate(1L)).thenReturn(LocalDate.now());
            when(categoryRepository.findByUsuarioId(1L)).thenReturn(List.of(testCategory));

            Budget budget = buildBudget(new BigDecimal("100000"));
            // First call (month -1): exceeded; second call (month -2): not exceeded
            when(budgetRepository.findActiveBudgetForCategoryAndMonth(
                    eq(10L), anyInt(), anyInt()))
                    .thenReturn(Optional.of(budget));
            when(transactionRepository.sumGastosByCategoryAndPeriod(
                    eq(10L), eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new BigDecimal("150000"), new BigDecimal("50000"));

            when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                    eq(1L), anyString(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ZERO);
            when(budgetRepository.existsActiveBudgetForCategory(
                    eq(10L), anyInt(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(true);

            List<RecommendationResponse> result = recommendationService.getRecommendations();

            assertTrue(result.stream().noneMatch(r -> "EXCESO_GASTO".equals(r.getTipo())));
        }

        @Test
        @DisplayName("Should NOT add EXCESO_GASTO when the category has no active budget")
        void shouldNotAddExcesoGastoWhenNoBudget() {
            LocalDate firstTx = LocalDate.now().minusDays(60);
            when(transactionRepository.findFechaFirstTransaction(1L)).thenReturn(firstTx);
            when(transactionRepository.findLastTransactionDate(1L)).thenReturn(LocalDate.now());
            when(categoryRepository.findByUsuarioId(1L)).thenReturn(List.of(testCategory));

            when(budgetRepository.findActiveBudgetForCategoryAndMonth(
                    eq(10L), anyInt(), anyInt()))
                    .thenReturn(Optional.empty());

            when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                    eq(1L), anyString(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.sumGastosByCategoryAndPeriod(
                    eq(10L), eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ZERO);
            when(budgetRepository.existsActiveBudgetForCategory(
                    eq(10L), anyInt(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(true);

            List<RecommendationResponse> result = recommendationService.getRecommendations();

            assertTrue(result.stream().noneMatch(r -> "EXCESO_GASTO".equals(r.getTipo())));
        }
    }

    // -------------------------------------------------------------------------
    // Escenario 2 — Balance positivo 3 meses consecutivos
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Escenario 2 — Balance positivo 3 meses consecutivos")
    class BalancePositivo {

        @Test
        @DisplayName("Should add AHORRO recommendation when income > expenses 3 months in a row")
        void shouldAddAhorroWhen3PositiveMonths() {
            LocalDate firstTx = LocalDate.now().minusDays(120);
            when(transactionRepository.findFechaFirstTransaction(1L)).thenReturn(firstTx);
            when(transactionRepository.findLastTransactionDate(1L)).thenReturn(LocalDate.now());
            when(categoryRepository.findByUsuarioId(1L)).thenReturn(Collections.emptyList());

            // All 3 months: income > expenses
            when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                    eq(1L), eq("INGRESO"), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new BigDecimal("3000000"));
            when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                    eq(1L), eq("GASTO"), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new BigDecimal("1000000"));

            List<RecommendationResponse> result = recommendationService.getRecommendations();

            assertTrue(result.stream().anyMatch(r -> "AHORRO".equals(r.getTipo())));
            RecommendationResponse rec = result.stream()
                    .filter(r -> "AHORRO".equals(r.getTipo()))
                    .findFirst().orElseThrow();
            assertTrue(rec.getMensaje().contains("ahorro") || rec.getMensaje().contains("excedente"));
        }

        @Test
        @DisplayName("Should NOT add AHORRO recommendation when only 2 months are positive")
        void shouldNotAddAhorroWhenOnly2PositiveMonths() {
            LocalDate firstTx = LocalDate.now().minusDays(120);
            when(transactionRepository.findFechaFirstTransaction(1L)).thenReturn(firstTx);
            when(transactionRepository.findLastTransactionDate(1L)).thenReturn(LocalDate.now());
            when(categoryRepository.findByUsuarioId(1L)).thenReturn(Collections.emptyList());

            // 2 months positive, 1 break-even
            when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                    eq(1L), eq("INGRESO"), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new BigDecimal("3000000"),
                            new BigDecimal("3000000"),
                            new BigDecimal("1000000"));  // third month break-even
            when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                    eq(1L), eq("GASTO"), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new BigDecimal("1000000"),
                            new BigDecimal("1000000"),
                            new BigDecimal("1000000"));

            List<RecommendationResponse> result = recommendationService.getRecommendations();

            assertTrue(result.stream().noneMatch(r -> "AHORRO".equals(r.getTipo())));
        }
    }

    // -------------------------------------------------------------------------
    // Escenario 3 — Balance negativo 2+ meses
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Escenario 3 — Balance negativo en 2+ meses")
    class BalanceNegativo {

        @Test
        @DisplayName("Should add BALANCE_NEGATIVO recommendation with top 3 categories")
        void shouldAddBalanceNegativoWith3TopCategories() {
            LocalDate firstTx = LocalDate.now().minusDays(120);
            when(transactionRepository.findFechaFirstTransaction(1L)).thenReturn(firstTx);
            when(transactionRepository.findLastTransactionDate(1L)).thenReturn(LocalDate.now());
            when(categoryRepository.findByUsuarioId(1L)).thenReturn(Collections.emptyList());

            // All 3 months negative (expenses > income)
            when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                    eq(1L), eq("INGRESO"), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new BigDecimal("500000"));
            when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                    eq(1L), eq("GASTO"), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new BigDecimal("1500000"));

            List<Object[]> top3 = List.of(
                    new Object[]{"Alimentación", new BigDecimal("600000")},
                    new Object[]{"Transporte", new BigDecimal("500000")},
                    new Object[]{"Entretenimiento", new BigDecimal("400000")}
            );
            when(transactionRepository.findTop3CategoriasByGasto(
                    eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(top3);

            List<RecommendationResponse> result = recommendationService.getRecommendations();

            assertTrue(result.stream().anyMatch(r -> "BALANCE_NEGATIVO".equals(r.getTipo())));
            RecommendationResponse rec = result.stream()
                    .filter(r -> "BALANCE_NEGATIVO".equals(r.getTipo()))
                    .findFirst().orElseThrow();
            assertNotNull(rec.getCategorias());
            assertEquals(3, rec.getCategorias().size());
            assertTrue(rec.getCategorias().contains("Alimentación"));
        }

        @Test
        @DisplayName("Should NOT add BALANCE_NEGATIVO when only 1 month is negative")
        void shouldNotAddBalanceNegativoWhenOnly1NegativeMonth() {
            LocalDate firstTx = LocalDate.now().minusDays(120);
            when(transactionRepository.findFechaFirstTransaction(1L)).thenReturn(firstTx);
            when(transactionRepository.findLastTransactionDate(1L)).thenReturn(LocalDate.now());
            when(categoryRepository.findByUsuarioId(1L)).thenReturn(Collections.emptyList());

            // Only 1 negative month
            when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                    eq(1L), eq("INGRESO"), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new BigDecimal("500000"),
                            new BigDecimal("2000000"),
                            new BigDecimal("2000000"));
            when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                    eq(1L), eq("GASTO"), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new BigDecimal("1500000"),
                            new BigDecimal("1000000"),
                            new BigDecimal("1000000"));

            List<RecommendationResponse> result = recommendationService.getRecommendations();

            assertTrue(result.stream().noneMatch(r -> "BALANCE_NEGATIVO".equals(r.getTipo())));
        }
    }

    // -------------------------------------------------------------------------
    // Escenario 4 — Categorías con gastos pero sin presupuesto
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Escenario 4 — Categorías con gastos sin presupuesto")
    class SinPresupuesto {

        @Test
        @DisplayName("Should add SIN_PRESUPUESTO recommendation when category has expenses but no budget")
        void shouldAddSinPresupuestoWhenCategoryHasExpensesButNoBudget() {
            LocalDate firstTx = LocalDate.now().minusDays(60);
            when(transactionRepository.findFechaFirstTransaction(1L)).thenReturn(firstTx);
            when(transactionRepository.findLastTransactionDate(1L)).thenReturn(LocalDate.now());
            when(categoryRepository.findByUsuarioId(1L)).thenReturn(List.of(testCategory));
            when(budgetRepository.findActiveBudgetForCategoryAndMonth(
                    eq(10L), anyInt(), anyInt()))
                    .thenReturn(Optional.empty());

            // No exceeded budgets (budget absent)
            when(transactionRepository.sumGastosByCategoryAndPeriod(
                    eq(10L), eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new BigDecimal("200000"));

            // Balance loop — break-even
            when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                    eq(1L), anyString(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ZERO);

            // Escenario 4: has expenses, no budget
            when(budgetRepository.existsActiveBudgetForCategory(
                    eq(10L), anyInt(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(false);

            List<RecommendationResponse> result = recommendationService.getRecommendations();

            assertTrue(result.stream().anyMatch(r -> "SIN_PRESUPUESTO".equals(r.getTipo())));
            RecommendationResponse rec = result.stream()
                    .filter(r -> "SIN_PRESUPUESTO".equals(r.getTipo()))
                    .findFirst().orElseThrow();
            assertNotNull(rec.getCategorias());
            assertTrue(rec.getCategorias().contains("Alimentación"));
        }

        @Test
        @DisplayName("Should NOT add SIN_PRESUPUESTO when category has no expenses this month")
        void shouldNotAddSinPresupuestoWhenNoExpenses() {
            LocalDate firstTx = LocalDate.now().minusDays(60);
            when(transactionRepository.findFechaFirstTransaction(1L)).thenReturn(firstTx);
            when(transactionRepository.findLastTransactionDate(1L)).thenReturn(LocalDate.now());
            when(categoryRepository.findByUsuarioId(1L)).thenReturn(List.of(testCategory));
            when(budgetRepository.findActiveBudgetForCategoryAndMonth(
                    eq(10L), anyInt(), anyInt()))
                    .thenReturn(Optional.empty());

            // No expenses this month
            when(transactionRepository.sumGastosByCategoryAndPeriod(
                    eq(10L), eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ZERO);

            when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                    eq(1L), anyString(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ZERO);

            List<RecommendationResponse> result = recommendationService.getRecommendations();

            assertTrue(result.stream().noneMatch(r -> "SIN_PRESUPUESTO".equals(r.getTipo())));
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------
    private Budget buildBudget(BigDecimal amount) {
        Budget b = new Budget();
        b.setId(1L);
        b.setCategory(testCategory);
        b.setAmount(amount);
        return b;
    }
}