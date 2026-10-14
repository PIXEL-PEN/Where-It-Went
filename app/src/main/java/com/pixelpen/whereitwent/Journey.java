package com.pixelpen.whereitwent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Journeys (v3.3): Travel accounts grouped under one name, e.g. a Vietnam
 * trip and a Cambodia trip in "SE Asia 2026". Each country keeps its own
 * account and currency; the journey adds them up in the home currency.
 * Pure Java, unit-tested.
 */
public final class Journey {

    private Journey() { }

    /** Trimmed name, or null for blank. */
    public static String clean(String name) {
        if (name == null) return null;
        String t = name.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }

    public static boolean same(String a, String b) {
        String x = clean(a), y = clean(b);
        return x != null && x.equalsIgnoreCase(y);
    }

    /** The trips in this journey (in the given order); empty when it has no name. */
    public static List<AccountEntity> members(List<AccountEntity> all, String journey) {
        List<AccountEntity> out = new ArrayList<>();
        if (clean(journey) == null) return out;
        for (AccountEntity a : all) {
            if ("travel".equals(a.type) && same(a.journey, journey)) out.add(a);
        }
        return out;
    }

    /** Journey names in use, sorted. */
    public static List<String> names(List<AccountEntity> all) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> out = new ArrayList<>();
        for (AccountEntity a : all) {
            String n = clean(a.journey);
            if (n != null && seen.add(n.toLowerCase())) out.add(n);
        }
        Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    /** The currency an account's stored amounts are in. */
    public static String homeOf(AccountEntity a, String settingsCode) {
        return a.homeCurrency != null && !a.homeCurrency.isEmpty() ? a.homeCurrency : settingsCode;
    }

    /** A journey's total in one home currency. */
    public static final class Total {
        public final double amount;
        public final int included;   // trips added up
        public final int excluded;   // trips kept in another home currency
        Total(double amount, int included, int excluded) {
            this.amount = amount; this.included = included; this.excluded = excluded;
        }
    }

    /**
     * Adds up the trips whose home currency is {@code homeCode}; the others
     * are counted as excluded, so baht and dollars are never mixed.
     *
     * @param homeTotals stored (home-currency) total per account id
     */
    public static Total total(List<AccountEntity> members, Map<Long, Double> homeTotals,
                              String homeCode, String settingsCode) {
        java.math.BigDecimal sum = java.math.BigDecimal.ZERO;
        int in = 0, out = 0;
        for (AccountEntity a : members) {
            if (!homeOf(a, settingsCode).equalsIgnoreCase(homeCode)) { out++; continue; }
            Double t = homeTotals.get(a.id);
            if (t != null) sum = sum.add(java.math.BigDecimal.valueOf(t));
            in++;
        }
        return new Total(sum.doubleValue(), in, out);
    }
}
