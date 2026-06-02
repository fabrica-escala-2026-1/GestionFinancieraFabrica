package com.finanzas.gestion_financiera.unit.service;

import com.finanzas.gestion_financiera.dto.CategoryDistributionResponse;
import com.finanzas.gestion_financiera.dto.ComparativeReportResponse;
import com.finanzas.gestion_financiera.dto.ComparativeRowResponse;
import com.finanzas.gestion_financiera.dto.GastoPorCategoriaResponse;
import com.finanzas.gestion_financiera.dto.SpendingReportResponse;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.TransactionRepository;
import com.finanzas.gestion_financiera.repository.UserRepository;
import com.finanzas.gestion_financiera.service.SpendingReportService;
import jakarta.persistence.EntityNotFoundException;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpendingReportService - Unit Tests")
class SpendingReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SpendingReportService spendingReportService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@email.com");

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
    // getCurrentMonthReport()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getCurrentMonthReport()")
    class GetCurrentMonthReport {

        @Test
        @DisplayName("Scenario 1 — Should return distribution and highest category for current month")
        void shouldReturnDistributionForCurrentMonth() {
            YearMonth now = YearMonth.now();
            List<GastoPorCategoriaResponse> expenses = List.of(
                    new GastoPorCategoriaResponse("Alimentación", new BigDecimal("600000")),
                    new GastoPorCategoriaResponse("Transporte", new BigDecimal("400000"))
            );
            when(transactionRepository.obtenerGastosPorCategoriaDelMes(
                    eq(1L), eq(now.atDay(1)), eq(now.atEndOfMonth())))
                    .thenReturn(expenses);

            SpendingReportResponse response = spendingReportService.getCurrentMonthReport();

            assertNotNull(response);
            assertEquals(now.getYear(), response.getYear());
            assertEquals(now.getMonthValue(), response.getMonth());
            assertEquals(new BigDecimal("1000000"), response.getTotalSpent());
            assertEquals(2, response.getDistribution().size());
            assertNull(response.getMessage());

            // highestSpendingCategory must be the first element returned by the repo (index 0)
            CategoryDistributionResponse highest = response.getHighestSpendingCategory();
            assertEquals("Alimentación", highest.getCategory());
            assertEquals(new BigDecimal("600000"), highest.getTotalSpent());
            assertEquals(60.0, highest.getPercentage());
        }

        @Test
        @DisplayName("Scenario 3 — Should calculate percentage correctly per category")
        void shouldCalculatePercentageCorrectly() {
            YearMonth now = YearMonth.now();
            List<GastoPorCategoriaResponse> expenses = List.of(
                    new GastoPorCategoriaResponse("Salud", new BigDecimal("250000")),
                    new GastoPorCategoriaResponse("Entretenimiento", new BigDecimal("750000"))
            );
            when(transactionRepository.obtenerGastosPorCategoriaDelMes(
                    eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(expenses);

            SpendingReportResponse response = spendingReportService.getCurrentMonthReport();

            List<CategoryDistributionResponse> dist = response.getDistribution();
            assertEquals(25.0, dist.get(0).getPercentage());
            assertEquals(75.0, dist.get(1).getPercentage());
        }

        @Test
        @DisplayName("Scenario 4 — Should return message when there are no expenses in current month")
        void shouldReturnMessageWhenNoExpenses() {
            YearMonth now = YearMonth.now();
            when(transactionRepository.obtenerGastosPorCategoriaDelMes(
                    eq(1L), eq(now.atDay(1)), eq(now.atEndOfMonth())))
                    .thenReturn(Collections.emptyList());

            SpendingReportResponse response = spendingReportService.getCurrentMonthReport();

            assertNotNull(response);
            assertEquals(BigDecimal.ZERO, response.getTotalSpent());
            assertTrue(response.getDistribution().isEmpty());
            assertNull(response.getHighestSpendingCategory());
            assertEquals(
                    "No hay datos suficientes para generar un reporte en este período",
                    response.getMessage());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when authenticated user does not exist")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> spendingReportService.getCurrentMonthReport());
        }
    }

    // -------------------------------------------------------------------------
    // getComparativeReport()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getComparativeReport()")
    class GetComparativeReport {

        @Test
        @DisplayName("Scenario 2 — Should return comparative rows with difference and percentage")
        void shouldReturnComparativeRows() {
            List<GastoPorCategoriaResponse> initial = List.of(
                    new GastoPorCategoriaResponse("Alimentación", new BigDecimal("500000")),
                    new GastoPorCategoriaResponse("Transporte", new BigDecimal("200000"))
            );
            List<GastoPorCategoriaResponse> finalMonth = List.of(
                    new GastoPorCategoriaResponse("Alimentación", new BigDecimal("600000")),
                    new GastoPorCategoriaResponse("Transporte", new BigDecimal("150000"))
            );

            when(transactionRepository.obtenerGastosPorCategoriaDelMes(
                    eq(1L), eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 1, 31))))
                    .thenReturn(initial);
            when(transactionRepository.obtenerGastosPorCategoriaDelMes(
                    eq(1L), eq(LocalDate.of(2026, 4, 1)), eq(LocalDate.of(2026, 4, 30))))
                    .thenReturn(finalMonth);

            ComparativeReportResponse response =
                    spendingReportService.getComparativeReport(2026, 1, 2026, 4);

            assertNotNull(response);
            assertEquals(2026, response.getInitialYear());
            assertEquals(1, response.getInitialMonth());
            assertEquals(2026, response.getFinalYear());
            assertEquals(4, response.getFinalMonth());
            assertNull(response.getMessage());
            assertEquals(2, response.getRows().size());

            ComparativeRowResponse alimentacion = response.getRows().stream()
                    .filter(r -> r.getCategory().equals("Alimentación"))
                    .findFirst().orElseThrow();
            assertEquals(new BigDecimal("500000"), alimentacion.getInitialMonthTotal());
            assertEquals(new BigDecimal("600000"), alimentacion.getFinalMonthTotal());
            assertEquals(new BigDecimal("100000"), alimentacion.getDifferenceCOP());
            assertEquals(20.0, alimentacion.getDifferencePercentage());

            ComparativeRowResponse transporte = response.getRows().stream()
                    .filter(r -> r.getCategory().equals("Transporte"))
                    .findFirst().orElseThrow();
            assertEquals(new BigDecimal("-50000"), transporte.getDifferenceCOP());
            assertEquals(-25.0, transporte.getDifferencePercentage());
        }

        @Test
        @DisplayName("Should include categories present only in the final month")
        void shouldIncludeCategoriesOnlyInFinalMonth() {
            List<GastoPorCategoriaResponse> initial = List.of(
                    new GastoPorCategoriaResponse("Alimentación", new BigDecimal("300000"))
            );
            List<GastoPorCategoriaResponse> finalMonth = List.of(
                    new GastoPorCategoriaResponse("Alimentación", new BigDecimal("400000")),
                    new GastoPorCategoriaResponse("Salud", new BigDecimal("100000"))
            );

            when(transactionRepository.obtenerGastosPorCategoriaDelMes(
                    eq(1L), eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 1, 31))))
                    .thenReturn(initial);
            when(transactionRepository.obtenerGastosPorCategoriaDelMes(
                    eq(1L), eq(LocalDate.of(2026, 2, 1)), eq(LocalDate.of(2026, 2, 28))))
                    .thenReturn(finalMonth);

            ComparativeReportResponse response =
                    spendingReportService.getComparativeReport(2026, 1, 2026, 2);

            assertEquals(2, response.getRows().size());

            ComparativeRowResponse salud = response.getRows().stream()
                    .filter(r -> r.getCategory().equals("Salud"))
                    .findFirst().orElseThrow();
            assertEquals(BigDecimal.ZERO, salud.getInitialMonthTotal());
            assertEquals(new BigDecimal("100000"), salud.getFinalMonthTotal());
            // differencePercentage must be null when initialMonthTotal is zero
            assertNull(salud.getDifferencePercentage());
        }

        @Test
        @DisplayName("Should include categories present only in the initial month")
        void shouldIncludeCategoriesOnlyInInitialMonth() {
            List<GastoPorCategoriaResponse> initial = List.of(
                    new GastoPorCategoriaResponse("Entretenimiento", new BigDecimal("200000"))
            );

            when(transactionRepository.obtenerGastosPorCategoriaDelMes(
                    eq(1L), eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 1, 31))))
                    .thenReturn(initial);
            when(transactionRepository.obtenerGastosPorCategoriaDelMes(
                    eq(1L), eq(LocalDate.of(2026, 2, 1)), eq(LocalDate.of(2026, 2, 28))))
                    .thenReturn(Collections.emptyList());

            ComparativeReportResponse response =
                    spendingReportService.getComparativeReport(2026, 1, 2026, 2);

            assertEquals(1, response.getRows().size());
            ComparativeRowResponse row = response.getRows().get(0);
            assertEquals("Entretenimiento", row.getCategory());
            assertEquals(BigDecimal.ZERO, row.getFinalMonthTotal());
            assertEquals(new BigDecimal("-200000"), row.getDifferenceCOP());
        }

        @Test
        @DisplayName("Scenario 4 — Should return message when both months have no data")
        void shouldReturnMessageWhenBothMonthsEmpty() {
            when(transactionRepository.obtenerGastosPorCategoriaDelMes(
                    eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());

            ComparativeReportResponse response =
                    spendingReportService.getComparativeReport(2026, 1, 2026, 2);

            assertNotNull(response);
            assertTrue(response.getRows().isEmpty());
            assertEquals(
                    "No hay datos suficientes para generar un reporte en este período",
                    response.getMessage());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when initialMonth is invalid (0)")
        void shouldThrowExceptionForInvalidInitialMonth() {
            assertThrows(IllegalArgumentException.class,
                    () -> spendingReportService.getComparativeReport(2026, 0, 2026, 3));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when finalMonth is invalid (13)")
        void shouldThrowExceptionForInvalidFinalMonth() {
            assertThrows(IllegalArgumentException.class,
                    () -> spendingReportService.getComparativeReport(2026, 1, 2026, 13));
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when authenticated user does not exist")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> spendingReportService.getComparativeReport(2026, 1, 2026, 2));
        }
    }
}