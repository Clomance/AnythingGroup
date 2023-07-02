package com.example.AnythingGroup.fragments.news;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkRequest;
import androidx.work.WorkerParameters;

import com.example.AnythingGroup.AppBase;
import com.example.AnythingGroup.LoadWorker;
import com.example.AnythingGroup.Network;
import com.example.AnythingGroup.R;
import com.example.AnythingGroup.fragments.ContentListFragment;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;


public class NewsSubfragment extends ContentListFragment {
    // Указывает на готовность фрагмента - окончание загрузки данных
    private boolean fragment_ready = false;

    private TextView errorView;

    private LinearLayout listView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.news_subfragment, container, false);

        errorView = root.findViewById(R.id.error);

        // Сам список, который показывается на экране
        listView = root.findViewById(R.id.list);

        super.refresh = root.findViewById(R.id.refresh);

        ScrollView scrollView = root.findViewById(R.id.scrollView);

        Log.wtf("News", "Create");

        // Загрузка более ранних новостей при достижении конца списка
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (!scrollView.canScrollVertically(1) && fragment_ready) {
                // Если ничего не загружается и не загружены все новости, то догрузить ещё
                if (super.load_worker_id == null && !super.loaded_all) {
                    Log.wtf("News", "Load");
                    fragment_ready = false;
                    super.refresh.setRefreshing(true);
                    loadNews();

                }
            }
        });

        // Обновление новостей при свайпе
        super.refresh.setOnRefreshListener(this);

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (AppBase.commonNewsList.isEmpty()){
            fragment_ready = false;
            loadNews();
            Log.wtf("News", "onResumeLoad");
        }
        else{
            updateList();
            fragment_ready = true;
            Log.wtf("News", "onResumeShow");
        }
    }

    @Override
    public void onRefresh() {
        Log.wtf("Reload", "News");
        if (super.load_worker_id == null) {
            fragment_ready = false;
            AppBase.commonNewsPage = 0;
            super.loaded_all = false;
            AppBase.commonNewsList.clear();
            loadNews();
        }
    }

    public void updateList(){
        listView.removeAllViews();
        for (NewsListItem item: AppBase.commonNewsList) {
            Context context = this.getContext();
            if (context != null) {
                View view = View.inflate(context, R.layout.news_list_item, null);

                ImageView image = view.findViewById(R.id.image);
                if (item.image != null){
                    image.setImageBitmap(item.image);
                }

                TextView title = view.findViewById(R.id.title);
                title.setText(item.title);

                TextView description = view.findViewById(R.id.description);
                if (item.description != null) {
                    description.setText(item.description);
                }

                TextView time = view.findViewById(R.id.datetime);
                time.setText(item.datetime);

                Log.wtf("News", "date " + item.datetime);

                listView.addView(view);
            }
        }
    }

    /// Загружает новости и добавляет их в конец.
    public void loadNews(){
        Log.wtf("News", "Load");
        AppBase.commonNewsPage++;

        // Создание процедуры для авторизации
        WorkRequest loadWorkRequest = new OneTimeWorkRequest
                .Builder(NewsSubfragment.NewsLoadWorker.class)
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
                                updateList();
                                break;

                            case FAILED:
                                AppBase.commonNewsPage--;
                                // Код ошибки 404 - страница не найдена - обозначает конец списка релизов
                                int error_code = workInfo.getOutputData().getInt("error_code", 0);
                                if (error_code == 404){
                                    Log.wtf("Новости", "Конец списка");
                                    super.loaded_all = true;
                                }
                                else {
                                    String error = workInfo.getOutputData().getString("error");
                                    errorView.setText(error);

                                    errorView.setVisibility(View.VISIBLE);
                                }
                                break;

                            case CANCELLED:
                                AppBase.commonNewsPage--;
                                break;

                            default:
                                return;
                        }

                        fragment_ready = true;
                        super.load_worker_id = null;
                        super.refresh.setRefreshing(false);
                    });
                });
    }

    public static class NewsLoadWorker extends LoadWorker {
        public NewsLoadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        public Result Work(Data input) throws IOException {
            Document document = Network.get("https://a-g.site/news?page=" + AppBase.commonNewsPage);

            Element list = document.getElementsByClass("default_list_one").get(0);
            Elements items = list.getElementsByClass("dlo_item_row");

            Log.wtf("News", "Items: " + items.size());

            for (Element item: items) {
                NewsListItem newsItem = new NewsListItem();

                Element image_container = item.getElementsByClass("dlo_image").get(0);
                Element reference_container = image_container.getElementsByTag("a").get(0);
                newsItem.reference = reference_container.attributes().get("href");

                Element image = reference_container.getElementsByTag("img").get(0);
                newsItem.image = AppBase.loadImageFromURL(image.attributes().get("src"));

                Element info_container = item.getElementsByClass("dlo_fields").get(0);

                Element title_container = info_container.getElementsByClass("dlo_fields_title").get(0);
                newsItem.title = title_container.getElementsByTag("a").get(0).html();

                Element date_container = info_container.getElementsByClass("dlo_date").get(0);
                newsItem.datetime = date_container.text();

                Elements description_check = info_container.getElementsByClass("dlo_teaser");
                if (description_check.size() != 0){
                    newsItem.description = description_check.get(0).html();
                }

                AppBase.commonNewsList.add(newsItem);
            }

            return null;
        }
    }
}