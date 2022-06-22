package com.example.AnythingGroup;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.AnythingGroup.fragments.my_profile.ProfileAdditionalInfo;
import com.example.AnythingGroup.fragments.my_profile.ProfileMainInfo;
import com.example.AnythingGroup.fragments.news.NewsListItem;
import com.example.AnythingGroup.fragments.title.Title;
import com.example.AnythingGroup.fragments.video.VideoListItem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    // Сессия
    public static String token;
    public static Map<String, String> cookies;

    // Пользователь
    public static ProfileMainInfo ProfileMain = new ProfileMainInfo(); // Основная информация
    public static ProfileAdditionalInfo ProfileAdditional; // Дополнительная информация
    // Данные для авторизации
    public static String ProfileEmail;
    public static String ProfilePassword;
    public static boolean Authorized = false;

    // Скачиваемые данные
    // Списки новостей
    public static final List<NewsListItem> commonNewsList = new ArrayList<>();
    public static int commonNewsPage = 0; // Номер страницы для парсинга

    public static final List<ReleaseContentListParser.ContentListItem> releaseNewsList = new ArrayList<>();
    public static int releaseNewsPage = 0; // Номер страницы для парсинга

    // Списки релизов
    public static int animeReleasePage = 0; // Номер страницы для парсинга
    public static final List<ReleaseContentListParser.ContentListItem> animeReleaseList = new ArrayList<>();

    public static int OVAONASpecialReleasePage = 0; // Номер страницы для парсинга
    public static final List<ReleaseContentListParser.ContentListItem> OVAONASpecialReleaseList = new ArrayList<>();

    public static int movieReleasePage = 0; // Номер страницы для парсинга
    public static final List<ReleaseContentListParser.ContentListItem> movieReleaseList = new ArrayList<>();

    public static int polnyiMetrReleasePage = 0; // Номер страницы для парсинга
    public static final List<ReleaseContentListParser.ContentListItem> polnyiMetrReleaseList = new ArrayList<>();

    public static int documentaryReleasePage = 0; // Номер страницы для парсинга
    public static final List<ReleaseContentListParser.ContentListItem> documentaryReleaseList = new ArrayList<>();

    public static int doramaReleasePage = 0; // Номер страницы для парсинга
    public static final List<ReleaseContentListParser.ContentListItem> doramaReleaseList = new ArrayList<>();

    // Матрица из ссылок на списки релизов для удобного обобщённого обращения через ReleaseSubfragment
    public static final List<List<ReleaseContentListParser.ContentListItem>> releaseMatrix = new ArrayList<>(6);

    // Ссылка на тайтл
    public static String title_reference;
    // Флаг полной загрузки данных тайтла
    public static boolean title_loaded = false;
    // Данные тайтла
    public static Title title = new Title();
    // Список видео на канале тайтла
    public static final List<VideoListItem> videoChannelList = new ArrayList<>();

    // ФУНКЦИИ
    public static void getSharedPreferences(Activity activity){
        AppSettings = activity.getSharedPreferences(AppBase.APP_SETTINGS_FILE, Context.MODE_PRIVATE);
    }

    public static boolean loadLoginPassword(){
        boolean auto = AppSettings.getBoolean(APP_SETTINGS_AUTO_AUTHORISATION,false);
        if (!auto){
            return false;
        }
        ProfileEmail = AppSettings.getString(APP_SETTINGS_EMAIL,null);
        ProfilePassword = AppSettings.getString(APP_SETTINGS_PASSWORD,null);

        return true;
    }

    public static void saveLoginPassword(){
        SharedPreferences.Editor editor = AppSettings.edit();

        editor.putBoolean(APP_SETTINGS_AUTO_AUTHORISATION, true);
        editor.putString(APP_SETTINGS_EMAIL, ProfileEmail);
        editor.putString(APP_SETTINGS_PASSWORD, ProfilePassword);

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

    // Загружает изображения.
    // Если указана лишь часть пути, то самостоятельно дополняет его.
    // Если такого файла нет, то возвращает null.
    public static Bitmap loadImageFromURL(String url) throws IOException{
        try {
            if (!url.contains("https")) {
                url = A_G_SITE + url;
            }

            InputStream in = new URL(url).openStream();

            return BitmapFactory.decodeStream(in);
        }
        catch (FileNotFoundException e){
            return null;
        }
    }

    public static SpannableStringBuilder textFormatter(Context context, String text, ForegroundColorSpan foregroundColorSpan){
        SpannableStringBuilder formattedText = new SpannableStringBuilder(text);

        if (foregroundColorSpan != null){
            formattedText.setSpan(foregroundColorSpan, 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        Log.wtf("format text", "go");

        // Разница между старым текстом и форматированным
        int offset = 0;

        // Открывающий тэг
        int open_tag_start = text.indexOf("<");
        while (open_tag_start != -1){
            Log.wtf("open_tag_start", "" + open_tag_start);
            // Закрывающий тэг
            int open_tag_end = text.indexOf(">", open_tag_start);

            Log.wtf("open_tag_end", "" + open_tag_end);

            Log.wtf("offset", "" + offset);

            if (open_tag_end == -1){
                open_tag_start = text.indexOf("<", open_tag_start + 1);
                continue;
            }

            int tag_end = open_tag_end;

            if (text.startsWith("img", open_tag_start + 1)){
                Log.wtf("HTML", "image");

                int smile_image_src_start = text.indexOf("src", open_tag_start) + 6;
                if (smile_image_src_start != 5){
                    int smile_image_src_end = text.indexOf("\"", smile_image_src_start);

                    if (smile_image_src_end == -1){
                        smile_image_src_end = text.indexOf("'", smile_image_src_start);
                    }

                    // Ссылка на смайлик
                    String smile_image_src = text.substring(smile_image_src_start, smile_image_src_end);
                    int smile_name_start = smile_image_src.lastIndexOf("/");
                    int smile_name_end = smile_image_src.lastIndexOf(".");
                    String smile_name = smile_image_src.substring(smile_name_start + 1, smile_name_end);

                    if (open_tag_start - offset == 0){
                        formattedText = formattedText.replace(0, open_tag_end - offset + 1, " ");
                    }
                    else{
                        formattedText = formattedText.replace(open_tag_start - 1 - offset, open_tag_end - offset + 1, " ");
                    }

                    // Поиск
                    int resourceId = context.getResources().getIdentifier(smile_name, "drawable", context.getPackageName());

                    if (resourceId != 0){
                        Resources res = context.getResources();
                        Drawable drawable = ResourcesCompat.getDrawable(res, resourceId, null);

                        assert drawable != null;
                        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                        ImageSpan image = new ImageSpan(drawable);
                        formattedText.setSpan(image, open_tag_start - 1 - offset, open_tag_start - offset, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    if (open_tag_start - offset == 0){
                        offset += open_tag_end - open_tag_start;
                    }
                    else{
                        offset += open_tag_end - open_tag_start + 1;
                    }
                }
            }
            else if (text.startsWith("em", open_tag_start + 1)){
                Log.wtf("HTML", "em");

                int em_text_end = text.indexOf("</em>", open_tag_start);

                formattedText = formattedText.replace(open_tag_start - offset, open_tag_start - offset + 4, "");

                formattedText.setSpan(
                        new BackgroundColorSpan(context.getResources().getColor(R.color.search_fragment_highlight)),
                        open_tag_start - offset,
                        em_text_end - offset,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                offset += 4;

                formattedText = formattedText.replace(em_text_end - offset, em_text_end - offset + 5, "");

                offset += 5;

                tag_end = em_text_end + 5;
            }
            else if (text.startsWith("div", open_tag_start + 1)){
                Log.wtf("HTML", "div");

                formattedText = formattedText.replace(open_tag_start - offset, open_tag_end - offset + 1, "");

                offset += open_tag_end - open_tag_start + 1;

                tag_end = open_tag_end + 1;
            }
            else if (text.startsWith("/div", open_tag_start + 1)){
                tag_end = open_tag_start;

                formattedText = formattedText.replace(tag_end - offset, open_tag_start - offset + 6, "");

                tag_end = open_tag_start + 6;

                offset += 6;
            }
            else if (text.startsWith("br", open_tag_start + 1)){
                Log.wtf("HTML", "br");

                formattedText = formattedText.replace(open_tag_start - offset, open_tag_start - offset + 4, "");

                tag_end = open_tag_end + 1;

                offset += 4;
            }
            else if (text.startsWith("span", open_tag_start + 1)){
                Log.wtf("HTML", "span");

                formattedText = formattedText.replace(open_tag_start - offset, open_tag_end - offset + 1, "");

                offset += open_tag_end - open_tag_start + 1;

                tag_end = text.indexOf("</span>", open_tag_end + 1);

                if (tag_end != -1){
                    formattedText = formattedText.replace(tag_end - offset, tag_end - offset + 7, "");

                    tag_end = tag_end + 7;

                    offset += 7;
                }
                else{
                    tag_end = open_tag_start + 6;
                }
            }

            open_tag_start = text.indexOf("<", tag_end);
        }

        return formattedText;
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
}
