package com.pixelpen.whereitwent;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Keeps a screen's content clear of the navigation bar (v3.4). On newer
 * Android the app draws edge to edge, so without this the bottom of a
 * screen sits underneath the bar and gets clipped.
 */
public final class EdgeInsets {

    private EdgeInsets() { }

    /** Pads the bottom of the activity's root view by the navigation bar's height. */
    public static void padBottom(Activity a) {
        ViewGroup content = a.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) return;
        View root = content.getChildAt(0);
        final int base = root.getPaddingBottom();
        // A scrolling page keeps scrolling under the bar; only its end is lifted
        if (root instanceof ScrollView) ((ScrollView) root).setClipToPadding(false);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    base + bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
