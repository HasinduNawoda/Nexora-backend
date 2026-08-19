package com.nexora.backend.service;

import com.nexora.backend.dto.NewsItemResponse;
import com.nexora.backend.dto.NewsPageResponse;
import com.nexora.backend.dto.ReadStatusResponse;
import com.nexora.backend.entity.NewsItem;
import com.nexora.backend.repository.NewsItemRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NewsService {

    private final NewsItemRepository newsItemRepository;

    public NewsService(NewsItemRepository newsItemRepository) {
        this.newsItemRepository = newsItemRepository;
    }

    public NewsPageResponse getNewsItems(String query, String source, String category,
                                         Boolean read, int page, int size) {
        Specification<NewsItem> spec = buildSpecification(query, source, category, read);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"));

        Page<NewsItem> resultPage = newsItemRepository.findAll(spec, pageRequest);

        List<NewsItemResponse> items = resultPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new NewsPageResponse(items, resultPage.getNumber(), resultPage.getTotalPages());
    }

    public NewsItemResponse getNewsItem(Long id) {
        Optional<NewsItem> opt = newsItemRepository.findById(id);
        if (opt.isEmpty()) {
            return null;
        }

        NewsItem item = opt.get();
        // Mark as read when viewing
        if (!item.isRead()) {
            item.setRead(true);
            newsItemRepository.save(item);
        }

        return toResponse(item);
    }

    public ReadStatusResponse updateReadStatus(Long id, boolean read) {
        Optional<NewsItem> opt = newsItemRepository.findById(id);
        if (opt.isEmpty()) {
            return null;
        }

        NewsItem item = opt.get();
        item.setRead(read);
        newsItemRepository.save(item);

        return new ReadStatusResponse(item.isRead());
    }

    public boolean dismissNewsItem(Long id) {
        if (!newsItemRepository.existsById(id)) {
            return false;
        }
        newsItemRepository.deleteById(id);
        return true;
    }

    // ─── Private Helpers ─────────────────────────────────────────────────

    private Specification<NewsItem> buildSpecification(String query, String source,
                                                       String category, Boolean read) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("preview")), pattern),
                        cb.like(cb.lower(root.get("source")), pattern)
                ));
            }

            if (source != null && !source.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("source")), source.toLowerCase()));
            }

            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
            }

            if (read != null) {
                predicates.add(cb.equal(root.get("read"), read));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private NewsItemResponse toResponse(NewsItem item) {
        return new NewsItemResponse(
                item.getId(),
                item.getTitle(),
                item.getSource(),
                item.getReceivedAt() != null ? item.getReceivedAt().toString() : null,
                item.getCategory(),
                item.getPreview(),
                item.getSourceUrl(),
                item.isRead()
        );
    }
}
