package com.finanzas.gestion_financiera.controller;

import com.finanzas.gestion_financiera.dto.BudgetComparisonResponse;
import com.finanzas.gestion_financiera.dto.BudgetRequest;
import com.finanzas.gestion_financiera.dto.BudgetResponse;
import com.finanzas.gestion_financiera.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/v1/presupuestos")
@RequiredArgsConstructor
@Tag(name = "Presupuestos", description = "Gestión de presupuestos y comparativa de gastos")
@SecurityRequirement(name = "Bearer Authentication")
public class BudgetController {

        private final BudgetService budgetService;

        @Operation(summary = "Crear presupuesto", description = "Crea un presupuesto para una categoría. No permite duplicados activos")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Presupuesto creado exitosamente"),
                        @ApiResponse(responseCode = "400", description = "Datos inválidos o ya existe un presupuesto activo"),
                        @ApiResponse(responseCode = "403", description = "No autorizado")
        })
        @PostMapping
        public ResponseEntity<BudgetResponse> create(
                        @Valid @RequestBody BudgetRequest request) {
                BudgetResponse response = budgetService.create(request);
                response.add(
                                linkTo(methodOn(BudgetController.class).list()).withRel("all-budgets"),
                                linkTo(methodOn(BudgetController.class).update(response.getId(), request))
                                                .withRel("update"),
                                linkTo(methodOn(BudgetController.class).delete(response.getId())).withRel("delete"));
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Listar presupuestos", description = "Retorna todos los presupuestos del usuario autenticado")
        @ApiResponse(responseCode = "200", description = "Lista de presupuestos")
        @GetMapping
        public ResponseEntity<List<BudgetResponse>> list() {
                List<BudgetResponse> budgets = budgetService.list();
                budgets.forEach(b -> b.add(
                                linkTo(methodOn(BudgetController.class).update(b.getId(), null)).withRel("update"),
                                linkTo(methodOn(BudgetController.class).delete(b.getId())).withRel("delete")));
                return ResponseEntity.ok(budgets);
        }

        @Operation(summary = "Actualizar presupuesto")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Presupuesto actualizado"),
                        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
                        @ApiResponse(responseCode = "404", description = "Presupuesto no encontrado")
        })
        @PutMapping("/{id}")
        public ResponseEntity<BudgetResponse> update(
                        @PathVariable Long id,
                        @Valid @RequestBody BudgetRequest request) {
                BudgetResponse response = budgetService.update(id, request);
                response.add(
                                linkTo(methodOn(BudgetController.class).list()).withRel("all-budgets"),
                                linkTo(methodOn(BudgetController.class).delete(response.getId())).withRel("delete"));
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Eliminar presupuesto")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Presupuesto eliminado"),
                        @ApiResponse(responseCode = "404", description = "Presupuesto no encontrado")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<Void> delete(@PathVariable Long id) {
                budgetService.delete(id);
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Comparativa de presupuestos", description = "Muestra cuánto se ha gastado vs el presupuesto asignado por categoría")
        @ApiResponse(responseCode = "200", description = "Comparativa generada exitosamente")
        @GetMapping("/comparativa")
        public ResponseEntity<CollectionModel<BudgetComparisonResponse>> comparativa() {
                List<BudgetComparisonResponse> lista = budgetService.comparativa();
                lista.forEach(b -> b.add(
                                linkTo(methodOn(BudgetController.class).comparativa()).withSelfRel()));
                CollectionModel<BudgetComparisonResponse> collection = CollectionModel.of(
                                lista,
                                linkTo(methodOn(BudgetController.class).comparativa()).withSelfRel(),
                                linkTo(methodOn(BudgetController.class).list()).withRel("presupuestos"));
                return ResponseEntity.ok(collection);
        }
}