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

    // 🧩 Default categories (always Fixed and locked)
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
    // Save category list
    // -------------------------------
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
    // Load full category items
    // -------------------------------
    private static List<CategoryItem> getCategoryItems(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_CATEGORIES, null);
        List<CategoryItem> list = new ArrayList<>();

        if (json == null) {
            for (Map.Entry<String, String> e : DEFAULTS.entrySet()) {
                list.add(new CategoryItem(e.getKey(), e.getValue()));
            }
            saveCategoryList(context, list);
            return list;
        }

        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String name = obj.optString("name", "");
                String tag = obj.optString("tag", "Fixed");
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
                if (!trimmed.isEmpty()) list.add(new CategoryItem(trimmed, "Fixed"));
            }
            saveCategoryList(context, list);
        }

        return list;
    }

    // -------------------------------
    // Public: Get all category names (ordered)
    // -------------------------------
    public static List<String> getOrderedCategories(Context context) {
        List<String> names = new ArrayList<>();
        for (CategoryItem item : getCategoryItems(context)) names.add(item.name);
        return names;
    }

    // -------------------------------
    // Public: Get tag for a given category
    // -------------------------------
    public static String getTagForCategory(Context context, String categoryName) {
        // Always Fixed for default categories
        if (DEFAULTS.containsKey(categoryName)) return "Fixed";

        for (CategoryItem item : getCategoryItems(context)) {
            if (item.name.equals(categoryName)) return item.tag;
        }
        return "Fixed"; // fallback
    }

    // -------------------------------
    // Save a single category with tag (editable only if not default)
    // -------------------------------
    public static void saveCategoryWithTag(Context context, String name, String tag) {
        // Prevent overwriting default "Fixed" categories
        if (DEFAULTS.containsKey(name)) return;

        List<CategoryItem> list = getCategoryItems(context);
        boolean updated = false;

        for (CategoryItem item : list) {
            if (item.name.equalsIgnoreCase(name.trim())) {
                item.tag = tag;
                updated = true;
                break;
            }
        }

        if (!updated) {
            list.add(new CategoryItem(name.trim(), tag));
        }

        saveCategoryList(context, list);
    }


    // -------------------------------
// Reset everything to defaults
// -------------------------------
    public static void resetToDefault(Context context) {
        List<CategoryItem> list = new ArrayList<>();
        for (Map.Entry<String, String> e : DEFAULTS.entrySet()) {
            list.add(new CategoryItem(e.getKey(), e.getValue()));
        }
        saveCategoryList(context, list);
    }

    // -------------------------------
// Remove a category completely (by name, case-insensitive)
// -------------------------------
    public static void removeCategory(Context context, String name) {
        List<CategoryItem> list = getCategoryItems(context);
        List<CategoryItem> updated = new ArrayList<>();

        for (CategoryItem item : list) {
            if (!item.name.equalsIgnoreCase(name.trim())) {
                updated.add(item);
            }
        }
        saveCategoryList(context, updated);
    }







    // -------------------------------
// Check if category is a built-in Fixed one
// -------------------------------
    public static boolean isDefaultCategory(String categoryName) {
        if (categoryName == null) return false;
        String[] defaults = {"Groceries", "Rent", "Utilities", "Bills", "Transport"};
        for (String def : defaults) {
            if (def.equalsIgnoreCase(categoryName.trim())) {
                return true;
            }
        }
        return false;
    }

    // -------------------------------
// Internal data holder
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
