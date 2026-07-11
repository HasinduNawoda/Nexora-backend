package com.nexora.backend.controller;

import com.nexora.backend.entity.Article;
import com.nexora.backend.repository.ArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    @Autowired
    private ArticleRepository articleRepository;
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
                    article.setCategory(updatedArticle.getCategory());
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