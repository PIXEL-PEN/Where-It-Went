package com.pixelpen.whereitwent;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Approximate exchange rates built into the app (v3.3), used only to
 * pre-fill the rate when a trip currency is set up. Offline; refreshed with
 * each app release. Source: the open fawazahmed0/currency-api dataset for
 * 2026-10-14 (market mid-rates; a bank's or exchange booth's rate will differ).
 *
 * Values are units of each currency per 1 US dollar, to 6 significant figures.
 */
public final class ApproxRates {

    /** When the table was taken, for the "approx." label. */
    public static final String AS_OF = "2026-10-14";
    public static final String AS_OF_LABEL = "Oct 2026";

    private static final Map<String, Double> PER_USD;
    static {
        Map<String, Double> r = new HashMap<>();
        r.put("USD", 1.0);
        r.put("EUR", 0.891987);
        r.put("GBP", 0.753304);
        r.put("THB", 33.566);
        r.put("JPY", 158.189);
        r.put("CNY", 6.69182);
        r.put("INR", 94.9475);
        r.put("AUD", 1.42868);
        r.put("CAD", 1.41979);
        r.put("SGD", 1.28042);
        r.put("HKD", 7.84816);
        r.put("MYR", 4.08204);
        r.put("VND", 25900.5);
        r.put("AED", 3.6725);
        r.put("ALL", 81.5795);
        r.put("AMD", 361.73);
        r.put("ARS", 1513.23);
        r.put("AZN", 1.7);
        r.put("BAM", 1.74458);
        r.put("BDT", 123.225);
        r.put("BGN", 1.74458);
        r.put("BHD", 0.376);
        r.put("BND", 1.28042);
        r.put("BOB", 11.715);
        r.put("BRL", 5.02743);
        r.put("BWP", 13.4811);
        r.put("CHF", 0.831824);
        r.put("CLP", 981.904);
        r.put("COP", 3185.2);
        r.put("CRC", 455.741);
        r.put("CZK", 21.7344);
        r.put("DKK", 6.66744);
        r.put("DOP", 61.2685);
        r.put("DZD", 134.472);
        r.put("EGP", 52.8191);
        r.put("ETB", 161.401);
        r.put("FJD", 2.24467);
        r.put("GEL", 2.59896);
        r.put("GHS", 11.691);
        r.put("GTQ", 7.64196);
        r.put("HNL", 26.8495);
        r.put("HUF", 324.522);
        r.put("IDR", 17894.7);
        r.put("ILS", 3.06769);
        r.put("ISK", 121.491);
        r.put("JMD", 158.348);
        r.put("JOD", 0.709);
        r.put("KES", 129.682);
        r.put("KHR", 4067.63);
        r.put("KRW", 1337.68);
        r.put("KWD", 0.311745);
        r.put("KZT", 448.909);
        r.put("LAK", 22323.8);
        r.put("LKR", 330.635);
        r.put("MAD", 9.8069);
        r.put("MMK", 2100.22);
        r.put("MNT", 3598.01);
        r.put("MOP", 8.0836);
        r.put("MUR", 47.6985);
        r.put("MVR", 15.46);
        r.put("MXN", 18.3706);
        r.put("NGN", 1324.27);
        r.put("NIO", 36.7923);
        r.put("NOK", 9.49743);
        r.put("NPR", 151.987);
        r.put("NZD", 1.77777);
        r.put("OMR", 0.385031);
        r.put("PEN", 3.44158);
        r.put("PHP", 62.8029);
        r.put("PKR", 276.843);
        r.put("PLN", 3.89775);
        r.put("PYG", 5715.14);
        r.put("QAR", 3.64);
        r.put("RON", 4.76066);
        r.put("RSD", 105.039);
        r.put("RUB", 83.8652);
        r.put("SAR", 3.75);
        r.put("SEK", 9.94672);
        r.put("TND", 2.99997);
        r.put("TRY", 49.3859);
        r.put("TTD", 6.80055);
        r.put("TWD", 32.1808);
        r.put("TZS", 2622.8);
        r.put("UAH", 45.0934);
        r.put("UGX", 4149.18);
        r.put("UYU", 40.2296);
        r.put("UZS", 11907.8);
        r.put("XAF", 585.105);
        r.put("XOF", 585.105);
        r.put("XPF", 106.442);
        r.put("ZAR", 16.462);
        r.put("ZMW", 19.9182);
        PER_USD = Collections.unmodifiableMap(r);
    }

    private ApproxRates() { }

    /**
     * Suggested rate "1 home = ? trip", rounded to 4 significant figures
     * (e.g. 1 THB = 771.6 VND, 1 USD = 6.692 CNY), or null if unknown.
     */
    public static Double suggest(String homeCode, String tripCode) {
        if (homeCode == null || tripCode == null) return null;
        Double h = PER_USD.get(homeCode.toUpperCase());
        Double t = PER_USD.get(tripCode.toUpperCase());
        if (h == null || t == null || h <= 0 || t <= 0 || homeCode.equalsIgnoreCase(tripCode)) return null;
        return new BigDecimal(t / h).round(new MathContext(4)).doubleValue();
    }

    /** True if the table has this currency. */
    public static boolean has(String code) {
        return code != null && PER_USD.containsKey(code.toUpperCase());
    }
}
