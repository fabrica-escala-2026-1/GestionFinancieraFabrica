package com.finanzas.gestion_financiera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class GastoPorCategoriaResponse {
    private String categoria;
    private BigDecimal totalGastado;
}
