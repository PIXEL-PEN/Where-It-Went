package com.pixelpen.whereitwent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Foreign currency for Travel accounts (v3.3): the currency list and all the
 * arithmetic, with no Android code so it can be unit-tested off-device.
 *
 * Rates are written "1 HOME = rate FOREIGN", e.g. 1 USD = 25,450 VND.
 * A foreign amount converts to home as foreign / rate, rounded to the home
 * currency's decimal places.
 */
public final class TripCurrency {

    /** One currency: ISO code, display name, symbol, decimal places. */
    public static final class Currency {
        public final String code, name, symbol;
        public final int decimals;

        Currency(String code, String name, String symbol, int decimals) {
            this.code = code;
            this.name = name;
            this.symbol = symbol;
            this.decimals = decimals;
        }

        /** "VND — Vietnamese Dong (₫)", as in the Settings list. */
        public String label() {
            return code + " — " + name + " (" + symbol + ")";
        }
    }

    /** Settings currencies first (same order), then common travel currencies. */
    public static final List<Currency> ALL = Collections.unmodifiableList(Arrays.asList(
            new Currency("USD", "US Dollar", "$", 2),
            new Currency("EUR", "Euro", "€", 2),
            new Currency("GBP", "British Pound", "£", 2),
            new Currency("THB", "Thai Baht", "฿", 2),
            new Currency("JPY", "Japanese Yen", "¥", 0),
            new Currency("CNY", "Chinese Yuan", "¥", 2),
            new Currency("INR", "Indian Rupee", "₹", 2),
            new Currency("AUD", "Australian Dollar", "$", 2),
            new Currency("CAD", "Canadian Dollar", "$", 2),
            new Currency("SGD", "Singapore Dollar", "$", 2),
            new Currency("HKD", "Hong Kong Dollar", "$", 2),
            new Currency("MYR", "Malaysian Ringgit", "RM", 2),
            new Currency("VND", "Vietnamese Dong", "₫", 0),
            new Currency("KRW", "South Korean Won", "₩", 0),
            new Currency("IDR", "Indonesian Rupiah", "Rp", 0),
            new Currency("PHP", "Philippine Peso", "₱", 2),
            new Currency("TWD", "New Taiwan Dollar", "NT$", 2),
            new Currency("LAK", "Lao Kip", "₭", 0),
            new Currency("KHR", "Cambodian Riel", "៛", 0),
            new Currency("NZD", "New Zealand Dollar", "$", 2),
            new Currency("CHF", "Swiss Franc", "CHF", 2)
    ));

    private TripCurrency() { }

    // --------------------------------------------------------
    // LOOKUP
    // --------------------------------------------------------

    /** The currency with this code, or null. */
    public static Currency byCode(String code) {
        if (code == null) return null;
        for (Currency c : ALL) {
            if (c.code.equalsIgnoreCase(code.trim())) return c;
        }
        return null;
    }

    /** Index in {@link #ALL}, or -1. */
    public static int indexOf(String code) {
        Currency c = byCode(code);
        return c == null ? -1 : ALL.indexOf(c);
    }

    /**
     * Code for the Settings currency, which the app stores only as a symbol.
     * "$" is ambiguous; the first match (USD) is used.
     */
    public static String codeForSymbol(String symbol) {
        if (symbol != null) {
            for (Currency c : ALL) {
                if (c.symbol.equals(symbol.trim())) return c.code;
            }
        }
        return "USD";
    }

    /** True when this currency uses the same symbol as the Settings currency. */
    public static boolean matchesSettings(String code, String settingsSymbol) {
        Currency c = byCode(code);
        return c != null && settingsSymbol != null && c.symbol.equals(settingsSymbol.trim());
    }

    // --------------------------------------------------------
    // ARITHMETIC
    // --------------------------------------------------------

    /** foreign / rate, rounded half-up to the home currency's decimals. */
    public static double toHome(double foreignAmount, double rate, String homeCode) {
        if (rate <= 0) throw new IllegalArgumentException("rate must be positive");
        Currency home = byCode(homeCode);
        int dp = home == null ? 2 : home.decimals;
        return BigDecimal.valueOf(foreignAmount)
                .divide(BigDecimal.valueOf(rate), dp, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /** Parses a typed rate; null unless it is a positive number. Commas allowed. */
    public static Double parseRate(String s) {
        if (s == null) return null;
        String t = s.trim().replace(",", "");
        if (t.isEmpty()) return null;
        try {
            double d = Double.parseDouble(t);
            if (Double.isNaN(d) || Double.isInfinite(d) || d <= 0) return null;
            return d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Rates equal to 6 significant figures (so 25450 and 25450.0000001 match). */
    public static boolean sameRate(double a, double b) {
        if (a <= 0 || b <= 0) return false;
        return Math.abs(a - b) <= Math.max(a, b) * 1e-6;
    }

    /**
     * Sum of foreign amounts, or null if any is missing (e.g. entries made
     * before the currency was switched on); a partial foreign total would mislead.
     */
    public static Double foreignTotal(Collection<Double> foreignAmounts) {
        if (foreignAmounts == null || foreignAmounts.isEmpty()) return null;
        BigDecimal sum = BigDecimal.ZERO;
        for (Double d : foreignAmounts) {
            if (d == null) return null;
            sum = sum.add(BigDecimal.valueOf(d));
        }
        return sum.doubleValue();
    }

    // --------------------------------------------------------
    // TEXT
    // --------------------------------------------------------

    /** "500,000 ₫", "20.00 $": number, space, symbol, like the rest of the app. */
    public static String format(double amount, String code) {
        return format(amount, code, false);
    }

    /** As {@link #format(double, String)}, or with the code instead of the symbol. */
    public static String format(double amount, String code, boolean useCode) {
        Currency c = byCode(code);
        int dp = c == null ? 2 : c.decimals;
        String sym = c == null ? String.valueOf(code) : (useCode ? c.code : c.symbol);
        return number(amount, dp, dp) + " " + sym;
    }

    /**
     * Whether a trip should show codes instead of symbols: when home and trip
     * currencies share a symbol (USD/CAD "$", JPY/CNY "¥") symbols can't tell
     * them apart.
     */
    public static boolean useCodes(String homeCode, String foreignCode) {
        Currency h = byCode(homeCode), f = byCode(foreignCode);
        return h != null && f != null && !h.code.equals(f.code) && h.symbol.equals(f.symbol);
    }

    /** "1 USD = 25,450 VND", "1 THB = 0.21 CNY". */
    public static String rateLabel(String homeCode, double rate, String foreignCode) {
        return "1 " + homeCode + " = " + rateNumber(rate) + " " + foreignCode;
    }

    /** A rate with grouping and only the decimals it needs (up to 6). */
    public static String rateNumber(double rate) {
        return number(rate, 0, 6);
    }

    private static String number(double v, int minDp, int maxDp) {
        DecimalFormat f = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
        f.setMinimumFractionDigits(minDp);
        f.setMaximumFractionDigits(maxDp);
        f.setRoundingMode(RoundingMode.HALF_UP);
        return f.format(v);
    }
}
