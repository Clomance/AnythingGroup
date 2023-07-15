package com.example.AnythingGroup.fragments.title;

import android.graphics.Bitmap;
import android.text.SpannableStringBuilder;

import java.util.ArrayList;

public class Title {
    // Изображение (постер)
    public Bitmap image;
    // Название на японском
    public String name_jp;
    // Название на русском
    public String name_ru;
    // Количество эпизодов
    public String episodes;
    // Страна-производитель
    public String country;
    // Год релиза
    public String release_year;
    // Взрастной рейтинг
    public String age_rating;
    // Жанры
    public ArrayList<String> genres = new ArrayList<>();
    // Продолжительность
    public String duration;
    // Режиссёры
    public ArrayList<String> directors = new ArrayList<>();
    // Сценаристы
    public ArrayList<String> plot_authors = new ArrayList<>();
    // Авторы оригинала
    public ArrayList<String> original_authors = new ArrayList<>();
    // Студии
    public ArrayList<String> studios = new ArrayList<>();
    // Ваш рейтинг
    public byte your_rating = 0;
    // Общий рейтинг
    public float rating = 0;
    // Количество оценок
    public int rating_voted = 0;
    // Перевод
    public String translation;
    // Тип озвучки и актёры
    public ArrayList<String> voicing = new ArrayList<>();
    // Техническая поддержка
    public String tech_support;
    // Работа со звуком
    public String sound;
    // Описание
    public SpannableStringBuilder description;
    // Ссылка на видео канал
    public String video_channel_reference;
    // false указывает на то, что это ссылка на канал, а не само видео
    public boolean video_reference = false;
    // Список эпизодов
    public SpannableStringBuilder episode_list;
    // Спасибо
    public ArrayList<String> thanks = new ArrayList<>();
    // Комментарии
    public ArrayList<CommentData> comments = new ArrayList<>();
}
