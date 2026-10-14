package com.pixelpen.whereitwent;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reading and writing Where It Went data files (v3.2).
 *
 * Understands three kinds of file:
 *  - Daily Living item export   ("Category,Item,Amount,Date")
 *  - Accounts item export       ("AccountType,AccountName,Date,Item,Category,Amount,Note",
 *                                 v3.3 adds ",ForeignAmount,ForeignCurrency,Rate,HomeCurrency,Journey")
 *  - Full backup                ("Where It Went backup,2,…" then one record per line)
 *
 * Full backup version 2 (v3.3) adds trip currencies and journeys. The new
 * fields are appended to the end of existing lines, so version 1 files read
 * as before (no trip details) and 3.2 can still read a version 2 file,
 * ignoring the extra fields.
 *
 * Item exports written before v3.2 did not quote their fields, so a comma inside
 * an item or note shifts the columns. Those rows are read by anchoring on the
 * columns whose position is certain (category / type / name at the start,
 * date / amount at the end) and joining whatever lies between.
 *
 * Pure Java, no Android imports, so it can be unit-tested off-device.
 */
public final class BackupFormat {

    public static final String DL_HEADER = "Category,Item,Amount,Date";
    public static final String ACC_HEADER = "AccountType,AccountName,Date,Item,Category,Amount,Note";
    public static final String BACKUP_MAGIC = "Where It Went backup";
    public static final String ACC_HEADER_V2 = ACC_HEADER
            + ",ForeignAmount,ForeignCurrency,Rate,HomeCurrency,Journey";
    public static final int BACKUP_VERSION = 2;

    private static final String ARCHIVED_SUFFIX = " (archived)";

    private BackupFormat() { }

    // --------------------------------------------------------
    // RECORDS
    // --------------------------------------------------------

    public enum Kind { DAILY_LIVING_CSV, ACCOUNTS_CSV, FULL_BACKUP, SUMMARY_CSV, UNKNOWN }

    public static final class ExpenseRec {
        public final String isoDate, category, item;
        public final double amount;
        public ExpenseRec(String isoDate, String category, String item, double amount) {
            this.isoDate = isoDate; this.category = category; this.item = item; this.amount = amount;
        }
    }

    public static final class AccountRec {
        public final String name, type;
        public final boolean archived;
        // v3.3 trip details (null / 0 = none)
        public String foreignCurrency, homeCurrency, journey;
        public double rate;
        public AccountRec(String name, String type, boolean archived) {
            this.name = name; this.type = type; this.archived = archived;
        }
        public boolean hasTripDetails() {
            return notBlank(foreignCurrency) || notBlank(homeCurrency) || notBlank(journey);
        }
    }

    public static final class AccountItemRec {
        public final String accountName, isoDate, item, category, note;
        public final double amount;
        // v3.3: amount as entered in the trip currency, and its rate (null = none)
        public Double foreignAmount, rate;
        public AccountItemRec(String accountName, String isoDate, String item,
                              String category, double amount, String note) {
            this.accountName = accountName; this.isoDate = isoDate; this.item = item;
            this.category = category; this.amount = amount; this.note = note;
        }
    }

    /** Everything read from one file. */
    public static final class Parsed {
        public Kind kind = Kind.UNKNOWN;
        public final List<ExpenseRec> expenses = new ArrayList<>();
        public final List<AccountRec> accounts = new ArrayList<>();
        public final List<AccountItemRec> accountItems = new ArrayList<>();
        /** Category name -> tag (full backup only), in file order. */
        public final Map<String, String> categories = new LinkedHashMap<>();
        public String currencySymbol = null;
        /** Settings currency code (full backup v2), e.g. "USD"; null in older files. */
        public String currencyCode = null;
        /** 1-based line numbers that could not be read. */
        public final List<Integer> badLines = new ArrayList<>();
    }

    // --------------------------------------------------------
    // PARSE
    // --------------------------------------------------------

