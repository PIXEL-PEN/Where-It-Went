package com.example.expensetracker;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

public class ExportUtils {

    /**
     * Export the raw database file to the user-chosen SAF location.
     *
     * @param context   The activity context
     * @param targetUri The Uri returned from ACTION_CREATE_DOCUMENT
     * @return true if copy succeeded
     */
    public static boolean exportDatabase(Context context, Uri targetUri) {
        try {
            File dbFile = context.getDatabasePath("expenses.db");

            try (FileInputStream fis = new FileInputStream(dbFile);
                 OutputStream os = context.getContentResolver().openOutputStream(targetUri)) {

                if (os == null) return false;

                byte[] buffer = new byte[8192];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }

                os.flush();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace(); // shows details in logcat
            return false;
        }
    }
}
