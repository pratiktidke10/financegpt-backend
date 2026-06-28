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
            String priceData = stockService.getCurrentPrice(symbol);
            double price = extractPrice(priceData);

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
                    quantity, symbol, price, totalCost);
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

            String priceData = stockService.getCurrentPrice(symbol);
            double currentPrice = extractPrice(priceData);

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
                    quantity, symbol, currentPrice, totalValue);

        } catch (Exception e) {
            return "Error selling stocks: " + e.getMessage();
        }
    }

    public String viewPortfolio(String username){
        try {
            List<Portfolio> positions = portfolioRepository.findByUsername(username);
            if (positions.isEmpty()) {
                return "Your portfolio is empty. Start by buying some stocks!";
            }
            StringBuilder result = new StringBuilder();
            result.append("\uD83D\uDCCA Your Portfolio:\\n\\n");
            for (Portfolio position : positions) {
                result.append(String.format("• %s: %d shares (bought at $%.2f)\n",
                        position.getSymbol(),
                        position.getQuantity(),
                        position.getBuyPrice()));
            }
            return result.toString();
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
