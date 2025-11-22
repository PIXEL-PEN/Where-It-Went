package com.pixelpen.whereitwent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    // --------------------------------------------
    // 1) PREFERRED SAVE FORMAT (INTERNAL)
    // --------------------------------------------
    public static final SimpleDateFormat ISO_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    // --------------------------------------------
    // 2) PREFERRED DISPLAY FORMAT (UI)
    // --------------------------------------------
    public static final SimpleDateFormat UI_FORMAT =
            new SimpleDateFormat("dd MMM. yyyy", Locale.ENGLISH);

    // --------------------------------------------
    // 3) ALL LEGACY FORMATS USED IN THIS PROJECT
    // --------------------------------------------
    private static final SimpleDateFormat[] LEGACY_FORMATS = new SimpleDateFormat[] {

            // A: "22 Nov. 2025"
            new SimpleDateFormat("dd MMM. yyyy", Locale.ENGLISH),

            // B: "Wed. September 14 2025"
            new SimpleDateFormat("EEE. MMMM dd yyyy", Locale.ENGLISH),

            // C: "07 Oct 2025"
            new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),

            // D: "07 Oct. 2025"
            new SimpleDateFormat("dd MMM. yyyy", Locale.ENGLISH),

            // E: "7 October 2025"
            new SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH),

            // F: "Wednesday 14 September 2025"
            new SimpleDateFormat("EEEE dd MMMM yyyy", Locale.ENGLISH)
    };

    // --------------------------------------------
    // 4) CLEAN → PARSE LEGACY → ISO
    // --------------------------------------------
    public static String toIso(String legacyDate) {

        if (legacyDate == null || legacyDate.trim().isEmpty())
            return null;

        String cleaned = legacyDate.trim().replaceAll(" +", " ");

        // Try every known format
        for (SimpleDateFormat fmt : LEGACY_FORMATS) {
            try {
                fmt.setLenient(true);
                Date d = fmt.parse(cleaned);
                return ISO_FORMAT.format(d);
            } catch (Exception ignored) {}
        }

        // Last chance: try natural English parsing
        try {
            return ISO_FORMAT.format(new Date(cleaned));
        } catch (Exception ignored) {}

        return null; // cannot parse
    }

    // --------------------------------------------
    // 5) UI FORMAT FOR MONTH VIEW STACKED LIST
    //    Input: "22 Nov. 2025"
    //    Output: "Nov 22" (NO period)
    // --------------------------------------------
    public static String toMonthStacked(String uiDate) {

        if (uiDate == null || uiDate.length() < 6) return uiDate;

        try {
            Date d = UI_FORMAT.parse(uiDate);    // e.g., "22 Nov. 2025"
            SimpleDateFormat stacked = new SimpleDateFormat("MMM dd", Locale.ENGLISH);
            return stacked.format(d).replace(".", ""); // ensure "Nov" not "Nov."
        } catch (Exception e) {
            return uiDate;
        }
    }

    // --------------------------------------------
    // 6) Convert ISO → Pretty UI ("22 Nov. 2025")
    // --------------------------------------------
    public static String isoToUi(String iso) {
        try {
            Date d = ISO_FORMAT.parse(iso);
            return UI_FORMAT.format(d);
        } catch (Exception e) {
            return iso;
        }
    }
}
