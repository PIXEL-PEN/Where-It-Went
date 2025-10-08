package com.example.expensetracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class DividerItemDecoration extends RecyclerView.ItemDecoration {
    private final Paint paint;

    public DividerItemDecoration(Context context) {
        paint = new Paint();
        paint.setColor(0xFFAAAAAA); // medium-dark gray, darker than #CCCCCC but lighter than #888888
        paint.setStrokeWidth(context.getResources().getDisplayMetrics().density * 0.75f); // ~0.75dp
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount - 1; i++) {
            View child = parent.getChildAt(i);
            float y = child.getBottom();
            c.drawLine(left, y, right, y, paint);
        }
    }
}
