package com.example.AnythingGroup;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.example.AnythingGroup.fragments.video.VideoSource;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Network {
    public static String token;
    public static Map<String, String> cookies = new HashMap<>();

    // Загружает изображения.
    // Если указана лишь часть пути, то самостоятельно дополняет его.
    // Если такого файла нет, то возвращает null.
    public static Bitmap getImageFromURL(String url) throws IOException{
        try {
            if (!url.contains("https")) {
                url = AppBase.A_G_SITE + url;
            }

            InputStream in = new URL(url).openStream();

            return BitmapFactory.decodeStream(in);
        }
        catch (FileNotFoundException e){
            return null;
        }
    }

    public static Document get(String url) throws IOException {
        Log.wtf("Cookies", cookies.toString());

        Connection.Response response = Jsoup.connect(url)
                .cookies(cookies)
                .method(Connection.Method.GET)
                .execute();

        cookies.putAll(response.cookies());

        Log.wtf("Cookies", cookies.toString());

        return response.parse();
    }

    public static VideoSource get_video_source(int id) throws IOException {
        Document video_source = Jsoup.connect("https://a-g.site/video/show_player/" + id + "?autopay=1&skip_ads=0")
                .cookies(Network.cookies)
                .header("X-Requested-With","XMLHttpRequest")
                .get();

        String html = video_source.toString();

        Log.wtf("Video", html);

        if (html.contains("sibnet")){
            int first_frame = html.indexOf("iframe");
            int ref_start = first_frame + 12;
            int ref_end = html.indexOf("\"", ref_start);

            String video_player_reference = html.substring(ref_start, ref_end);

            Log.wtf("Video", video_player_reference);

            return new VideoSource(VideoSource.VideoSourceType.Iframe, video_player_reference);
        }
        else if (html.contains("vk.com")){
            int first_frame = html.indexOf("iframe");
            int ref_start = first_frame + 12;
            int ref_end = html.indexOf("\"", ref_start);

            String video_player_reference = html.substring(ref_start, ref_end);

            if (!video_player_reference.contains("https:")){
                video_player_reference = "https:" + video_player_reference;
            }

            video_player_reference = video_player_reference.replaceAll("&amp;", "&");

            Log.wtf("Video", video_player_reference);

            return new VideoSource(VideoSource.VideoSourceType.Iframe, video_player_reference);
        }
        else{
            // Проверка есть ли разобранная ссылка на видео
            // Если есть, значит, видео хранится на a-g.online
            int check_video_end = html.indexOf(".mp4");

            Log.wtf("jw_video", "r: " + check_video_end);

            if (check_video_end != -1) {
                // Если check_video_end, то сразу ищем ссылку на видео
                int src_end = html.indexOf(".mp4");
                int ref_end = src_end + 4;
                String html_part = html.substring(0, ref_end);
                int ref_start = html_part.lastIndexOf("\"");

                String video_reference = html.substring(ref_start + 1, ref_end);
                // Четыре \, чтобы была одна \
                video_reference = video_reference.replaceAll("\\\\", "");

                Log.wtf("video_reference", video_reference);

                return new VideoSource(VideoSource.VideoSourceType.StreamingUrl, video_reference);
            }
        }

        return null;
    }
}