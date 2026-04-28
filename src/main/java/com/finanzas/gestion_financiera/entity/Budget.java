package com.finanzas.gestion_financiera.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    @Column(name = "presupuesto", nullable = false)
    private BigDecimal amount;

    @CreationTimestamp
    @Column(name = "fecha_inicio", nullable = false, updatable = false)
    private LocalDate startDate;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate endDate;
}