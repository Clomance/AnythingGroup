package com.example.AnythingGroup.fragments.title;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.example.AnythingGroup.AppBase;
import com.example.AnythingGroup.CommentData;
import com.example.AnythingGroup.LoadWorker;
import com.example.AnythingGroup.Network;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Objects;

public class TitleLoadWorker extends LoadWorker {
    public TitleLoadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public Result Work(Data input) throws IOException{
        String title_reference = input.getString("title_reference");

        Document document = Network.get(Objects.requireNonNull(title_reference));

        if (AppBase.title.name_jp == null){
            Element content = document.getElementById("controller_wrap");
            assert content != null;
            Element title_name_container = content.getElementsByTag("h1").get(0);
            AppBase.title.name_jp = title_name_container.html().trim();
        }

        if (AppBase.title.image == null){
            Element image_element = document.getElementsByClass("f_photo").get(0);
            Element image_container = image_element.getElementsByTag("div").get(1);
            Element image = image_container.getElementsByTag("img").get(0);
            AppBase.title.image = AppBase.loadImageFromURL(image.attributes().get("src"));
        }

        // Русское название
        Element russian_name_element = document.getElementsByClass("f_russian").get(0);
        Element russian_name = russian_name_element.getElementsByTag("div").get(2);
        AppBase.title.name_ru = russian_name.html();

        // Эпизоды
        Elements episodes_element_check = document.getElementsByClass("f_episodes");
        AppBase.title.episodes = null;
        if (episodes_element_check.size() != 0) {
            Element episodes_element = episodes_element_check.get(0);
            Element episodes = episodes_element.getElementsByTag("div").get(2);
            AppBase.title.episodes = episodes.html();
        }

        // Страна
        Element country_element = document.getElementsByClass("f_r_country").get(0);
        Element country = country_element.getElementsByTag("div").get(2);
        AppBase.title.country = country.html();

        // Год выпуска
        Element release_year_element = document.getElementsByClass("f_R_Year").get(0);
        Element release_year_container = release_year_element.getElementsByTag("div").get(2);
        Element release_year_reference_element = release_year_container.getElementsByTag("a").get(0);
        AppBase.title.release_year = release_year_reference_element.html();

        // Возростной рейтинг
        Elements age_rating_element_check = document.getElementsByClass("f_age_rating");
        if (age_rating_element_check.size() != 0) {
            Element age_rating_element = age_rating_element_check.get(0);
            Element age_rating_container = age_rating_element.getElementsByTag("div").get(2);
            Element age_rating_image = age_rating_container.getElementsByTag("img").get(0);
            String age_rating_image_reference = age_rating_image.attributes().get("src");
            AppBase.title.age_rating = null;
            if (!age_rating_image_reference.isEmpty()) {
                int age_rating_start = age_rating_image_reference.lastIndexOf("/");
                int age_rating_end = age_rating_image_reference.lastIndexOf(".");
                if (age_rating_start != -1 && age_rating_end != -1) {
                    AppBase.title.age_rating = age_rating_image_reference.substring(age_rating_start + 1, age_rating_end);
                }
            }
        }

        // Жанры
        Element genres_element = document.getElementsByClass("f_R_Genre").get(0);
        Element genres_container = genres_element.getElementsByTag("div").get(2);
        Elements genres = genres_container.getElementsByTag("a");
        AppBase.title.genres.clear();
        for (Element genre: genres) {
            AppBase.title.genres.add(genre.html());
        }

        // Продолжительность
        Element duration_element = document.getElementsByClass("f_r_duration").get(0);
        Element duration = duration_element.getElementsByTag("div").get(2);
        AppBase.title.duration = duration.html();

        // Режиссёры
        Elements directors_element_check = document.getElementsByClass("f_R_Director");
        if (directors_element_check.size() != 0) {
            Element directors_element = directors_element_check.get(0);
            Element directors_container = directors_element.getElementsByTag("div").get(2);
            Elements directors = directors_container.getElementsByTag("a");
            AppBase.title.directors.clear();
            for (Element director : directors) {
                AppBase.title.directors.add(director.html());
            }
        }

        // Сценаристы
        Elements plot_authors_element_check = document.getElementsByClass("f_r_scenario");
        AppBase.title.plot_authors.clear();
        if (plot_authors_element_check.size() != 0) {
            Element plot_authors_element = plot_authors_element_check.get(0);
            Element plot_authors_container = plot_authors_element.getElementsByTag("div").get(2);
            Elements plot_authors = plot_authors_container.getElementsByTag("a");
            for (Element original_author : plot_authors) {
                AppBase.title.plot_authors.add(original_author.html());
            }
        }

        // Авторы оригинала
        Elements original_authors_element_check = document.getElementsByClass("f_R_Author");
        AppBase.title.original_authors.clear();
        if (original_authors_element_check.size() != 0){
            Element original_authors_element = original_authors_element_check.get(0);
            Element original_authors_container = original_authors_element.getElementsByTag("div").get(2);
            Elements original_authors = original_authors_container.getElementsByTag("a");
            for (Element original_author: original_authors) {
                AppBase.title.original_authors.add(original_author.html());
            }
        }

        // Студии
        Element studios_element = document.getElementsByClass("f_studio_img").get(0);
        Element studios_container = studios_element.getElementsByTag("div").get(2);
        Elements studio_images = studios_container.getElementsByTag("img");
        AppBase.title.studios.clear();
        for (Element studio_image: studio_images) {
            String studio_image_reference = studio_image.attributes().get("src");
            int studio_name_start = studio_image_reference.lastIndexOf("/");
            int studio_name_end = studio_image_reference.lastIndexOf(".");
            if (studio_name_start == -1){
                continue;
            }
            String studio_name = studio_image_reference.substring(studio_name_start + 1, studio_name_end);
            AppBase.title.studios.add(studio_name);
        }

        // Рейтинг
        Element rating_element = document.getElementsByClass("wf_r_your_rating").get(0);
        Element rating = rating_element.getElementsByTag("div").get(1);
        AppBase.title.rating = Float.parseFloat(rating.attributes().get("data-webrating"));
        AppBase.title.rating_voted = Integer.parseInt(rating.attributes().get("data-webratingn"));

        // Перевод
        Elements translation_element_check = document.getElementsByClass("f_R_Translation");
        AppBase.title.translation = null;
        if (translation_element_check.size() != 0){
            Element translation_element = translation_element_check.get(0);
            Element translation = translation_element.getElementsByTag("div").get(2);
            AppBase.title.translation = translation.html();
        }

        // Озвучивание
        Elements voicing_element_check = document.getElementsByClass("f_vo_type");
        if (voicing_element_check.size() != 0) {
            Element voicing_element = voicing_element_check.get(0);
            Element voicing_container = voicing_element.getElementsByTag("div").get(2);
            Elements voicing_tags = voicing_container.getElementsByTag("li");
            AppBase.title.voicing.clear();
            for (Element voicing_tag : voicing_tags) {
                Elements voicing = voicing_tag.getElementsByTag("a");
                AppBase.title.voicing.add(voicing.html());
            }
        }

        // Тех. поддержка
        Elements tech_support_element_check = document.getElementsByClass("f_r_support");
        AppBase.title.tech_support = null;
        if (tech_support_element_check.size() != 0){
            Element tech_support_element = tech_support_element_check.get(0);
            Element tech_support = tech_support_element.getElementsByTag("div").get(2);
            AppBase.title.tech_support = tech_support.html();
        }

        // Работа со звуком
        Elements sound_element_check = document.getElementsByClass("f_R_Sound");
        AppBase.title.sound = null;
        if (sound_element_check.size() != 0) {
            Element sound_element = sound_element_check.get(0);
            Element sound = sound_element.getElementsByTag("div").get(2);
            AppBase.title.sound = sound.html();
        }

        // Описание
        Element description_element = document.getElementsByClass("f_content").get(0);
        Element description = description_element.getElementsByTag("div").get(2);
        AppBase.title.description = description.html();

        /// Онлайн просмотр
        Elements video_reference_element_check = document.getElementsByClass("f_onlinevideo");
        AppBase.title.video_channel_reference = null;
        if (video_reference_element_check.size() != 0) {
            Element video_reference_element = video_reference_element_check.get(0);
            Element video_reference = video_reference_element.getElementsByTag("a").get(0);
            AppBase.title.video_channel_reference = video_reference.attributes().get("href");

            if (!AppBase.title.video_channel_reference.startsWith("https")){
                AppBase.title.video_channel_reference = AppBase.title.video_channel_reference.replace("http", "https");
            }

            // Если ссылка указывает на видео, а не на канал
            AppBase.title.video_reference = AppBase.title.video_channel_reference.contains("/video/");
        }

        // Список эпизодов
        Elements episode_list_element_check = document.getElementsByClass("f_eposodes");
        if (episode_list_element_check.size() != 0) {
            Element episode_list_element = episode_list_element_check.get(0);
            Element episode_list_container = episode_list_element.getElementsByTag("div").get(2);
            Elements episode_list = episode_list_container.getElementsByTag("div");
            AppBase.title.episode_list.clear();
            if (episode_list.size() == 1) {
                AppBase.title.episode_list.add(episode_list.get(0).html());
            }
            else {
                episode_list.remove(0);

                for (Element episode : episode_list) {
                    AppBase.title.episode_list.add(episode.html());
                }
            }
        }

        // Спасибо
        Elements thanks_element_check = document.getElementsByClass("f_thanks");
        if (thanks_element_check.size() != 0) {
            Element thanks_element = thanks_element_check.get(0);
            Element thanks_container = thanks_element.getElementsByTag("div").get(2);
            Element thanks_subcontainer = thanks_container.getElementsByTag("div").get(1);
            Element thanks_list = thanks_subcontainer.getElementsByTag("div").get(2);
            Elements thanks_users = thanks_list.getElementsByTag("li");
            AppBase.title.thanks.clear();
            for (Element user : thanks_users) {
                Elements thanks = user.getElementsByTag("a");
                AppBase.title.thanks.add(thanks.html());
            }
        }

        // Комментарии
        Element comment_list_container = document.getElementById("comments_list");
        Elements comment_list = Objects.requireNonNull(comment_list_container)
                .getElementsByClass("comment");
        AppBase.title.comments.clear();
        for (Element comment: comment_list){
            CommentData comment_data = new CommentData();

            // Уровень комментария
            comment_data.level = Integer.parseInt(comment.attributes().get("data-level")) - 1;

            // Имя пользователя
            Elements user_name_element_check = comment.getElementsByClass("name");
            if (user_name_element_check.size() == 0){
                continue;
            }
            Element user_name_element = user_name_element_check.get(0);
            Element user_name = user_name_element.getElementsByClass("user").get(0);
            comment_data.user_name = user_name.html();

            // Дата и время
            Element date_element = comment.getElementsByClass("date").get(0);
            Element date_container = date_element.getElementsByTag("span").get(0);
            String time_str = date_container.getElementsByTag("time").get(0).html();
            Element time = date_element.getElementsByTag("span").get(1);
            time_str += " " + time.html();
            comment_data.time = time_str;

            // Аватарка
            Element image_element = comment.getElementsByClass("avatar").get(0);
            Element image_container = image_element.getElementsByTag("a").get(0);
            Element image = image_container.getElementsByTag("img").get(0);
            comment_data.image = AppBase.loadImageFromURL(image.attributes().get("src"));

            // Текст
            Element text = comment.getElementsByClass("text").get(0);
            comment_data.text = text.html();

            AppBase.title.comments.add(comment_data);
        }

        return null;
    }
}