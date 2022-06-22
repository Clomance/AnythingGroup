package com.example.AnythingGroup;

import android.annotation.SuppressLint;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.work.WorkManager;

import com.example.AnythingGroup.fragments.video.VideoMediaController;
import com.example.AnythingGroup.fragments.video.WebClient;

import java.util.UUID;

public class VideoViewActivity extends AppCompatActivity {
    private UUID video_channel_load_worker_id = null;

    // Ссылка на страницу с видео
    String video_player_reference;
    // Ссылка на само видео или ссылка из iframe
    String video_reference;

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

    // Скрипт для получения кода страницы
    private final String js_get_body = "(function() { return (document.getElementsByTagName('body')[0].innerHTML); })();";

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

        loadVideoPlayerPage();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        updateVideoViewLayoutParams();
    }

    @Override
    public void onDestroy() {
        if (video_channel_load_worker_id != null) {
            WorkManager.getInstance(this).cancelWorkById(video_channel_load_worker_id);
            video_channel_load_worker_id = null;
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

    public void loadVideoPlayerPage(){
        // Отключение всех элементов интерфейса,
        // кроме иконки прогресса
        error_view.setVisibility(View.GONE);
        web_view.setVisibility(View.GONE);
        video_view.setVisibility(View.GONE);

        progress_bar.setVisibility(View.VISIBLE);

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

        // Обработчик страницы с плеером (на самом сайте a-g)
        WebViewClient web_handler1 = new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript(
                    js_get_body,
                    html -> {
                        // Проверка есть ли разобранная ссылка на видео
                        // Если есть значит видео хранится на a-g.online
                        int check_video_end = html.indexOf(".mp4");

                        Log.wtf("jw_video", "r: " + check_video_end);

                        if (check_video_end != -1){
                            // Если check_video_end, то сразу ищем ссылку на видео
                            int src_end = html.indexOf(".mp4");
                            int ref_end = src_end + 4;
                            String html_part = html.substring(0, ref_end);
                            int ref_start = html_part.lastIndexOf("\"");

                            video_reference = html.substring(ref_start + 1, ref_end);
                            video_reference = video_reference.replaceAll("\\\\","");
                            Log.wtf("video_reference", video_reference);

                            // Очистка браузера
                            view.loadUrl("about:blank");
                            view.clearCache(false);
                            view.setWebViewClient(null);

                            // Отлючение иконки загрузки
                            progress_bar.setVisibility(View.GONE);

                            // Запуск видео
                            video_view.setVideoURI(Uri.parse(video_reference));
                            video_view.setVisibility(View.VISIBLE);
                            video_view.start();
                        }
                        else{
                            // Если не check_video_end, то проверяем дальше
                            int first_frame = html.indexOf("iframe");
                            int ref_start = first_frame + 13;
                            int ref_end = html.indexOf("\"", ref_start) - 1;

                            video_player_reference = html.substring(ref_start, ref_end);

                            if (video_player_reference.contains("vk")){
                                if (!video_player_reference.contains("https:")){
                                    video_player_reference = "https:" + video_player_reference;
                                }
                                video_player_reference = video_player_reference.replaceAll(";","&");
                            }
                            Log.wtf("video_player_reference", video_player_reference);

                            view.setWebViewClient(web_visible);
                            view.loadUrl(video_player_reference);
                        }
                    }
                );
            }

            @SuppressLint("WebViewClientOnReceivedSslError")
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error){
                handler.proceed();
            }
        };

        web_view.setWebViewClient(web_handler1);

        web_view.loadUrl(video_player_reference);
    }

    // Показывает/прячет системные панели управления
    // (взято с официального примера: https://developer.android.google.cn/training/system-ui/immersive)
    // Полезная ссылка - https://developer.android.google.cn/training/gestures/edge-to-edge
    public void showSystemBars(boolean show) {
        WindowInsetsControllerCompat windowInsetsController = ViewCompat.getWindowInsetsController(this.getWindow().getDecorView());
        if (windowInsetsController == null) {
            return;
        }
        // Configure the behavior of the hidden system bars
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
}
