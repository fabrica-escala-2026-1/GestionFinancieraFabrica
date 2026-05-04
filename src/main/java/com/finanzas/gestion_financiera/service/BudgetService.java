package com.finanzas.gestion_financiera.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public BudgetResponse create(BudgetRequest request) {
        User user = getAuthenticatedUser();

        Category category = categoryRepository
                .findByIdAndUsuarioId(request.getCategoryId(), user.getId())
                .orElseThrow(() -> new RuntimeException("Categoría no válida"));

        // Mes y año actuales — se asignan automáticamente
        YearMonth now = YearMonth.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        // Calcular mes/año de fin
        YearMonth endYearMonth = now.plusMonths(request.getDurationMonths());

        // Validar que no exista presupuesto activo para esa categoría en el período
        if (budgetRepository.existsActiveBudgetForCategory(
                request.getCategoryId(), currentMonth, currentYear,
                endYearMonth.getMonthValue(), endYearMonth.getYear())) {
            throw new RuntimeException(
                    "Ya existe un presupuesto activo para esta categoría en ese período");
        }

        Budget budget = new Budget();
        budget.setCategory(category);
        budget.setAmount(request.getAmount());
        budget.setStartMonth(currentMonth);
        budget.setStartYear(currentYear);
        budget.setDurationMonths(request.getDurationMonths());

        budgetRepository.save(budget);
        return toResponse(budget);
    }

    public List<BudgetResponse> list() {
        User user = getAuthenticatedUser();
        return budgetRepository.findByCategoryUsuarioId(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public BudgetResponse update(Long id, BudgetRequest request) {
        User user = getAuthenticatedUser();

        Budget budget = budgetRepository
                .findByIdAndCategoryUsuarioId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Presupuesto no encontrado"));

        Category category = categoryRepository
                .findByIdAndUsuarioId(request.getCategoryId(), user.getId())
                .orElseThrow(() -> new RuntimeException("Categoría no válida"));

        budget.setCategory(category);
        budget.setAmount(request.getAmount());
        budget.setDurationMonths(request.getDurationMonths());

        budgetRepository.save(budget);
        return toResponse(budget);
    }

    public void delete(Long id) {
        User user = getAuthenticatedUser();
        Budget budget = budgetRepository
                .findByIdAndCategoryUsuarioId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Presupuesto no encontrado"));
        budgetRepository.delete(budget);
    }

    public List<BudgetComparisonResponse> comparativa() {
        User user = getAuthenticatedUser();
        YearMonth now = YearMonth.now();

        List<Category> categorias = categoryRepository.findByUsuarioId(user.getId());

        return categorias.stream()
                .filter(c -> c.getTipo().name().equals("GASTO"))
                .map(category -> {

                    // Buscar presupuesto activo para el mes actual
                    Optional<Budget> budgetOpt = budgetRepository
                            .findActiveBudgetForCategoryAndMonth(
                                    category.getId(),
                                    now.getMonthValue(),
                                    now.getYear());

                    // Sumar gastos del mes actual
                    LocalDate startDate = now.atDay(1);
                    LocalDate endDate = now.atEndOfMonth();

                    BigDecimal gastado = transactionRepository
                            .sumGastosByCategoryAndPeriod(
                                    category.getId(), user.getId(),
                                    startDate, endDate);

                    if (budgetOpt.isEmpty()) {
                        return new BudgetComparisonResponse(
                                category.getNombre(), null, gastado,
                                null, null, null);
                    }

                    Budget budget = budgetOpt.get();
                    BigDecimal limite = budget.getAmount();
                    BigDecimal disponible = limite.subtract(gastado);

                    double porcentaje = gastado
                            .multiply(BigDecimal.valueOf(100))
                            .divide(limite, 2, RoundingMode.HALF_UP)
                            .doubleValue();

                    String alerta;
                    if (porcentaje > 100) {
                        BigDecimal excedido = gastado.subtract(limite);
                        alerta = "Has excedido el presupuesto de "
                                + category.getNombre() + " en " + excedido + " COP";
                    } else if (porcentaje >= 80) {
                        alerta = "Has superado el 80% del presupuesto de "
                                + category.getNombre();
                    } else {
                        alerta = "Llevas el " + porcentaje
                                + "% del presupuesto de "
                                + category.getNombre() + " utilizado";
                    }

                    return new BudgetComparisonResponse(
                            category.getNombre(), limite, gastado,
                            disponible, porcentaje, alerta);
                })
                .collect(Collectors.toList());
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private BudgetResponse toResponse(Budget budget) {
        YearMonth end = YearMonth.of(budget.getStartYear(), budget.getStartMonth())
                .plusMonths(budget.getDurationMonths());
        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().getNombre(),
                budget.getAmount(),
                budget.getStartMonth(),
                budget.getStartYear(),
                budget.getDurationMonths(),
                end.getMonthValue(),
                end.getYear()
        );
    }
}