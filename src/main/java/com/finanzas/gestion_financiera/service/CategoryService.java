package com.finanzas.gestion_financiera.service;

import com.finanzas.gestion_financiera.dto.CategoryRequest;
import com.finanzas.gestion_financiera.dto.CategoryResponse;
import com.finanzas.gestion_financiera.entity.Category;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.CategoryRepository;
import com.finanzas.gestion_financiera.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryResponse crear(CategoryRequest request) {
        User user = getUsuarioAutenticado();
        Category category = new Category();
        category.setNombre(request.getNombre());
        category.setUsuario(user);
        categoryRepository.save(category);
        return toResponse(category);
    }

    public List<CategoryResponse> listar() {
        User user = getUsuarioAutenticado();
        return categoryRepository.findByUsuarioId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse obtener(Long id) {
        User user = getUsuarioAutenticado();
        Category category = categoryRepository.findByIdAndUsuarioId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada"));
        return toResponse(category);
    }

    public CategoryResponse actualizar(Long id, CategoryRequest request) {
        User user = getUsuarioAutenticado();
        Category category = categoryRepository.findByIdAndUsuarioId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada"));
        category.setNombre(request.getNombre());
        categoryRepository.save(category);
        return toResponse(category);
    }

    public void eliminar(Long id) {
        User user = getUsuarioAutenticado();
        Category category = categoryRepository.findByIdAndUsuarioId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada"));
        categoryRepository.delete(category);
    }

    private User getUsuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getNombre());
    }
}