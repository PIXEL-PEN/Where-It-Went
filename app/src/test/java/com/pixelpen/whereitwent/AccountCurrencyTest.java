package com.pixelpen.whereitwent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

public class AccountCurrencyTest {

    private static AccountEntity trip(String foreign, String home, double rate) {
        AccountEntity a = new AccountEntity("Vietnam trip", "travel", false);
        a.id = 1;
        a.foreignCurrency = foreign;
        a.homeCurrency = home;
        a.rate = rate;
        return a;
    }

    private static AccountItemEntity entry(double typed, AccountEntity a) {
        AccountItemEntity it = new AccountItemEntity(a.id, "02 Oct 2026", 0, "Dinner", "Meals", typed, null);
        AccountCurrency.applyForeign(a, it, typed);
        return it;
    }

    @Test
    public void newEntryIsConvertedAndKeepsItsRate() {
        AccountEntity vn = trip("VND", "THB", 780);
        AccountItemEntity dinner = entry(1_560_000, vn);
        assertEquals(1_560_000, dinner.foreignAmount, 0.0001);
        assertEquals(780, dinner.rate, 0.0001);
        assertEquals(2000.00, dinner.amount, 0.0001);

        // The trip's rate changes later: this entry keeps its own
        vn.rate = 790;
        assertEquals("@ 780", AccountCurrency.rateNote(vn, dinner));
        AccountItemEntity lunch = entry(395_000, vn);
        assertEquals(500.00, lunch.amount, 0.0001);
        assertEquals("", AccountCurrency.rateNote(vn, lunch));

        // Editing re-converts at the entry's own rate
        AccountCurrency.reapplyForeign(vn, dinner, 780_000);
        assertEquals(1000.00, dinner.amount, 0.0001);
        assertEquals(780, dinner.rate, 0.0001);
    }

    @Test
    public void ordinaryAccountsAreUntouched() {
        AccountEntity project = new AccountEntity("Condo", "project", false);
        AccountItemEntity it = new AccountItemEntity(1, "02 Oct 2026", 0, "Tiles", "Materials", 1200, null);
        AccountCurrency.applyForeign(project, it, 1200);
        assertNull(it.foreignAmount);
        assertEquals(1200, it.amount, 0.0001);
        assertEquals("1,200.00 ฿", AccountCurrency.home(project, 1200, "฿"));
        assertFalse(AccountCurrency.isSeparate(project, "THB"));
        assertNull(AccountCurrency.foreignTotal(project, Arrays.asList(it)));
    }

    @Test
    public void homeCurrencyFormattingAndSeparation() {
        AccountEntity vn = trip("VND", "THB", 780);
        AccountEntity cn = trip("CNY", "USD", 7.10);
        assertEquals("2,000.00 ฿", AccountCurrency.home(vn, 2000, "฿"));
        assertEquals("18.10 $", AccountCurrency.home(cn, 18.10, "฿"));
        assertFalse(AccountCurrency.isSeparate(vn, "THB"));
        assertTrue(AccountCurrency.isSeparate(cn, "THB"));
        // Same symbol is not enough: Australian vs US dollars are separate
        assertTrue(AccountCurrency.isSeparate(cn, "AUD"));
        assertFalse(AccountCurrency.isSeparate(cn, "USD"));
        assertEquals("1,560,000 ₫", AccountCurrency.foreign(vn, 1_560_000));
        assertEquals("1 USD = 7.1 CNY", AccountCurrency.rateLabel(cn));
    }

    @Test
    public void sharedSymbolsSwitchToCodes() {
        AccountEntity jp = trip("JPY", "CNY", 20.5);   // both "¥"
        assertEquals("2,050 JPY", AccountCurrency.foreign(jp, 2050));
        assertEquals("100.00 CNY", AccountCurrency.home(jp, 100, "฿"));
    }

    @Test
    public void foreignTotalNeedsEveryEntry() {
        AccountEntity vn = trip("VND", "THB", 780);
        AccountItemEntity a = entry(1_560_000, vn);
        AccountItemEntity b = entry(395_000, vn);
        assertEquals(1_955_000, AccountCurrency.foreignTotal(vn, Arrays.asList(a, b)), 0.0001);

        AccountItemEntity old = new AccountItemEntity(1, "01 Sep 2026", 0, "Visa", "Other", 800, null);
        assertNull(AccountCurrency.foreignTotal(vn, Arrays.asList(a, old)));

        vn.rate = 0;   // currency switched off
        assertNull(AccountCurrency.foreignTotal(vn, Arrays.asList(a, b)));
        assertEquals("", AccountCurrency.rateNote(vn, a));
    }
}
