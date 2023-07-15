package com.example.AnythingGroup.fragments.releases;

import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.util.DisplayMetrics;
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
import com.example.AnythingGroup.activities.MainActivity;
import com.example.AnythingGroup.R;
import com.example.AnythingGroup.fragments.ContentListFragment;
import com.example.AnythingGroup.fragments.title.TitleMain;

import java.io.IOException;
import java.util.ArrayList;

public class ReleaseSubfragment extends ContentListFragment {
    // Тип странички с релизами
    // 0 - Аниме
    // 1 - Дорамы
    // 2 - Документальные
    private Integer type;

    // Список релизов
    private GridLayout listView;
    // Количество колонок
    private int columnCount = 1;

    private ReleaseContentListParser.ContentList<ReleaseContentListParser.ContentListItem> contentList;

    // Нужно для стабильной работы,
    // если фрагменты восстанавливаются из стека.
    public ReleaseSubfragment(){}

    public ReleaseSubfragment(int type){
        this.type = type;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            type = savedInstanceState.getInt("type");
        }

        contentList = AppBase.releases.releaseMatrix.get(type);

        super.contentListState = contentList.state;
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

        if (dpWidth > 512){
            columnCount = 3;
        }
        else{
            columnCount = 2;
        }

        listView.setColumnCount(columnCount);

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (super.contentListState.state == ReleaseContentListParser.ContentState.None){
            loadReleases();
        }
        else{
            updateList(-1);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("type", type);
    }

    @Override
    public void onRefresh() {
        if (super.contentListState.state != ReleaseContentListParser.ContentState.Loading) {
            contentList.state.pagesLoaded = 0;
            contentList.clear();
            listView.removeAllViews();
            loadReleases();
        }
    }

    @Override
    public void onEndReached(ExtendedScrollView scrollView) {
        // Если ничего не загружается и не загружены все релизы, то догрузить ещё
        if (super.contentListState.state == ReleaseContentListParser.ContentState.Loaded) {
            super.refreshLayout.setRefreshing(true);
            loadReleases();
        }
    }

    public void updateList(int items){
        if (items == -1){
            listView.removeAllViews();
            items = AppBase.releases.releaseMatrix.get(type).size();
        }

        int rows = AppBase.releases.releaseMatrix.get(type).size() / columnCount;

        listView.setRowCount(rows + 1);

        Point size = new Point();
        requireActivity().getWindowManager().getDefaultDisplay().getSize(size);
        int screenWidth = size.x;

        int itemWidth = screenWidth / columnCount;

        int start = AppBase.releases.releaseMatrix.get(type).size() - items;
        for (int i = start; i < items; i++){
            int current_column = i % columnCount;
            int current_row = i / columnCount;

            Context context = getContext();

            if (context != null){
                ReleaseContentListParser.ContentListItem item = AppBase.releases.releaseMatrix.get(type).get(i);

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

    /// Загружает релизы и добавляет их в конец.
    public void loadReleases(){
        super.errorView.setVisibility(View.GONE);

        super.contentListState.state = ReleaseContentListParser.ContentState.Loading;
        super.contentListState.pagesLoaded++;

        Data.Builder args = new Data.Builder();
        args.putInt("release_type", type);

        // Создание процедуры для загрузки релизов
        WorkRequest loadWorkRequest = new OneTimeWorkRequest
                .Builder(ReleaseLoader.class)
                .setInputData(args.build())
                .build();

        // Добавление процедуры в очередь выполнения
        super.workManager.enqueue(loadWorkRequest);

        super.contentListState.workerId = loadWorkRequest.getId();

        // Подключение функции для ожидания завершения загрузки
        super.workManager.getWorkInfoByIdLiveData(super.contentListState.workerId).observe(
                getViewLifecycleOwner(),
                workInfo -> {
                    switch (workInfo.getState()){
                        case SUCCEEDED: {
                            if (listView == null) return;
                            listView.post(() -> {
                                updateList(-1);
                                super.refreshLayout.setRefreshing(false);
                            });
                            break;
                        }

                        case FAILED: {
                            // Код ошибки 404 - страница не найдена - обозначает конец списка релизов
                            int error_code = workInfo.getOutputData().getInt("error_code", 0);

                            if (listView == null) return;
                            listView.post(() -> {
                                if (error_code != 404) {

                                    String error = workInfo.getOutputData().getString("error");
                                    super.errorView.setText(error);
                                    super.errorView.setVisibility(View.VISIBLE);

                                    super.refreshLayout.setRefreshing(false);
                                }
                            });

                            break;
                        }

                        default: break;
                    }
                }
        );
    }

    public static class ReleaseLoader extends ReleaseLoadWorker {
        public ReleaseLoader(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        public Result Work(Data input) throws IOException {
            int type = input.getInt("release_type", 0);

            String page_url = "https://a-g.site/relise/";

            ReleaseContentListParser.ContentList<ReleaseContentListParser.ContentListItem> contentList = AppBase.releases.releaseMatrix.get(type);

            super.contentListState = contentList.state;

            switch (type){
                case 0:
                    page_url += "anime-serialy";
                    break;
                case 1:
                    page_url += "ovaonaspecial";
                    break;
                case 2:
                    page_url += "movie";
                    break;
                case 3:
                    page_url += "polnyi-metr";
                    break;
                case 4:
                    page_url += "dokumentalnye";
                    break;
                case 5:
                    page_url += "doramy";
                    break;
            }
            page_url += "?page=" + contentList.state.pagesLoaded;

            ArrayList<ReleaseContentListParser.ContentListItem> items = ReleaseContentListParser.parse(page_url);

            // Проверка релизов на повтор
            // Иногда страницы дублируют последний тайтл предыдущей страницы
            if (!contentList.isEmpty()) {
                String last_title = contentList.get(contentList.size() - 1).title;
                ReleaseContentListParser.ContentListItem item = items.get(0);

                if (item.title.equals(last_title)) {
                    items.remove(0);
                }
            }

            contentList.addAll(items);

            return null;
        }
    }
}