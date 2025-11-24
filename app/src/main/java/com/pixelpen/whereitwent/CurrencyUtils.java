package com.pixelpen.whereitwent;

import java.util.HashMap;
import java.util.Map;

public class CurrencyUtils {
    private static final Map<String, String> symbols = new HashMap<>();

    // Format a value with commas and a currency symbol
    public static String format(double value, String symbol) {
        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(value) + " " + symbol;
    }


    static {
        symbols.put("USD", "$");
        symbols.put("EUR", "€");
        symbols.put("GBP", "£");
        symbols.put("JPY", "¥");
        symbols.put("CNY", "¥");
        symbols.put("INR", "₹");
        symbols.put("AUD", "$");
        symbols.put("CAD", "$");
        symbols.put("SGD", "$");
        symbols.put("HKD", "$");
        symbols.put("MYR", "RM");
        symbols.put("THB", "฿");
    }

    public static String symbolFor(String code) {
        return symbols.getOrDefault(code, code);
    }
}
