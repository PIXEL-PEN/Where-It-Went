package com.pixelpen.whereitwent;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DateUtils {

    // Standard ISO date
    private static final SimpleDateFormat ISO_FMT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    // Display format
    private static final SimpleDateFormat UI_FMT =
            new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);

    // Legacy incoming formats your database historically used
    private static final String[] LEGACY_PATTERNS = new String[]{
            "dd MMM yyyy",
            "dd MMM. yyyy",
            "d MMM yyyy",
            "d MMM. yyyy",
            "d MMMM yyyy",
            "dd/MM/yyyy",
            "MM/dd/yyyy"
    };

    /**
     * Convert ANY known date format → ISO yyyy-MM-dd.
     * Returns null if not parseable.
     */
    public static String toIso(String raw) {
        if (raw == null) return null;

        // Already ISO?
        if (raw.matches("\\d{4}-\\d{2}-\\d{2}"))
            return raw;

        // Try legacy patterns
        for (String p : LEGACY_PATTERNS) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(p, Locale.ENGLISH);
                f.setLenient(false);
                Date d = f.parse(raw);
                return ISO_FMT.format(d);
            } catch (Exception ignore) {}
        }

        return null;
    }

    /**
     * ISO yyyy-MM-dd → UI "dd MMM yyyy"
     */
    public static String isoToUi(String iso) {
        try {
            Date d = ISO_FMT.parse(iso);
            return UI_FMT.format(d);
        } catch (Exception e) {
            return iso;
        }
    }

    /**
     * UI format "dd MMM yyyy" → stacked "Nov 24"
     */
    public static String toMonthStacked(String uiDate) {
        try {
            Date d = UI_FMT.parse(uiDate);

            SimpleDateFormat mmm = new SimpleDateFormat("MMM", Locale.ENGLISH);
            SimpleDateFormat dd  = new SimpleDateFormat("dd",  Locale.ENGLISH);

            return mmm.format(d) + " " + dd.format(d);
        } catch (Exception e) {
            return uiDate;
        }
    }
}
