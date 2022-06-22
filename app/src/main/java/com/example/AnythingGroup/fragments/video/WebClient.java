package com.example.AnythingGroup.fragments.video;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;

// Отвечает за открытие видео в браузере в полноэранном режиме.
public class WebClient extends WebChromeClient {
    private View mCustomView;
    private CustomViewCallback mCustomViewCallback;
    private int mOriginalSystemUiVisibility;

    private final Activity activity;

    public static final int FULLSCREEN_SETTING =
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_IMMERSIVE;

    public WebClient(Activity activity){
        this.activity = activity;
    }

    @Override
    public void onShowCustomView(View view, CustomViewCallback customViewCallback) {
        Log.wtf("CustomView", "Show");
        if (this.mCustomView != null) {
            onHideCustomView();
            return;
        }
        this.mCustomView = view;
        this.mOriginalSystemUiVisibility = activity.getWindow().getDecorView().getSystemUiVisibility();
        this.mCustomViewCallback = customViewCallback;

        ((FrameLayout) activity.getWindow().getDecorView()).addView(
                this.mCustomView,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );
        activity.getWindow().getDecorView().setSystemUiVisibility(FULLSCREEN_SETTING);

        this.mCustomView.setOnSystemUiVisibilityChangeListener(visibility -> updateControls());
    }

    @Override
    public void onHideCustomView() {
        Log.wtf("CustomView", "Hide");

        ((FrameLayout) activity.getWindow().getDecorView()).removeView(this.mCustomView);

        this.mCustomView = null;

        activity.getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);

        this.mCustomViewCallback.onCustomViewHidden();
        this.mCustomViewCallback = null;
    }

    @Override
    public Bitmap getDefaultVideoPoster() {
        return Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
    }

    public void updateControls() {
        Log.wtf("CustomView", "updateControls");
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) this.mCustomView.getLayoutParams();
        params.bottomMargin = 0;
        params.topMargin = 0;
        params.leftMargin = 0;
        params.rightMargin = 0;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        this.mCustomView.setLayoutParams(params);
        activity.getWindow().getDecorView().setSystemUiVisibility(FULLSCREEN_SETTING);
    }
}
