package com.finanzas.gestion_financiera.unit.service;

import com.finanzas.gestion_financiera.dto.BalanceResponse;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.TransactionRepository;
import com.finanzas.gestion_financiera.repository.UserRepository;
import com.finanzas.gestion_financiera.service.BalanceService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("BalanceService - Unit Tests")
class BalanceServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BalanceService balanceService;

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

    @Nested
    @DisplayName("obtenerBalanceMesActual()")
    class ObtenerBalanceMesActual {

        @Test
        @DisplayName("Should return positive balance when income exceeds expenses")
        void shouldReturnPositiveBalance() {
            YearMonth now = YearMonth.now();
            LocalDate start = now.atDay(1);
            LocalDate end = now.atEndOfMonth();

            lenient().when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                            eq(1L), eq("INGRESO"), any(), any()))
                    .thenReturn(new BigDecimal("3000000"));
            lenient().when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                            eq(1L), eq("GASTO"), any(), any()))
                    .thenReturn(new BigDecimal("1000000"));

            BalanceResponse response = balanceService.obtenerBalanceMesActual();

            assertNotNull(response);
            assertEquals(new BigDecimal("3000000"), response.getIngresosTotales());
            assertEquals(new BigDecimal("1000000"), response.getGastosTotales());
            assertEquals(new BigDecimal("2000000"), response.getBalanceNeto());
            assertFalse(response.getBalanceNegativo());
            assertEquals("Tu balance financiero es positivo", response.getMensaje());
            assertEquals(start, response.getFechaInicio());
            assertEquals(end, response.getFechaFin());
        }

        @Test
        @DisplayName("Should return negative balance when expenses exceed income")
        void shouldReturnNegativeBalance() {
            lenient().when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                            eq(1L), eq("INGRESO"), any(), any()))
                    .thenReturn(new BigDecimal("500000"));
            lenient().when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                            eq(1L), eq("GASTO"), any(), any()))
                    .thenReturn(new BigDecimal("800000"));

            BalanceResponse response = balanceService.obtenerBalanceMesActual();

            assertTrue(response.getBalanceNegativo());
            assertEquals(new BigDecimal("-300000"), response.getBalanceNeto());
            assertTrue(response.getMensaje().contains("negativo"));
        }

        @Test
        @DisplayName("Should return zero balance when income equals expenses")
        void shouldReturnZeroBalance() {
            lenient().when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                            eq(1L), eq("INGRESO"), any(), any()))
                    .thenReturn(new BigDecimal("1000000"));
            lenient().when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                            eq(1L), eq("GASTO"), any(), any()))
                    .thenReturn(new BigDecimal("1000000"));

            BalanceResponse response = balanceService.obtenerBalanceMesActual();

            assertEquals(BigDecimal.ZERO, response.getBalanceNeto());
            assertFalse(response.getBalanceNegativo());
        }
    }

    @Nested
    @DisplayName("obtenerBalancePorPeriodo()")
    class ObtenerBalancePorPeriodo {

        @Test
        @DisplayName("Should return correct balance for a specific period")
        void shouldReturnBalanceForPeriod() {
            LocalDate start = LocalDate.of(2026, 1, 1);
            LocalDate end = LocalDate.of(2026, 1, 31);

            lenient().when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                            eq(1L), eq("INGRESO"), eq(start), eq(end)))
                    .thenReturn(new BigDecimal("5000000"));
            lenient().when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                            eq(1L), eq("GASTO"), eq(start), eq(end)))
                    .thenReturn(new BigDecimal("2000000"));

            BalanceResponse response = balanceService.obtenerBalancePorPeriodo(start, end);

            assertNotNull(response);
            assertEquals(start, response.getFechaInicio());
            assertEquals(end, response.getFechaFin());
            assertEquals(new BigDecimal("5000000"), response.getIngresosTotales());
            assertEquals(new BigDecimal("2000000"), response.getGastosTotales());
            assertEquals(new BigDecimal("3000000"), response.getBalanceNeto());
            assertFalse(response.getBalanceNegativo());
        }

        @Test
        @DisplayName("Should throw exception when fechaInicio is null")
        void shouldThrowExceptionWhenFechaInicioIsNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> balanceService.obtenerBalancePorPeriodo(null,
                            LocalDate.of(2026, 1, 31)));
            assertEquals("Las fechas de inicio y fin son obligatorias", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when fechaFin is null")
        void shouldThrowExceptionWhenFechaFinIsNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> balanceService.obtenerBalancePorPeriodo(
                            LocalDate.of(2026, 1, 1), null));
            assertEquals("Las fechas de inicio y fin son obligatorias", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when fechaInicio is after fechaFin")
        void shouldThrowExceptionWhenStartIsAfterEnd() {
            LocalDate start = LocalDate.of(2026, 3, 1);
            LocalDate end = LocalDate.of(2026, 1, 1);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> balanceService.obtenerBalancePorPeriodo(start, end));
            assertEquals("La fecha de inicio no puede ser posterior a la fecha final",
                    ex.getMessage());
        }

        @Test
        @DisplayName("Should work correctly when fechaInicio equals fechaFin")
        void shouldWorkWhenStartEqualsFechaFin() {
            LocalDate date = LocalDate.of(2026, 5, 15);

            lenient().when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                            eq(1L), eq("INGRESO"), eq(date), eq(date)))
                    .thenReturn(new BigDecimal("200000"));
            lenient().when(transactionRepository.sumByTipoAndUsuarioAndPeriod(
                            eq(1L), eq("GASTO"), eq(date), eq(date)))
                    .thenReturn(BigDecimal.ZERO);

            BalanceResponse response = balanceService.obtenerBalancePorPeriodo(date, date);

            assertNotNull(response);
            assertEquals(new BigDecimal("200000"), response.getBalanceNeto());
            assertFalse(response.getBalanceNegativo());
        }
    }
}