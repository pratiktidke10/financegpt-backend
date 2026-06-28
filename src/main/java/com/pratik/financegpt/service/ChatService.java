package com.pratik.financegpt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pratik.financegpt.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ChatService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final StockService stockService;
    private final ObjectMapper objectMapper;
    private final PortfolioService portfolioService;


    private static final String SYSTEM_PROMPT = """
        You are FinanceGPT, an AI-powered financial assistant.
        Analyze the user's message and respond ONLY with a JSON object.
        No extra text, no markdown, just pure JSON.
        
        Respond with this exact format:
        {
            "intent": "INTENT_HERE",
            "symbol": "SYMBOL_HERE",
            "symbols": ["SYMBOL1", "SYMBOL2"],
            "quantity": 1,
            "message": "MESSAGE_HERE"
        }
        
        Intents:
        - STOCK_PRICE: user wants current price of a stock
        - STOCK_PERFORMANCE: user wants performance or history of a stock
        - STOCK_COMPARISON: user wants to compare multiple stocks
        - BUY_STOCK: user wants to buy shares of a stock
        - SELL_STOCK: user wants to sell shares of a stock
        - VIEW_PORTFOLIO: user wants to see their portfolio or holdings or investments or what stocks they own
        - GENERAL: general financial question or anything else
        
        Rules:
        - For STOCK_PRICE, STOCK_PERFORMANCE, BUY_STOCK, SELL_STOCK fill symbol field with correct US ticker
        - For STOCK_COMPARISON fill symbols array
        - For BUY_STOCK and SELL_STOCK fill quantity field with the number mentioned, default 1
        - For VIEW_PORTFOLIO leave symbol empty
        - For GENERAL fill message field with your answer
        - Always use correct US stock ticker symbols (AAPL, GOOGL, TTM, INFY etc.)
        - quantity must always be an integer number
        
        Examples:
        "What is Apple's price?" → {"intent":"STOCK_PRICE","symbol":"AAPL","symbols":[],"quantity":1,"message":""}
        "Buy 5 shares of Tesla" → {"intent":"BUY_STOCK","symbol":"TSLA","symbols":[],"quantity":5,"message":""}
        "Show my portfolio" → {"intent":"VIEW_PORTFOLIO","symbol":"","symbols":[],"quantity":0,"message":""}
        "What is a stock?" → {"intent":"GENERAL","symbol":"","symbols":[],"quantity":0,"message":"A stock is..."}
        """;

    public ChatService(RestTemplate restTemplate, StockService stockService, ObjectMapper objectMapper, PortfolioService portfolioService){
        this.restTemplate = restTemplate;
        this.stockService = stockService;
        this.objectMapper = objectMapper;
        this.portfolioService = portfolioService;
    }

    public String processMessage(String userMessage) {
        try {

            String geminiResponse = callGemini(SYSTEM_PROMPT + "\n\nUser message: " + userMessage);
            geminiResponse = geminiResponse.replaceAll("```json", "").replaceAll("```", "").trim();
            Map<String , Object> intentData = objectMapper.readValue(geminiResponse , Map.class);

            String intent = (String) intentData.get("intent");
            String symbol = (String) intentData.get("symbol");

            return switch (intent) {
                case "STOCK_PRICE" -> stockService.getCurrentPrice(symbol);
                case "STOCK_PERFORMANCE" -> stockService.getPerformance(symbol);
                case "STOCK_COMPARISON" -> {
                    List<String> symbols = (List<String>) intentData.get("symbols");
                    yield stockService.compareStocks(symbols);
                }
                case "BUY_STOCK" -> {
                    Integer quantity = (Integer) intentData.getOrDefault("quantity", 1);
                    yield portfolioService.buyStock("user1", symbol, quantity);
                }
                case "SELL_STOCK" -> {
                    Integer quantity = (Integer) intentData.getOrDefault("quantity", 1);
                    yield portfolioService.sellStock("user1", symbol, quantity);
                }
                case "VIEW_PORTFOLIO" -> portfolioService.viewPortfolio("user1");
                case "GENERAL" -> (String) intentData.get("message");
                default -> "I can help you with stock prices, performance, comparisons and portfolio management!";
            };

        } catch (Exception e) {
            return "Error calling gemini API: " + e.getMessage();
        }
    }
    private String callGemini(String prompt){
        Map<String , Object> part = new HashMap<>();
        part.put("text" , prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String urlWithKey = apiUrl + "?key=" + apiKey;
        ResponseEntity<Map> response = restTemplate.exchange(
                urlWithKey,
                HttpMethod.POST,
                entity,
                Map.class
        );

        Map responseBody = response.getBody();
        List candidates = (List) responseBody.get("candidates");
        Map candidate = (Map) candidates.get(0);
        Map responseContent = (Map) candidate.get("content");
        List parts = (List) responseContent.get("parts");
        Map firstPart = (Map) parts.get(0);

        return (String) firstPart.get("text");
    }
}
