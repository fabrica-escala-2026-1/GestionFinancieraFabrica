package com.finanzas.gestion_financiera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;
import java.util.List;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ComparativeReportResponse extends RepresentationModel<ComparativeReportResponse> {
    private Integer initialYear;
    private Integer initialMonth;
    private Integer finalYear;
    private Integer finalMonth;
    private List<ComparativeRowResponse> rows;
    private String message;
}