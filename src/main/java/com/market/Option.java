package com.market;

import java.time.LocalDate;

/**
 * Represents a stock option contract
 */
public class Option extends TradeItem {
    public enum OptionType {
        CALL, PUT
    }
    
    private final String underlyingSymbol;
    private final OptionType type;
    private final double strikePrice;
    private final LocalDate expirationDate;
    
    /**
     * Create a new option contract
     * @param underlyingSymbol The underlying stock symbol
     * @param type The option type (CALL or PUT)
     * @param strikePrice The strike price
     * @param expirationDate The expiration date
     */
    public Option(String underlyingSymbol, OptionType type, double strikePrice, LocalDate expirationDate) {
        super(generateName(underlyingSymbol, type, strikePrice, expirationDate), 
              generateSymbol(underlyingSymbol, type, strikePrice, expirationDate));
        
        this.underlyingSymbol = underlyingSymbol;
        this.type = type;
        this.strikePrice = strikePrice;
        this.expirationDate = expirationDate;
    }
    
    /**
     * Generate a name for the option
     */
    private static String generateName(String underlyingSymbol, OptionType type, double strikePrice, LocalDate expirationDate) {
        return String.format("%s %s $%.2f %s", 
                underlyingSymbol, 
                type.name(), 
                strikePrice, 
                expirationDate.toString());
    }
    
    /**
     * Generate a symbol for the option
     */
    private static String generateSymbol(String underlyingSymbol, OptionType type, double strikePrice, LocalDate expirationDate) {
        return String.format("%s%s%02d%02d%05.0f", 
                underlyingSymbol, 
                type == OptionType.CALL ? "C" : "P",
                expirationDate.getMonthValue(),
                expirationDate.getDayOfMonth(),
                strikePrice * 100);
    }
    
    /**
     * Get the underlying stock symbol
     * @return The underlying symbol
     */
    public String getUnderlyingSymbol() {
        return underlyingSymbol;
    }
    
    /**
     * Get the option type
     * @return The option type
     */
    public OptionType getType() {
        return type;
    }
    
    /**
     * Get the strike price
     * @return The strike price
     */
    public double getStrikePrice() {
        return strikePrice;
    }
    
    /**
     * Get the expiration date
     * @return The expiration date
     */
    public LocalDate getExpirationDate() {
        return expirationDate;
    }
    
    /**
     * Calculate the option's intrinsic value based on the underlying stock price
     * @param underlyingPrice The current price of the underlying stock
     * @return The intrinsic value of the option
     */
    public double calculateIntrinsicValue(double underlyingPrice) {
        if (type == OptionType.CALL) {
            return Math.max(0, underlyingPrice - strikePrice);
        } else {
            return Math.max(0, strikePrice - underlyingPrice);
        }
    }

    @Override
    public double getPrice() {
        // For now, return 0 as option pricing is complex
        return 0.0;
    }
}
