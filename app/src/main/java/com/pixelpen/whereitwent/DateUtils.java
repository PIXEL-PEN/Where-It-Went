package com.pixelpen.whereitwent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    // --------------------------------------------
    // 1) INTERNAL SAVE FORMAT
    // --------------------------------------------
    public static final SimpleDateFormat ISO_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    // --------------------------------------------
    // 2) UI DISPLAY FORMAT (UPDATED — NO DOTS)
    // --------------------------------------------
    public static final SimpleDateFormat UI_FORMAT =
            new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);

    // --------------------------------------------
    // 3) ALL LEGACY FORMATS (EXPANDED)
    //    MUST include *both* dotted and non-dotted forms
    // --------------------------------------------
    private static final SimpleDateFormat[] LEGACY_FORMATS = new SimpleDateFormat[] {

            // A: "22 Nov 2025"
            new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),

            // B: "22 Nov. 2025"
            new SimpleDateFormat("dd MMM. yyyy", Locale.ENGLISH),

            // C: "07 Oct 2025"
            new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),

            // D: "07 Oct. 2025"
            new SimpleDateFormat("dd MMM. yyyy", Locale.ENGLISH),

            // E: "7 October 2025"
            new SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH),

            // F: "Wednesday 14 September 2025"
            new SimpleDateFormat("EEEE dd MMMM yyyy", Locale.ENGLISH),

            // G: "Wed September 14 2025"
            new SimpleDateFormat("EEE MMMM dd yyyy", Locale.ENGLISH),

            // H: "Wed. September 14 2025"
            new SimpleDateFormat("EEE. MMMM dd yyyy", Locale.ENGLISH)
    };

    // --------------------------------------------
// Convert *any* UI/legacy string → ISO yyyy-MM-dd
// --------------------------------------------
    public static String toIso(String legacyDate) {

        if (legacyDate == null || legacyDate.trim().isEmpty())
            return null;

        String cleaned = legacyDate.trim().replaceAll(" +", " ");

        // 1) Already in ISO yyyy-MM-dd → return directly
        if (cleaned.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return cleaned;
        }

        // 2) Try "22 Nov. 2025"
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("dd MMM. yyyy", Locale.ENGLISH);
            fmt.setLenient(true);
            Date d = fmt.parse(cleaned);
            return ISO_FORMAT.format(d);
        } catch (Exception ignored) {}

        // 3) Try "22 Nov 2025"
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
            fmt.setLenient(true);
            Date d = fmt.parse(cleaned);
            return ISO_FORMAT.format(d);
        } catch (Exception ignored) {}

        // 4) Try "7 October 2025"
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH);
            fmt.setLenient(true);
            Date d = fmt.parse(cleaned);
            return ISO_FORMAT.format(d);
        } catch (Exception ignored) {}

        // 5) Try "Wednesday 14 September 2025"
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("EEEE dd MMMM yyyy", Locale.ENGLISH);
            fmt.setLenient(true);
            Date d = fmt.parse(cleaned);
            return ISO_FORMAT.format(d);
        } catch (Exception ignored) {}

        // 6) Try "Wed. September 14 2025"
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("EEE. MMMM dd yyyy", Locale.ENGLISH);
            fmt.setLenient(true);
            Date d = fmt.parse(cleaned);
            return ISO_FORMAT.format(d);
        } catch (Exception ignored) {}

        // 7) Try "Wed September 14 2025"
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("EEE MMMM dd yyyy", Locale.ENGLISH);
            fmt.setLenient(true);
            Date d = fmt.parse(cleaned);
            return ISO_FORMAT.format(d);
        } catch (Exception ignored) {}

        // Parsing failed
        return null;
    }


    // --------------------------------------------
    // 5) Convert UI date → "MMM dd" (stacked month view)
    //    Accepts both dotted and non-dotted inputs
    // --------------------------------------------
    public static String toMonthStacked(String uiDate) {

        if (uiDate == null || uiDate.length() < 6) return uiDate;

        // Clean dotted month abbrev → non-dotted
        String cleaned = uiDate.replace(".", "");

        try {
            // Try non-dotted first
            Date d = UI_FORMAT.parse(cleaned);

            SimpleDateFormat stacked = new SimpleDateFormat("MMM dd", Locale.ENGLISH);
            return stacked.format(d);
        } catch (Exception e) {
            return cleaned;
        }
    }

    public static boolean matchesMonth(String monthLabel, String ym) {
        try {
            Date d = new SimpleDateFormat("MMM yyyy", Locale.ENGLISH).parse(monthLabel);
            String produced = new SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(d);
            return produced.equals(ym);
        } catch (Exception e) {
            return false;
        }
    }


    // --------------------------------------------
    // 6) ISO → Pretty UI ("22 Nov 2025")
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
