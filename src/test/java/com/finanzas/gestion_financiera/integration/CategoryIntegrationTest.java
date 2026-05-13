package com.finanzas.gestion_financiera.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finanzas.gestion_financiera.controller.CategoryController;
import com.finanzas.gestion_financiera.dto.CategoryRequest;
import com.finanzas.gestion_financiera.dto.CategoryResponse;
import com.finanzas.gestion_financiera.entity.Category.TipoCategoria;
import com.finanzas.gestion_financiera.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("Category Feature - API /api/v1/categorias")
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

    @Nested
    @DisplayName("POST /api/v1/categorias")
    class CreateEndpoint {

        @Test
        @DisplayName("Should create category and return 200")
        void shouldCreateCategorySuccessfully() throws Exception {
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

        @Test
        @DisplayName("Should return 400 when name is empty")
        void shouldReturn400WhenNameIsEmpty() throws Exception {
            CategoryRequest request = new CategoryRequest();
            request.setNombre("");
            request.setTipo(TipoCategoria.GASTO);

            mockMvc.perform(post("/api/v1/categorias")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when type is missing")
        void shouldReturn400WhenTypeIsMissing() throws Exception {
            String json = """
                    {"nombre": "Test"}
                    """;

            mockMvc.perform(post("/api/v1/categorias")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/categorias")
    class ListEndpoint {

        @Test
        @DisplayName("Should list categories of the authenticated user")
        void shouldListCategories() throws Exception {
            List<CategoryResponse> categories = List.of(
                    new CategoryResponse(1L, "Salario", TipoCategoria.INGRESO),
                    new CategoryResponse(2L, "Alimentación", TipoCategoria.GASTO)
            );
            when(categoryService.listar()).thenReturn(categories);

            mockMvc.perform(get("/api/v1/categorias"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].nombre").value("Salario"))
                    .andExpect(jsonPath("$[1].nombre").value("Alimentación"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/categorias/{id}")
    class GetEndpoint {

        @Test
        @DisplayName("Should fetch category by ID")
        void shouldGetCategoryById() throws Exception {
            CategoryResponse response = new CategoryResponse(5L, "Transporte", TipoCategoria.GASTO);
            when(categoryService.obtener(5L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/categorias/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5))
                    .andExpect(jsonPath("$.nombre").value("Transporte"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/categorias/{id}")
    class UpdateEndpoint {

        @Test
        @DisplayName("Should update an existing category")
        void shouldUpdateCategory() throws Exception {
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

        @Test
        @DisplayName("Should return 400 when update payload is invalid")
        void shouldReturn400WhenPayloadIsInvalid() throws Exception {
            String json = """
                    {"nombre": "", "tipo": null}
                    """;

            mockMvc.perform(put("/api/v1/categorias/3")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/categorias/{id}")
    class DeleteEndpoint {

        @Test
        @DisplayName("Should delete category and return 204")
        void shouldDeleteCategory() throws Exception {
            doNothing().when(categoryService).eliminar(4L);

            mockMvc.perform(delete("/api/v1/categorias/4"))
                    .andExpect(status().isNoContent());
            verify(categoryService).eliminar(4L);
        }
    }
}
