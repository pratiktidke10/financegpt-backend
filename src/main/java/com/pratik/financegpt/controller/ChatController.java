package com.pratik.financegpt.controller;

import com.pratik.financegpt.entity.ChatHistory;
import com.pratik.financegpt.model.ChatRequest;
import com.pratik.financegpt.model.ChatResponse;
import com.pratik.financegpt.service.ChatService;
import com.pratik.financegpt.service.StockService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.pratik.financegpt.repository.ChatHistoryRepository;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ChatController {

    private final ChatService chatService;
    private final StockService stockService;
    private final ChatHistoryRepository chatHistoryRepository;

    public ChatController(ChatService chatService , StockService stockService , ChatHistoryRepository chatHistoryRepository){
        this.chatService = chatService;
        this.stockService = stockService;
        this.chatHistoryRepository = chatHistoryRepository;
    }

    @GetMapping("/api/stock/{symbol}")
    public String getStock(@PathVariable String symbol){
        return stockService.getCurrentPrice(symbol);
    }

    @PostMapping("/api/chat")
    public ChatResponse chat(@RequestBody ChatRequest request, @AuthenticationPrincipal UserDetails userDetails){
        String username = userDetails.getUsername();
        String result = chatService.processMessage(request.getMessage(),username);
        return new ChatResponse(result);
    }

    @GetMapping("/api/history")
    public List<Map<String , Object>> getHistory(@AuthenticationPrincipal UserDetails userDetails){
        String username = userDetails.getUsername();
        List<ChatHistory> history = chatHistoryRepository.findTop20ByUsernameOrderByCreatedAtDesc(username);

        List<Map<String,Object>> result = new ArrayList<>();
        for(ChatHistory chat : history){
            Map<String , Object> item = new HashMap<>();
            item.put("id",chat.getId());
            item.put("userMessage", chat.getUserMessage());
            item.put("aiResponse", chat.getAiResponse());
            item.put("createdAt", chat.getCreatedAt());
            result.add(item);
        }
        return result;
    }

}
