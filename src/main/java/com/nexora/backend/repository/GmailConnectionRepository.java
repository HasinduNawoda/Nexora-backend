package com.nexora.backend.repository;

import com.nexora.backend.entity.GmailConnection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GmailConnectionRepository extends JpaRepository<GmailConnection, Long> {
}
