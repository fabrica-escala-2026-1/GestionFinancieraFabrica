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

    // Suma total de ingresos o gastos por mes
    @Query("""
    SELECT COALESCE(SUM(t.monto), 0)
    FROM Transaction t
    WHERE t.usuario.id = :userId
      AND t.tipo = :tipo
      AND t.fecha >= :startDate
      AND t.fecha <= :endDate
    """)
    BigDecimal sumByTipoAndUsuarioAndPeriod(
            @Param("userId") Long userId,
            @Param("tipo") String tipo,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );





    // Top 3 categorías con mayor gasto en un período
    @Query("SELECT t.categoria.nombre, SUM(t.monto) as total " +
            "FROM Transaction t " +
            "WHERE t.usuario.id = :userId " +
            "AND t.tipo = 'GASTO' " +
            "AND t.fecha >= :startDate " +
            "AND t.fecha <= :endDate " +
            "GROUP BY t.categoria.nombre " +
            "ORDER BY total DESC")
    List<Object[]> findTop3CategoriasByGasto(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Fecha de la primera transacción del usuario
    @Query("SELECT MIN(t.fecha) FROM Transaction t WHERE t.usuario.id = :userId")
    LocalDate findFechaFirstTransaction(@Param("userId") Long userId);

    //Encuentra la ultima transaccion del usuario
    @Query("""
    SELECT MAX(t.fecha)
    FROM Transaction t
    WHERE t.usuario.id = :userId
""")
    LocalDate findLastTransactionDate(@Param("userId") Long userId);

}

