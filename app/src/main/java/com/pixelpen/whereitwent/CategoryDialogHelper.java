package com.pixelpen.whereitwent;

import android.app.AlertDialog;
import android.content.Context;
import java.util.List;

public class CategoryDialogHelper {

    public static void showManageCategoriesDialog(Context context) {
        List<String> categories = CategoryManager.getOrderedCategories(context);

        new AlertDialog.Builder(context)
                .setTitle("Manage Categories")
                .setItems(categories.toArray(new String[0]), (dialog, which) -> {
                    // You can plug in your existing edit / delete logic here later
                })
                .setPositiveButton("Add", (d, w) -> {
                    // placeholder – call your AddCategoryDialog here
                })
                .setNegativeButton("Close", null)
                .show();
    }
}
