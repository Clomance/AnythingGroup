package com.example.AnythingGroup;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StrikethroughSpan;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.AnythingGroup.fragments.my_profile.ProfileAdditionalInfo;
import com.example.AnythingGroup.fragments.my_profile.ProfileMainInfo;
import com.example.AnythingGroup.fragments.news.NewsListItem;
import com.example.AnythingGroup.fragments.releases.ReleaseContentListParser;
import com.example.AnythingGroup.fragments.title.Title;
import com.example.AnythingGroup.fragments.video.VideoListItem;
import com.example.AnythingGroup.fragments.video.VideoSource;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.ArrayList;
import java.util.List;

// Хранилище общей информации
public class AppBase {
    // ПОСТОЯННЫЕ
    // Коды для проверки разрешений
    final static int REQUEST_INTERNET_ID = 0;

    // Имена для локального файла приложения
    private static final String APP_SETTINGS_FILE = "APP_SETTINGS_FILE"; // Название файла с настройками
    private static final String APP_SETTINGS_AUTO_AUTHORISATION = "AUTO"; // Автоматическая авторизация
    private static final String APP_SETTINGS_EMAIL = "EMAIL"; // Номер пользователя
    private static final String APP_SETTINGS_PASSWORD = "PASSWORD"; // Пароль пользователя

    // Ссылка на сайт.
    // Используется для задания полного пути страниц или файлов.
    // (см. AppBase.loadImageFromURL)
    public static final String A_G_SITE = "https://a-g.site";

    // ГЛОБАЛЬНЫЕ ПЕРЕМЕНЕННЫЕ
    // Системные: флаги разрешений и локальные файлы
    public static boolean internet_permission_on = false; // Флаг разрешения на использование интернета
    public static SharedPreferences AppSettings; // Файл с настройками приложения

    // Текущее время года
    public static LogoOption logoOption = LogoOption.Default;

    // Пользователь
    public static User user = new User();

    // Скачиваемые данные
    // Списки новостей
    public static News news = new News();

    public static Releases releases = new Releases();

    // Ссылка на тайтл
    public static String title_reference;
    // Флаг полной загрузки данных тайтла
    public static boolean title_loaded = false;
    // Данные тайтла
    public static Title title = new Title();
    // Список видео на канале тайтла
    public static final List<VideoListItem> videoChannelList = new ArrayList<>();

    public static VideoSource video_source;

    // ФУНКЦИИ
    public static void getSharedPreferences(Activity activity){
        AppSettings = activity.getSharedPreferences(AppBase.APP_SETTINGS_FILE, Context.MODE_PRIVATE);
    }

    public static boolean loadLoginPassword(){
        boolean auto = AppSettings.getBoolean(APP_SETTINGS_AUTO_AUTHORISATION,false);
        if (!auto){
            return false;
        }
        user.email = AppSettings.getString(APP_SETTINGS_EMAIL,null);
        user.password = AppSettings.getString(APP_SETTINGS_PASSWORD,null);

        return true;
    }

    public static void saveLoginPassword(){
        SharedPreferences.Editor editor = AppSettings.edit();

        editor.putBoolean(APP_SETTINGS_AUTO_AUTHORISATION, true);
        editor.putString(APP_SETTINGS_EMAIL, user.email);
        editor.putString(APP_SETTINGS_PASSWORD, user.password);

        editor.apply();
    }

    public static void setAutoAuthorisation(boolean auto){
        SharedPreferences.Editor editor = AppSettings.edit();
        editor.putBoolean(APP_SETTINGS_AUTO_AUTHORISATION, auto);
        editor.apply();
    }