    public static Parsed parse(String text) {
        Parsed p = new Parsed();
        if (text == null) return p;

        if (text.startsWith("﻿")) text = text.substring(1);       // BOM
        String[] lines = text.split("\r\n|\n|\r", -1);

        int first = 0;
        while (first < lines.length && lines[first].trim().isEmpty()) first++;
        if (first >= lines.length) return p;

        String header = lines[first].trim();
        boolean accountsV2 = false;
        List<String> headerFields = split(header);
        String h0 = headerFields.isEmpty() ? "" : headerFields.get(0);

        if (header.equalsIgnoreCase(DL_HEADER)) {
            p.kind = Kind.DAILY_LIVING_CSV;
        } else if (header.equalsIgnoreCase(ACC_HEADER)) {
            p.kind = Kind.ACCOUNTS_CSV;
        } else if (header.equalsIgnoreCase(ACC_HEADER_V2)) {
            p.kind = Kind.ACCOUNTS_CSV;
            accountsV2 = true;
        } else if (h0.equals(BACKUP_MAGIC)) {
            p.kind = Kind.FULL_BACKUP;
        } else if (h0.equals("Category Summary")) {
            p.kind = Kind.SUMMARY_CSV;
            return p;
        } else {
            return p;
        }

        for (int i = first + 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().isEmpty()) continue;
            boolean ok;
            switch (p.kind) {
                case DAILY_LIVING_CSV: ok = parseDailyLivingRow(line, p); break;
                case ACCOUNTS_CSV:     ok = parseAccountsRow(line, p, accountsV2); break;
                default:               ok = parseBackupRow(line, p);      break;
            }
            if (!ok) p.badLines.add(i + 1);
        }
        return p;
    }

    /** Category,Item,Amount,Date — item may contain unquoted commas in old exports. */
    private static boolean parseDailyLivingRow(String line, Parsed p) {
        List<String> f = split(line);
        if (f.size() < 4) return false;

        int n = f.size();
        String category = f.get(0).trim();
        String iso = DateUtils.toIso(f.get(n - 1).trim());
        Double amount = parseAmount(f.get(n - 2));
        String item = cleanText(joinRange(f, 1, n - 2));

        if (iso == null || amount == null) return false;
        if (category.isEmpty()) category = "Other";
        p.expenses.add(new ExpenseRec(iso, category, item, amount));
        return true;
    }

    /**
     * AccountType,AccountName,Date,Item,Category,Amount,Note.
     * Old exports: name or item may contain commas; notes had commas removed.
     * The date is found as the first field (from index 2) that reads as a date.
     */
    private static boolean parseAccountsRow(String line, Parsed p, boolean v2) {
        List<String> f = split(line);
        // v3.3 export: five trip columns at the end (written quoted, so exact)
        List<String> trip = null;
        if (v2) {
            if (f.size() < 12) return false;
            trip = new ArrayList<>(f.subList(f.size() - 5, f.size()));
            f = new ArrayList<>(f.subList(0, f.size() - 5));
        }
        if (f.size() < 7) return false;
        int n = f.size();

        int dateIdx = -1;
        String iso = null;
        for (int i = 2; i <= n - 5; i++) {
            iso = DateUtils.toIso(f.get(i).trim());
            if (iso != null) { dateIdx = i; break; }
        }
        if (dateIdx < 0) return false;

        Double amount = parseAmount(f.get(n - 2));
        if (amount == null) return false;

        String type = f.get(0).trim();
        String rawName = joinRange(f, 1, dateIdx).trim();
        String item = cleanText(joinRange(f, dateIdx + 1, n - 3));
        String category = f.get(n - 3).trim();
        String note = cleanText(f.get(n - 1));

        boolean archived = rawName.endsWith(ARCHIVED_SUFFIX);
        String name = archived
                ? rawName.substring(0, rawName.length() - ARCHIVED_SUFFIX.length()).trim()
                : rawName;
        if (name.isEmpty()) return false;

        AccountRec acc = new AccountRec(name, type, archived);
        AccountItemRec rec = new AccountItemRec(name, iso, item, category, amount, note);
        if (trip != null) {
            rec.foreignAmount = parseAmount(trip.get(0));
            rec.rate = parseAmount(trip.get(2));
            acc.foreignCurrency = blankToNull(trip.get(1));
            acc.homeCurrency = blankToNull(trip.get(3));
            acc.journey = blankToNull(trip.get(4));
            if (rec.foreignAmount == null || rec.rate == null || rec.rate <= 0) {
                rec.foreignAmount = null;
                rec.rate = null;
            }
        }
        AccountRec kept = addAccountOnce(p, acc);
        // The trip's current rate: the rate of its latest entry in the file
        if (rec.rate != null && (kept.rate <= 0 || newerThanAll(p, name, iso))) kept.rate = rec.rate;
        if (kept.foreignCurrency == null) kept.foreignCurrency = acc.foreignCurrency;
        if (kept.homeCurrency == null) kept.homeCurrency = acc.homeCurrency;
        if (kept.journey == null) kept.journey = acc.journey;
        p.accountItems.add(rec);
        return true;
    }

    /** True if no item already read for this account is dated after {@code iso}. */
    private static boolean newerThanAll(Parsed p, String account, String iso) {
        for (AccountItemRec r : p.accountItems) {
            if (r.accountName.equalsIgnoreCase(account) && r.rate != null
                    && r.isoDate.compareTo(iso) > 0) return false;
        }
        return true;
    }

    private static boolean parseBackupRow(String line, Parsed p) {
        List<String> f = split(line);
        if (f.isEmpty()) return false;
        String rec = f.get(0);
        try {
            switch (rec) {
                case "SETTING":
                    if (f.size() < 3) return false;
                    if ("currency_symbol".equals(f.get(1))) p.currencySymbol = f.get(2);
                    if ("currency_code".equals(f.get(1))) p.currencyCode = blankToNull(f.get(2));
                    return true;
                case "CATEGORY":
                    if (f.size() < 3 || f.get(1).trim().isEmpty()) return false;
                    p.categories.put(f.get(1).trim(), f.get(2).trim());
                    return true;
                case "ACCOUNT":
                    if (f.size() < 4 || f.get(1).trim().isEmpty()) return false;
                    AccountRec a = new AccountRec(f.get(1).trim(), f.get(2).trim(),
                            "1".equals(f.get(3).trim()));
                    // v2: foreignCurrency, homeCurrency, rate, journey
                    if (f.size() >= 8) {
                        a.foreignCurrency = blankToNull(f.get(4));
                        a.homeCurrency = blankToNull(f.get(5));
                        Double r = parseAmount(f.get(6));
                        a.rate = r != null && r > 0 ? r : 0;
                        a.journey = blankToNull(f.get(7));
                    }
                    addAccountOnce(p, a);
                    return true;
                case "EXPENSE": {
                    if (f.size() < 5) return false;
                    String iso = DateUtils.toIso(f.get(1).trim());
                    Double amt = parseAmount(f.get(4));
                    if (iso == null || amt == null) return false;
                    String cat = f.get(2).trim().isEmpty() ? "Other" : f.get(2).trim();
                    p.expenses.add(new ExpenseRec(iso, cat, f.get(3), amt));
                    return true;
                }
                case "ACCOUNT_ITEM": {
                    if (f.size() < 7) return false;
                    String iso = DateUtils.toIso(f.get(2).trim());
                    Double amt = parseAmount(f.get(5));
                    if (iso == null || amt == null || f.get(1).trim().isEmpty()) return false;
                    AccountItemRec it = new AccountItemRec(f.get(1).trim(), iso, f.get(3),
                            f.get(4).trim(), amt, f.get(6));
                    // v2: foreignAmount, rate
                    if (f.size() >= 9) {
                        Double fa = parseAmount(f.get(7)), r = parseAmount(f.get(8));
                        if (fa != null && r != null && r > 0) { it.foreignAmount = fa; it.rate = r; }
                    }
                    p.accountItems.add(it);
                    return true;
                }
                default:
                    return false;
            }
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Adds the account unless one of that name is already listed; returns the listed one. */
    private static AccountRec addAccountOnce(Parsed p, AccountRec a) {
        for (AccountRec existing : p.accounts) {
            if (existing.name.equalsIgnoreCase(a.name)) return existing;
        }
        p.accounts.add(a);
        return a;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String blankToNull(String s) {
        return notBlank(s) ? s.trim() : null;
    }

    private static String opt(String s) {
        return s == null ? "" : s;
    }

    /** A rate or trip amount, written exactly ("" for none). */
    private static String exact(Double v) {
        if (v == null) return "";
        return BigDecimal.valueOf(v).stripTrailingZeros().toPlainString();
    }

    // --------------------------------------------------------
    // WRITE
    // --------------------------------------------------------

    /** Builds a full backup file. Lists may be empty but not null. */
    public static String writeBackup(String currencySymbol,
                                      Map<String, String> categories,
                                      List<AccountRec> accounts,
                                      List<ExpenseRec> expenses,
                                      List<AccountItemRec> accountItems,
                                      Date now) {
        return writeBackup(currencySymbol, null, categories, accounts, expenses, accountItems, now);
    }

    /** Full backup, version 2: with the Settings currency code and trip details. */
    public static String writeBackup(String currencySymbol, String currencyCode,
                                      Map<String, String> categories,
                                      List<AccountRec> accounts,
                                      List<ExpenseRec> expenses,
                                      List<AccountItemRec> accountItems,
                                      Date now) {
        StringBuilder sb = new StringBuilder();
        sb.append(row(BACKUP_MAGIC, String.valueOf(BACKUP_VERSION),
                new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(now)));
        if (currencySymbol != null) sb.append(row("SETTING", "currency_symbol", currencySymbol));
        if (currencyCode != null) sb.append(row("SETTING", "currency_code", currencyCode));
        for (Map.Entry<String, String> c : categories.entrySet()) {
            sb.append(row("CATEGORY", c.getKey(), c.getValue()));
        }
        for (AccountRec a : accounts) {
            if (a.hasTripDetails() || a.rate > 0) {
                sb.append(row("ACCOUNT", a.name, a.type, a.archived ? "1" : "0",
                        opt(a.foreignCurrency), opt(a.homeCurrency),
                        a.rate > 0 ? exact(a.rate) : "", opt(a.journey)));
            } else {
                sb.append(row("ACCOUNT", a.name, a.type, a.archived ? "1" : "0"));
            }
        }
        for (ExpenseRec e : expenses) {
            sb.append(row("EXPENSE", e.isoDate, e.category, e.item, plainAmount(e.amount)));
        }
        for (AccountItemRec it : accountItems) {
            if (it.foreignAmount != null && it.rate != null) {
                sb.append(row("ACCOUNT_ITEM", it.accountName, it.isoDate, it.item, it.category,
                        plainAmount(it.amount), it.note, exact(it.foreignAmount), exact(it.rate)));
            } else {
                sb.append(row("ACCOUNT_ITEM", it.accountName, it.isoDate, it.item, it.category,
                        plainAmount(it.amount), it.note));
            }
        }
        return sb.toString();
    }

    /** One line of the Accounts "All items" CSV export (v3.3 columns included). */
    public static String accountsCsvRow(String type, String displayName, String date,
                                        String item, String category, double amount, String note,
                                        Double foreignAmount, Double rate,
                                        String foreignCurrency, String homeCurrency,
                                        String journey) {
        boolean trip = foreignAmount != null && rate != null;
        return row(type, displayName, date, opt(item), opt(category), plainAmount(amount), opt(note),
                trip ? exact(foreignAmount) : "", opt(foreignCurrency),
                trip ? exact(rate) : "", opt(homeCurrency), opt(journey));
    }

    // --------------------------------------------------------
    // CSV HELPERS (shared with the item exports)
    // --------------------------------------------------------

    /** One CSV line, fields quoted when needed, ending in "\n". */
    public static String row(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(quote(fields[i]));
        }
        return sb.append('\n').toString();
    }

    public static String quote(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")
                || (!s.isEmpty() && (s.charAt(0) == ' ' || s.charAt(s.length() - 1) == ' '))) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    /** Splits one CSV line, honouring double quotes. Unquoted lines split on every comma. */
    public static List<String> split(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else inQuotes = false;
                } else {
                    cur.append(c);
                }
            } else if (c == '"' && cur.length() == 0) {
                inQuotes = true;
            } else if (c == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    /** Exact, human-readable amount: 1500.5 -> "1500.5", 1.0E7 -> "10000000". */
    public static String plainAmount(double v) {
        BigDecimal bd = BigDecimal.valueOf(v).stripTrailingZeros();
        if (bd.scale() < 0) bd = bd.setScale(0);
        return bd.toPlainString();
    }

    static Double parseAmount(String s) {
        if (s == null) return null;
        String t = s.trim().replace(",", "");
        if (t.isEmpty()) return null;
        try {
            double d = Double.parseDouble(t);
            if (Double.isNaN(d) || Double.isInfinite(d)) return null;
            return d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String joinRange(List<String> f, int from, int toExclusive) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < toExclusive; i++) {
            if (i > from) sb.append(',');
            sb.append(f.get(i));
        }
        return sb.toString();
    }

    /** Old exports wrote a missing value as the text "null". */
    private static String cleanText(String s) {
        if (s == null) return "";
        String t = s.trim();
        return "null".equals(t) ? "" : t;
    }
}
