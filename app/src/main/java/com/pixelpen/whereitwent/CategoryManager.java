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

    // Canonical tag constants
    public static final String TAG_FIXED = "Fixed";
    public static final String TAG_BASIC = "Necessity";
    public static final String TAG_DISC  = "Discretionary";
    public static final String TAG_OFF   = "Off-Budget";

    // Built-in defaults (order preserved)
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();
    static {
        DEFAULTS.put("Groceries",  TAG_FIXED);
        DEFAULTS.put("Rent",       TAG_FIXED);
        DEFAULTS.put("Utilities",  TAG_FIXED);
        DEFAULTS.put("Bills",      TAG_FIXED);
        DEFAULTS.put("Transport",  TAG_FIXED);
        DEFAULTS.put("Other",      TAG_FIXED);
    }

    // ----- Public helpers for tags -----

    public static boolean isOffBudgetTag(String tag) {
        return TAG_OFF.equalsIgnoreCase(normalizeTag(tag));
    }

    public static boolean isCountedInDistribution(String tag) {
        return !isOffBudgetTag(tag);
    }

    public static String normalizeTag(String tag) {
        if (tag == null) return TAG_FIXED;
        String t = tag.trim();
        if (t.equalsIgnoreCase(TAG_FIXED)) return TAG_FIXED;
        if (t.equalsIgnoreCase(TAG_BASIC)) return TAG_BASIC;
        if (t.equalsIgnoreCase("Necessities")) return TAG_BASIC;
        if (t.equalsIgnoreCase(TAG_DISC)) return TAG_DISC;
        if (t.equalsIgnoreCase(TAG_OFF))  return TAG_OFF;
        return TAG_FIXED;
    }

    // ----- Persistence core -----

    private static void saveCategoryList(Context context, List<CategoryItem> list) {
        JSONArray array = new JSONArray();
        for (CategoryItem item : list) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("name", item.name);
                obj.put("tag", normalizeTag(item.tag));
                array.put(obj);
            } catch (JSONException ignored) {}
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CATEGORIES, array.toString()).apply();
    }

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
                String name = obj.optString("name", "").trim();
                String tag  = normalizeTag(obj.optString("tag", TAG_FIXED));
                if (!name.isEmpty()) list.add(new CategoryItem(name, tag));
            }
        } catch (JSONException e) {
            list.clear();
            String[] legacy = json.replace("[", "")
                    .replace("]", "")
                    .replace("\"", "")
                    .split(",");
            for (String raw : legacy) {
                String nm = raw.trim();
                if (!nm.isEmpty()) list.add(new CategoryItem(nm, TAG_FIXED));
            }
            ensureDefaultsPresent(list);
            saveCategoryList(context, list);
        }

        boolean changed = ensureDefaultsPresent(list);
        if (changed) saveCategoryList(context, list);

        return list;
    }

    private static boolean ensureDefaultsPresent(List<CategoryItem> list) {
        boolean changed = false;
        for (CategoryItem it : list) {
            if (DEFAULTS.containsKey(it.name)) {
                if (!TAG_FIXED.equals(it.tag)) {
                    it.tag = TAG_FIXED;
                    changed = true;
                }
            }
        }
        for (Map.Entry<String, String> e : DEFAULTS.entrySet()) {
            if (!containsName(list, e.getKey())) {
                list.add(new CategoryItem(e.getKey(), e.getValue()));
                changed = true;
            }
        }
        return changed;
    }

    private static boolean containsName(List<CategoryItem> list, String name) {
        for (CategoryItem it : list) {
            if (it.name.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    // ----- Public API -----

    public static List<String> getOrderedCategories(Context context) {
        List<CategoryItem> items = getCategoryItems(context);
        List<String> names = new ArrayList<>();

        for (String def : DEFAULTS.keySet()) {
            for (CategoryItem it : items) {
                if (it.name.equals(def)) {
                    names.add(it.name);
                    break;
                }
            }
        }
        for (CategoryItem it : items) {
            if (!DEFAULTS.containsKey(it.name)) names.add(it.name);
        }
        return names;
    }

    /** ✨ FIXED — now correctly returns Off-Budget tag */
    public static String getTagForCategory(Context context, String categoryName) {
        if (categoryName == null) return TAG_FIXED;
        String key = categoryName.trim();

        // NEW: explicitly map Off-Budget category name → Off-Budget tag
        if (key.equalsIgnoreCase("Off-Budget"))
            return TAG_OFF;

        if (DEFAULTS.containsKey(key)) return TAG_FIXED;
        for (CategoryItem item : getCategoryItems(context)) {
            if (item.name.equals(key)) return normalizeTag(item.tag);
        }
        return TAG_FIXED;
    }

    public static void saveCategoryWithTag(Context context, String name, String tag) {
        if (name == null || name.trim().isEmpty()) return;
        String nm = name.trim();
        if (DEFAULTS.containsKey(nm)) return;

        List<CategoryItem> list = getCategoryItems(context);
        boolean exists = false;
        for (CategoryItem item : list) {
            if (item.name.equals(nm)) {
                item.tag = normalizeTag(tag);
                exists = true;
                break;
            }
        }
        if (!exists) list.add(new CategoryItem(nm, normalizeTag(tag)));
        saveCategoryList(context, list);
    }

    public static void resetToDefault(Context context) {
        List<CategoryItem> list = new ArrayList<>();
        for (Map.Entry<String, String> e : DEFAULTS.entrySet()) {
            list.add(new CategoryItem(e.getKey(), e.getValue()));
        }
        saveCategoryList(context, list);
    }

    public static void removeCategory(Context context, String name) {
        if (name == null || name.trim().isEmpty()) return;
        String nm = name.trim();
        if (DEFAULTS.containsKey(nm)) return;

        List<CategoryItem> list = getCategoryItems(context);
        List<CategoryItem> updated = new ArrayList<>();
        for (CategoryItem item : list) {
            if (!item.name.equalsIgnoreCase(nm)) {
                updated.add(item);
            }
        }
        saveCategoryList(context, updated);
    }

    public static boolean isDefaultCategory(String categoryName) {
        if (categoryName == null) return false;
        return DEFAULTS.containsKey(categoryName.trim());
    }

    private static class CategoryItem {
        String name;
        String tag;
        CategoryItem(String n, String t) {
            name = n;
            tag  = normalizeTag(t);
        }
    }
}
