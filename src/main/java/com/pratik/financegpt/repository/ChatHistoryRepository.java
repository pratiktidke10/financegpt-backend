package com.pratik.financegpt.repository;

import com.pratik.financegpt.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory , Long> {
    List<ChatHistory> findByUsernameOrderByCreatedAtDesc(String username);
    List<ChatHistory> findTop20ByUsernameOrderByCreatedAtDesc(String username);
}
