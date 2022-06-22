package com.example.AnythingGroup.fragments.video;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.example.AnythingGroup.AppBase;
import com.example.AnythingGroup.LoadWorker;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class VideoChannelLoadWorker extends LoadWorker {
    public VideoChannelLoadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result Work(Data input) throws IOException {
        String video_channel_reference = input.getString("video_channel_reference");

        Log.wtf("video_channel_reference", video_channel_reference);

        assert video_channel_reference != null;
        if (!video_channel_reference.contains("https")){
            video_channel_reference = video_channel_reference.replace("http", "https");
        }

        Document document = Jsoup.connect(video_channel_reference).get();

        Element video_list = document.getElementsByClass("field_movie_list").get(0);

        Elements video_elements = video_list.getElementsByClass("iwrap");

        for (Element video_element: video_elements) {
            VideoListItem video = new VideoListItem();

            // Обложка
            Element image_container = video_element.getElementsByTag("div").get(1);
            Elements image_check = image_container.getElementsByTag("img");
            if (image_check.size() != 0) {
                Element image = image_check.get(0);
                video.image = AppBase.loadImageFromURL(image.attributes().get("src"));
            }

            // Ссылка на страницу с видео
            Element video_reference = video_element.getElementsByClass("ioverlay").get(0);
            video.reference = video_reference.attributes().get("href");

            // Продолжительность
            Elements video_duration_check = video_element.getElementsByClass("iduration");
            if (video_duration_check.size() != 0) {
                Element video_duration = video_duration_check.get(0);
                video.duration = video_duration.text();
            }

            // Заголовок
            Element video_title = video_element.getElementsByClass("ifield_title").get(0);
            video.title = video_title.attributes().get("title");

            AppBase.videoChannelList.add(video);
        }

        return null;
    }
}
