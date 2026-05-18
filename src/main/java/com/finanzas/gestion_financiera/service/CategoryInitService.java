package com.finanzas.gestion_financiera.service;

import com.finanzas.gestion_financiera.entity.Category;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryInitService {

    private final CategoryRepository categoryRepository;

    public void crearCategoriasPorDefecto(User user) {
        List<Category> categorias = List.of(
                crearCategoria("Salario",        user),
                crearCategoria("Freelance",      user),
                crearCategoria("Inversiones",    user),
                crearCategoria("Otros ingresos", user),
                crearCategoria("Alimentación",   user),
                crearCategoria("Transporte",     user),
                crearCategoria("Vivienda",       user),
                crearCategoria("Salud",          user),
                crearCategoria("Entretenimiento",user),
                crearCategoria("Educación",      user)
        );
        categoryRepository.saveAll(categorias);
    }

    private Category crearCategoria(String nombre, User user) {
        Category category = new Category();
        category.setNombre(nombre);
        category.setUsuario(user);
        return category;
    }
}
