package com.pratik.financegpt.controller;

import com.pratik.financegpt.model.ChatRequest;
import com.pratik.financegpt.model.ChatResponse;
import com.pratik.financegpt.service.ChatService;
import com.pratik.financegpt.service.StockService;
import org.springframework.web.bind.annotation.*;

@RestController
public class ChatController {

    private final ChatService chatService;
    private final StockService stockService;

    public ChatController(ChatService chatService , StockService stockService){
        this.chatService = chatService;
        this.stockService = stockService;
    }

    @GetMapping("/api/stock/{symbol}")
    public String getStock(@PathVariable String symbol){
        return stockService.getCurrentPrice(symbol);
    }

    @PostMapping("/api/chat")
    public ChatResponse chat(@RequestBody ChatRequest request){
        String result = chatService.processMessage(request.getMessage());
        return new ChatResponse(result);
    }
}
