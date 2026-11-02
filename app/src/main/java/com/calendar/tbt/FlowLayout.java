package com.calendar.tbt;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Hàng avatars tự dồn hết dòng này sang dòng khác khi quá rộng.
 * Làm tay vì theme của app là AppCompat thường, không có ChipGroup của Material.
 */
public class FlowLayout extends ViewGroup {

    public FlowLayout(Context context) {
        this(context, null);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int available = Math.max(0, MeasureSpec.getSize(widthMeasureSpec)
                - getPaddingLeft() - getPaddingRight());
        int innerWidth = MeasureSpec.makeMeasureSpec(available, MeasureSpec.AT_MOST);
        int desiredHeight = 0;
        int rowHeight = 0;
        int x = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            child.measure(innerWidth, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            if (x > 0 && x + child.getMeasuredWidth() > available) {
                desiredHeight += rowHeight + dp(10);
                rowHeight = 0;
                x = 0;
            }
            rowHeight = Math.max(rowHeight, child.getMeasuredHeight());
            x += child.getMeasuredWidth() + dp(8);
        }
        desiredHeight += rowHeight + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l;
        int x = getPaddingLeft();
        int y = getPaddingTop();
        int rowHeight = 0;
        int horizontalSpacing = dp(8);
        int verticalSpacing = dp(10);
        int limit = width - getPaddingRight();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            int childWidth = child.getMeasuredWidth();
            if (x > getPaddingLeft() && x + childWidth > limit) {
                x = getPaddingLeft();
                y += rowHeight + verticalSpacing;
                rowHeight = 0;
            }
            child.layout(x, y, x + childWidth, y + child.getMeasuredHeight());
            x += childWidth + horizontalSpacing;
            rowHeight = Math.max(rowHeight, child.getMeasuredHeight());
        }
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
