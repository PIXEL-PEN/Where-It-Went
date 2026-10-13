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
        return new AccountItemEntity(1, date, millis, "x", cat, amt, null);
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
        CategorySummary s = CategorySummary.build(ex, null, "2026-10");

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
        CategorySummary s = CategorySummary.build(ex, null, "2026-03");
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
        CategorySummary s = CategorySummary.build(null, items, "2026-10");
        assertEquals(75.0, s.accounts.get("Repairs"), 0.001);
        assertEquals(7.0, s.accounts.get("Uncategorised"), 0.001);
        assertFalse(s.accounts.containsKey("Tax"));
        assertEquals(82.0, s.accountsTotal, 0.001);
        assertEquals(82.0, s.grandTotal(), 0.001);
    }

    @Test
    public void januaryStarterGetsCalendarYears() {
        List<Expense> ex = Collections.singletonList(exp("Food", 1, "2026-01-01"));
        assertEquals(Collections.singletonList("2026-12"),
                CategorySummary.anchoredPeriodEnds(ex, null, "2026-10"));

        // In January 2027 a second, fresh year appears, newest first
        assertEquals(Arrays.asList("2027-12", "2026-12"),
                CategorySummary.anchoredPeriodEnds(ex, null, "2027-01"));

        CategorySummary y = CategorySummary.build(ex, null, "2026-12");
        assertEquals("Jan 2026 – Dec 2026", y.periodLabel());
        assertEquals("In progress · 10 of 12 months", y.statusLabel("2026-10"));
        assertEquals("In progress · 12 of 12 months", y.statusLabel("2026-12"));
        assertEquals("Complete", y.statusLabel("2027-01"));
    }

    @Test
    public void mayStarterGetsMayToApril() {
        List<AccountItemEntity> it = Collections.singletonList(item("A", 1, "20 May 2026", 0));
        List<String> ends = CategorySummary.anchoredPeriodEnds(null, it, "2026-10");
        assertEquals(Collections.singletonList("2027-04"), ends);

        CategorySummary s = CategorySummary.build(null, it, ends.get(0));
        assertEquals("May 2026 – Apr 2027", s.periodLabel());
        assertEquals("In progress · 6 of 12 months", s.statusLabel("2026-10"));

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
        String csv = CategorySummary.build(ex, null, "2026-10").toCsv("2026-11");
        assertTrue(csv.startsWith("Category Summary,Nov 2025 – Oct 2026,Complete\n"));
        assertTrue(csv.contains("Daily Living,\"Food, drink\",12.50\n"));
        assertTrue(csv.contains("Daily Living,TOTAL,112.50\n"));
        assertTrue(csv.contains("Accounts,TOTAL,0.00\n"));
        assertTrue(csv.contains("All,GRAND TOTAL,112.50\n"));
    }

    @Test
    public void htmlEscapesCategoryNames() {
        String html = CategorySummary.build(
                Collections.singletonList(exp("<b>Kids & Pets</b>", 3, "2026-10-01")),
                null, "2026-10").toHtml("$", "2026-11");
        assertTrue(html.contains("&lt;b&gt;Kids &amp; Pets&lt;/b&gt;"));
        assertTrue(html.contains("No records in this period")); // empty Accounts section
        assertTrue(html.contains("Grand total: 3.00 $"));
        assertTrue(html.contains("<i>Complete</i>"));
    }
}
