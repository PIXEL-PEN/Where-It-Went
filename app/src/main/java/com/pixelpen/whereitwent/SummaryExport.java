package com.pixelpen.whereitwent;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Shared export flow for the 12-month category summary (v3.1), used by
 * Settings → Export and by the Yearly Summary screen.
 *
 * CSV goes through the system "save file" picker, so the calling activity
 * must forward {@code onActivityResult} to {@link #onActivityResult}.
 */
public final class SummaryExport {

    public static final int REQ_SAVE_CSV = 3101;

    // Content waiting for the save picker to return
    private static String pendingCsv = null;

    private SummaryExport() { }

    /** Ask CSV or Email, then export. */
    public static void show(Activity activity, CategorySummary summary) {
        String nowYm = CategorySummary.currentYm();
        new AlertDialog.Builder(activity)
                .setTitle("Export Category Summary")
                .setMessage(summary.periodLabel() + "\n" + summary.statusLabel(nowYm)
                        + "\n\nChoose export method:")
                .setPositiveButton("Storage (CSV)", (d, w) -> saveCsv(activity, summary))
                .setNegativeButton("Email (HTML)", (d, w) -> email(activity, summary))
                .setNeutralButton("Cancel", null)
                .show();
    }

    private static void saveCsv(Activity activity, CategorySummary summary) {
        pendingCsv = summary.toCsv(CategorySummary.currentYm());

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "Summary_" + summary.fileTag() + ".csv");
        activity.startActivityForResult(intent, REQ_SAVE_CSV);
    }

    /** @return true if the result belonged to the summary export and was handled. */
    public static boolean onActivityResult(Activity activity, int requestCode,
                                           int resultCode, Intent data) {
        if (requestCode != REQ_SAVE_CSV) return false;

        String csv = pendingCsv;
        pendingCsv = null;

        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            return true; // user cancelled
        }
        if (csv == null) {
            Toast.makeText(activity, "Export failed: please try again",
                    Toast.LENGTH_LONG).show();
            return true;
        }

        try (OutputStream os = activity.getContentResolver().openOutputStream(data.getData())) {
            if (os != null) {
                os.write(csv.getBytes(StandardCharsets.UTF_8));
                Toast.makeText(activity, "Summary exported successfully",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(activity, "Export failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
        return true;
    }

    private static void email(Activity activity, CategorySummary summary) {
        try {
            File file = new File(activity.getExternalFilesDir(null),
                    "Summary_" + summary.fileTag() + ".html");

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(summary.toHtml(
                        AppPrefs.getCurrencySymbol(activity),
                        CategorySummary.currentYm()));
            }

            Uri uri = FileProvider.getUriForFile(activity,
                    activity.getPackageName() + ".fileprovider", file);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/html");
            intent.putExtra(Intent.EXTRA_SUBJECT,
                    "Where It Went – Category Summary " + summary.periodLabel());
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            activity.startActivity(Intent.createChooser(intent, "Send email..."));
        } catch (Exception e) {
            Toast.makeText(activity, "Export failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
}
