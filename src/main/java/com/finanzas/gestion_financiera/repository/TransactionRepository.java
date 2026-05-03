package com.finanzas.gestion_financiera.repository;
import com.finanzas.gestion_financiera.dto.GastoPorCategoriaResponse;
import com.finanzas.gestion_financiera.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUsuarioId(Long usuarioId);

    @Query("SELECT COALESCE(SUM(t.monto), 0) FROM Transaction t " +
            "WHERE t.categoria.id = :categoryId " +
            "AND t.usuario.id = :userId " +
            "AND t.fecha >= :startDate " +
            "AND t.fecha <= :endDate " +
            "AND t.tipo = 'GASTO'")
    BigDecimal sumGastosByCategoryAndPeriod(
            @Param("categoryId") Long categoryId,
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT new com.finanzas.gestion_financiera.dto.GastoPorCategoriaResponse(
            t.categoria.nombre,
            SUM(t.monto)
        )
        FROM Transaction t
        WHERE t.usuario.id = :userId
          AND t.tipo = 'GASTO'
          AND t.fecha >= :startDate
          AND t.fecha <= :endDate
        GROUP BY t.categoria.nombre
        ORDER BY SUM(t.monto) DESC
    """)
    List<GastoPorCategoriaResponse> obtenerGastosPorCategoriaDelMes(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
