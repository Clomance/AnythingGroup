package com.example.AnythingGroup.fragments.releases;

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
import java.util.ArrayList;

public class ReleaseSubfragment extends ContentListFragment {

    // Указывает на готовность фрагмента - окончание загрузки данных
    private boolean fragment_ready = false;

    // Тип странички с релизами
    // 0 - Аниме
    // 1 - Дорамы
    // 2 - Документальные
    private Integer type;

    private TextView errorView;

    // Список релизов
    private GridLayout listView;
    // Количество колонок
    private int columnCount = 1;

    // Нужно для стабильной работы,
    // если фрагменты восстанавливаются из стека.
    public ReleaseSubfragment(){

    }

    public ReleaseSubfragment(int type){
        this.type = type;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.release_subfragment, container, false);

        errorView = root.findViewById(R.id.error);

        super.refresh = root.findViewById(R.id.refresh);

        ExtendedScrollView scrollView = root.findViewById(R.id.scrollView);

        if (savedInstanceState != null) {
            type = savedInstanceState.getInt("type");
        }

        Log.wtf("ReleasesSub", "Create: type " + type);

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

        // Загрузка более ранних релизов при достижении конца списка
        scrollView.setOnScrollListener(scrollView1 -> {
            Log.wtf("Releases", "LoadMore");
            // Если ничего не загружается и не загружены все релизы, то догрузить ещё
            if (super.load_worker_id == null && !super.loaded_all && fragment_ready) {
                Log.wtf("Releases", "Load");
                fragment_ready = false;
                super.refresh.setRefreshing(true);
                loadReleases();
            }
        });

        // Обновление релизов при свайпе вниз
        super.refresh.setOnRefreshListener(this);

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (AppBase.releaseMatrix.get(type).isEmpty()){
            fragment_ready = false;
            loadReleases();
        }
        else{
            updateList(-1);
            fragment_ready = true;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("type", type);
    }

    @Override
    public void onRefresh() {
        Log.wtf("Reload", "Releases");
        if (super.load_worker_id == null) {
            switch (type) {
                case 0:
                    AppBase.animeReleasePage = 0;
                    break;
                case 1:
                    AppBase.OVAONASpecialReleasePage = 0;
                    break;
                case 2:
                    AppBase.movieReleasePage = 0;
                    break;
                case 3:
                    AppBase.polnyiMetrReleasePage = 0;
                    break;
                case 4:
                    AppBase.documentaryReleasePage = 0;
                    break;
                case 5:
                    AppBase.doramaReleasePage = 0;
                    break;
            }
            fragment_ready = false;
            super.loaded_all = false;
            AppBase.releaseMatrix.get(this.type).clear();
            listView.removeAllViews();
            loadReleases();
        }
    }

