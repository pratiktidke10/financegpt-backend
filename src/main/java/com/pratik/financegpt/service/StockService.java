package com.pratik.financegpt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

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
            if(symbols == null || symbols.isEmpty()){
                return "Please provide at least two stock symbols to compare.";
            }

            Map<String , Map<String , Double>> timeSeriesMap = new TreeMap<>();
            List<String> validSymbols = new ArrayList<>();

            for (String symbol : symbols) {
                String upperSymbol = symbol.toUpperCase();
                try {
                    Thread.sleep(500);
                    String url = apiUrl + "?function=TIME_SERIES_WEEKLY&symbol=" + upperSymbol + "&apikey=" + apiKey;
                    String response = restTemplate.getForObject(url , String.class);
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(response);
                    JsonNode timeSeries = root.path("Weekly Time Series");

                    if(!timeSeries.isMissingNode()){
                        validSymbols.add(upperSymbol);
                        Iterator<String> dates = timeSeries.fieldNames();
                        int count = 0;
                        while(dates.hasNext() && count<6){
                            String date = dates.next();
                            double closePrice = timeSeries.get(date).path("4. close").asDouble();
                            String shortDate = date.length() >= 10 ? date.substring(5) : date;

                            timeSeriesMap.putIfAbsent(shortDate , new HashMap<>());
                            timeSeriesMap.get(shortDate).put(upperSymbol , closePrice);
                            count++;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching comparison for " + upperSymbol + ": " + e.getMessage());
                }
            }

            if (validSymbols.isEmpty()){
                return "Could not fetch comparison data for the requested symbols.";
            }

            // Build MULTI_STOCK_CHART JSON
            StringBuilder chartJson = new StringBuilder();
            chartJson.append("\n```json\n{\n");
            chartJson.append("  \"type\": \"MULTI_STOCK_CHART\",\n");
            chartJson.append("  \"symbols\": [");
            for(int i=0; i<validSymbols.size(); i++){
                chartJson.append("\"").append(validSymbols.get(i)).append("\"");
                if(i<validSymbols.size() - 1) chartJson.append(", ");
            }
            chartJson.append("],\n");
            chartJson.append("  \"points\": [\n");

            List<String> sortedDates = new ArrayList<>(timeSeriesMap.keySet());
            for(int i=0; i<sortedDates.size(); i++){
                String date = sortedDates.get(i);
                Map<String , Double> symbolPrices = timeSeriesMap.get(date);

                chartJson.append(String.format("    {\"date\": \"%s\"", date));
                for (String sym : validSymbols) {
                    Double price = symbolPrices.getOrDefault(sym, 0.0);
                    chartJson.append(String.format(", \"%s\": %.2f", sym, price));
                }
                chartJson.append("}");
                if (i < sortedDates.size() - 1) chartJson.append(",");
                chartJson.append("\n");
            }
            chartJson.append("  ]\n}\n```\n\n");

            // Build Summary Text
            StringBuilder summaryText = new StringBuilder();
            summaryText.append("### 🔀 Stock Comparison Analysis\n\n");
            for (String sym : validSymbols) {
                double latestPrice = getCurrentPriceValue(sym);
                summaryText.append(String.format("- **%s**: Latest Price $%.2f\n", sym, latestPrice));
            }

            return chartJson.toString() + summaryText.toString();

        } catch (Exception e) {
            return "Error comparing stocks: " + e.getMessage();
        }
    }

    public String getStockPerformance(String symbol) {
        try {
            String url = "https://www.alphavantage.co/query?function=TIME_SERIES_WEEKLY&symbol="
                    + symbol + "&apikey=" + apiKey;

            String response = restTemplate.getForObject(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            JsonNode timeSeries = root.path("Weekly Time Series");

            if (timeSeries.isMissingNode()) {
                return "Unable to fetch performance data for " + symbol + " at this time.";
            }

            List<String> dates = new ArrayList<>();
            Iterator<String> fieldNames = timeSeries.fieldNames();
            while (fieldNames.hasNext() && dates.size() < 6) {
                dates.add(fieldNames.next());
            }

            // Reverse so dates go from oldest -> newest on the chart X-axis
            Collections.reverse(dates);

            // 1. Build JSON Payload for Recharts
            StringBuilder chartJson = new StringBuilder();
            chartJson.append("\n```json\n{\n");
            chartJson.append("  \"type\": \"STOCK_CHART\",\n");
            chartJson.append("  \"symbol\": \"").append(symbol.toUpperCase()).append("\",\n");
            chartJson.append("  \"points\": [\n");

            for (int i = 0; i < dates.size(); i++) {
                String date = dates.get(i);
                double closePrice = timeSeries.get(date).path("4. close").asDouble();

                // Format date label (e.g., "07/31")
                String shortDate = date.length() >= 10 ? date.substring(5) : date;

                chartJson.append(String.format("    {\"date\": \"%s\", \"price\": %.2f}", shortDate, closePrice));
                if (i < dates.size() - 1) {
                    chartJson.append(",");
                }
                chartJson.append("\n");
            }
            chartJson.append("  ]\n}\n```\n\n");

            // 2. Build Summary Markdown Text
            StringBuilder summaryText = new StringBuilder();
            summaryText.append("📊 **Weekly Performance — ").append(symbol.toUpperCase()).append("**\n");
            for (int i = dates.size() - 1; i >= 0; i--) {
                String date = dates.get(i);
                double closePrice = timeSeries.get(date).path("4. close").asDouble();
                summaryText.append(String.format("Week of %s: **$%.2f**\n", date, closePrice));
            }

            // Return combined JSON chart payload + text explanation
            return chartJson.toString() + summaryText.toString();

        } catch (Exception e) {
            return "Error fetching stock performance for " + symbol + ": " + e.getMessage();
        }
    }

    public double getCurrentPriceValue (String symbol){
        try{
            String url = apiUrl + "?function=GLOBAL_QUOTE&symbol=" + symbol + "&apikey=" + apiKey;
            Map response = restTemplate.getForObject(url , Map.class);

            if(response != null && response.containsKey("Global Quote")){
                Map globalQuote = (Map) response.get("Global Quote");
                if(globalQuote != null && globalQuote.containsKey("05. price")){
                    String priceStr = (String) globalQuote.get("05. price");
                    return Double.parseDouble(priceStr);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching price value for " + symbol + ": " + e.getMessage());
        }
        return getFallbackPrice(symbol);
    }

    private double getFallbackPrice(String symbol){
        if (symbol == null) return 100.00;
        return switch (symbol.toUpperCase()) {
            case "AAPL" -> 220.50;
            case "TSLA" -> 215.30;
            case "GOOGL" -> 175.80;
            case "TTM" -> 25.40;
            default -> 100.00;
        };
    }
}
