package com.nexora.backend.controller;

import com.nexora.backend.entity.Article;
import com.nexora.backend.entity.Category;
import com.nexora.backend.repository.ArticleRepository;
import com.nexora.backend.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // The request body only carries category {id} placeholders (deserialized
    // as partial, non-managed Category objects). Re-fetch the real, managed
    // entities by those ids before persisting — saving the partial objects
    // directly could cause Hibernate to misinterpret them as new rows.
    private Set<Category> resolveCategories(Article incoming) {
        if (incoming.getCategories() == null || incoming.getCategories().isEmpty()) {
            return new HashSet<>();
        }
        List<Long> ids = incoming.getCategories().stream()
                .map(Category::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        return new HashSet<>(categoryRepository.findAllById(ids));
    }
    @GetMapping("/search")
    public List<Article> searchArticles(@RequestParam String query) {
        return articleRepository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(query, query);
    }

    // GET all articles
    @GetMapping
    public List<Article> getAllArticles() {
        return articleRepository.findAll();
    }

    // GET one article by slug
    @GetMapping("/{slug}")
    public ResponseEntity<Article> getArticleBySlug(@PathVariable String slug) {
        return articleRepository.findBySlug(slug)
                .map(article -> {
                    article.setViews(article.getViews() + 1); // increment view count
                    articleRepository.save(article);
                    return ResponseEntity.ok(article);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // POST create a new article
    @PostMapping
    public Article createArticle(@Valid @RequestBody Article article) {
        article.setCategories(resolveCategories(article));
        return articleRepository.save(article);
    }

    // PUT update an existing article
    @PutMapping("/{id}")
    public ResponseEntity<Article> updateArticle(@PathVariable Long id, @Valid @RequestBody Article updatedArticle) {
        return articleRepository.findById(id)
                .map(article -> {
                    article.setTitle(updatedArticle.getTitle());
                    article.setSlug(updatedArticle.getSlug());
                    article.setExcerpt(updatedArticle.getExcerpt());
                    article.setContent(updatedArticle.getContent());
                    article.setAuthor(updatedArticle.getAuthor());
                    article.setDate(updatedArticle.getDate());
                    article.setReadTime(updatedArticle.getReadTime());
                    article.setFeatured(updatedArticle.isFeatured());
                    article.setStatus(updatedArticle.getStatus());
                    article.setImagePath(updatedArticle.getImagePath());
                    article.setMetaTitle(updatedArticle.getMetaTitle());
                    article.setMetaDescription(updatedArticle.getMetaDescription());
                    article.setCategories(resolveCategories(updatedArticle));
                    return ResponseEntity.ok(articleRepository.save(article));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE an article
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        if (!articleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        articleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }


}