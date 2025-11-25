package com.pixelpen.whereitwent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    // --------------------------------------------------------
    // PUBLIC: Convert ANY supported format → ISO yyyy-MM-dd
    // --------------------------------------------------------
    public static String toIso(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        raw = raw.trim();

        // 1) Already ISO
        if (raw.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return raw;
        }

        // 2) Try dd MMM yyyy  (25 Nov 2025)
        String[] fmts = {
                "dd MMM yyyy",
                "dd MMM. yyyy",
                "d MMM yyyy",
                "d MMM. yyyy",
                "dd MMMM yyyy",
                "d MMMM yyyy",
                "dd/MM/yyyy",
                "MM/dd/yyyy"
        };

        for (String f : fmts) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(f, Locale.ENGLISH);
                sdf.setLenient(false);
                Date d = sdf.parse(raw);
                if (d != null) {
                    SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                    return iso.format(d);
                }
            } catch (Exception ignore) {}
        }

        // Nothing matched
        return null;
    }

    // --------------------------------------------------------
    // PUBLIC: Convert ISO → UI "dd MMM yyyy"
    // --------------------------------------------------------
    public static String isoToUi(String iso) {
        if (iso == null) return null;

        // Already UI?
        if (iso.matches("\\d{2} \\w{3} ?\\.? \\d{4}")) {
            return iso.replace(".", "");
        }

        try {
            SimpleDateFormat i = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            SimpleDateFormat o = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
            Date d = i.parse(iso);
            return o.format(d);
        } catch (Exception e) {
            return iso;
        }
    }

    // --------------------------------------------------------
    // PUBLIC: Convert ISO → Stacked month/day like "Nov 25"
    // --------------------------------------------------------
    public static String toMonthStacked(String raw) {
        if (raw == null) return "";

        raw = raw.trim();

        // When input is ISO yyyy-MM-dd
        if (raw.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                Date d = iso.parse(raw);
                SimpleDateFormat m = new SimpleDateFormat("MMM", Locale.ENGLISH);
                SimpleDateFormat day = new SimpleDateFormat("dd", Locale.ENGLISH);
                return m.format(d) + " " + day.format(d);
            } catch (Exception e) {
                return "";
            }
        }

        // When input is already UI, e.g. "25 Nov 2025"
        if (raw.matches("\\d{1,2} \\w{3} ?\\.? \\d{4}")) {
            try {
                raw = raw.replace(".", "");
                SimpleDateFormat ui = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
                Date d = ui.parse(raw);
                SimpleDateFormat m = new SimpleDateFormat("MMM", Locale.ENGLISH);
                SimpleDateFormat day = new SimpleDateFormat("dd", Locale.ENGLISH);
                return m.format(d) + " " + day.format(d);
            } catch (Exception e) {
                return "";
            }
        }

        // Otherwise, try to convert via ISO
        String iso = toIso(raw);
        if (iso != null) return toMonthStacked(iso);

        return "";
    }
}
