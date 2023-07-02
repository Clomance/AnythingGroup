package com.example.AnythingGroup.fragments.video;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MediaController;

import com.example.AnythingGroup.R;
import com.example.AnythingGroup.VideoViewActivity;

// Собственный видеоконтроллер
// Нужен, чтобы добавить новые элементы управления,
// в данном случае: кнопка переключения полноэкранного режима.
public class VideoMediaController  extends MediaController {
    private boolean fullscreen = false;

    private VideoViewActivity activity;

    public VideoMediaController(Context context){
        super(context);
    }

    public VideoMediaController(VideoViewActivity activity) {
        super(activity);
        this.activity = activity;
    }

    @Override
    public void setAnchorView(View view) {
        super.setAnchorView(view);

        // Основа иконки переключения полноэкранного режима
        final ImageView fullscreen_image = new ImageView(super.getContext());

        // Параметры для вставки
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.END;
        params.rightMargin = 80;
        params.topMargin = 40;

        // Добавление в интерфейс
        addView(fullscreen_image, params);

        // Устанавливка иконки в зависимости от стартового режима
        if (fullscreen) {
            fullscreen_image.setImageResource(R.drawable.common_screen_white);
        }
        else {
            fullscreen_image.setImageResource(R.drawable.fullscreen_white);
        }

        // Действия при нажатии на иконку
        fullscreen_image.setOnClickListener(image_view -> {
            activity.showSystemBars(fullscreen);
            fullscreen = !fullscreen;
            if (fullscreen) {
                fullscreen_image.setImageResource(R.drawable.common_screen_white);
            }
            else {
                fullscreen_image.setImageResource(R.drawable.fullscreen_white);
            }
        });
    }
}
