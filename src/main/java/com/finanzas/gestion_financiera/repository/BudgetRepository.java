package com.finanzas.gestion_financiera.repository;

import com.finanzas.gestion_financiera.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByCategoryUsuarioId(Long usuarioId);

    Optional<Budget> findByIdAndCategoryUsuarioId(Long id, Long usuarioId);

    @Query("""
        SELECT COUNT(b) > 0 FROM Budget b
        WHERE b.category.id = :categoryId
        AND (b.startYear * 12 + b.startMonth) <= (:endYear * 12 + :endMonth)
        AND (b.startYear * 12 + b.startMonth + b.durationMonths) >= (:startYear * 12 + :startMonth)
    """)
    boolean existsActiveBudgetForCategory(
            @Param("categoryId") Long categoryId,
            @Param("startMonth") int startMonth,
            @Param("startYear") int startYear,
            @Param("endMonth") int endMonth,
            @Param("endYear") int endYear);

    @Query("""
        SELECT b FROM Budget b
        WHERE b.category.id = :categoryId
        AND (b.startYear * 12 + b.startMonth) <= (:year * 12 + :month)
        AND (b.startYear * 12 + b.startMonth + b.durationMonths) >= (:year * 12 + :month)
    """)
    Optional<Budget> findActiveBudgetForCategoryAndMonth(
            @Param("categoryId") Long categoryId,
            @Param("month") int month,
            @Param("year") int year);

    @Query("SELECT b FROM Budget b WHERE b.category.usuario.id = :userId " +
            "AND b.startYear = :year AND b.startMonth = :month")
    List<Budget> findAllActiveByUserAndMonth(
            @Param("userId") Long userId,
            @Param("year") Integer year,
            @Param("month") Integer month);
}