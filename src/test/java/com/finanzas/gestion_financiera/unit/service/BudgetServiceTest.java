package com.finanzas.gestion_financiera.unit.service;

import com.finanzas.gestion_financiera.dto.BudgetComparisonResponse;
import com.finanzas.gestion_financiera.dto.BudgetRequest;
import com.finanzas.gestion_financiera.dto.BudgetResponse;
import com.finanzas.gestion_financiera.entity.Budget;
import com.finanzas.gestion_financiera.entity.Category;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.BudgetRepository;
import com.finanzas.gestion_financiera.repository.CategoryRepository;
import com.finanzas.gestion_financiera.repository.TransactionRepository;
import com.finanzas.gestion_financiera.repository.UserRepository;
import com.finanzas.gestion_financiera.service.BudgetService;
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
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BudgetService - Unit Tests")
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BudgetService budgetService;

    private User testUser;
    private Category expenseCategory;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@email.com");

        expenseCategory = new Category();
        expenseCategory.setId(10L);
        expenseCategory.setNombre("Alimentación");
        expenseCategory.setUsuario(testUser);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("test@email.com")
                .password("{noop}")
                .authorities(List.of())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        lenient().when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.of(testUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("Should create budget successfully")
        void shouldCreateBudget() {
            BudgetRequest request = new BudgetRequest();
            request.setCategoryId(10L);
            request.setAmount(new BigDecimal("500000"));
            request.setDurationMonths(2);

            when(categoryRepository.findByIdAndUsuarioId(10L, 1L))
                    .thenReturn(Optional.of(expenseCategory));
            when(budgetRepository.existsActiveBudgetForCategory(
                    eq(10L), anyInt(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(false);
            when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
                Budget b = invocation.getArgument(0);
                b.setId(100L);
                return b;
            });

            BudgetResponse response = budgetService.create(request);

            YearMonth now = YearMonth.now();
            YearMonth end = now.plusMonths(2);

            assertNotNull(response);
            assertEquals(100L, response.getId());
            assertEquals("Alimentación", response.getCategoryName());
            assertEquals(new BigDecimal("500000"), response.getAmount());
            assertEquals(now.getMonthValue(), response.getStartMonth());
            assertEquals(now.getYear(), response.getStartYear());
            assertEquals(2, response.getDurationMonths());
            assertEquals(end.getMonthValue(), response.getEndMonth());
            assertEquals(end.getYear(), response.getEndYear());
            verify(budgetRepository).save(any(Budget.class));
        }

        @Test
        @DisplayName("Should throw exception when category does not belong to the user")
        void shouldThrowExceptionWhenCategoryDoesNotBelongToUser() {
            BudgetRequest request = new BudgetRequest();
            request.setCategoryId(99L);
            request.setAmount(new BigDecimal("100000"));
            request.setDurationMonths(1);

            when(categoryRepository.findByIdAndUsuarioId(99L, 1L))
                    .thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> budgetService.create(request));
            assertEquals("Categoría no válida", exception.getMessage());
            verify(budgetRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when an active budget already exists for the period")
        void shouldThrowExceptionWhenActiveBudgetAlreadyExists() {
            BudgetRequest request = new BudgetRequest();
            request.setCategoryId(10L);
            request.setAmount(new BigDecimal("200000"));
            request.setDurationMonths(3);

            when(categoryRepository.findByIdAndUsuarioId(10L, 1L))
                    .thenReturn(Optional.of(expenseCategory));
            when(budgetRepository.existsActiveBudgetForCategory(
                    eq(10L), anyInt(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(true);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> budgetService.create(request));
            assertEquals("Ya existe un presupuesto activo para esta categoría en ese período",
                    exception.getMessage());
            verify(budgetRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when authenticated user does not exist")
        void shouldThrowExceptionWhenUserDoesNotExist() {
            BudgetRequest request = new BudgetRequest();
            request.setCategoryId(10L);
            request.setAmount(new BigDecimal("100000"));
            request.setDurationMonths(1);

            when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> budgetService.create(request));
        }
    }

    @Nested
    @DisplayName("list()")
    class List_ {

        @Test
        @DisplayName("Should list budgets for the authenticated user")
        void shouldListBudgets() {
            Budget b1 = new Budget();
            b1.setId(1L);
            b1.setCategory(expenseCategory);
            b1.setAmount(new BigDecimal("300000"));
            b1.setStartMonth(5);
            b1.setStartYear(2026);
            b1.setDurationMonths(2);

            Category otherCategory = new Category();
            otherCategory.setId(11L);
            otherCategory.setNombre("Transporte");
            otherCategory.setUsuario(testUser);

            Budget b2 = new Budget();
            b2.setId(2L);
            b2.setCategory(otherCategory);
            b2.setAmount(new BigDecimal("150000"));
            b2.setStartMonth(5);
            b2.setStartYear(2026);
            b2.setDurationMonths(0);

            when(budgetRepository.findByCategoryUsuarioId(1L)).thenReturn(List.of(b1, b2));

            List<BudgetResponse> result = budgetService.list();

            assertEquals(2, result.size());
            assertEquals("Alimentación", result.get(0).getCategoryName());
            assertEquals(new BigDecimal("300000"), result.get(0).getAmount());
            assertEquals(2, result.get(0).getDurationMonths());
            assertEquals(7, result.get(0).getEndMonth()); // May + 2 = July
            assertEquals(2026, result.get(0).getEndYear());
            assertEquals("Transporte", result.get(1).getCategoryName());
            assertEquals(5, result.get(1).getEndMonth());
        }

        @Test
        @DisplayName("Should return an empty list when there are no budgets")
        void shouldReturnEmptyList() {
            when(budgetRepository.findByCategoryUsuarioId(1L)).thenReturn(List.of());

            List<BudgetResponse> result = budgetService.list();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("Should update budget amount and duration")
        void shouldUpdateBudget() {
            Budget existing = new Budget();
            existing.setId(5L);
            existing.setCategory(expenseCategory);
            existing.setAmount(new BigDecimal("100000"));
            existing.setStartMonth(5);
            existing.setStartYear(2026);
            existing.setDurationMonths(1);

            BudgetRequest request = new BudgetRequest();
            request.setCategoryId(10L);
            request.setAmount(new BigDecimal("250000"));
            request.setDurationMonths(4);

            when(budgetRepository.findByIdAndCategoryUsuarioId(5L, 1L))
                    .thenReturn(Optional.of(existing));
            when(categoryRepository.findByIdAndUsuarioId(10L, 1L))
                    .thenReturn(Optional.of(expenseCategory));
            when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArgument(0));

            BudgetResponse response = budgetService.update(5L, request);

            assertEquals(new BigDecimal("250000"), response.getAmount());
            assertEquals(4, response.getDurationMonths());
            assertEquals(9, response.getEndMonth()); // May + 4 = September
            assertEquals(2026, response.getEndYear());
            verify(budgetRepository).save(existing);
        }

        @Test
        @DisplayName("Should throw exception when budget does not exist")
        void shouldThrowExceptionWhenBudgetNotFound() {
            BudgetRequest request = new BudgetRequest();
            request.setCategoryId(10L);
            request.setAmount(new BigDecimal("100000"));
            request.setDurationMonths(1);

            when(budgetRepository.findByIdAndCategoryUsuarioId(99L, 1L))
                    .thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> budgetService.update(99L, request));
            assertEquals("Presupuesto no encontrado", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when category is invalid")
        void shouldThrowExceptionWhenCategoryIsInvalid() {
            Budget existing = new Budget();
            existing.setId(5L);
            existing.setCategory(expenseCategory);
            existing.setAmount(new BigDecimal("100000"));
            existing.setStartMonth(5);
            existing.setStartYear(2026);
            existing.setDurationMonths(1);

            BudgetRequest request = new BudgetRequest();
            request.setCategoryId(99L);
            request.setAmount(new BigDecimal("100000"));
            request.setDurationMonths(1);

            when(budgetRepository.findByIdAndCategoryUsuarioId(5L, 1L))
                    .thenReturn(Optional.of(existing));
            when(categoryRepository.findByIdAndUsuarioId(99L, 1L))
                    .thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> budgetService.update(5L, request));
            assertEquals("Categoría no válida", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("Should delete an existing budget for the user")
        void shouldDeleteBudget() {
            Budget budget = new Budget();
            budget.setId(7L);
            budget.setCategory(expenseCategory);
            budget.setAmount(new BigDecimal("100000"));
            budget.setStartMonth(5);
            budget.setStartYear(2026);
            budget.setDurationMonths(1);

            when(budgetRepository.findByIdAndCategoryUsuarioId(7L, 1L))
                    .thenReturn(Optional.of(budget));

            budgetService.delete(7L);

            verify(budgetRepository).delete(budget);
        }

        @Test
        @DisplayName("Should throw exception when deleting a nonexistent budget")
        void shouldThrowExceptionWhenDeletingNonexistent() {
            when(budgetRepository.findByIdAndCategoryUsuarioId(99L, 1L))
                    .thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> budgetService.delete(99L));
            assertEquals("Presupuesto no encontrado", exception.getMessage());
            verify(budgetRepository, never()).delete(any(Budget.class));
        }
    }

    @Nested
    @DisplayName("comparativa()")
    class Comparativa {

        @Test
        @DisplayName("Scenario 1 - Should show normal message when usage < 80%")
        void scenarioNormalUsageBelow80() {
            Budget budget = new Budget();
            budget.setId(1L);
            budget.setCategory(expenseCategory);
            budget.setAmount(new BigDecimal("100000"));
            budget.setStartMonth(YearMonth.now().getMonthValue());
            budget.setStartYear(YearMonth.now().getYear());
            budget.setDurationMonths(1);

            when(categoryRepository.findByUsuarioId(1L)).thenReturn(List.of(expenseCategory));
            when(budgetRepository.findActiveBudgetForCategoryAndMonth(
                    eq(10L), anyInt(), anyInt()))
                    .thenReturn(Optional.of(budget));
            when(transactionRepository.sumGastosByCategoryAndPeriod(
                    eq(10L), eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new BigDecimal("50000"));

            List<BudgetComparisonResponse> result = budgetService.comparativa();

            assertEquals(1, result.size());
            BudgetComparisonResponse comp = result.get(0);
            assertEquals("Alimentación", comp.getCategoriaNombre());
            assertEquals(new BigDecimal("100000"), comp.getPresupuesto());
            assertEquals(new BigDecimal("50000"), comp.getGastado());
            assertEquals(new BigDecimal("50000"), comp.getDisponible());
            assertEquals(50.0, comp.getPorcentaje());
            assertTrue(comp.getAlerta().contains("50.0%"));
            assertTrue(comp.getAlerta().contains("Alimentación"));
        }

        @Test
        @DisplayName("Scenario 2 - Should warn when usage >= 80%")
        void scenarioCloseToLimit() {
            Budget budget = new Budget();
            budget.setId(1L);
            budget.setCategory(expenseCategory);
            budget.setAmount(new BigDecimal("100000"));
            budget.setStartMonth(YearMonth.now().getMonthValue());
            budget.setStartYear(YearMonth.now().getYear());
            budget.setDurationMonths(1);

            when(categoryRepository.findByUsuarioId(1L)).thenReturn(List.of(expenseCategory));
            when(budgetRepository.findActiveBudgetForCategoryAndMonth(
                    eq(10L), anyInt(), anyInt()))
                    .thenReturn(Optional.of(budget));
            when(transactionRepository.sumGastosByCategoryAndPeriod(
                    eq(10L), eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new BigDecimal("85000"));

            List<BudgetComparisonResponse> result = budgetService.comparativa();

            BudgetComparisonResponse comp = result.get(0);
            assertEquals(85.0, comp.getPorcentaje());
            assertTrue(comp.getAlerta().contains("80%"));
            assertTrue(comp.getAlerta().contains("Alimentación"));
        }

        @Test
        @DisplayName("Scenario 3 - Should warn when budget has been exceeded")
        void scenarioExceeded() {
            Budget budget = new Budget();
            budget.setId(1L);
            budget.setCategory(expenseCategory);
            budget.setAmount(new BigDecimal("100000"));
            budget.setStartMonth(YearMonth.now().getMonthValue());
            budget.setStartYear(YearMonth.now().getYear());
            budget.setDurationMonths(1);

            when(categoryRepository.findByUsuarioId(1L)).thenReturn(List.of(expenseCategory));
            when(budgetRepository.findActiveBudgetForCategoryAndMonth(
                    eq(10L), anyInt(), anyInt()))
                    .thenReturn(Optional.of(budget));
            when(transactionRepository.sumGastosByCategoryAndPeriod(
                    eq(10L), eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new BigDecimal("130000"));

            List<BudgetComparisonResponse> result = budgetService.comparativa();

            BudgetComparisonResponse comp = result.get(0);
            assertEquals(130.0, comp.getPorcentaje());
            assertEquals(new BigDecimal("-30000"), comp.getDisponible());
            assertTrue(comp.getAlerta().contains("excedido"));
            assertTrue(comp.getAlerta().contains("30000"));
            assertTrue(comp.getAlerta().contains("COP"));
        }

        @Test
        @DisplayName("Scenario 4 - Should return comparison without budget when category has none")
        void scenarioNoBudget() {
            when(categoryRepository.findByUsuarioId(1L)).thenReturn(List.of(expenseCategory));
            when(budgetRepository.findActiveBudgetForCategoryAndMonth(
                    eq(10L), anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            when(transactionRepository.sumGastosByCategoryAndPeriod(
                    eq(10L), eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new BigDecimal("75000"));

            List<BudgetComparisonResponse> result = budgetService.comparativa();

            assertEquals(1, result.size());
            BudgetComparisonResponse comp = result.get(0);
            assertEquals("Alimentación", comp.getCategoriaNombre());
            assertNull(comp.getPresupuesto());
            assertEquals(new BigDecimal("75000"), comp.getGastado());
            assertNull(comp.getDisponible());
            assertNull(comp.getPorcentaje());
            assertNull(comp.getAlerta());
        }

        @Test
        @DisplayName("Should return an empty list when the user has no expense categories")
        void shouldReturnEmptyListWithoutExpenseCategories() {
            when(categoryRepository.findByUsuarioId(1L)).thenReturn(List.of());

            List<BudgetComparisonResponse> result = budgetService.comparativa();

            assertTrue(result.isEmpty());
        }
    }
}
