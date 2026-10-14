package com.pixelpen.whereitwent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Full backup v2 and the Accounts CSV with trip currencies (v3.3). */
public class BackupV2Test {

    private static BackupFormat.AccountRec vietnam() {
        BackupFormat.AccountRec a = new BackupFormat.AccountRec("Vietnam", "travel", false);
        a.foreignCurrency = "VND";
        a.homeCurrency = "THB";
        a.rate = 771.6;
        a.journey = "SE Asia 2026";
        return a;
    }

    private static BackupFormat.AccountRec usVisit() {   // kept in dollars, no conversion
        BackupFormat.AccountRec a = new BackupFormat.AccountRec("US visit", "travel", false);
        a.homeCurrency = "USD";
        return a;
    }

    private static BackupFormat.AccountItemRec dinner() {
        BackupFormat.AccountItemRec it = new BackupFormat.AccountItemRec(
                "Vietnam", "2026-10-02", "Dinner", "Meals", 2000.0, "Pho, \"Thin\"");
        it.foreignAmount = 1_560_000.0;
        it.rate = 780.0;
        return it;
    }

    private static String backup() {
        Map<String, String> cats = new LinkedHashMap<>();
        cats.put("Groceries", "Necessity");
        BackupFormat.AccountRec condo = new BackupFormat.AccountRec("Condo", "project", false);
        BackupFormat.AccountItemRec tiles = new BackupFormat.AccountItemRec(
                "Condo", "2026-09-01", "Tiles", "Materials", 1200.0, "");
        BackupFormat.AccountItemRec burger = new BackupFormat.AccountItemRec(
                "US visit", "2026-10-05", "Burger", "Meals", 18.5, "");
        return BackupFormat.writeBackup("₫", "VND", cats,
                Arrays.asList(condo, vietnam(), usVisit()),
                Collections.<BackupFormat.ExpenseRec>emptyList(),
                Arrays.asList(tiles, dinner(), burger), new Date());
    }

    @Test
    public void tripDetailsSurviveARoundTrip() {
        String text = backup();
        assertTrue(text.startsWith("Where It Went backup,2,"));
        BackupFormat.Parsed p = BackupFormat.parse(text);
        assertTrue(p.badLines.isEmpty());
        assertEquals("VND", p.currencyCode);
        assertEquals("₫", p.currencySymbol);

        BackupFormat.AccountRec condo = p.accounts.get(0), vn = p.accounts.get(1), us = p.accounts.get(2);
        assertFalse(condo.hasTripDetails());
        assertEquals("VND", vn.foreignCurrency);
        assertEquals("THB", vn.homeCurrency);
        assertEquals(771.6, vn.rate, 0);
        assertEquals("SE Asia 2026", vn.journey);
        assertNull(us.foreignCurrency);
        assertEquals("USD", us.homeCurrency);
        assertEquals(0, us.rate, 0);

        BackupFormat.AccountItemRec tiles = p.accountItems.get(0), d = p.accountItems.get(1);
        assertNull(tiles.foreignAmount);
        assertEquals(1_560_000.0, d.foreignAmount, 0);
        assertEquals(780.0, d.rate, 0);
        assertEquals(2000.0, d.amount, 0);
        assertEquals("Pho, \"Thin\"", d.note);
    }

    @Test
    public void ordinaryLinesKeepTheVersion1Shape() {
        // So 3.2 can still read accounts and entries from a version 2 backup
        String text = backup();
        assertTrue(text.contains("\nACCOUNT,Condo,project,0\n"));
        assertTrue(text.contains("\nACCOUNT_ITEM,Condo,2026-09-01,Tiles,Materials,1200,\n"));
        assertTrue(text.contains("\nACCOUNT,Vietnam,travel,0,VND,THB,771.6,SE Asia 2026\n"));
    }

    @Test
    public void version1BackupsStillRead() {
        String v1 = "Where It Went backup,1,2026-10-01\n"
                + "SETTING,currency_symbol,฿\n"
                + "ACCOUNT,Japan,travel,0\n"
                + "ACCOUNT_ITEM,Japan,2026-09-03,Hotel,Lodging,6000,\n";
        BackupFormat.Parsed p = BackupFormat.parse(v1);
        assertTrue(p.badLines.isEmpty());
        assertNull(p.currencyCode);
        assertFalse(p.accounts.get(0).hasTripDetails());
        assertNull(p.accountItems.get(0).foreignAmount);
    }

