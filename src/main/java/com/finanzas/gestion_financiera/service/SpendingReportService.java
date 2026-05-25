package com.finanzas.gestion_financiera.service;

import com.finanzas.gestion_financiera.dto.*;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.TransactionRepository;
import com.finanzas.gestion_financiera.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpendingReportService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    // Scenarios 1, 3 y 4 — mes actual automático
    public SpendingReportResponse getCurrentMonthReport() {
        YearMonth now = YearMonth.now();
        return buildMonthlyReport(now.getYear(), now.getMonthValue());
    }

    // Scenario 2 — comparativo entre dos meses
    public ComparativeReportResponse getComparativeReport(
            Integer initialYear, Integer initialMonth,
            Integer finalYear, Integer finalMonth) {

        validateMonth(initialMonth);
        validateMonth(finalMonth);

        User user = getAuthenticatedUser();

        YearMonth ymInitial = YearMonth.of(initialYear, initialMonth);
        YearMonth ymFinal = YearMonth.of(finalYear, finalMonth);

        List<GastoPorCategoriaResponse> initialExpenses =
                transactionRepository.obtenerGastosPorCategoriaDelMes(
                        user.getId(), ymInitial.atDay(1), ymInitial.atEndOfMonth());

        List<GastoPorCategoriaResponse> finalExpenses =
                transactionRepository.obtenerGastosPorCategoriaDelMes(
                        user.getId(), ymFinal.atDay(1), ymFinal.atEndOfMonth());

        // Scenario 4
        if (initialExpenses.isEmpty() && finalExpenses.isEmpty()) {
            return new ComparativeReportResponse(
                    initialYear, initialMonth, finalYear, finalMonth,
                    Collections.emptyList(),
                    "No hay datos suficientes para generar un reporte en este período");
        }

        // Unir todas las categorías de ambos meses — toList() es inmutable
        // así que usamos ArrayList para poder agregar elementos
        List<String> allCategories = new java.util.ArrayList<>(
                initialExpenses.stream()
                        .map(GastoPorCategoriaResponse::getCategoria)
                        .toList()
        );

        finalExpenses.stream()
                .map(GastoPorCategoriaResponse::getCategoria)
                .filter(c -> !allCategories.contains(c))
                .forEach(allCategories::add);

        List<ComparativeRowResponse> rows = allCategories.stream()
                .map(cat -> {
                    BigDecimal initialAmount = initialExpenses.stream()
                            .filter(g -> g.getCategoria().equals(cat))
                            .map(GastoPorCategoriaResponse::getTotalGastado)
                            .findFirst().orElse(BigDecimal.ZERO);

                    BigDecimal finalAmount = finalExpenses.stream()
                            .filter(g -> g.getCategoria().equals(cat))
                            .map(GastoPorCategoriaResponse::getTotalGastado)
                            .findFirst().orElse(BigDecimal.ZERO);

                    BigDecimal differenceCOP = finalAmount.subtract(initialAmount);

                    Double differencePercentage = initialAmount.compareTo(BigDecimal.ZERO) == 0
                            ? null
                            : differenceCOP
                              .multiply(BigDecimal.valueOf(100))
                              .divide(initialAmount, 2, RoundingMode.HALF_UP)
                              .doubleValue();

                    return new ComparativeRowResponse(
                            cat, initialAmount, finalAmount,
                            differenceCOP, differencePercentage);
                })
                .toList();

        return new ComparativeReportResponse(
                initialYear, initialMonth, finalYear, finalMonth, rows, null);
    }

    // Método privado reutilizable para construir el reporte mensual
    private SpendingReportResponse buildMonthlyReport(Integer year, Integer month) {
        User user = getAuthenticatedUser();
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<GastoPorCategoriaResponse> expenses =
                transactionRepository.obtenerGastosPorCategoriaDelMes(
                        user.getId(), startDate, endDate);

        if (expenses.isEmpty()) {
            return new SpendingReportResponse(
                    year, month, BigDecimal.ZERO,
                    Collections.emptyList(), null,
                    "No hay datos suficientes para generar un reporte en este período");
        }

        BigDecimal total = expenses.stream()
                .map(GastoPorCategoriaResponse::getTotalGastado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryDistributionResponse> distribution = expenses.stream()
                .map(g -> {
                    double percentage = g.getTotalGastado()
                            .multiply(BigDecimal.valueOf(100))
                            .divide(total, 2, RoundingMode.HALF_UP)
                            .doubleValue();
                    return new CategoryDistributionResponse(
                            g.getCategoria(), g.getTotalGastado(), percentage);
                })
                .collect(Collectors.toList());

        CategoryDistributionResponse highest = distribution.get(0);

        return new SpendingReportResponse(
                year, month, total, distribution, highest, null);
    }

    private void validateMonth(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("El mes debe estar entre 1 y 12");
        }
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
    }
}