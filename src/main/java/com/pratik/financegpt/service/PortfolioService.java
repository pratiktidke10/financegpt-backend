package com.pratik.financegpt.service;

import com.pratik.financegpt.entity.Portfolio;
import com.pratik.financegpt.repository.PortfolioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final StockService stockService;

    public PortfolioService(PortfolioRepository portfolioRepository , StockService stockService){
        this.portfolioRepository = portfolioRepository;
        this.stockService = stockService;
    }

    public String buyStock(String username , String symbol , Integer quantity){
        try {
            double price = stockService.getCurrentPriceValue(symbol);

            if(price <=0 ){
                return "Could not fetch price for: " + symbol;
            }

            List<Portfolio> existing = portfolioRepository.findByUsernameAndSymbol(username , symbol);

            if(!existing.isEmpty()){
                Portfolio position = existing.get(0);
                position.setQuantity(position.getQuantity() + quantity);
                portfolioRepository.save(position);
            }else {
                Portfolio newPositon = new Portfolio(username , symbol , quantity ,price);
                portfolioRepository.save(newPositon);
            }

            double totalCost = price * quantity;
            return String.format("✅ Successfully bought %d shares of %s at $%.2f each. Total cost: $%.2f",
                    quantity, symbol.toUpperCase(), price, totalCost);
        } catch (Exception e) {
            return "Error buying stock: " + e.getMessage();
        }
    }

    public String sellStock(String username, String symbol, Integer quantity) {
        try {
            List<Portfolio> existing = portfolioRepository.findByUsernameAndSymbol(username, symbol);

            if (existing.isEmpty()) {
                return "You don't own any shares of " + symbol;
            }

            Portfolio position = existing.get(0);

            // Validate quantity before doing anything
            if (position.getQuantity() <= 0 || position.getQuantity() < quantity) {
                return String.format("You only own %d shares of %s, cannot sell %d",
                        position.getQuantity(), symbol, quantity);
            }

            double currentPrice = stockService.getCurrentPriceValue(symbol);

            if (position.getQuantity().equals(quantity)) {
                // Sell all — delete record
                portfolioRepository.delete(position);
            } else {
                // Sell partial — update quantity
                position.setQuantity(position.getQuantity() - quantity);
                portfolioRepository.save(position);
            }

            double totalValue = currentPrice * quantity;
            return String.format("✅ Successfully sold %d shares of %s at $%.2f each. Total value: $%.2f",
                    quantity, symbol.toUpperCase(), currentPrice, totalValue);

        } catch (Exception e) {
            return "Error selling stocks: " + e.getMessage();
        }
    }

    public String viewPortfolio(String username) {
        try {
            List<Portfolio> positions = portfolioRepository.findByUsername(username);

            if (positions.isEmpty()) {
                return "Your portfolio is empty. Start by buying some stocks!";
            }

            double totalValue = 0.0;
            StringBuilder jsonHoldings = new StringBuilder();
            StringBuilder summaryText = new StringBuilder();

            summaryText.append("### 📊 Your Holdings Summary\n\n");

            for(int i=0; i<positions.size(); i++){
                Portfolio position = positions.get(i);
                String symbol = position.getSymbol().toUpperCase();
                int qty = position.getQuantity();
                double buyPrice = position.getBuyPrice();
                double currentPrice = stockService.getCurrentPriceValue(symbol);
                double positionValue = currentPrice * qty;
                totalValue += positionValue;

                double pnl = (currentPrice - buyPrice) * qty;
                String pnlSign = pnl >= 0 ? "▲ +" : "▼ ";

                summaryText.append(String.format("- **%s**: %d shares @ $%.2f (Current: $%.2f | P&L: %s$%.2f)\n",
                        symbol, qty, buyPrice, currentPrice, pnlSign, pnl));

                jsonHoldings.append(String.format("    {\"symbol\": \"%s\", \"value\": %.2f}", symbol, positionValue));
                if (i < positions.size() - 1) {
                    jsonHoldings.append(",");
                }
                jsonHoldings.append("\n");
            }

            StringBuilder chartJson = new StringBuilder();
            chartJson.append("\n```json\n{\n");
            chartJson.append("  \"type\": \"PORTFOLIO_CHART\",\n");
            chartJson.append(String.format("  \"totalValue\": %.2f,\n", totalValue));
            chartJson.append("  \"holdings\": [\n");
            chartJson.append(jsonHoldings);
            chartJson.append("  ]\n}\n```\n\n");

            return chartJson.toString() + summaryText.toString();

        } catch (Exception e) {
            return "Error fetching portfolio: " + e.getMessage();
        }
    }

    private double extractPrice(String priceData){
        try{
            String[] parts = priceData.split("\\$");
            String priceStr = parts[1].split(" ")[0];
            return Double.parseDouble(priceStr);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
