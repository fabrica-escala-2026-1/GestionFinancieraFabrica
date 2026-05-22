package com.finanzas.gestion_financiera.controller;

import com.finanzas.gestion_financiera.dto.BalanceResponse;
import com.finanzas.gestion_financiera.service.BalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/v1/balance")
@RequiredArgsConstructor
@Tag(name = "Balance", description = "Consulta del balance financiero global")
@SecurityRequirement(name = "Bearer Authentication")
public class BalanceController {

    private final BalanceService balanceService;

    @Operation(
            summary = "Consultar balance del mes actual",
            description = "Retorna ingresos, gastos y balance neto del mes actual"
    )
    @ApiResponse(responseCode = "200", description = "Balance mensual generado exitosamente")
    @GetMapping("/mes-actual")
    public ResponseEntity<BalanceResponse> obtenerBalanceMesActual() {
        BalanceResponse response = balanceService.obtenerBalanceMesActual();

        response.add(
                linkTo(methodOn(BalanceController.class).obtenerBalanceMesActual()).withSelfRel(),
                linkTo(methodOn(BalanceController.class).obtenerBalancePorPeriodo(null, null))
                        .withRel("balance-por-periodo")
        );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Consultar balance por período",
            description = "Retorna ingresos, gastos y balance neto entre dos fechas"
    )
    @ApiResponse(responseCode = "200", description = "Balance por período generado exitosamente")
    @GetMapping
    public ResponseEntity<BalanceResponse> obtenerBalancePorPeriodo(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin
    ) {
        BalanceResponse response = balanceService.obtenerBalancePorPeriodo(fechaInicio, fechaFin);

        response.add(
                linkTo(methodOn(BalanceController.class)
                        .obtenerBalancePorPeriodo(fechaInicio, fechaFin))
                        .withSelfRel(),
                linkTo(methodOn(BalanceController.class).obtenerBalanceMesActual())
                        .withRel("balance-mes-actual")
        );

        return ResponseEntity.ok(response);
    }
}