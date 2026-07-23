package com.nexora.backend.repository;

import com.nexora.backend.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findBySlug(String slug);

    List<Article> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(String title, String content);

    List<Article> findByCategoryId(Long categoryId);
}