package com.finanzas.gestion_financiera.service;

import com.finanzas.gestion_financiera.dto.BudgetRequest;
import com.finanzas.gestion_financiera.dto.BudgetResponse;
import com.finanzas.gestion_financiera.entity.Budget;
import com.finanzas.gestion_financiera.entity.Category;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.BudgetRepository;
import com.finanzas.gestion_financiera.repository.CategoryRepository;
import com.finanzas.gestion_financiera.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public BudgetResponse create(BudgetRequest request) {
        User user = getAuthenticatedUser();

        // Validar que la categoría pertenece al usuario autenticado
        Category category = categoryRepository
                .findByIdAndUsuarioId(request.getCategoryId(), user.getId())
                .orElseThrow(() -> new RuntimeException("Categoría no válida"));

        // Validar que no exista un presupuesto activo para esa categoría
        if (budgetRepository.existsByCategoryIdAndEndDateGreaterThanEqual(
                request.getCategoryId(), LocalDate.now())) {
            throw new RuntimeException(
                    "Ya existe un presupuesto activo para esta categoría");
        }

        Budget budget = new Budget();
        budget.setCategory(category);
        budget.setAmount(request.getAmount());
        budget.setEndDate(request.getEndDate());
        // startDate lo asigna @CreationTimestamp automáticamente

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

        // Validar presupuesto activo solo si cambia de categoría
        if (!budget.getCategory().getId().equals(request.getCategoryId()) &&
                budgetRepository.existsByCategoryIdAndEndDateGreaterThanEqual(
                        request.getCategoryId(), LocalDate.now())) {
            throw new RuntimeException(
                    "Ya existe un presupuesto activo para esta categoría");
        }

        budget.setCategory(category);
        budget.setAmount(request.getAmount());
        budget.setEndDate(request.getEndDate());

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

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private BudgetResponse toResponse(Budget budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().getNombre(),
                budget.getAmount(),
                budget.getStartDate(),
                budget.getEndDate()
        );
    }
}