    /// Проверка разрешений и запрос, если их нет.
    public static void checkPermissions(Activity activity) {
        // Установка флага разрешений "Интернет"
        AppBase.internet_permission_on = ActivityCompat.checkSelfPermission(activity, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;
        // Если разрещение не получено, отправка запроса
        if (!AppBase.internet_permission_on) {
            String[] internet = new String[]{Manifest.permission.INTERNET};
            ActivityCompat.requestPermissions(activity, internet, AppBase.REQUEST_INTERNET_ID);
        }
    }

    // Применение запрошенных разрешений
    public static void onRequestPermissionsResult(int requestCode, int[] grantResults){
        if (requestCode == AppBase.REQUEST_INTERNET_ID) {
            AppBase.internet_permission_on = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
    }


    public static SpannableString colorText(String text, int color){
        SpannableString styledText = new SpannableString(text);
        styledText.setSpan(new ForegroundColorSpan(color), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return styledText;
    }

    public static SpannableStringBuilder htmlToText(Element element) {
        return htmlToText(element, null, new TextSettings());
    }

    public static SpannableStringBuilder htmlToText(Element element, Context context) {
        return htmlToText(element, context, new TextSettings());
    }

    public static SpannableStringBuilder htmlToText(Element element, Context context, TextSettings textSettings){
        SpannableStringBuilder textBuffer = new SpannableStringBuilder();

        nodeToText(element, textBuffer, context, textSettings);

        return textBuffer;
    }

    public enum TextSpanType{
        None,
        Foreground,
        Background,
        Strikethrough
    }

    public static class TextSettings{
        public boolean newLine = false;
        public TextSpanType spanType;
        public int color = 0;

        public TextSettings(){
            this.spanType = TextSpanType.None;
        }

        public TextSettings(TextSpanType spanType){
            this.spanType = spanType;
        }

        public TextSettings(TextSpanType spanType, int color){
            this.spanType = spanType;
            this.color = color;
        }

        public static TextSettings foreground(int color){
            return new TextSettings(TextSpanType.Foreground, color);
        }
    }

    public static void nodeToText(Node node, SpannableStringBuilder textBuffer, Context context, TextSettings textSettings){
        List<Node> children = node.childNodes();

        for (Node child: children){
            String nodeName = node.nodeName();

            switch (nodeName) {
                // Изображение (смайл)
                case "img": case "div":{
                    if (context == null) continue;
                    String src = child.attributes().get("src");

                    if (src.isEmpty()) {
                        textSettings.newLine = true;
                        break;
                    };

                    int smile_name_start = src.lastIndexOf("/");
                    int smile_name_end = src.lastIndexOf(".");
                    String smile_name = src.substring(smile_name_start + 1, smile_name_end);

                    int start = textBuffer.length();
                    int end = start + 1;
                    textBuffer.append("s");

                    // TODO optimize
                    int resourceId = context.getResources().getIdentifier(smile_name, "drawable", context.getPackageName());

                    if (resourceId != 0) {
                        Resources res = context.getResources();
                        Drawable drawable = ResourcesCompat.getDrawable(res, resourceId, null);

                        assert drawable != null;
                        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                        ImageSpan image = new ImageSpan(drawable);

                        textBuffer.setSpan(image, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    continue;
                }

                // Вертикальный пробел
                case "br": {
                    textBuffer.append("\n");
                    break;
                }

                // Текст, выделенный жёлтым
                case "em": {
                    if (context == null) break;
                    textSettings.spanType = TextSpanType.Background;
                    textSettings.color = context.getResources().getColor(R.color.search_fragment_highlight);
                    break;
                }

                // Зачёркнутый текст
                case "s": {
                    textSettings.spanType = TextSpanType.Strikethrough;
                    break;
                }

                default: break;
            }

            if (child instanceof TextNode) {
                TextNode textNode = (TextNode) child;

                String text = textNode.text().trim();

                if (text.isEmpty()) continue;

                if (textSettings.newLine){
                    textSettings.newLine = false;
                    if (textBuffer.length() != 0){
                        textBuffer.append("\n");
                    }
                }

                Log.wtf(nodeName, text);

                int start = textBuffer.length();
                textBuffer.append(textNode.text());
                int end = textBuffer.length();

                switch (textSettings.spanType){
                    case None:

                        break;
                    case Foreground:
                        textBuffer.setSpan(new ForegroundColorSpan(textSettings.color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case Background:
                        textBuffer.setSpan(new BackgroundColorSpan(textSettings.color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case Strikethrough:
                        textBuffer.setSpan(new StrikethroughSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                }
            }
            else{
                nodeToText(child, textBuffer, context, textSettings);
            }
        }
    }

    public enum LogoOption{
        // Обычный
        Default,
        // Новогодний
        NewYear,
        // Весенний
        Spring,
        // Ко дню космонавтики
        Cosmos,
        // На Хеллоуин
        Halloween
    }

    public static class User {
        public ProfileMainInfo mainInfo = new ProfileMainInfo(); // Основная информация
        public ProfileAdditionalInfo additionalInfo; // Дополнительная информация
        // Данные для авторизации
        public String email;
        public String password;
        public boolean authorized = false;
    }

    public static class News {
        public final ReleaseContentListParser.ContentList<NewsListItem> commonNewsList = new ReleaseContentListParser.ContentList<>();

        public final ReleaseContentListParser.ContentList<ReleaseContentListParser.ContentListItem> releaseNewsList = new ReleaseContentListParser.ContentList<>();
    }

    public static class Releases {
        public final ReleaseContentListParser.ContentList<ReleaseContentListParser.ContentListItem> animeReleaseList = new ReleaseContentListParser.ContentList<>();

        public final ReleaseContentListParser.ContentList<ReleaseContentListParser.ContentListItem> OVAONASpecialReleaseList = new ReleaseContentListParser.ContentList<>();

        public final ReleaseContentListParser.ContentList<ReleaseContentListParser.ContentListItem> movieReleaseList = new ReleaseContentListParser.ContentList<>();

        public final ReleaseContentListParser.ContentList<ReleaseContentListParser.ContentListItem> polnyiMetrReleaseList = new ReleaseContentListParser.ContentList<>();

        public final ReleaseContentListParser.ContentList<ReleaseContentListParser.ContentListItem> documentaryReleaseList = new ReleaseContentListParser.ContentList<>();

        public final ReleaseContentListParser.ContentList<ReleaseContentListParser.ContentListItem> doramaReleaseList = new ReleaseContentListParser.ContentList<>();

        // Матрица релизов для удобной работы с ними
        public final List<ReleaseContentListParser.ContentList<ReleaseContentListParser.ContentListItem>> releaseMatrix = new ArrayList<>(6);
    }
}