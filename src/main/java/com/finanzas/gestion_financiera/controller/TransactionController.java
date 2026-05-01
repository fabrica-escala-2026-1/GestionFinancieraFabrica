package com.finanzas.gestion_financiera.controller;

import com.finanzas.gestion_financiera.dto.ResumenGastosMensualesResponse;
import com.finanzas.gestion_financiera.dto.TransactionRequest;
import com.finanzas.gestion_financiera.dto.TransactionResponse;
import com.finanzas.gestion_financiera.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/v1/transacciones")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> crear(
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.crear(request);
        agregarLinks(response);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<CollectionModel<TransactionResponse>> listar() {
        List<TransactionResponse> lista = transactionService.listar();
        lista.forEach(this::agregarLinks);

        CollectionModel<TransactionResponse> collection = CollectionModel.of(
                lista,
                linkTo(methodOn(TransactionController.class).listar()).withSelfRel()
        );
        return ResponseEntity.ok(collection);
    }

    @GetMapping("/gastos-por-categoria")
    public ResponseEntity<ResumenGastosMensualesResponse> obtenerGastosPorCategoria(
            @RequestParam Integer anio,
            @RequestParam Integer mes) {

        return ResponseEntity.ok(
                transactionService.obtenerResumenGastosPorCategoria(anio, mes)
        );
    }

    private void agregarLinks(TransactionResponse response) {
        response.add(
                linkTo(methodOn(TransactionController.class).listar()).withRel("todas"),
                linkTo(methodOn(TransactionController.class).crear(null)).withRel("crear")
        );
    }
}