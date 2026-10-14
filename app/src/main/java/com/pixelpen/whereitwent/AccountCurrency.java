package com.pixelpen.whereitwent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * How an account's amounts are shown (v3.3). Pure Java, unit-tested.
 *
 * An account's stored amounts are in its home currency: the Settings currency
 * for ordinary accounts, or the trip's home currency for a Travel account with
 * foreign currency. A trip whose home currency differs from Settings is
 * "separate": shown in its own currency and left out of mixed totals.
 */
public final class AccountCurrency {

    private AccountCurrency() { }

    /** True when the account's amounts are in a currency other than Settings'. */
    public static boolean isSeparate(AccountEntity a, String settingsCode) {
        return a != null && a.homeCurrency != null && !a.homeCurrency.isEmpty()
                && !a.homeCurrency.equalsIgnoreCase(settingsCode);
    }

    /** Codes rather than symbols when home and trip currencies share a symbol. */
    public static boolean useCodes(AccountEntity a) {
        return a != null && TripCurrency.useCodes(a.homeCurrency, a.foreignCurrency);
    }

    /** A stored (home-currency) amount, formatted for this account. */
    public static String home(AccountEntity a, double amount, String settingsSymbol) {
        if (a != null && a.homeCurrency != null && !a.homeCurrency.isEmpty()
                && TripCurrency.byCode(a.homeCurrency) != null) {
            return TripCurrency.format(amount, a.homeCurrency, useCodes(a));
        }
        return CurrencyUtils.format(amount, settingsSymbol);
    }

    /** An amount in the trip currency, e.g. "500,000 ₫". */
    public static String foreign(AccountEntity a, double amount) {
        return TripCurrency.format(amount, a.foreignCurrency, useCodes(a));
    }

    /** Whole-number quote for the account's current rate: "1 USD = 25,900 VND". */
    public static String rateLabel(AccountEntity a) {
        return TripCurrency.rateLabel(a.homeCurrency, a.rate, a.foreignCurrency);
    }

    /**
     * Total in the trip currency for these entries, or null when the account
     * has no foreign currency or any entry lacks a foreign amount.
     */
    public static Double foreignTotal(AccountEntity a, Collection<AccountItemEntity> items) {
        if (a == null || !a.hasForeignCurrency() || items == null) return null;
        List<Double> amounts = new ArrayList<>();
        for (AccountItemEntity it : items) {
            amounts.add(it.hasForeignAmount() ? it.foreignAmount : null);
        }
        return TripCurrency.foreignTotal(amounts);
    }

    /**
     * For an entry made at a rate other than the trip's current one, that rate
     * as a whole-number quote ("1 THB = 780 VND"); empty otherwise.
     */
    public static String rateNote(AccountEntity a, AccountItemEntity it) {
        if (a == null || !a.hasForeignCurrency() || it == null || !it.hasForeignAmount()) return "";
        if (TripCurrency.sameRate(a.rate, it.rate)) return "";
        return TripCurrency.rateLabel(a.homeCurrency, it.rate, a.foreignCurrency);
    }

    /** A converted (home-currency) amount on a trip, without cents: "3,000 ฿". */
    public static String homeWhole(AccountEntity a, double amount, String settingsSymbol) {
        if (a != null && a.homeCurrency != null && TripCurrency.byCode(a.homeCurrency) != null) {
            return TripCurrency.formatWhole(amount, a.homeCurrency, useCodes(a));
        }
        return TripCurrency.formatWhole(amount, TripCurrency.codeForSymbol(settingsSymbol), false);
    }

    /**
     * Converts a typed trip-currency amount for a new entry: sets
     * foreignAmount, rate and amount (home value). No-op for other accounts.
     */
    public static void applyForeign(AccountEntity a, AccountItemEntity it, double typedAmount) {
        if (a == null || !a.hasForeignCurrency()) return;
        it.foreignAmount = typedAmount;
        it.rate = a.rate;
        it.amount = TripCurrency.toHome(typedAmount, a.rate, a.homeCurrency);
    }

    /** Re-converts an edited entry at its own rate (the rate it was entered at). */
    public static void reapplyForeign(AccountEntity a, AccountItemEntity it, double typedAmount) {
        if (a == null || it == null || !it.hasForeignAmount()) return;
        it.foreignAmount = typedAmount;
        it.amount = TripCurrency.toHome(typedAmount, it.rate, a.homeCurrency);
    }
}
