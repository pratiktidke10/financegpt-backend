package com.pratik.financegpt.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class StockService {

    @Value("${alphavantage.api.key}")
    private String apiKey;

    @Value("${alphavantage.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public StockService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getCurrentPrice(String symbol) {
        try {
            String url = apiUrl + "?function=GLOBAL_QUOTE&symbol=" + symbol + "&apikey=" + apiKey;
            Map response = restTemplate.getForObject(url, Map.class);

            Map globalQuote = (Map) response.get("Global Quote");

            if (globalQuote == null || globalQuote.isEmpty()) {
                return "Stock symbol not found: " + symbol;
            }

            String price = (String) globalQuote.get("05. price");
            String change = (String) globalQuote.get("09. change");
            String changePercent = (String) globalQuote.get("10. change percent");

            return "Current price of " + symbol + ": $" + price +
                    " | Change: " + change +
                    " | Change %: " + changePercent;

        } catch (Exception e) {
            return "Error fetching stock data: " + e.getMessage();
        }
    }
}
