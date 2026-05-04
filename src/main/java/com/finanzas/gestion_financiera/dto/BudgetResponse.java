package com.finanzas.gestion_financiera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class BudgetResponse extends RepresentationModel<BudgetResponse> {

    private Long id;
    private String categoryName;
    private BigDecimal amount;
    private Integer startMonth;  // mes de inicio (1-12)
    private Integer startYear;   // año de inicio
    private Integer durationMonths; // duración en meses adicionales
    private Integer endMonth;    // mes de fin calculado
    private Integer endYear;     // año de fin calculado
}