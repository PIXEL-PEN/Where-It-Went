package com.pixelpen.whereitwent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CategorySummaryTest {

    private static Expense exp(String cat, double amt, String date) {
        Expense e = new Expense();
        e.category = cat;
        e.description = "x";
        e.amount = amt;
        e.date = date;
        return e;
    }

    private static AccountItemEntity item(String cat, double amt, String date, long millis) {
        return item(1, cat, amt, date, millis);
    }

    private static AccountItemEntity item(long acct, String cat, double amt, String date, long millis) {
        return new AccountItemEntity(acct, date, millis, "x", cat, amt, null);
    }

    private static AccountEntity acct(long id, String name, boolean archived) {
        AccountEntity a = new AccountEntity(name, "Project", archived);
        a.id = id;
        return a;
    }

    @Test
    public void addMonthsCrossesYears() {
        assertEquals("2025-11", CategorySummary.addMonths("2026-10", -11));
        assertEquals("2026-01", CategorySummary.addMonths("2025-12", 1));
        assertEquals("2024-12", CategorySummary.addMonths("2025-01", -1));
        assertEquals("2023-10", CategorySummary.addMonths("2026-10", -36));
    }

    @Test
    public void periodIsTwelveInclusiveMonths() {
        List<Expense> ex = Arrays.asList(
                exp("Food", 10, "2025-10-31"),   // just before period
                exp("Food", 20, "2025-11-01"),   // first day of period
                exp("food", 5, "2026-10-13"),    // last month, case-insensitive merge
                exp("Rent", 100, "2026-10-31"),  // last day of period
                exp("Rent", 999, "2026-11-01")   // just after period
        );
        CategorySummary s = CategorySummary.build(ex, null, null, "2026-10");

        assertEquals("2025-11", s.startYm);
        assertEquals("Nov 2025 – Oct 2026", s.periodLabel());
        assertEquals(2, s.dailyLiving.size());
        assertEquals(25.0, s.dailyLiving.get("Food"), 0.001);
        assertEquals(100.0, s.dailyLiving.get("Rent"), 0.001);
        assertEquals(125.0, s.dailyLivingTotal, 0.001);
        assertTrue(s.accounts.isEmpty());
    }

    @Test
    public void legacyDateFormatsAreRecognised() {
        List<Expense> ex = Arrays.asList(
                exp("Food", 1, "14 Mar 2026"),
                exp("Food", 2, "4 Mar. 2026"));
        CategorySummary s = CategorySummary.build(ex, null, null, "2026-03");
        assertEquals(3.0, s.dailyLivingTotal, 0.001);
    }

    @Test
    public void accountsUseDateStringThenMillisFallback() {
        long mar2026 = 1773000000000L; // 8 Mar 2026 UTC
        List<AccountItemEntity> items = Arrays.asList(
                item("Repairs", 50, "05 Feb 2026", 0),
                item("Repairs", 25, null, mar2026),
                item("Tax", 300, "05 Feb 2024", 0),   // older than period
                item("", 7, "01 Jan 2026", 0)          // blank category
        );
        CategorySummary s = CategorySummary.build(null, items,
                Collections.singletonList(acct(1, "House", false)), "2026-10");
        assertEquals(1, s.accounts.size());
        CategorySummary.AccountGroup g = s.accounts.get(0);
        assertEquals("House", g.displayName());
        assertEquals(75.0, g.categories.get("Repairs"), 0.001);
        assertEquals(7.0, g.categories.get("Uncategorised"), 0.001);
        assertFalse(g.categories.containsKey("Tax"));
        assertEquals(82.0, g.total(), 0.001);
        assertEquals(82.0, s.accountsTotal, 0.001);
        assertEquals(82.0, s.grandTotal(), 0.001);
    }

    @Test
    public void accountsAreGroupedActiveFirstThenArchived() {
        List<AccountEntity> accts = Arrays.asList(
                acct(1, "Zeta trip", false),
                acct(2, "Condo", true),
                acct(3, "alpha build", false),
                acct(4, "Empty", false));            // no items in period -> not listed
        List<AccountItemEntity> items = Arrays.asList(
                item(1, "Lodging", 100, "02 Sep 2026", 0),
                item(1, "Lodging", 50, "03 Oct 2026", 0),
                item(2, "Materials", 900, "10 Sep 2026", 0),
                item(3, "Materials", 40, "11 Oct 2026", 0),
                item(3, "Labour", 60, "12 Oct 2026", 0),
                item(9, "Misc", 5, "12 Oct 2026", 0)); // account no longer exists
        CategorySummary s = CategorySummary.build(null, items, accts, "2027-08");

        assertEquals(4, s.accounts.size());
        assertEquals("alpha build", s.accounts.get(0).name);
        assertEquals("Unknown account", s.accounts.get(1).name);
        assertEquals("Zeta trip", s.accounts.get(2).name);
        assertEquals("Condo (archived)", s.accounts.get(3).displayName());

        assertEquals(100.0, s.accounts.get(0).total(), 0.001);
        assertEquals(2, s.accounts.get(0).categories.size());
        assertEquals(150.0, s.accounts.get(2).categories.get("Lodging"), 0.001);
        assertEquals(1155.0, s.accountsTotal, 0.001);
    }

    @Test
    public void itemsOfDeletedAccountsAreIgnored() {
        List<AccountEntity> accts = Collections.singletonList(acct(1, "House", false));
        List<AccountItemEntity> items = Arrays.asList(
                item(1, "Repairs", 50, "05 Oct 2026", 0),
                item(2, "Lodging", 900, "02 Sep 2026", 0));   // account 2 was deleted
        List<AccountItemEntity> kept = CategorySummary.withoutOrphans(items, accts);
        assertEquals(1, kept.size());

        // The deleted account's September item no longer moves the start of the year
        List<Expense> ex = Collections.singletonList(exp("Food", 1, "2026-10-01"));
        assertEquals(Collections.singletonList("2027-09"),
                CategorySummary.anchoredPeriodEnds(ex, kept, "2026-10"));
        CategorySummary s = CategorySummary.build(ex, kept, accts, "2027-09");
        assertEquals(1, s.accounts.size());
        assertEquals(50.0, s.accountsTotal, 0.001);
    }

    @Test
    public void tripInAnotherCurrencyIsKeptOutOfTotals() {
        AccountEntity vn = acct(1, "Vietnam trip", false);
        vn.foreignCurrency = "VND"; vn.homeCurrency = "THB"; vn.rate = 780;
        AccountEntity cn = acct(2, "China trip", false);
        cn.foreignCurrency = "CNY"; cn.homeCurrency = "USD"; cn.rate = 7.10;
        List<AccountItemEntity> items = Arrays.asList(
                item(1, "Lodging", 3000, "18 Sep 2026", 0),     // baht
                item(2, "Lodging", 180.28, "10 Aug 2026", 0));  // dollars
        CategorySummary s = CategorySummary.build(null, items, Arrays.asList(vn, cn), "2027-07", "THB");

        assertEquals(3000.0, s.accountsTotal, 0.001);           // dollars not added to baht
        assertEquals(3000.0, s.grandTotal(), 0.001);
        CategorySummary.AccountGroup china = s.accounts.get(0);
        assertEquals("China trip", china.name);
        assertTrue(china.separate);
        assertEquals("180.28 $", china.format(china.total(), "฿"));
        assertFalse(s.accounts.get(1).separate);
        assertEquals("3,000.00 ฿", s.accounts.get(1).format(3000, "฿"));

        String csv = s.toCsv("2026-10");
        assertTrue(csv, csv.contains("\"Accounts (USD, not in totals)\",China trip,SUBTOTAL,180.28\n"));
        assertTrue(csv, csv.contains("Accounts,,TOTAL,3000.00\n"));
        String html = s.toHtml("฿", "2026-10");
        assertTrue(html, html.contains("(in USD, not included in totals)"));
        assertTrue(html, html.contains("180.28 $"));

        // Without the Settings symbol (older callers) nothing is separated
        assertEquals(3180.28, CategorySummary.build(null, items, Arrays.asList(vn, cn), "2027-07")
                .accountsTotal, 0.001);
    }

    @Test
    public void csvAndHtmlListEachAccount() {
        List<AccountItemEntity> items = Arrays.asList(
                item(1, "Lodging", 100, "02 Oct 2026", 0),
                item(2, "Materials", 900, "10 Oct 2026", 0));
        List<AccountEntity> accts = Arrays.asList(
                acct(1, "Trip, Chiang Mai", false), acct(2, "Condo", true));
        CategorySummary s = CategorySummary.build(null, items, accts, "2027-08");

        String csv = s.toCsv("2026-10");
        assertTrue(csv.contains("Section,Account,Category,Total\n"));
        assertTrue(csv.contains("Accounts,\"Trip, Chiang Mai\",Lodging,100.00\n"));
        assertTrue(csv.contains("Accounts,\"Trip, Chiang Mai\",SUBTOTAL,100.00\n"));
        assertTrue(csv.contains("Accounts,Condo (archived),Materials,900.00\n"));
        assertTrue(csv.contains("Accounts,,TOTAL,1000.00\n"));
        assertTrue(csv.contains("All,,GRAND TOTAL,1000.00\n"));

        String html = s.toHtml("฿", "2026-10");
        assertTrue(html.contains("<h4>Trip, Chiang Mai</h4>"));
        assertTrue(html.contains("<h4>Condo (archived)</h4>"));
        assertTrue(html.contains("Sep – Oct 2026<br><i>In progress · 2 of 12 months · ends Aug 2027"));
    }

    @Test
    public void januaryStarterGetsCalendarYears() {
        List<Expense> ex = Collections.singletonList(exp("Food", 1, "2026-01-01"));
        assertEquals(Collections.singletonList("2026-12"),
                CategorySummary.anchoredPeriodEnds(ex, null, "2026-10"));

        // In January 2027 a second, fresh year appears, newest first
        assertEquals(Arrays.asList("2027-12", "2026-12"),
                CategorySummary.anchoredPeriodEnds(ex, null, "2027-01"));

        CategorySummary y = CategorySummary.build(ex, null, null, "2026-12");
        assertEquals("Jan – Dec 2026", y.periodLabel());
        assertEquals("Jan – Oct 2026", y.displayLabel("2026-10"));
        assertEquals("In progress · 10 of 12 months · ends Dec 2026", y.statusLabel("2026-10"));
        assertEquals("Jan – Dec 2026", y.displayLabel("2026-12"));
        assertEquals("In progress · 12 of 12 months · ends Dec 2026", y.statusLabel("2026-12"));
        assertEquals("Jan – Dec 2026", y.displayLabel("2027-01"));
        assertEquals("Complete", y.statusLabel("2027-01"));
        assertEquals("Jan 2026", y.displayLabel("2026-01"));
    }

    @Test
    public void mayStarterGetsMayToApril() {
        List<AccountItemEntity> it = Collections.singletonList(item("A", 1, "20 May 2026", 0));
        List<String> ends = CategorySummary.anchoredPeriodEnds(null, it, "2026-10");
        assertEquals(Collections.singletonList("2027-04"), ends);

        CategorySummary s = CategorySummary.build(null, it, null, ends.get(0));
        assertEquals("May 2026 – Apr 2027", s.periodLabel());
        assertEquals("May – Oct 2026", s.displayLabel("2026-10"));
        assertEquals("In progress · 6 of 12 months · ends Apr 2027", s.statusLabel("2026-10"));
        assertEquals("May 2026 – Jan 2027", s.displayLabel("2027-01"));
        assertEquals("May 2026 – Apr 2027", s.displayLabel("2027-05"));

        assertEquals(Arrays.asList("2028-04", "2027-04"),
                CategorySummary.anchoredPeriodEnds(null, it, "2027-05"));
    }

    @Test
    public void earliestRecordAcrossBothSectionsSetsTheAnchor() {
        List<Expense> ex = Collections.singletonList(exp("Food", 1, "2026-03-02"));
        List<AccountItemEntity> it = Collections.singletonList(item("A", 1, "10 Feb 2026", 0));
        assertEquals("2026-02", CategorySummary.earliestMonth(ex, it));
        assertEquals(Collections.singletonList("2027-01"),
                CategorySummary.anchoredPeriodEnds(ex, it, "2026-10"));
    }

    @Test
    public void noDataStartsAPeriodThisMonth() {
        assertEquals(Collections.singletonList("2027-09"),
                CategorySummary.anchoredPeriodEnds(null, null, "2026-10"));
        // Future-dated only: anchor falls back to the current month
        List<Expense> future = Collections.singletonList(exp("Food", 1, "2027-03-01"));
        assertEquals(Collections.singletonList("2027-09"),
                CategorySummary.anchoredPeriodEnds(future, null, "2026-10"));
    }

    @Test
    public void manyYearsListNewestFirst() {
        List<Expense> ex = Collections.singletonList(exp("Food", 1, "2023-01-15"));
        assertEquals(Arrays.asList("2026-12", "2025-12", "2024-12", "2023-12"),
                CategorySummary.anchoredPeriodEnds(ex, null, "2026-10"));
    }

    @Test
    public void csvEscapesAndTotals() {
        List<Expense> ex = Arrays.asList(
                exp("Food, drink", 12.5, "2026-10-01"),
                exp("Rent", 100, "2026-09-01"));
        String csv = CategorySummary.build(ex, null, null, "2026-10").toCsv("2026-11");
        assertTrue(csv.startsWith("Category Summary,Nov 2025 – Oct 2026,Complete\n"));
        assertEquals("Nov 2025 – Oct 2026", CategorySummary.build(null, null, null, "2026-10").displayLabel("2026-11"));
        assertTrue(csv.contains("Daily Living,,\"Food, drink\",12.50\n"));
        assertTrue(csv.contains("Daily Living,,TOTAL,112.50\n"));
        assertTrue(csv.contains("Accounts,,TOTAL,0.00\n"));
        assertTrue(csv.contains("All,,GRAND TOTAL,112.50\n"));
    }

    @Test
    public void htmlEscapesCategoryNames() {
        String html = CategorySummary.build(
                Collections.singletonList(exp("<b>Kids & Pets</b>", 3, "2026-10-01")),
                null, null, "2026-10").toHtml("$", "2026-11");
        assertTrue(html.contains("&lt;b&gt;Kids &amp; Pets&lt;/b&gt;"));
        assertTrue(html.contains("No records in this period")); // empty Accounts section
        assertTrue(html.contains("Grand total: 3.00 $"));
        assertTrue(html.contains("<i>Complete</i>"));
    }
}
