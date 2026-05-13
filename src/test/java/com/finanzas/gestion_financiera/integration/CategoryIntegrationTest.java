package com.finanzas.gestion_financiera.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finanzas.gestion_financiera.controller.CategoryController;
import com.finanzas.gestion_financiera.dto.CategoryRequest;
import com.finanzas.gestion_financiera.dto.CategoryResponse;
import com.finanzas.gestion_financiera.entity.Category.TipoCategoria;
import com.finanzas.gestion_financiera.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Category Integration Tests for SonarQube Coverage")
class CategoryIntegrationTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController).build();
    }

    @Test // CF-01
    @DisplayName("POST /api/v1/categorias - Crear categoría exitosamente")
    void crearCategoria_Exito() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setNombre("Bonificación");
        request.setTipo(TipoCategoria.INGRESO);

        CategoryResponse response = new CategoryResponse(1L, "Bonificación", TipoCategoria.INGRESO);
        when(categoryService.crear(any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Bonificación"))
                .andExpect(jsonPath("$.tipo").value("INGRESO"));
    }

    @Test // CF-02
    @DisplayName("POST /api/v1/categorias - Error por nombre vacío")
    void crearCategoria_NombreVacio() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setNombre("");
        request.setTipo(TipoCategoria.GASTO);

        mockMvc.perform(post("/api/v1/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test // CF-03
    @DisplayName("POST /api/v1/categorias - Error por tipo ausente")
    void crearCategoria_TipoAusente() throws Exception {
        String json = """
                {"nombre": "Test"}
                """;

        mockMvc.perform(post("/api/v1/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test // CF-04
    @DisplayName("GET /api/v1/categorias - Listar categorías del usuario autenticado")
    void listarCategorias_Exito() throws Exception {
        List<CategoryResponse> categories = List.of(
                new CategoryResponse(1L, "Salario", TipoCategoria.INGRESO),
                new CategoryResponse(2L, "Alimentación", TipoCategoria.GASTO));
        when(categoryService.listar()).thenReturn(categories);

        mockMvc.perform(get("/api/v1/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Salario"))
                .andExpect(jsonPath("$[1].nombre").value("Alimentación"));
    }

    @Test // CF-05
    @DisplayName("GET /api/v1/categorias/{id} - Obtener categoría por ID")
    void obtenerCategoria_Exito() throws Exception {
        CategoryResponse response = new CategoryResponse(5L, "Transporte", TipoCategoria.GASTO);
        when(categoryService.obtener(5L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/categorias/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.nombre").value("Transporte"));
    }

    @Test // CF-06
    @DisplayName("PUT /api/v1/categorias/{id} - Actualizar categoría existente")
    void actualizarCategoria_Exito() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setNombre("Nombre Actualizado");
        request.setTipo(TipoCategoria.INGRESO);

        CategoryResponse response = new CategoryResponse(3L, "Nombre Actualizado", TipoCategoria.INGRESO);
        when(categoryService.actualizar(eq(3L), any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/categorias/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nombre Actualizado"));
    }

    @Test // CF-07
    @DisplayName("PUT /api/v1/categorias/{id} - Error por payload inválido")
    void actualizarCategoria_PayloadInvalido() throws Exception {
        String json = """
                {"nombre": "", "tipo": null}
                """;

        mockMvc.perform(put("/api/v1/categorias/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test // CF-08
    @DisplayName("DELETE /api/v1/categorias/{id} - Eliminar categoría exitosamente")
    void eliminarCategoria_Exito() throws Exception {
        doNothing().when(categoryService).eliminar(4L);

        mockMvc.perform(delete("/api/v1/categorias/4"))
                .andExpect(status().isNoContent());
        verify(categoryService).eliminar(4L);
    }
}