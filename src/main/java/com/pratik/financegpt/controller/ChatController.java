package com.pratik.financegpt.controller;

import com.pratik.financegpt.entity.ChatHistory;
import com.pratik.financegpt.model.ChatRequest;
import com.pratik.financegpt.model.ChatResponse;
import com.pratik.financegpt.service.ChatService;
import com.pratik.financegpt.service.StockService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.pratik.financegpt.repository.ChatHistoryRepository;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.ZoneId;

@RestController
public class ChatController {

    private final ChatService chatService;
    private final StockService stockService;
    private final ChatHistoryRepository chatHistoryRepository;

    public ChatController(ChatService chatService, StockService stockService, ChatHistoryRepository chatHistoryRepository) {
        this.chatService = chatService;
        this.stockService = stockService;
        this.chatHistoryRepository = chatHistoryRepository;
    }

    @GetMapping("/api/stock/{symbol}")
    public String getStock(@PathVariable String symbol) {
        return stockService.getCurrentPrice(symbol);
    }

    @PostMapping("/api/chat")
    public ChatResponse chat(@RequestBody ChatRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        String result = chatService.processMessage(request.getMessage(), username, request.getConversationId());
        return new ChatResponse(result);
    }

    @GetMapping("/api/history")
    public List<Map<String, Object>> getHistory(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        List<ChatHistory> allHistory = chatHistoryRepository.findTop10ByUsernameOrderByCreatedAtDesc(username);

        List<Map<String, Object>> result = new ArrayList<>();
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();

        for (ChatHistory chat : allHistory) {
            if (chat.getConversationId() != null && !chat.getConversationId().isEmpty() && !seen.contains(chat.getConversationId())) {
                seen.add(chat.getConversationId());
                Map<String, Object> item = new HashMap<>();
                item.put("conversationId", chat.getConversationId());

                String displayTitle = (chat.getTitle() != null && !chat.getTitle().isEmpty())
                        ? chat.getTitle()
                        : chat.getUserMessage();

                item.put("firstMessage", displayTitle);
                item.put("createdAt", chat.getCreatedAt());
                result.add(item);
            }
        }
        return result;
    }

    @GetMapping("/api/history/{conversationId}")
    public List<Map<String , Object>> getConversationDetails(
            @PathVariable String conversationId,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        String username = userDetails.getUsername();

        List<ChatHistory> historyList = chatHistoryRepository
                .findByUsernameAndConversationIdOrderByCreatedAtAsc(username , conversationId);

        List<Map<String , Object>> messages = new ArrayList<>();

        for(ChatHistory chat : historyList){

            long timestamp = chat.getCreatedAt() != null
                    ? chat.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    : System.currentTimeMillis();

            Map<String , Object> userMsg = new HashMap<>();
            userMsg.put("id",chat.getId() + "_user");
            userMsg.put("text",chat.getUserMessage());
            userMsg.put("ai",false);
            userMsg.put("createdAt",timestamp);
            messages.add(userMsg);

            if(chat.getAiResponse() != null && !chat.getAiResponse().isEmpty()){
                Map<String , Object> aiMsg = new HashMap<>();
                aiMsg.put("id",chat.getId()+"_ai");
                aiMsg.put("text",chat.getAiResponse());
                aiMsg.put("ai",true);
                aiMsg.put("createdAt",timestamp);
                messages.add(aiMsg);
            }
        }
        return messages;
    }

    @DeleteMapping("/api/history/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable String conversationId,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        String username = userDetails.getUsername();
        chatHistoryRepository.deleteByUsernameAndConversationId(username , conversationId);
        return ResponseEntity.ok().build();
    }
}
