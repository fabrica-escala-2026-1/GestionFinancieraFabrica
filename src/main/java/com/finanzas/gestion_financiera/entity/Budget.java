package com.finanzas.gestion_financiera.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "presupuestos")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "categoria_id", nullable = false)
    private Category category;

    @Column(name = "monto", nullable = false)
    private BigDecimal amount;

    // Mes y año en que inicia el presupuesto (se asigna automáticamente al crear)
    @Column(name = "mes_inicio", nullable = false)
    private Integer startMonth;

    @Column(name = "anio_inicio", nullable = false)
    private Integer startYear;

    // Cuántos meses adicionales aplica (0 = solo el mes actual, máximo 12)
    @Column(name = "duracion_meses", nullable = false)
    private Integer durationMonths;
}