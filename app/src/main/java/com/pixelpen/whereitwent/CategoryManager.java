package com.pixelpen.whereitwent;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CategoryManager {

    private static final String PREFS_NAME = "categories_prefs";
    private static final String KEY_CATEGORIES = "categories_json";

    // Default categories (all tagged as Fixed)
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();
    static {
        DEFAULTS.put("Groceries", "Fixed");
        DEFAULTS.put("Rent", "Fixed");
        DEFAULTS.put("Utilities", "Fixed");
        DEFAULTS.put("Bills", "Fixed");
        DEFAULTS.put("Transport", "Fixed");
        DEFAULTS.put("Other", "Fixed");
    }

    // -------------------------------
    // Save categories (names only)
    // -------------------------------
    public static void saveCategories(Context context, List<String> names) {
        JSONArray array = new JSONArray();
        for (String name : names) {
            String tag = DEFAULTS.containsKey(name) ? "Fixed" : "Fixed"; // start all as Fixed
            JSONObject obj = new JSONObject();
            try {
                obj.put("name", name);
                obj.put("tag", tag);
                array.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CATEGORIES, array.toString()).apply();
    }

    // -------------------------------
    // Save single category with tag
    // -------------------------------
    public static void saveCategoryWithTag(Context context, String name, String tag) {
        List<CategoryItem> list = getCategoryItems(context);
        boolean exists = false;
        for (CategoryItem item : list) {
            if (item.name.equals(name)) {
                item.tag = tag;
                exists = true;
                break;
            }
        }
        if (!exists) list.add(new CategoryItem(name, tag));
        saveCategoryList(context, list);
    }

    private static void saveCategoryList(Context context, List<CategoryItem> list) {
        JSONArray array = new JSONArray();
        for (CategoryItem item : list) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("name", item.name);
                obj.put("tag", item.tag);
                array.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CATEGORIES, array.toString()).apply();
    }

    // -------------------------------
    // Load ordered category names
    // -------------------------------
    public static List<String> getOrderedCategories(Context context) {
        List<CategoryItem> list = getCategoryItems(context);
        List<String> names = new ArrayList<>();
        for (CategoryItem item : list) names.add(item.name);
        return names;
    }

    // -------------------------------
    // Get tag for category
    // -------------------------------
    public static String getTagForCategory(Context context, String name) {
        for (CategoryItem item : getCategoryItems(context)) {
            if (item.name.equals(name)) return item.tag;
        }
        // default fallback — always Fixed
        return "Fixed";
    }

    // -------------------------------
    // Reset to default categories
    // -------------------------------
    public static void resetToDefault(Context context) {
        List<CategoryItem> list = new ArrayList<>();
        for (Map.Entry<String, String> e : DEFAULTS.entrySet()) {
            list.add(new CategoryItem(e.getKey(), "Fixed"));
        }
        saveCategoryList(context, list);
    }

    // -------------------------------
    // Get full category items
    // -------------------------------
    private static List<CategoryItem> getCategoryItems(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_CATEGORIES, null);
        List<CategoryItem> list = new ArrayList<>();

        if (json == null) {
            for (Map.Entry<String, String> e : DEFAULTS.entrySet()) {
                list.add(new CategoryItem(e.getKey(), "Fixed"));
            }
            saveCategoryList(context, list);
            return list;
        }

        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String name = obj.optString("name", "");
                String tag = obj.optString("tag", "Fixed"); // fallback Fixed
                if (!name.isEmpty()) list.add(new CategoryItem(name, tag));
            }
        } catch (JSONException e) {
            // Legacy fallback
            list.clear();
            String[] legacy = json.replace("[", "")
                    .replace("]", "")
                    .replace("\"", "")
                    .split(",");
            for (String raw : legacy) {
                String trimmed = raw.trim();
                if (!trimmed.isEmpty()) {
                    list.add(new CategoryItem(trimmed, "Fixed"));
                }
            }
            saveCategoryList(context, list);
        }

        return list;
    }



    // -------------------------------
    // Inner data holder
    // -------------------------------
    private static class CategoryItem {
        String name;
        String tag;

        CategoryItem(String n, String t) {
            name = n;
            tag = t;
        }
    }
}
