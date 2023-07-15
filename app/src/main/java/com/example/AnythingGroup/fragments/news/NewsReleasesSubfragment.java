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
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkRequest;
import androidx.work.WorkerParameters;

import com.example.AnythingGroup.AppBase;
import com.example.AnythingGroup.extendedUI.ExtendedScrollView;
import com.example.AnythingGroup.fragments.releases.ReleaseContentListParser;
import com.example.AnythingGroup.activities.MainActivity;
import com.example.AnythingGroup.R;
import com.example.AnythingGroup.fragments.ContentListFragment;
import com.example.AnythingGroup.fragments.releases.ReleaseLoadWorker;
import com.example.AnythingGroup.fragments.title.TitleMain;

import java.io.IOException;

public class NewsReleasesSubfragment extends ContentListFragment {
    // Список релизов
    private GridLayout listView;
    // Количество столбцов
    private int columnCount = 1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        super.contentListState = AppBase.news.releaseNewsList.state;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.release_subfragment, container, false);

        super.errorView = root.findViewById(R.id.error);
        super.refreshLayout = root.findViewById(R.id.refresh);
        super.scrollView = root.findViewById(R.id.scrollView);

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

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (super.contentListState.state == ReleaseContentListParser.ContentState.None) {
            loadReleases();
            Log.wtf("NewsReleases", "onResumeLoad");
        }
        else {
            updateList(-1);
            Log.wtf("NewsReleases", "onResumeShow");
        }
    }

    /// Перезагружает всю ленту новостей релизов.
    @Override
    public void onRefresh() {
        if (super.contentListState.state != ReleaseContentListParser.ContentState.Loading) {
            AppBase.news.releaseNewsList.state.pagesLoaded = 0;
            AppBase.news.releaseNewsList.clear();
            listView.removeAllViews();
            loadReleases();
        }
    }

    @Override
    public void onEndReached(ExtendedScrollView scrollView) {
        if (super.contentListState.state == ReleaseContentListParser.ContentState.Loaded) {
            super.refreshLayout.setRefreshing(true);
            loadReleases();
        }
    }

    public void updateList(int items) {
        if (items == -1) {
            listView.removeAllViews();
            items = AppBase.news.releaseNewsList.size();
        }

        Log.wtf("ListView", "Add " + items);

        int rows = AppBase.news.releaseNewsList.size() / columnCount;

        listView.setRowCount(rows + 1);

        Point size = new Point();
        requireActivity().getWindowManager().getDefaultDisplay().getSize(size);
        int screenWidth = size.x;

        int itemWidth = screenWidth / columnCount;

        int start = AppBase.news.releaseNewsList.size() - items;
        for (int i = start; i < items; i++) {
            Log.wtf("ListView", "Add View");

            int current_column = i % columnCount;
            int current_row = i / columnCount;

            Context context = getContext();

            if (context != null) {
                ReleaseContentListParser.ContentListItem item = AppBase.news.releaseNewsList.get(i);

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
        super.errorView.setVisibility(View.GONE);

        super.contentListState.state = ReleaseContentListParser.ContentState.Loading;

        super.contentListState.pagesLoaded++;

        // Создание процедуры для загрузки релизов
        WorkRequest loadWorkRequest = new OneTimeWorkRequest
                .Builder(NewsReleasesLoader.class)
                .build();

        // Добавление процедуры в очередь выполнения
        super.workManager.enqueue(loadWorkRequest);

        super.contentListState.workerId = loadWorkRequest.getId();

        // Подключение функции для ожидания завершения загрузки
        super.workManager.getWorkInfoByIdLiveData(super.contentListState.workerId).observe(
                getViewLifecycleOwner(),
                workInfo -> {
                    switch (workInfo.getState()) {
                        case SUCCEEDED:
                            if (listView == null) return;
                            listView.post(() -> {
                                updateList(-1);
                                super.refreshLayout.setRefreshing(false);
                            });
                            break;

                        case FAILED:
                            if (listView == null) return;
                            listView.post(() -> {
                                // Код ошибки 404 - страница не найдена - обозначает конец списка релизов
                                int error_code = workInfo.getOutputData().getInt("error_code", 0);
                                if (error_code != 404) {
                                    String error = workInfo.getOutputData().getString("error");
                                    super.errorView.setText(error);
                                    super.errorView.setVisibility(View.VISIBLE);
                                }

                                super.refreshLayout.setRefreshing(false);
                            });
                            break;

                        default: break;
                    }
                }
        );
    }

    public static class NewsReleasesLoader extends ReleaseLoadWorker {
        public NewsReleasesLoader(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        public Result Work(Data input) throws IOException {
            super.contentListState = AppBase.news.releaseNewsList.state;

            String page_url = "https://a-g.site/relise?page=" + AppBase.news.releaseNewsList.state.pagesLoaded;

            AppBase.news.releaseNewsList.addAll(ReleaseContentListParser.parse(page_url));

            return null;
        }
    }
}