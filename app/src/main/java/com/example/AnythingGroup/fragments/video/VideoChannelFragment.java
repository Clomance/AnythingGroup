package com.example.AnythingGroup.fragments.video;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.example.AnythingGroup.activities.MainActivity;
import com.example.AnythingGroup.R;
import com.example.AnythingGroup.activities.VideoViewActivity;

import java.util.UUID;

public class VideoChannelFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private UUID video_channel_load_worker_id = null;

    // Ссылка на канал с видео
    String video_channel_reference;

    // Основание списка из видео
    LinearLayout listView;

    TextView error_view;

    SwipeRefreshLayout video_list_refresh;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.video_channel_fragment, container, false);

        error_view = root.findViewById(R.id.error_view);
        video_list_refresh = root.findViewById(R.id.video_list_refresh);
        listView = root.findViewById(R.id.video_list);

        video_list_refresh.setOnRefreshListener(this);
        video_list_refresh.setEnabled(true);

        if (savedInstanceState == null){
            assert getArguments() != null;
            video_channel_reference = getArguments().getString("video_channel_reference");
        }
        else{
            video_channel_reference = savedInstanceState.getString("video_channel_reference");
        }

        // Загрузка данных канала с видео
        // либо отобразить уже загруженные
        AppBase.videoChannelList.clear(); // Временно
        loadVideoChannel();

        return root;
    }

    @Override
    public void onDestroy() {
        if (video_channel_load_worker_id != null) {
            WorkManager.getInstance(requireContext()).cancelWorkById(video_channel_load_worker_id);
            video_channel_load_worker_id = null;
        }
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("video_channel_reference", video_channel_reference);
    }

    @Override
    public void onRefresh() {
        error_view.setVisibility(View.GONE);
        listView.removeAllViews();

        AppBase.videoChannelList.clear();

        loadVideoChannel();
    }

    private void setVideoChannel(){
        Context context = getContext();
        for (VideoListItem video: AppBase.videoChannelList){
            View view = View.inflate(context, R.layout.video_list_item, null);

            ImageView image = view.findViewById(R.id.image);
            TextView reference = view.findViewById(R.id.reference);
            TextView description = view.findViewById(R.id.title);
            TextView duration = view.findViewById(R.id.duration);

            if (video.image != null){
                image.setImageBitmap(video.image);
            }
            else {
                image.setImageResource(R.drawable.video_empty);
            }
            reference.setText(video.reference);
            description.setText(video.title);
            if (video.duration != null) {
                duration.setText(video.duration);
                duration.setVisibility(View.VISIBLE);
            }
            else{
                duration.setVisibility(View.GONE);
            }

            view.setOnClickListener(this::openVideoPlayerPage);

            listView.addView(view);
        }
    }

    public void openVideoPlayerPage(View view) {
        TextView reference = view.findViewById(R.id.reference);
        String video_player_reference = reference.getText().toString();

        Bundle args = new Bundle();
        args.putString("video_player_reference", video_player_reference);

        MainActivity activity = (MainActivity) requireActivity();

        Intent intent = new Intent(activity, VideoViewActivity.class);
        intent.putExtras(args);
        activity.startActivity(intent);
    }

    // Загрузка данных видео канала:
    //  - загрузка превью каждого видео
    //  - загрузка ссылки на страницу каждого видео
    private void loadVideoChannel() {
        error_view.setVisibility(View.GONE);
        video_list_refresh.setRefreshing(true);

        // Создание процедуры для загрузки данных канала
        WorkRequest loadWorkRequest = new OneTimeWorkRequest
                .Builder(VideoChannelLoadWorker.class)
                .setInputData(new Data.Builder().putString("video_channel_reference", video_channel_reference).build())
                .build();

        video_channel_load_worker_id = loadWorkRequest.getId();

        WorkManager workManager = WorkManager.getInstance(requireContext());
        // Добавление процедуры в очередь выполнения
        workManager.enqueue(loadWorkRequest);

        // Подключение функции для ожидания завершения загрузки
        workManager.getWorkInfoByIdLiveData(video_channel_load_worker_id).observe(
                getViewLifecycleOwner(),
                workInfo -> {
                    if (listView == null) return;
                    listView.post(() -> {
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            setVideoChannel();
                        }
                        else if (workInfo.getState() == WorkInfo.State.FAILED){
                            String error = workInfo.getOutputData().getString("error");
                            error_view.setText(error);
                            error_view.setVisibility(View.VISIBLE);
                        }
                        else{
                            return;
                        }

                        video_list_refresh.setRefreshing(false);
                    });
                });
    }
}