    public void updateList(int items){
        if (items == -1){
            listView.removeAllViews();
            items = AppBase.releaseMatrix.get(type).size();
        }

        Log.wtf("ListView", "Add " + items);

        int rows = AppBase.releaseMatrix.get(type).size() / columnCount;

        listView.setRowCount(rows + 1);

        Point size = new Point();
        requireActivity().getWindowManager().getDefaultDisplay().getSize(size);
        int screenWidth = size.x;

        int itemWidth = screenWidth / columnCount;

        int start = AppBase.releaseMatrix.get(type).size() - items;
        for (int i = start; i < items; i++){
            int current_column = i % columnCount;
            int current_row = i / columnCount;

            Context context = getContext();

            if (context != null){
                ReleaseContentListParser.ContentListItem item = AppBase.releaseMatrix.get(type).get(i);

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
        errorView.setVisibility(View.GONE);

        Data.Builder args = new Data.Builder();
        args.putInt("release_type", type);

        // Создание процедуры для загрузки релизов
        WorkRequest loadWorkRequest = new OneTimeWorkRequest
                .Builder(ReleaseLoadWorker.class)
                .setInputData(args.build())
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
                        switch (workInfo.getState()){
                            case SUCCEEDED:
                                updateList(-1);
                                break;

                            case FAILED:
                                switch (type){
                                    case 0:
                                        AppBase.animeReleasePage--;
                                        break;
                                    case 1:
                                        AppBase.OVAONASpecialReleasePage--;
                                        break;
                                    case 2:
                                        AppBase.movieReleasePage--;
                                        break;
                                    case 3:
                                        AppBase.polnyiMetrReleasePage = 0;
                                        break;
                                    case 4:
                                        AppBase.documentaryReleasePage = 0;
                                        break;
                                    case 5:
                                        AppBase.doramaReleasePage = 0;
                                        break;
                                }

                                // Код ошибки 404 - страница не найдена - обозначает конец списка релизов
                                int error_code = workInfo.getOutputData().getInt("error_code", 0);
                                if (error_code == 404){
                                    Log.wtf("Релизы", "Конец списка");
                                    super.loaded_all = true;
                                }
                                else{
                                    String error = workInfo.getOutputData().getString("error");
                                    errorView.setText(error);
                                    errorView.setVisibility(View.VISIBLE);
                                }
                                break;

                            case CANCELLED:
                                switch (type){
                                    case 0:
                                        AppBase.animeReleasePage--;
                                        break;
                                    case 1:
                                        AppBase.OVAONASpecialReleasePage--;
                                        break;
                                    case 2:
                                        AppBase.movieReleasePage--;
                                        break;
                                    case 3:
                                        AppBase.polnyiMetrReleasePage = 0;
                                        break;
                                    case 4:
                                        AppBase.documentaryReleasePage = 0;
                                        break;
                                    case 5:
                                        AppBase.doramaReleasePage = 0;
                                        break;
                                }
                                break;

                            default:
                                return;
                        }

                        Log.wtf("Releases", "LoadOver " + workInfo.getState());
                        fragment_ready = true;
                        super.load_worker_id = null;
                        super.refresh.setRefreshing(false);
                    });
                });
    }

    public static class ReleaseLoadWorker extends LoadWorker {
        public ReleaseLoadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        public Result Work(Data input) throws IOException{
            int type = input.getInt("release_type", 0);

            String page_url = "https://a-g.site/relise/";
            switch (type){
                case 0:
                    AppBase.animeReleasePage++;
                    page_url += "anime-serialy";
                    page_url += "?page=" + AppBase.animeReleasePage;
                    break;
                case 1:
                    AppBase.OVAONASpecialReleasePage++;
                    page_url += "ovaonaspecial";
                    page_url += "?page=" + AppBase.OVAONASpecialReleasePage;
                    break;
                case 2:
                    AppBase.movieReleasePage++;

                    page_url += "movie";
                    page_url += "?page=" + AppBase.movieReleasePage;
                    break;
                case 3:
                    AppBase.polnyiMetrReleasePage++;
                    page_url += "polnyi-metr";
                    page_url += "?page=" + AppBase.polnyiMetrReleasePage;
                    break;
                case 4:
                    AppBase.documentaryReleasePage++;
                    page_url += "dokumentalnye";
                    page_url += "?page=" + AppBase.documentaryReleasePage;
                    break;
                case 5:
                    AppBase.doramaReleasePage++;
                    page_url += "doramy";
                    page_url += "?page=" + AppBase.doramaReleasePage;
                    break;
            }

            ArrayList<ReleaseContentListParser.ContentListItem> items = ReleaseContentListParser.parse(page_url);

            // Проверка релизов на повтор
            // Иногда страницы дублируют последний тайтл предыдущей страницы
            if (!AppBase.releaseMatrix.get(type).isEmpty()) {
                String last_title = AppBase.releaseMatrix.get(type).get(AppBase.releaseMatrix.get(type).size() - 1).title;
                ReleaseContentListParser.ContentListItem item = items.get(0);

                if (item.title.equals(last_title)) {
                    items.remove(0);
                }
            }

            AppBase.releaseMatrix.get(type).addAll(items);

            return null;
        }
    }
}