package com.example.AnythingGroup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.WorkerParameters;

import com.example.AnythingGroup.fragments.video.VideoMediaController;
import com.example.AnythingGroup.fragments.video.WebClient;

import java.io.IOException;
import java.util.UUID;

public class VideoViewActivity extends AppCompatActivity {
    private UUID video_source_load_worker_id = null;

    // Ссылка на страницу с видео
    String video_player_reference;

    TextView error_view;

    ProgressBar progress_bar;

    // Браузер для обработки страниц с видео и для просмотра видео, встроенных через iframe
    // С sibnet ещё можно как-нибудь запусть через VideoView, но с ВК уже никак (пока не знаю как, возможно через VK API)
    WebView web_view;
    WebClient web_client;

    // Нужен для полноэкранного проигрывания видео
    // В него вставляется полноэкранный элемент видео из web_view
    public FrameLayout web_video_fullscreen_view;

    // Проигрыватель видео;
    // для отдельного случая, когда видео встроены с помощью собственного стриминга
    // В этом случае можно найти ссылку в коде страницы
    VideoView video_view;
    MediaPlayer video_media_player;
    int videoWidth = 0;
    int videoHeight = 0;


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_view);

        error_view = findViewById(R.id.error_view);
        progress_bar = findViewById(R.id.video_progress_bar);

        web_view = findViewById(R.id.web_view);
        web_video_fullscreen_view = findViewById(R.id.web_video_fullscreen_view);

        video_view = findViewById(R.id.video_view);

        WebSettings settings = web_view.getSettings();
        settings.setBlockNetworkImage(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setJavaScriptEnabled(true);

        web_client = new WebClient(this);
        web_view.setWebChromeClient(web_client);

        video_view.setMediaController(new VideoMediaController(this));
        video_view.setOnPreparedListener(mp -> {
            videoWidth = mp.getVideoWidth();
            videoHeight = mp.getVideoHeight();

            video_media_player = mp;

            updateVideoViewLayoutParams();
        });

        Bundle args = getIntent().getExtras();
        video_player_reference = args.getString("video_player_reference");

        Log.wtf("Video", video_player_reference);

        // Отключение всех элементов интерфейса,
        // кроме иконки прогресса
        error_view.setVisibility(View.GONE);
        web_view.setVisibility(View.GONE);
        video_view.setVisibility(View.GONE);

        progress_bar.setVisibility(View.VISIBLE);

        AppBase.video_source = null;
    }

    @Override
    public void onStart() {
        super.onStart();

        int end = video_player_reference.indexOf("-", 23);

        int id = Integer.parseInt(video_player_reference.substring(23, end));

        WorkRequest loadWorkRequest = new OneTimeWorkRequest
                .Builder(VideoSourceLoadWorker.class)
                .setInputData(new Data.Builder().putInt("video_id", id).build())
                .build();

        WorkManager workManager = WorkManager.getInstance(this);
        // Добавление процедуры в очередь выполнения
        workManager.enqueue(loadWorkRequest);

        video_source_load_worker_id = loadWorkRequest.getId();

        // Подключение функции для ожидания завершения загрузки
        workManager.getWorkInfoByIdLiveData(video_source_load_worker_id).observe(
                this,
                workInfo -> {
                    if (workInfo.getState() != WorkInfo.State.SUCCEEDED) return;
                    if (web_view == null) return;
                    if (AppBase.video_source == null) return;

                    web_view.post(() -> {
                        switch (AppBase.video_source.type){
                            case StreamingUrl:
                                // Отлючение иконки загрузки
                                progress_bar.setVisibility(View.GONE);

                                // Запуск видео
                                video_view.setVideoURI(Uri.parse(AppBase.video_source.data));
                                video_view.setVisibility(View.VISIBLE);
                                video_view.start();
                                break;

                            case Iframe:
                                WebViewClient web_visible = new WebViewClient() {
                                    public void onPageFinished(WebView view, String url) {
                                        progress_bar.setVisibility(View.GONE);
                                        Log.wtf("WebView", "Visible");
                                        view.setVisibility(View.VISIBLE);
                                    }

                                    @SuppressLint("WebViewClientOnReceivedSslError")
                                    @Override
                                    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error){
                                        handler.proceed();
                                    }
                                };

                                web_view.setWebViewClient(web_visible);

                                web_view.loadUrl(AppBase.video_source.data);
                                break;
                        }
                    });
                });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        updateVideoViewLayoutParams();
    }

    @Override
    public void onDestroy() {
        if (video_source_load_worker_id != null) {
            WorkManager.getInstance(this).cancelWorkById(video_source_load_worker_id);
            video_source_load_worker_id = null;
        }
        super.onDestroy();
    }

    public void updateVideoViewLayoutParams(){
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;

        float videoRatio = videoWidth / (float) videoHeight;
        float screenRatio = screenWidth / (float) screenHeight;

        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) video_view.getLayoutParams();
        if (screenRatio > videoRatio) {
            params.width = ConstraintLayout.LayoutParams.WRAP_CONTENT;
            params.height = 0;
        }
        else {
            params.width = 0;
            params.height = ConstraintLayout.LayoutParams.WRAP_CONTENT;
        }

        video_view.setLayoutParams(params);
    }


    // Показывает/прячет системные панели управления
    // (взято с официального примера: https://developer.android.google.cn/training/system-ui/immersive)
    // Полезная ссылка - https://developer.android.google.cn/training/gestures/edge-to-edge
    public void showSystemBars(boolean show) {
        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        // Remember that you should never show the action bar if the
        // status bar is hidden, so hide that too if necessary.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.hide();
        }

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );

        if (show){
            // Show both the status bar and the navigation bar
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
        }
        else{
            // Hide both the status bar and the navigation bar
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        }
    }

    public static class VideoSourceLoadWorker extends LoadWorker {
        public VideoSourceLoadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
        }

        @Override
        public Result Work(Data input) throws IOException {
            int video_id = input.getInt("video_id", -1);

            AppBase.video_source = Network.get_video_source(video_id);

            return null;
        }
    }
}