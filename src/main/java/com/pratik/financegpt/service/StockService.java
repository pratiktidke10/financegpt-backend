package com.pratik.financegpt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class StockService {

    @Value("${finnhub.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public StockService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Normalizes informal stock names to valid US market tickers
     */
    public String cleanSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) return "";
        String clean = symbol.trim().toUpperCase();
        return switch (clean) {
            case "TATA", "TATA MOTORS", "TATAMOTORS" -> "TTM";
            case "NVIDIA" -> "NVDA";
            case "APPLE" -> "AAPL";
            case "TESLA" -> "TSLA";
            case "GOOGLE", "ALPHABET" -> "GOOGL";
            case "MICROSOFT" -> "MSFT";
            default -> clean;
        };
    }

    /**
     * 1. Get Current Stock Price (Finnhub Quote)
     */
    public String getCurrentPrice(String rawSymbol) {
        String symbol = cleanSymbol(rawSymbol);
        try {
            String url = String.format("https://finnhub.io/api/v1/quote?symbol=%s&token=%s", symbol, apiKey);
            Map response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("c")) {
                Number currentPrice = (Number) response.get("c");
                Number change = (Number) response.get("d");
                Number percentChange = (Number) response.get("dp");

                if (currentPrice.doubleValue() == 0) {
                    return "Stock symbol not found: " + symbol;
                }

                return String.format("### 📈 %s Stock Price\n\n- **Current Price:** $%.2f\n- **Change:** $%.2f\n- **Change %%:** %.2f%%",
                        symbol, currentPrice.doubleValue(), change.doubleValue(), percentChange.doubleValue());
            }

            return "Stock symbol not found: " + symbol;

        } catch (Exception e) {
            return "Error fetching stock data for " + symbol + ": " + e.getMessage();
        }
    }

    /**
     * 2. Single Stock Performance with Chart JSON (Finnhub Candle W)
     */
    public String getStockPerformance(String rawSymbol) {
        String symbol = cleanSymbol(rawSymbol);
        try {
            long now = Instant.now().getEpochSecond();
            long sixWeeksAgo = now - (6L * 7L * 24L * 60L * 60L);

            String url = String.format("https://finnhub.io/api/v1/stock/candle?symbol=%s&resolution=W&from=%d&to=%d&token=%s",
                    symbol, sixWeeksAgo, now, apiKey);

            Map response = restTemplate.getForObject(url, Map.class);

            if (response == null || !"ok".equals(response.get("s"))) {
                return "Unable to fetch performance data for " + symbol + " at this time.";
            }

            List<Number> closePrices = (List<Number>) response.get("c");
            List<Number> timestamps = (List<Number>) response.get("t");

            if (closePrices == null || closePrices.isEmpty()) {
                return "Unable to fetch performance data for " + symbol + " at this time.";
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd").withZone(ZoneId.of("UTC"));

            // Build Recharts JSON
            StringBuilder chartJson = new StringBuilder();
            chartJson.append("\n```json\n{\n");
            chartJson.append("  \"type\": \"STOCK_CHART\",\n");
            chartJson.append("  \"symbol\": \"").append(symbol).append("\",\n");
            chartJson.append("  \"points\": [\n");

            StringBuilder summaryText = new StringBuilder();
            summaryText.append("📊 **Weekly Performance — ").append(symbol).append("**\n");

            int size = Math.min(closePrices.size(), timestamps.size());
            for (int i = 0; i < size; i++) {
                double price = closePrices.get(i).doubleValue();
                String dateStr = formatter.format(Instant.ofEpochSecond(timestamps.get(i).longValue()));

                chartJson.append(String.format("    {\"date\": \"%s\", \"price\": %.2f}", dateStr, price));
                if (i < size - 1) chartJson.append(",");
                chartJson.append("\n");

                summaryText.append(String.format("Week of %s: **$%.2f**\n", dateStr, price));
            }
            chartJson.append("  ]\n}\n```\n\n");

            return chartJson.toString() + summaryText.toString();

        } catch (Exception e) {
            return "Error fetching stock performance for " + symbol + ": " + e.getMessage();
        }
    }

    /**
     * 3. Compare Multiple Stocks (Finnhub Quotes + Candle Data)
     */
    public String compareStocks(List<String> rawSymbols) {
        try {
            if (rawSymbols == null || rawSymbols.isEmpty()) {
                return "Please provide at least two stock symbols to compare.";
            }

            List<String> validSymbols = rawSymbols.stream()
                    .map(this::cleanSymbol)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .toList();

            Map<String, Map<String, Double>> timeSeriesMap = new TreeMap<>();
            List<String> activeSymbols = new ArrayList<>();

            long now = Instant.now().getEpochSecond();
            long sixWeeksAgo = now - (6L * 7L * 24L * 60L * 60L);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd").withZone(ZoneId.of("UTC"));

            for (String symbol : validSymbols) {
                try {
                    String url = String.format("https://finnhub.io/api/v1/stock/candle?symbol=%s&resolution=W&from=%d&to=%d&token=%s",
                            symbol, sixWeeksAgo, now, apiKey);

                    Map response = restTemplate.getForObject(url, Map.class);

                    if (response != null && "ok".equals(response.get("s"))) {
                        List<Number> closePrices = (List<Number>) response.get("c");
                        List<Number> timestamps = (List<Number>) response.get("t");

                        if (closePrices != null && !closePrices.isEmpty()) {
                            activeSymbols.add(symbol);
                            int size = Math.min(closePrices.size(), timestamps.size());
                            for (int i = 0; i < size; i++) {
                                String dateStr = formatter.format(Instant.ofEpochSecond(timestamps.get(i).longValue()));
                                double price = closePrices.get(i).doubleValue();

                                timeSeriesMap.putIfAbsent(dateStr, new HashMap<>());
                                timeSeriesMap.get(dateStr).put(symbol, price);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching comparison for " + symbol + ": " + e.getMessage());
                }
            }

            if (activeSymbols.isEmpty()) {
                return "Could not fetch comparison data for the requested symbols.";
            }

            // Build MULTI_STOCK_CHART JSON
            StringBuilder chartJson = new StringBuilder();
            chartJson.append("\n```json\n{\n");
            chartJson.append("  \"type\": \"MULTI_STOCK_CHART\",\n");
            chartJson.append("  \"symbols\": [");
            for (int i = 0; i < activeSymbols.size(); i++) {
                chartJson.append("\"").append(activeSymbols.get(i)).append("\"");
                if (i < activeSymbols.size() - 1) chartJson.append(", ");
            }
            chartJson.append("],\n");
            chartJson.append("  \"points\": [\n");

            List<String> sortedDates = new ArrayList<>(timeSeriesMap.keySet());
            for (int i = 0; i < sortedDates.size(); i++) {
                String date = sortedDates.get(i);
                Map<String, Double> symbolPrices = timeSeriesMap.get(date);

                chartJson.append(String.format("    {\"date\": \"%s\"", date));
                for (String sym : activeSymbols) {
                    Double price = symbolPrices.getOrDefault(sym, getCurrentPriceValue(sym));
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
            for (String sym : activeSymbols) {
                double latestPrice = getCurrentPriceValue(sym);
                summaryText.append(String.format("- **%s**: Latest Price $%.2f\n", sym, latestPrice));
            }

            return chartJson.toString() + summaryText.toString();

        } catch (Exception e) {
            return "Error comparing stocks: " + e.getMessage();
        }
    }

    /**
     * Helper: Get Raw Double Price Value for Stock
     */
    public double getCurrentPriceValue(String rawSymbol) {
        String symbol = cleanSymbol(rawSymbol);
        try {
            String url = String.format("https://finnhub.io/api/v1/quote?symbol=%s&token=%s", symbol, apiKey);
            Map response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("c")) {
                Number price = (Number) response.get("c");
                if (price.doubleValue() > 0) {
                    return price.doubleValue();
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching price value for " + symbol + ": " + e.getMessage());
        }
        return getFallbackPrice(symbol);
    }

    private double getFallbackPrice(String symbol) {
        if (symbol == null) return 100.00;
        return switch (symbol.toUpperCase()) {
            case "AAPL" -> 220.50;
            case "TSLA" -> 215.30;
            case "GOOGL" -> 175.80;
            case "NVDA" -> 120.40;
            case "TTM" -> 25.40;
            default -> 100.00;
        };
    }
}