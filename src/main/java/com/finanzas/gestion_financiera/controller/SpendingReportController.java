package com.finanzas.gestion_financiera.controller;

import com.finanzas.gestion_financiera.dto.ComparativeReportResponse;
import com.finanzas.gestion_financiera.dto.SpendingReportResponse;
import com.finanzas.gestion_financiera.service.SpendingReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes de hábitos", description = "Reportes de distribución y comparativa de gastos")
@SecurityRequirement(name = "Bearer Authentication")
public class SpendingReportController {

    private final SpendingReportService spendingReportService;

    @Operation(
            summary = "Reporte del mes actual",
            description = "Retorna distribución de gastos por categoría y la categoría con mayor gasto del mes en curso"
    )
    @ApiResponse(responseCode = "200", description = "Reporte generado exitosamente")
    @GetMapping("/current-month")
    public ResponseEntity<SpendingReportResponse> getCurrentMonthReport() {
        SpendingReportResponse response = spendingReportService.getCurrentMonthReport();
        response.add(
                linkTo(methodOn(SpendingReportController.class)
                        .getCurrentMonthReport()).withSelfRel(),
                linkTo(methodOn(SpendingReportController.class)
                        .getComparativeReport(
                                YearMonth.now().getYear(), YearMonth.now().getMonthValue(),
                                YearMonth.now().getYear(), YearMonth.now().getMonthValue()))
                        .withRel("comparative")
        );
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Reporte comparativo entre dos meses",
            description = "Compara gastos por categoría entre dos meses con diferencia en COP y porcentual"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte comparativo generado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Mes o año inválido"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    @GetMapping("/comparative")
    public ResponseEntity<ComparativeReportResponse> getComparativeReport(
            @RequestParam Integer initialYear,
            @RequestParam Integer initialMonth,
            @RequestParam Integer finalYear,
            @RequestParam Integer finalMonth) {

        ComparativeReportResponse response = spendingReportService
                .getComparativeReport(initialYear, initialMonth, finalYear, finalMonth);
        response.add(
                linkTo(methodOn(SpendingReportController.class)
                        .getComparativeReport(initialYear, initialMonth, finalYear, finalMonth)).withSelfRel(),
                linkTo(methodOn(SpendingReportController.class)
                        .getCurrentMonthReport()).withRel("current-month-report")
        );
        return ResponseEntity.ok(response);
    }
}