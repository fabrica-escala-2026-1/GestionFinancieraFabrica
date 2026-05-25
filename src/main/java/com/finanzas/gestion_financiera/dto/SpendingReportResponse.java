// ── SpendingReportResponse.java ──────────────────────────
package com.finanzas.gestion_financiera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SpendingReportResponse extends RepresentationModel<SpendingReportResponse> {
    private Integer year;
    private Integer month;
    private BigDecimal totalSpent;
    private List<CategoryDistributionResponse> distribution;
    private CategoryDistributionResponse highestSpendingCategory;
    private String message;
}