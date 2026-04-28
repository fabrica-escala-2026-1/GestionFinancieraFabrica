package com.finanzas.gestion_financiera.repository;

import com.finanzas.gestion_financiera.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByCategoryUsuarioId(Long usuarioId);
    Optional<Budget> findByIdAndCategoryUsuarioId(Long id, Long usuarioId);
    boolean existsByCategoryIdAndEndDateGreaterThanEqual(Long categoryId, LocalDate today);
}