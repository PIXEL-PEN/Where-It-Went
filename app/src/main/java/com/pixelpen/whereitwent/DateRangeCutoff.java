package com.pixelpen.whereitwent;

import android.content.Context;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class DateRangeCutoff {

    // Supported display formats in the app:
    //  yyyy-MM-dd
    //  12 Nov 2025
    //  12 Nov. 2025
    //  12 November 2025
    //  12/11/2025 (dd/MM/yyyy)
    //  11/12/2025 (MM/dd/yyyy)
    private static final List<DateTimeFormatter> FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.ENGLISH).withResolverStyle(ResolverStyle.SMART),
            DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH).withResolverStyle(ResolverStyle.SMART),
            DateTimeFormatter.ofPattern("d MMM'.' uuuu", Locale.ENGLISH).withResolverStyle(ResolverStyle.SMART),
            DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH).withResolverStyle(ResolverStyle.SMART),
            DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ENGLISH).withResolverStyle(ResolverStyle.SMART),
            DateTimeFormatter.ofPattern("MM/dd/uuuu", Locale.ENGLISH).withResolverStyle(ResolverStyle.SMART)
    );

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.ENGLISH);

    private DateRangeCutoff() {}

    /** Start-of-month for the selected window (e.g., 3 months -> first day of month two months ago). */
    public static LocalDate getCutoffLocalDate(Context ctx) {
        int months = Math.max(1, DateRangePrefs.getMonths(ctx));
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return today.minusMonths(months - 1L).withDayOfMonth(1);
    }

    public static String getCutoffIsoString(Context ctx) {
        return getCutoffLocalDate(ctx).format(ISO);
    }

    /** In-memory filter: keep expenses with parsed date >= cutoff. */
    public static List<Expense> filterByMonths(Context ctx, List<Expense> input) {
        if (input == null) return new ArrayList<>();
        LocalDate cutoff = getCutoffLocalDate(ctx);
        List<Expense> out = new ArrayList<>(input.size());
        for (Expense e : input) {
            LocalDate d = parseExpenseDate(e.date);
            if (d != null && !d.isBefore(cutoff)) {
                out.add(e);
            }
        }
        return out;
    }

    /** Best-effort parse across known app formats. */
    private static LocalDate parseExpenseDate(String s) {
        if (s == null || s.isEmpty()) return null;
        String t = s.trim();
        for (DateTimeFormatter f : FORMATTERS) {
            try {
                return LocalDate.parse(t, f);
            } catch (Exception ignored) {}
        }
        return null;
    }
}
