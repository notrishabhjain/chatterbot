package com.taskflow.automate.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class SwipeCallback extends ItemTouchHelper.SimpleCallback {

    public interface SwipeActionListener {
        void onSwipeLeft(int position);
        void onSwipeRight(int position);
    }

    private final SwipeActionListener listener;
    private final Paint paint;

    public SwipeCallback(SwipeActionListener listener) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.listener = listener;
        this.paint = new Paint();
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        if (direction == ItemTouchHelper.LEFT) {
            listener.onSwipeLeft(position);
        } else if (direction == ItemTouchHelper.RIGHT) {
            listener.onSwipeRight(position);
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {

        View itemView = viewHolder.itemView;
        float itemHeight = itemView.getBottom() - itemView.getTop();

        if (dX < 0) {
            // Swiping left - Complete (Red)
            paint.setColor(Color.parseColor("#F44336"));
            RectF background = new RectF(
                    itemView.getRight() + dX,
                    itemView.getTop(),
                    itemView.getRight(),
                    itemView.getBottom());
            c.drawRect(background, paint);

            // Draw checkmark icon
            paint.setColor(Color.WHITE);
            paint.setTextSize(itemHeight * 0.4f);
            paint.setTextAlign(Paint.Align.CENTER);
            float textY = itemView.getTop() + (itemHeight / 2) + (paint.getTextSize() / 3);
            float textX = itemView.getRight() - (itemHeight * 0.5f);
            c.drawText("\u2713", textX, textY, paint);

        } else if (dX > 0) {
            // Swiping right - Snooze (Blue)
            paint.setColor(Color.parseColor("#2196F3"));
            RectF background = new RectF(
                    itemView.getLeft(),
                    itemView.getTop(),
                    itemView.getLeft() + dX,
                    itemView.getBottom());
            c.drawRect(background, paint);

            // Draw clock icon
            paint.setColor(Color.WHITE);
            paint.setTextSize(itemHeight * 0.4f);
            paint.setTextAlign(Paint.Align.CENTER);
            float textY = itemView.getTop() + (itemHeight / 2) + (paint.getTextSize() / 3);
            float textX = itemView.getLeft() + (itemHeight * 0.5f);
            c.drawText("\u23F0", textX, textY, paint);
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
