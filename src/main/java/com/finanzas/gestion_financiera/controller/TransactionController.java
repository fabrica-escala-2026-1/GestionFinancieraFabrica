package com.finanzas.gestion_financiera.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finanzas.gestion_financiera.dto.ResumenGastosMensualesResponse;
import com.finanzas.gestion_financiera.dto.TransactionRequest;
import com.finanzas.gestion_financiera.dto.TransactionResponse;
import com.finanzas.gestion_financiera.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/transacciones")
@RequiredArgsConstructor
@Tag(name = "Transacciones", description = "Registro y consulta de ingresos y gastos")
@SecurityRequirement(name = "Bearer Authentication")
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(summary = "Crear transacción",
            description = "Registra un nuevo ingreso o gasto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transacción registrada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o categoría no válida"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    @PostMapping
    public ResponseEntity<TransactionResponse> crear(
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.crear(request));
    }

    @Operation(summary = "Listar transacciones",
            description = "Retorna todas las transacciones del usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Lista de transacciones")
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> listar() {
        return ResponseEntity.ok(transactionService.listar());
    }

    @Operation(summary = "Obtener resumen de gastos",
            description = "Retorna el total de gastos agrupados por categoría para un mes y año específicos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen generado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Mes o datos inválidos")
    })
    @GetMapping("/resumen/{anio}/{mes}")
    public ResponseEntity<ResumenGastosMensualesResponse> obtenerResumen(
            @PathVariable Integer anio,
            @PathVariable Integer mes) {
        return ResponseEntity.ok(transactionService.obtenerResumenGastosPorCategoria(anio, mes));
    }
}