package com.finanzas.gestion_financiera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class BudgetResponse extends RepresentationModel<BudgetResponse> {

    private Long id;
    private String categoryName;
    private BigDecimal amount;
    private LocalDate startDate;
    private LocalDate endDate;
}