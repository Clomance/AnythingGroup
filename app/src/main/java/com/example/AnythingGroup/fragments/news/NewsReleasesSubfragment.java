package com.example.AnythingGroup.fragments.news;

import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkRequest;
import androidx.work.WorkerParameters;

import com.example.AnythingGroup.AppBase;
import com.example.AnythingGroup.extendedUI.ExtendedScrollView;
import com.example.AnythingGroup.ReleaseContentListParser;
import com.example.AnythingGroup.LoadWorker;
import com.example.AnythingGroup.MainActivity;
import com.example.AnythingGroup.R;
import com.example.AnythingGroup.fragments.ContentListFragment;
import com.example.AnythingGroup.fragments.title.TitleMain;

import java.io.IOException;

public class NewsReleasesSubfragment extends ContentListFragment {
    // Указывает на готовность фрагмента - окончание загрузки данных
    private boolean fragment_ready = false;

    private TextView errorView;

    // Список релизов
    private GridLayout listView;
    // Количество колонок
    private int columnCount = 1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.release_subfragment, container, false);

        errorView = root.findViewById(R.id.error);

        super.refresh = root.findViewById(R.id.refresh);

        ExtendedScrollView scrollView = root.findViewById(R.id.scrollView);

        // Список, который показывается на экране
        listView = root.findViewById(R.id.list);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int dpWidth = (int) (displayMetrics.widthPixels / displayMetrics.density);

        if (dpWidth > 512) {
            columnCount = 3;
        }
        else {
            columnCount = 2;
        }

        listView.setColumnCount(columnCount);

        // Загрузка более ранних новостей при достижении конца списка
        scrollView.setOnScrollListener(scrollView1 -> {
            Log.wtf("NewsReleases", "LoadMore");

            if (super.load_worker_id == null && !loaded_all && fragment_ready) {
                Log.wtf("NewsReleases", "LoadMore");
                fragment_ready = false;
                super.refresh.setRefreshing(true);
                loadReleases();
            }
        });

        // Обновление новостей при свайпе вниз
        super.refresh.setOnRefreshListener(this);

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (AppBase.releaseNewsList.isEmpty()) {
            fragment_ready = false;
            loadReleases();
            Log.wtf("NewsReleases", "onResumeLoad");
        }
        else {
            updateList(-1);
            fragment_ready = true;
            Log.wtf("NewsReleases", "onResumeShow");
        }
    }

    public void updateList(int items) {
        if (items == -1) {
            listView.removeAllViews();
            items = AppBase.releaseNewsList.size();
        }

        Log.wtf("ListView", "Add " + items);

        int rows = AppBase.releaseNewsList.size() / columnCount;

        listView.setRowCount(rows + 1);

        Point size = new Point();
        requireActivity().getWindowManager().getDefaultDisplay().getSize(size);
        int screenWidth = size.x;

        int itemWidth = screenWidth / columnCount;

        int start = AppBase.releaseNewsList.size() - items;
        for (int i = start; i < items; i++) {
            Log.wtf("ListView", "Add View");

            int current_column = i % columnCount;
            int current_row = i / columnCount;

            Context context = getContext();

            if (context != null) {
                ReleaseContentListParser.ContentListItem item = AppBase.releaseNewsList.get(i);

                View itemView = View.inflate(context, R.layout.release_list_item, null);

                ImageView image = itemView.findViewById(R.id.image);
                TextView header = itemView.findViewById(R.id.title);
                ImageView state = itemView.findViewById(R.id.state);

                // Установка постера
                image.setImageBitmap(item.image);

                // Установка названия
                header.setText(item.title);

                // Установка метки состояния
                int state_resource = R.drawable.in_progress;
                switch (item.state) {
                    case WorkInProgress:
                        state_resource = R.drawable.in_progress;
                        break;
                    case Ongoing:
                        state_resource = R.drawable.ongoing;
                        break;
                    case Complete:
                        state_resource = R.drawable.complete;
                        break;
                    case Stopped:
                        state_resource = R.drawable.freeze;
                        break;
                }
                state.setImageResource(state_resource);

                // Привязка действия при нажатии на элемент
                View.OnClickListener onClickListener = view -> {
                    // Определение аргументов
                    TitleMain titleMainArguments = new TitleMain();
                    titleMainArguments.reference = item.reference;
                    titleMainArguments.name = item.title;
                    titleMainArguments.image = item.image;

                    // Упаковка агрументов
                    Bundle args = new Bundle();
                    args.putParcelable("title_main", titleMainArguments);

                    // Переход к странице тайтла
                    MainActivity activity = (MainActivity) requireActivity();
                    activity.navController.navigate(R.id.fragment_title, args);
                };
                itemView.setOnClickListener(onClickListener);

                GridLayout.LayoutParams param = new GridLayout.LayoutParams();
                param.width = itemWidth;
                param.rowSpec = GridLayout.spec(current_row);
                param.columnSpec = GridLayout.spec(current_column);

                itemView.setLayoutParams(param);
                listView.addView(itemView);
            }
        }
    }

    /// Загружает новости и добавляет их в конец.
    public void loadReleases() {
        errorView.setVisibility(View.GONE);

        AppBase.releaseNewsPage++;

        // Создание процедуры для загрузки релизов
        WorkRequest loadWorkRequest = new OneTimeWorkRequest
                .Builder(NewsReleasesLoadWorker.class)
                .build();

        // Добавление процедуры в очередь выполнения
        super.workManager.enqueue(loadWorkRequest);

        super.load_worker_id = loadWorkRequest.getId();

        // Подключение функции для ожидания завершения загрузки
        super.workManager.getWorkInfoByIdLiveData(super.load_worker_id).observe(
                getViewLifecycleOwner(),
                workInfo -> {
                    if (listView == null) return;
                    listView.post(() -> {
                        switch (workInfo.getState()) {
                            case SUCCEEDED:
                                updateList(-1);
                                break;

                            case FAILED:
                                AppBase.releaseNewsPage--;

                                String error = workInfo.getOutputData().getString("error");
                                errorView.setText(error);
                                errorView.setVisibility(View.VISIBLE);

                                // Код ошибки 404 - страница не найдена - обозначает конец списка релизов
                                int error_code = workInfo.getOutputData().getInt("error_code", 0);
                                if (error_code == 404) {
                                    Log.wtf("Релизы", "Конец списка");
                                    loaded_all = true;
                                }
                                break;

                            case CANCELLED:
                                AppBase.releaseNewsPage--;
                                break;

                            default:
                                return;
                        }

                        fragment_ready = true;
                        super.refresh.setRefreshing(false);
                        super.load_worker_id = null;
                    });
                });
    }

    /// Перезагружает всю ленту новостей релизов.
    @Override
    public void onRefresh() {
        if (super.load_worker_id == null) {
            AppBase.releaseNewsPage = 0;
            super.loaded_all = false;
            AppBase.releaseNewsList.clear();
            listView.removeAllViews();
            loadReleases();
        }
    }

    public static class NewsReleasesLoadWorker extends LoadWorker {
        public NewsReleasesLoadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        public Result Work(Data input) throws IOException {
            String page_url = "https://a-g.site/relise?page=" + AppBase.releaseNewsPage;

            AppBase.releaseNewsList.addAll(ReleaseContentListParser.parse(page_url));

            return null;
        }
    }
}