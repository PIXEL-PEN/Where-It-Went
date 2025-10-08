package com.example.expensetracker;

import java.util.HashMap;
import java.util.Map;

public class CurrencyUtils {
    private static final Map<String, String> symbols = new HashMap<>();

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
