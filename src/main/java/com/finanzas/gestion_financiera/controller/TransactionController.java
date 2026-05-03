package com.finanzas.gestion_financiera.controller;

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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}