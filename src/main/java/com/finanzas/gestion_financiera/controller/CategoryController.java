package com.finanzas.gestion_financiera.controller;

import com.finanzas.gestion_financiera.dto.CategoryRequest;
import com.finanzas.gestion_financiera.dto.CategoryResponse;
import com.finanzas.gestion_financiera.service.CategoryService;
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
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
@Tag(name = "Categorías", description = "Gestión de categorías de ingresos y gastos")
@SecurityRequirement(name = "Bearer Authentication")
public class CategoryController {

    private final CategoryService categoryService;

    // Múltiples responses → se mantiene @ApiResponses
    @Operation(summary = "Crear categoría")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    @PostMapping
    public ResponseEntity<CategoryResponse> crear(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.crear(request));
    }

    // Un solo response → sin @ApiResponses
    @Operation(summary = "Listar categorías")
    @ApiResponse(responseCode = "200", description = "Lista de categorías del usuario")
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> listar() {
        return ResponseEntity.ok(categoryService.listar());
    }

    // Múltiples responses → se mantiene @ApiResponses
    @Operation(summary = "Obtener categoría por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría encontrada"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.obtener(id));
    }

    // Múltiples responses → se mantiene @ApiResponses
    @Operation(summary = "Actualizar categoría")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría actualizada"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.actualizar(id, request));
    }

    // Múltiples responses → se mantiene @ApiResponses
    @Operation(summary = "Eliminar categoría")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoría eliminada"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        categoryService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}