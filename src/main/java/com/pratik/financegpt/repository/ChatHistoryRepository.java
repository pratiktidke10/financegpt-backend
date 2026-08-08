package com.pratik.financegpt.repository;

import com.pratik.financegpt.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {

    // Fetch all user chat records ordered by newest first
    @Query("SELECT c FROM ChatHistory c WHERE c.username = :username ORDER BY c.createdAt DESC")
    List<ChatHistory> findByUsernameOrderByCreatedAtDesc(@Param("username") String username);

    // Fetch chronological messages for a specific active conversation
    List<ChatHistory> findByUsernameAndConversationIdOrderByCreatedAtAsc(String username, String conversationId);

    @Transactional
    void deleteByUsernameAndConversationId(String username, String conversationId);

}