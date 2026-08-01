package com.pratik.financegpt.repository;

import com.pratik.financegpt.entity.ChatHistory;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory , Long> {
    List<ChatHistory> findTop10ByUsernameOrderByCreatedAtDesc(String username);

    List<ChatHistory> findByUsernameAndConversationIdOrderByCreatedAtAsc(String username , String conversationId);

    @Transactional
    void deleteByUsernameAndConversationId(String username , String conversationId);

}
