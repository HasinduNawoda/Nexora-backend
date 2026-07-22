package com.nexora.backend.controller;

import com.nexora.backend.entity.Category;
import com.nexora.backend.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    // GET all categories
    @GetMapping
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // POST a new category
    @PostMapping
    public Category createCategory(@Valid @RequestBody Category category) {
        return categoryRepository.save(category);
    }

    // PUT rename an existing category
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @Valid @RequestBody Category updated) {
        return categoryRepository.findById(id)
                .map(category -> {
                    category.setName(updated.getName());
                    return ResponseEntity.ok(categoryRepository.save(category));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE a category
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        if (!categoryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        categoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}