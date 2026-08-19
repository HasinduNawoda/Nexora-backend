package com.nexora.backend.repository;

import com.nexora.backend.entity.NewsItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NewsItemRepository extends JpaRepository<NewsItem, Long>, JpaSpecificationExecutor<NewsItem> {
    boolean existsByGmailMessageId(String gmailMessageId);
}
