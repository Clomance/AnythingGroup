package com.example.AnythingGroup.fragments.title;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.example.AnythingGroup.AppBase;
import com.example.AnythingGroup.CommentData;
import com.example.AnythingGroup.MainActivity;
import com.example.AnythingGroup.R;
import com.example.AnythingGroup.VideoViewActivity;

import java.util.Objects;
import java.util.UUID;

public class TitleFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{
    // Номер процедуры загрузки данных тайтла
    private UUID title_load_worker_id = null;

    private SwipeRefreshLayout title_refresh;
    private TextView title_error;
    private ScrollView title_page;

    private ImageView image;
    private TextView name_ru;
    private TextView episodes;
    private TextView country;
    private TextView release;
    private TextView age_rating;
    private TextView genres;
    private TextView duration;
    private TextView director;
    private TextView plot_authors;
    private TextView original_author;
    private TextView studio;
    private TextView rating;
    private TextView your_rating;
    private TextView translation;
    private TextView voicing;
    private TextView tech_support;
    private TextView sound;
    private TextView description;
    private ImageView video_reference;
    private TextView episode_list;
    private TextView thanks;

    private LinearLayout comment_list;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.title_fragment, container, false);

        title_refresh = root.findViewById(R.id.title_refresh);
        title_refresh.setOnRefreshListener(this);

        title_error = root.findViewById(R.id.title_error);

        title_page = root.findViewById(R.id.title_page);
        title_page.setVisibility(View.GONE);

        image = root.findViewById(R.id.title_image);
        name_ru = root.findViewById(R.id.title_name_ru);
        episodes = root.findViewById(R.id.title_episodes);
        country = root.findViewById(R.id.title_country);
        release = root.findViewById(R.id.title_release_year);
        age_rating = root.findViewById(R.id.title_age_rating);
        genres = root.findViewById(R.id.title_genres);
        duration = root.findViewById(R.id.title_duration);
        director = root.findViewById(R.id.title_director);
        plot_authors = root.findViewById(R.id.title_plot_author);
        original_author = root.findViewById(R.id.title_original_author);
        studio = root.findViewById(R.id.title_studio);
        rating = root.findViewById(R.id.title_rating);
        your_rating = root.findViewById(R.id.title_your_rating);
        translation = root.findViewById(R.id.title_translation);
        voicing = root.findViewById(R.id.title_voicing);
        tech_support = root.findViewById(R.id.title_tech_support);
        sound = root.findViewById(R.id.title_sound);
        description = root.findViewById(R.id.title_description);
        video_reference = root.findViewById(R.id.title_video_reference);
        video_reference.setOnClickListener(this::openVideoChannelPage);
        episode_list = root.findViewById(R.id.title_series_list);
        thanks = root.findViewById(R.id.title_thanks);
        comment_list = root.findViewById(R.id.comment_list);

        // Получение основных данных тайтла
        TitleMain title_main;
        if (savedInstanceState == null){
            Log.wtf("Title", "GetArguments");
            Bundle args = getArguments();
            title_main = Objects.requireNonNull(args).getParcelable("title_main");

            if (
                    AppBase.title_loaded &&
                    AppBase.title_reference != null &&
                    AppBase.title_reference.equals(title_main.reference)
            ){
                Log.wtf("Title", "setInfo");
                setTitleInfo();
            }
            else{
                Log.wtf("Title", "LoadInfo");
                AppBase.title_reference = title_main.reference;
                AppBase.title.image = title_main.image;
                AppBase.title.name_jp = title_main.name;

                title_refresh.setRefreshing(true);
                loadTitle();
            }
        }
        else{
            Log.wtf("Title", "GetSavedInstanceState");
            AppBase.title_reference = savedInstanceState.getString("title_reference");

            setTitleInfo();
        }

        // Установака заголовка с название тайтла
        ((MainActivity) requireActivity()).setToolbarTitle(AppBase.title.name_jp);

        return root;
    }

    @Override
    public void onDestroy() {
        // Отмена загрузки данных тайтла при закрытии фрагмента
        if (title_load_worker_id != null) {
            WorkManager.getInstance(requireContext()).cancelWorkById(title_load_worker_id);
            title_load_worker_id = null;
        }
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.wtf("Title", "SaveInstanceState");

        outState.putString("title_reference", AppBase.title_reference);
    }

    @Override
    public void onRefresh() {
        title_refresh.setRefreshing(true);
        title_page.setVisibility(View.INVISIBLE);
        title_error.setVisibility(View.GONE);
        loadTitle();
    }

    public void setText(TextView view, String field, String text){
        view.setText(field);
        Spannable styled_text = new SpannableString(text);
        styled_text.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.title_fragment_text)), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        view.append(styled_text);
    }

    private void setTitleInfo(){
        // Установака заголовка с название тайтла
        // (Повторно, потому что фрагмент может быть открыт без начальных аргрументов)
        ((MainActivity) requireActivity()).setToolbarTitle(AppBase.title.name_jp);

        // Обложка
        image.setImageBitmap(AppBase.title.image);

        // Русское название
        setText(name_ru, getResources().getString(R.string.title_field_name_rus), AppBase.title.name_ru);

        // Эпизоды
        if (AppBase.title.episodes == null){
            episodes.setVisibility(View.GONE);
        }
        else {
            setText(episodes, getResources().getString(R.string.title_field_episodes), AppBase.title.episodes);
            episodes.setVisibility(View.VISIBLE);
        }

        // Страна
        setText(country, getResources().getString(R.string.title_field_country), AppBase.title.country);

        // Год релиза
        setText(release, getResources().getString(R.string.title_field_release_year), AppBase.title.release_year);

        // Возрастной рейтинг
        if (AppBase.title.age_rating == null){
            age_rating.setVisibility(View.GONE);
        }
        else {
            setText(age_rating, getResources().getString(R.string.title_field_age_rating), AppBase.title.age_rating);
            age_rating.setVisibility(View.VISIBLE);
        }

        // Жанры
        if (AppBase.title.genres.size() == 0) {
            genres.setVisibility(View.GONE);
        }
        else{
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < AppBase.title.genres.size() - 1; i++) {
                text.append(AppBase.title.genres.get(i)).append(", ");
            }
            text.append(AppBase.title.genres.get(AppBase.title.genres.size() - 1));

            setText(genres, getResources().getString(R.string.title_field_genres), text.toString());
            genres.setVisibility(View.VISIBLE);
        }

        // Длительность
        setText(duration, getResources().getString(R.string.title_field_duration), AppBase.title.duration);

        // Режиссёры
        if (AppBase.title.directors.size() == 0){
            director.setVisibility(View.GONE);
        }
        else {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < AppBase.title.directors.size() - 1; i++) {
                text.append(AppBase.title.directors.get(i)).append(", ");
            }
            text.append(AppBase.title.directors.get(AppBase.title.directors.size() - 1));

            setText(director, getResources().getString(R.string.title_field_directors), text.toString());

            director.setVisibility(View.VISIBLE);
        }

        // Сценаристы
        if (AppBase.title.plot_authors.size() == 0){
            plot_authors.setVisibility(View.GONE);
        }
        else {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < AppBase.title.plot_authors.size() - 1; i++) {
                text.append(AppBase.title.plot_authors.get(i)).append(", ");
            }
            text.append(AppBase.title.plot_authors.get(AppBase.title.plot_authors.size() - 1));

            setText(plot_authors, getResources().getString(R.string.title_field_plot_authors), text.toString());

            plot_authors.setVisibility(View.VISIBLE);
        }

        // Авторы оригинала
        if (AppBase.title.original_authors.size() == 0){
            original_author.setVisibility(View.GONE);
        }
        else {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < AppBase.title.original_authors.size() - 1; i++) {
                text.append(AppBase.title.original_authors.get(i)).append(", ");
            }
            text.append(AppBase.title.original_authors.get(AppBase.title.original_authors.size() - 1));

            setText(original_author, getResources().getString(R.string.title_field_original_author), text.toString());

            original_author.setVisibility(View.VISIBLE);
        }

        // Студии
        if (AppBase.title.studios.size() == 0){
            studio.setVisibility(View.GONE);
        }
        else {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < AppBase.title.studios.size() - 1; i++) {
                text.append(AppBase.title.studios.get(i)).append(", ");
            }
            text.append(AppBase.title.studios.get(AppBase.title.studios.size() - 1));

            setText(studio, getResources().getString(R.string.title_field_studio), text.toString());

            studio.setVisibility(View.VISIBLE);
        }

        // Общий рейтинг
        if (AppBase.title.rating == 0){
            setText(rating, getResources().getString(R.string.title_field_rating), "нет оценки");
        }
        else{
            String text = AppBase.title.rating + ", проголосовало " + AppBase.title.rating_voted;
            setText(rating, getResources().getString(R.string.title_field_rating), text);
        }

        // Ваш рейтинг
        if (AppBase.title.your_rating == 0){
            setText(your_rating, getResources().getString(R.string.title_field_your_rating), "нет оценки");
        }
        else{
            setText(your_rating, getResources().getString(R.string.title_field_your_rating), Byte.toString(AppBase.title.your_rating));
        }

        // Перевод
        if (AppBase.title.translation == null){
            translation.setVisibility(View.GONE);
        }
        else {
            setText(translation, getResources().getString(R.string.title_field_translation), AppBase.title.translation);
            translation.setVisibility(View.VISIBLE);
        }

        // Озвучка
        if (AppBase.title.voicing.size() == 0){
            voicing.setVisibility(View.GONE);
        }
        else {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < AppBase.title.voicing.size() - 1; i++) {
                text.append(AppBase.title.voicing.get(i)).append(", ");
            }
            text.append(AppBase.title.voicing.get(AppBase.title.voicing.size() - 1));

            setText(voicing, getResources().getString(R.string.title_field_voicing), text.toString());

            voicing.setVisibility(View.VISIBLE);
        }

        // Тех. поддержка
        if (AppBase.title.tech_support == null){
            tech_support.setVisibility(View.GONE);
        }
        else {
            setText(tech_support, getResources().getString(R.string.title_field_tech_support), AppBase.title.tech_support);
            tech_support.setVisibility(View.VISIBLE);
        }

        // Работа со звуком
        if (AppBase.title.sound == null){
            sound.setVisibility(View.GONE);
        }
        else {
            setText(sound, getResources().getString(R.string.title_field_sound), AppBase.title.sound);
            sound.setVisibility(View.VISIBLE);
        }

        // Описание
        if (AppBase.title.description == null) {
            description.setVisibility(View.GONE);
        }
        else{
            String header = getResources().getString(R.string.title_field_description) + "\n";
            description.setText(header);
            description.append(AppBase.textFormatter(getContext(), AppBase.title.description, new ForegroundColorSpan(getResources().getColor(R.color.title_fragment_text))));
            description.setVisibility(View.VISIBLE);
        }

        // Ссылка на канал с озвучкой
        if (AppBase.title.video_channel_reference == null){
            video_reference.setVisibility(View.GONE);
        }
        else{
            video_reference.setVisibility(View.VISIBLE);
        }

        // Список эпизодов
        if (AppBase.title.episode_list.size() == 0){
            episode_list.setVisibility(View.GONE);
        }
        else {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < AppBase.title.episode_list.size(); i++) {
                text.append("\n").append(AppBase.title.episode_list.get(i));
            }

            String header = getResources().getString(R.string.title_field_episode_list) + "\n";
            episode_list.setText(header);
            episode_list.append(
                    AppBase.textFormatter(
                            getContext(),
                            text.toString(),
                            new ForegroundColorSpan(getResources().getColor(R.color.title_fragment_text))
                    )
            );

            episode_list.setVisibility(View.VISIBLE);
        }

        // Спасибо
        if (AppBase.title.thanks.size() == 0){
            thanks.append("Пока никто не отметился");
        }
        else {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < AppBase.title.thanks.size() - 1; i++) {
                text.append(AppBase.title.thanks.get(i)).append(", ");
            }
            text.append(AppBase.title.thanks.get(AppBase.title.thanks.size() - 1));

            setText(thanks, getResources().getString(R.string.title_field_thanks), text.toString());
        }

        // Комментарии
        comment_list.removeAllViews();
        if (AppBase.title.comments.size() == 0){
            TextView comment = new TextView(this.getContext());
            comment.setText("Нет комментариев. Ваш будет первым!");
            comment_list.addView(comment);
        }
        else {
            Context context = this.getContext();
            if (context != null) {
                for (CommentData comment: AppBase.title.comments){
                    View comment_view = View.inflate(context, R.layout.comment_view, null);

                    TextView header_view = comment_view.findViewById(R.id.title);
                    header_view.setText(comment.user_name);

                    header_view.append(" ");
                    header_view.append(comment.time);

                    ImageView image_view = comment_view.findViewById(R.id.image);
                    image_view.setImageBitmap(comment.image);

                    TextView text_view = comment_view.findViewById(R.id.comment_text);

                    SpannableStringBuilder comment_text = AppBase.textFormatter(context, comment.text, null);

                    text_view.setText(comment_text);

                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

                    // Сдвиг по уровню комментария
                    int left_margin = comment.level * 32;
                    layoutParams.setMargins(left_margin, 0, 0, 0);

                    comment_list.addView(comment_view, layoutParams);
                }
            }
        }

        title_page.setVisibility(View.VISIBLE);
    }

    private void loadTitle(){
        AppBase.title_loaded = false;

        // Создание процедуры для загрузки данных тайтла
        WorkRequest loadWorkRequest = new OneTimeWorkRequest
                .Builder(TitleLoadWorker.class)
                .setInputData(new Data.Builder().putString("title_reference", AppBase.A_G_SITE + AppBase.title_reference).build())
                .build();

        title_load_worker_id = loadWorkRequest.getId();

        WorkManager workManager = WorkManager.getInstance(requireContext());
        // Добавление процедуры в очередь выполнения
        workManager.enqueue(loadWorkRequest);

        // Подключение функции для ожидания завершения загрузки
        workManager.getWorkInfoByIdLiveData(title_load_worker_id)
                .observe(getViewLifecycleOwner(), workInfo -> {
                    if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                        title_refresh.setRefreshing(false);

                        AppBase.title_loaded = true;

                        setTitleInfo();
                    }
                    else if (workInfo.getState() == WorkInfo.State.FAILED){
                        title_refresh.setRefreshing(false);

                        String error = workInfo.getOutputData().getString("error");
                        title_error.setText(error);
                        title_error.setVisibility(View.VISIBLE);
                    }
                });
    }

    public void openVideoChannelPage(View view){
        Bundle args = new Bundle();

        MainActivity activity = (MainActivity) requireActivity();
        if (AppBase.title.video_reference){
            args.putString("video_player_reference", AppBase.title.video_channel_reference);

            Intent intent = new Intent(activity, VideoViewActivity.class);
            intent.putExtras(args);
            activity.startActivity(intent);
        }
        else{
            args.putString("video_channel_reference", AppBase.title.video_channel_reference);
            activity.navController.navigate(R.id.videoChannelFragment, args);
        }
    }
}