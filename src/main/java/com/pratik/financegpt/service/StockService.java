package com.pratik.financegpt.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
            System.out.println("DEBUG - Calling URL: " + url);
            Map response = restTemplate.getForObject(url, Map.class);
            System.out.println("DEBUG - Response: " + response);

            Map globalQuote = (Map) response.get("Global Quote");

            if (globalQuote == null || globalQuote.isEmpty()) {
                return "Stock symbol not found: " + symbol;
            }

            String price = (String) globalQuote.get("05. price");
            String change = (String) globalQuote.get("09. change");
            String changePercent = (String) globalQuote.get("10. change percent");

            return String.format("### 📈 %s Stock Price\n\n- **Current Price:** $%s\n- **Change:** %s\n- **Change %%:** %s",
                    symbol, price, change, changePercent);

        } catch (Exception e) {
            return "Error fetching stock data: " + e.getMessage();
        }
    }

    public String getPerformance(String symbol) {
        try {
            String url = apiUrl + "?function=TIME_SERIES_WEEKLY&symbol=" + symbol + "&apikey=" + apiKey;
            Map response = restTemplate.getForObject(url, Map.class);

            Map weeklyData = (Map) response.get("Weekly Time Series");

            if (weeklyData == null || weeklyData.isEmpty()) {
                return "No performance data found for: " + symbol;
            }

            // Get last 4 weeks
            List<String> dates = new ArrayList<>(weeklyData.keySet());
            Collections.sort(dates, Collections.reverseOrder());

            StringBuilder result = new StringBuilder();
            result.append("### 📊 Weekly Performance — ").append(symbol).append("\n\n");

            for (int i = 0; i < Math.min(4, dates.size()); i++) {
                String date = dates.get(i);
                Map weekData = (Map) weeklyData.get(date);
                String closePrice = (String) weekData.get("4. close");
                result.append("Week of ").append(date).append(": $").append(closePrice).append("\n");
            }

            return result.toString();

        } catch (Exception e) {
            return "Error fetching performance data: " + e.getMessage();
        }
    }

    public String compareStocks(List<String> symbols) {
        try {
            StringBuilder result = new StringBuilder();
            result.append("### 🔀 Stock Comparison\n\n");

            for (String symbol : symbols) {
                try {
                    Thread.sleep(1000); // wait 1 second between requests
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                String url = apiUrl + "?function=GLOBAL_QUOTE&symbol=" + symbol + "&apikey=" + apiKey;
                Map response = restTemplate.getForObject(url, Map.class);
                Map globalQuote = (Map) response.get("Global Quote");

                if (globalQuote != null && !globalQuote.isEmpty()) {
                    String price = (String) globalQuote.get("05. price");
                    String changePercent = (String) globalQuote.get("10. change percent");
                    result.append(String.format("- **%s**: $%s (%s)\n", symbol, price, changePercent));
                }
            }

            return result.toString();

        } catch (Exception e) {
            return "Error comparing stocks: " + e.getMessage();
        }
    }
}
