package com.finanzas.gestion_financiera.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "categorias")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;


}