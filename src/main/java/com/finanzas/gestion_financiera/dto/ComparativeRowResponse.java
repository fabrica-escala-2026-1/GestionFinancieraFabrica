package com.finanzas.gestion_financiera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ComparativeRowResponse {
    private String category;
    private BigDecimal initialMonthTotal;
    private BigDecimal finalMonthTotal;
    private BigDecimal differenceCOP;
    private Double differencePercentage;
}