    @Test
    public void restoringIntoAnEmptyAppCreatesTripsAndSetsTheExactCurrency() {
        BackupFormat.Parsed p = BackupFormat.parse(backup());
        ImportPlan plan = ImportPlan.build(p, null, null, null,
                Collections.<String, String>emptyMap(), "$", "USD", "Fixed");
        assertEquals(3, plan.accountsToCreate.size());
        assertEquals("SE Asia 2026", plan.accountsToCreate.get(1).journey);
        assertEquals("VND", plan.currencyCodeToApply);
        assertEquals(1_560_000.0, plan.accountItemsToAdd.get(1).foreignAmount, 0);
        assertTrue(plan.describe("$").contains("Trip currencies and journeys: Vietnam, US visit"));

        // Same code already: nothing to apply, even if the symbol was saved differently
        ImportPlan same = ImportPlan.build(p, null, null, null,
                Collections.<String, String>emptyMap(), "VND", "VND", "Fixed");
        assertNull(same.currencyCodeToApply);
        assertNull(same.currencyToApply);
    }

    @Test
    public void existingAccountsOnlyGetDetailsThatChangeNoAmounts() {
        BackupFormat.Parsed p = BackupFormat.parse(backup());

        AccountEntity vnEmpty = new AccountEntity("Vietnam", "travel", false);
        vnEmpty.id = 1;
        AccountEntity usWithEntries = new AccountEntity("US visit", "travel", false);
        usWithEntries.id = 2;
        usWithEntries.journey = "Home leave";   // already has a journey: kept
        AccountItemEntity coffee = new AccountItemEntity(2, "04 Oct 2026", 0, "Coffee", "Meals", 4, null);

        ImportPlan plan = ImportPlan.build(p, null, Arrays.asList(vnEmpty, usWithEntries),
                Collections.singletonList(coffee), Collections.<String, String>emptyMap(),
                "₫", "VND", "Fixed");

        assertEquals(1, plan.tripUpdates.size());   // US visit: nothing safe to change
        ImportPlan.TripUpdate u = plan.tripUpdates.get(0);
        assertEquals(1, u.accountId);
        assertTrue(u.currency);
        assertTrue(u.journey);
        assertTrue(plan.describe("₫").contains("only the details listed above are updated"));
    }

    @Test
    public void accountsCsvCarriesTripColumns() {
        String csv = BackupFormat.ACC_HEADER_V2 + "\n"
                + BackupFormat.accountsCsvRow("travel", "Vietnam", "01 Oct 2026", "Taxi, airport",
                        "Transportation", 500, "", 390_000.0, 780.0, "VND", "THB", "SE Asia 2026")
                + BackupFormat.accountsCsvRow("travel", "Vietnam", "03 Oct 2026", "Dinner",
                        "Meals", 2000, "Pho", 1_560_000.0, 790.0, "VND", "THB", "SE Asia 2026")
                + BackupFormat.accountsCsvRow("travel", "Vietnam", "02 Oct 2026", "Visa",
                        "Other", 800, "", null, null, "VND", "THB", "SE Asia 2026")
                + BackupFormat.accountsCsvRow("project", "Condo", "01 Sep 2026", "Tiles",
                        "Materials", 1200, "", null, null, null, null, null);
        BackupFormat.Parsed p = BackupFormat.parse(csv);
        assertEquals(BackupFormat.Kind.ACCOUNTS_CSV, p.kind);
        assertTrue(p.badLines.isEmpty());
        assertEquals(4, p.accountItems.size());
        assertEquals("Taxi, airport", p.accountItems.get(0).item);
        assertEquals(390_000.0, p.accountItems.get(0).foreignAmount, 0);
        assertNull(p.accountItems.get(2).foreignAmount);

        BackupFormat.AccountRec vn = p.accounts.get(0);
        assertEquals("VND", vn.foreignCurrency);
        assertEquals("THB", vn.homeCurrency);
        assertEquals("SE Asia 2026", vn.journey);
        assertEquals(790.0, vn.rate, 0);   // the latest entry's rate
        assertFalse(p.accounts.get(1).hasTripDetails());
    }

    @Test
    public void oldAccountsCsvStillReads() {
        List<String> lines = Arrays.asList(
                BackupFormat.ACC_HEADER,
                "travel,Japan,03 Sep 2026,Hotel,Lodging,6000,");
        BackupFormat.Parsed p = BackupFormat.parse(lines.get(0) + "\n" + lines.get(1) + "\n");
        assertEquals(1, p.accountItems.size());
        assertFalse(p.accounts.get(0).hasTripDetails());
    }
}
