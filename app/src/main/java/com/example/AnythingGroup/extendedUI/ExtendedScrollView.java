package com.example.AnythingGroup.extendedUI;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

public class ExtendedScrollView extends ScrollView {
    public interface OnScrollListener {
        void onEndReached(ExtendedScrollView scrollView);
    }

    private OnScrollListener mOnScrollListener;

    public ExtendedScrollView(Context context) {
        this(context, null, 0);
    }

    public ExtendedScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExtendedScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onScrollChanged(int x, int y, int oldX, int oldY) {
        super.onScrollChanged(x, y, oldX, oldY);

        if (this.getChildCount() > 0) {
            View view = (View) this.getChildAt(this.getChildCount() - 1);
            int dy = (view.getBottom() - (this.getHeight() + this.getScrollY()));

            if (dy == 0 && mOnScrollListener != null) {
                mOnScrollListener.onEndReached(ExtendedScrollView.this);
            }
        }
    }

    public void setOnScrollListener(OnScrollListener mOnEndScrollListener) {
        this.mOnScrollListener = mOnEndScrollListener;
    }
}
