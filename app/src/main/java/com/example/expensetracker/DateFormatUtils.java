package com.example.expensetracker;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateFormatUtils {

    public static String formatDate(Context context, Date d, String fallback) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String pattern = prefs.getString("date_format", fallback);
        try {
            return new SimpleDateFormat(pattern, Locale.ENGLISH).format(d);
        } catch (Exception e) {
            return new SimpleDateFormat(fallback, Locale.ENGLISH).format(d);
        }
    }
}
