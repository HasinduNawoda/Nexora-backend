package com.nexora.backend.controller;

import com.nexora.backend.entity.Article;
import com.nexora.backend.entity.Category;
import com.nexora.backend.repository.ArticleRepository;
import com.nexora.backend.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ArticleRepository articleRepository;

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

    // DELETE a category — articles in this category are reassigned to
    // "Uncategorized" (category = null) instead of being deleted or blocking
    // the deletion with a foreign key error.
    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        if (!categoryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        List<Article> articlesInCategory = articleRepository.findByCategoryId(id);
        for (Article article : articlesInCategory) {
            article.setCategory(null);
        }
        articleRepository.saveAll(articlesInCategory);

        categoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}