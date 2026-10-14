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

    /**
     * World currencies (v3.3). The 13 that Settings has always listed come
     * first, in the same order; the rest follow alphabetically by code.
     * Decimals are what people use day to day (0 for dong, yen, won, rupiah…;
     * 3 for the Gulf dinars).
     */
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
            new Currency("AED", "UAE Dirham", "AED", 2),
            new Currency("ALL", "Albanian Lek", "L", 2),
            new Currency("AMD", "Armenian Dram", "֏", 0),
            new Currency("ARS", "Argentine Peso", "$", 2),
            new Currency("AZN", "Azerbaijani Manat", "₼", 2),
            new Currency("BAM", "Bosnian Convertible Mark", "KM", 2),
            new Currency("BDT", "Bangladeshi Taka", "৳", 2),
            new Currency("BGN", "Bulgarian Lev", "лв", 2),
            new Currency("BHD", "Bahraini Dinar", "BD", 3),
            new Currency("BND", "Brunei Dollar", "$", 2),
            new Currency("BOB", "Bolivian Boliviano", "Bs", 2),
            new Currency("BRL", "Brazilian Real", "R$", 2),
            new Currency("BWP", "Botswana Pula", "P", 2),
            new Currency("CHF", "Swiss Franc", "CHF", 2),
            new Currency("CLP", "Chilean Peso", "$", 0),
            new Currency("COP", "Colombian Peso", "$", 0),
            new Currency("CRC", "Costa Rican Colón", "₡", 0),
            new Currency("CZK", "Czech Koruna", "Kč", 2),
            new Currency("DKK", "Danish Krone", "kr", 2),
            new Currency("DOP", "Dominican Peso", "RD$", 2),
            new Currency("DZD", "Algerian Dinar", "DA", 2),
            new Currency("EGP", "Egyptian Pound", "E£", 2),
            new Currency("ETB", "Ethiopian Birr", "Br", 2),
            new Currency("FJD", "Fijian Dollar", "$", 2),
            new Currency("GEL", "Georgian Lari", "₾", 2),
            new Currency("GHS", "Ghanaian Cedi", "GH₵", 2),
            new Currency("GTQ", "Guatemalan Quetzal", "Q", 2),
            new Currency("HNL", "Honduran Lempira", "L", 2),
            new Currency("HUF", "Hungarian Forint", "Ft", 0),
            new Currency("IDR", "Indonesian Rupiah", "Rp", 0),
            new Currency("ILS", "Israeli Shekel", "₪", 2),
            new Currency("ISK", "Icelandic Króna", "kr", 0),
            new Currency("JMD", "Jamaican Dollar", "$", 2),
            new Currency("JOD", "Jordanian Dinar", "JD", 3),
            new Currency("KES", "Kenyan Shilling", "KSh", 2),
            new Currency("KHR", "Cambodian Riel", "៛", 0),
            new Currency("KRW", "South Korean Won", "₩", 0),
            new Currency("KWD", "Kuwaiti Dinar", "KD", 3),
            new Currency("KZT", "Kazakhstani Tenge", "₸", 2),
            new Currency("LAK", "Lao Kip", "₭", 0),
            new Currency("LKR", "Sri Lankan Rupee", "Rs", 2),
            new Currency("MAD", "Moroccan Dirham", "MAD", 2),
            new Currency("MMK", "Myanmar Kyat", "K", 0),
            new Currency("MNT", "Mongolian Tögrög", "₮", 0),
            new Currency("MOP", "Macanese Pataca", "MOP$", 2),
            new Currency("MUR", "Mauritian Rupee", "Rs", 2),
            new Currency("MVR", "Maldivian Rufiyaa", "Rf", 2),
            new Currency("MXN", "Mexican Peso", "$", 2),
            new Currency("NGN", "Nigerian Naira", "₦", 2),
            new Currency("NIO", "Nicaraguan Córdoba", "C$", 2),
            new Currency("NOK", "Norwegian Krone", "kr", 2),
            new Currency("NPR", "Nepalese Rupee", "Rs", 2),
            new Currency("NZD", "New Zealand Dollar", "$", 2),
            new Currency("OMR", "Omani Rial", "OMR", 3),
            new Currency("PEN", "Peruvian Sol", "S/", 2),
            new Currency("PHP", "Philippine Peso", "₱", 2),
            new Currency("PKR", "Pakistani Rupee", "Rs", 2),
            new Currency("PLN", "Polish Złoty", "zł", 2),
            new Currency("PYG", "Paraguayan Guaraní", "₲", 0),
            new Currency("QAR", "Qatari Riyal", "QAR", 2),
            new Currency("RON", "Romanian Leu", "lei", 2),
            new Currency("RSD", "Serbian Dinar", "din", 2),
            new Currency("RUB", "Russian Ruble", "₽", 2),
            new Currency("SAR", "Saudi Riyal", "SAR", 2),
            new Currency("SEK", "Swedish Krona", "kr", 2),
            new Currency("TND", "Tunisian Dinar", "DT", 3),
            new Currency("TRY", "Turkish Lira", "₺", 2),
            new Currency("TTD", "Trinidad and Tobago Dollar", "$", 2),
            new Currency("TWD", "New Taiwan Dollar", "NT$", 2),
            new Currency("TZS", "Tanzanian Shilling", "TSh", 0),
            new Currency("UAH", "Ukrainian Hryvnia", "₴", 2),
            new Currency("UGX", "Ugandan Shilling", "USh", 0),
            new Currency("UYU", "Uruguayan Peso", "$", 2),
            new Currency("UZS", "Uzbekistani Som", "soʻm", 0),
            new Currency("XAF", "Central African CFA Franc", "FCFA", 0),
            new Currency("XOF", "West African CFA Franc", "CFA", 0),
            new Currency("XPF", "CFP Franc", "₣", 0),
            new Currency("ZAR", "South African Rand", "R", 2),
            new Currency("ZMW", "Zambian Kwacha", "ZK", 2)
    ));

    /** How many of {@link #ALL} were in the original Settings list. */
    public static final int ORIGINAL_SETTINGS_COUNT = 13;

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
     * Code for a currency symbol, for settings saved before v3.3 (which stored
     * only the symbol). "$" is ambiguous; the first match (USD) is used.
     */
    public static String codeForSymbol(String symbol) {
        if (symbol != null) {
            for (Currency c : ALL) {
                if (c.symbol.equals(symbol.trim())) return c.code;
            }
        }
        return "USD";
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

    // --------------------------------------------------------
    // WHOLE-NUMBER RATES (v3.3: "always whole denominations")
    // --------------------------------------------------------

    /**
     * A rate as people quote it: the stronger currency is the base, in a round
     * quantity (1, 10, 100…) chosen so the other side is a whole number of at
     * least 20 (so rounding to whole costs at most about 2.5%).
     * "1 USD = 25,900 VND", "1 USD = 34 THB", "10 CNY = 50 THB", "100 CAD = 104 USD".
     */
    public static final class Quote {
        public final String base, other;
        public final long qty;
        /** Exact amount of {@code other} for {@code qty} of {@code base}. */
        public final double value;

        Quote(String base, String other, long qty, double value) {
            this.base = base;
            this.other = other;
            this.qty = qty;
            this.value = value;
        }

        /** True when the home currency is the base (rate = value / qty). */
        public boolean homeIsBase(String homeCode) {
            return base.equalsIgnoreCase(homeCode);
        }
    }

    /** Bank-style quote for a rate stored as foreign units per 1 home unit. */
    public static Quote quote(String homeCode, double rate, String foreignCode) {
        if (rate <= 0) throw new IllegalArgumentException("rate must be positive");
        boolean homeBase = rate >= 1;
        double per = homeBase ? rate : 1 / rate;   // other units per 1 base unit
        long q = 1;
        while (q * per < 20 && q < 1_000_000_000L) q *= 10;
        return homeBase
                ? new Quote(homeCode, foreignCode, q, q * per)
                : new Quote(foreignCode, homeCode, q, q * per);
    }

    /** Stored rate (foreign per 1 home) from a quote's shape and a typed value. */
    public static double rateFromQuote(String homeCode, Quote shape, double value) {
        if (value <= 0) throw new IllegalArgumentException("value must be positive");
        return shape.homeIsBase(homeCode) ? value / shape.qty : shape.qty / value;
    }

    /** "1 USD = 25,900 VND", "10 CNY = 50 THB": whole numbers only. */
    public static String rateLabel(String homeCode, double rate, String foreignCode) {
        Quote q = quote(homeCode, rate, foreignCode);
        return number(q.qty, 0, 0) + " " + q.base + " = "
                + number(Math.round(q.value), 0, 0) + " " + q.other;
    }

    /** An amount rounded to whole units: "3,000 ฿", "19 $". For converted amounts. */
    public static String formatWhole(double amount, String code, boolean useCode) {
        Currency c = byCode(code);
        String sym = c == null ? String.valueOf(code) : (useCode ? c.code : c.symbol);
        return number(amount, 0, 0) + " " + sym;
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
