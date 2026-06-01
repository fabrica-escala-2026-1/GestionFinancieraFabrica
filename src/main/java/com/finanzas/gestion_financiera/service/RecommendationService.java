package com.finanzas.gestion_financiera.service;

import com.finanzas.gestion_financiera.dto.RecommendationResponse;
import com.finanzas.gestion_financiera.entity.Budget;
import com.finanzas.gestion_financiera.entity.Category;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.BudgetRepository;
import com.finanzas.gestion_financiera.repository.CategoryRepository;
import com.finanzas.gestion_financiera.repository.TransactionRepository;
import com.finanzas.gestion_financiera.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public List<RecommendationResponse> getRecommendations() {
        User user = getAuthenticatedUser();
        List<RecommendationResponse> recommendations = new ArrayList<>();

        // Escenario 5 — Sin datos suficientes (menos de 30 días desde primera transacción)
        LocalDate primeraTransaccion = transactionRepository.findFechaFirstTransaction(user.getId());

        if (primeraTransaccion == null) {
            recommendations.add(new RecommendationResponse(
                    "SIN_DATOS",
                    "Sigue registrando tus movimientos para recibir recomendaciones personalizadas",
                    null
            ));
            return recommendations;
        }

        LocalDate fechaReferencia = transactionRepository.findLastTransactionDate(user.getId());

        if (fechaReferencia == null) {
            fechaReferencia = LocalDate.now();
        }

        long diasDeUso = ChronoUnit.DAYS.between(primeraTransaccion, LocalDate.now());
        if (diasDeUso < 30) {
            recommendations.add(new RecommendationResponse(
                    "SIN_DATOS",
                    "Sigue registrando tus movimientos para recibir recomendaciones personalizadas",
                    null
            ));
            return recommendations;
        }

        // Escenario 1 — Exceso de gasto en una categoría por 2 meses consecutivos
        List<Category> categorias = categoryRepository.findByUsuarioId(user.getId());


        // Escenario 1 — Exceso de gasto 2 meses consecutivos
        for (Category category : categorias) {
            int mesesExcedidos = 0;
            for (int i = 1; i <= 2; i++) {
                LocalDate fecha = fechaReferencia.minusMonths(i);
                int mes = fecha.getMonthValue();
                int anio = fecha.getYear();
                LocalDate startDate = LocalDate.of(anio, mes, 1);
                LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

                Optional<Budget> budgetOpt = budgetRepository
                        .findActiveBudgetForCategoryAndMonth(category.getId(), mes, anio);

                if (budgetOpt.isPresent()) {
                    BigDecimal gastado = transactionRepository.sumGastosByCategoryAndPeriod(
                            category.getId(), user.getId(), startDate, endDate);
                    if (gastado.compareTo(budgetOpt.get().getAmount()) > 0) {
                        mesesExcedidos++;
                    }
                }
            }
            if (mesesExcedidos >= 2) {
                recommendations.add(new RecommendationResponse(
                        "EXCESO_GASTO",
                        "Has superado tu presupuesto de " + category.getNombre() +
                                " por 2 meses seguidos. Considera ajustar tu presupuesto o revisar tus gastos en esta categoría",
                        null
                ));
            }
        }

        // Escenarios 2 y 3 — Balance neto de los últimos 3 meses
        int mesesPositivos = 0;
        int mesesNegativos = 0;
        for (int i = 1; i <= 3; i++) {
            LocalDate startDate = LocalDate.now().minusMonths(i).withDayOfMonth(1);
            LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

            BigDecimal ingresos = transactionRepository.sumByTipoAndUsuarioAndPeriod(
                    user.getId(), "INGRESO", startDate, endDate);
            BigDecimal gastos = transactionRepository.sumByTipoAndUsuarioAndPeriod(
                    user.getId(), "GASTO", startDate, endDate);

            int comparacion = ingresos.compareTo(gastos);
            if (comparacion > 0) mesesPositivos++;
            else if (comparacion < 0) mesesNegativos++;
        }

        // Escenario 2 — Balance positivo 3 meses consecutivos
        if (mesesPositivos == 3) {
            recommendations.add(new RecommendationResponse(
                    "AHORRO",
                    "Tus ingresos han superado tus gastos en los últimos 3 meses. " +
                            "Considera destinar parte de ese excedente al ahorro",
                    null
            ));
        }

        // Escenario 3 — Balance negativo 2 meses consecutivos
        if (mesesNegativos >= 2) {
            LocalDate startDate = LocalDate.now().minusMonths(1).withDayOfMonth(1);
            LocalDate endDate = LocalDate.now();

            List<Object[]> top3 = transactionRepository
                    .findTop3CategoriasByGasto(user.getId(), startDate, endDate);

            List<String> top3Nombres = top3.stream()
                    .limit(3)
                    .map(row -> (String) row[0])
                    .collect(Collectors.toList());

            recommendations.add(new RecommendationResponse(
                    "BALANCE_NEGATIVO",
                    "Tus gastos han superado tus ingresos en los últimos meses. " +
                            "Te sugerimos revisar tus categorías de mayor gasto",
                    top3Nombres
            ));
        }

        // Escenario 4 — Categorías con gastos pero sin presupuesto
        List<String> sinPresupuesto = categorias.stream()
                .filter(c -> {
                    int mes = LocalDate.now().getMonthValue();
                    int anio = LocalDate.now().getYear();
                    BigDecimal gastado = transactionRepository.sumGastosByCategoryAndPeriod(
                            c.getId(), user.getId(),
                            LocalDate.now().withDayOfMonth(1),
                            LocalDate.now());
                    int mesActual = LocalDate.now().getMonthValue();
                    int anioActual = LocalDate.now().getYear();
                    boolean tienePres = budgetRepository
                            .existsActiveBudgetForCategory(c.getId(), mesActual, anioActual, mesActual, anioActual);
                    return gastado.compareTo(BigDecimal.ZERO) > 0 && !tienePres;
                })
                .map(Category::getNombre)
                .collect(Collectors.toList());

        if (!sinPresupuesto.isEmpty()) {
            recommendations.add(new RecommendationResponse(
                    "SIN_PRESUPUESTO",
                    "Tienes categorías con gastos pero sin presupuesto definido. " +
                            "Establece un presupuesto para controlar mejor tus finanzas",
                    sinPresupuesto
            ));
        }

        return recommendations;
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }


}