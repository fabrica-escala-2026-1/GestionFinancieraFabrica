package com.finanzas.gestion_financiera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CategoryDistributionResponse {
    private String category;
    private BigDecimal totalSpent;
    private Double percentage;
}