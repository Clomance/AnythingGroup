package com.example.AnythingGroup.fragments.news;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkRequest;
import androidx.work.WorkerParameters;

import com.example.AnythingGroup.AppBase;
import com.example.AnythingGroup.LoadWorker;
import com.example.AnythingGroup.Network;
import com.example.AnythingGroup.R;
import com.example.AnythingGroup.extendedUI.ExtendedScrollView;
import com.example.AnythingGroup.fragments.ContentListFragment;
import com.example.AnythingGroup.fragments.releases.ReleaseContentListParser;
import com.example.AnythingGroup.fragments.releases.ReleaseLoadWorker;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;


public class NewsSubfragment extends ContentListFragment {
    private LinearLayout listView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        super.contentListState = AppBase.news.commonNewsList.state;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.news_subfragment, container, false);

        super.errorView = root.findViewById(R.id.error);
        super.refreshLayout = root.findViewById(R.id.refresh);
        super.scrollView = root.findViewById(R.id.scrollView);

        // Сам список, который показывается на экране
        listView = root.findViewById(R.id.list);

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (super.contentListState.state == ReleaseContentListParser.ContentState.None){
            loadNews();
            Log.wtf("News", "onStartLoad");
        }
        else{
            updateList();
            Log.wtf("News", "onStartShow");
        }
    }

    @Override
    public void onRefresh() {
        Log.wtf("Reload", "News");
        if (super.contentListState.state != ReleaseContentListParser.ContentState.Loading) {
            AppBase.news.commonNewsList.state.pagesLoaded = 0;
            AppBase.news.commonNewsList.clear();
            loadNews();
        }
    }


    @Override
    public void onEndReached(ExtendedScrollView scrollView) {
        // Если ничего не загружается и не загружены все новости, то догрузить ещё
        if (super.contentListState.state == ReleaseContentListParser.ContentState.Loaded) {
            super.refreshLayout.setRefreshing(true);
            loadNews();
        }
    }

    public void updateList(){
        listView.removeAllViews();
        for (NewsListItem item: AppBase.news.commonNewsList) {
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
        super.errorView.setVisibility(View.GONE);

        super.contentListState.state = ReleaseContentListParser.ContentState.Loading;

        super.contentListState.pagesLoaded++;

        // Создание процедуры для авторизации
        WorkRequest loadWorkRequest = new OneTimeWorkRequest
                .Builder(NewsSubfragment.NewsLoader.class)
                .build();

        // Добавление процедуры в очередь выполнения
        super.workManager.enqueue(loadWorkRequest);

        super.contentListState.workerId = loadWorkRequest.getId();

        // Подключение функции для ожидания завершения загрузки
        super.workManager.getWorkInfoByIdLiveData(super.contentListState.workerId).observe(
                getViewLifecycleOwner(),
                workInfo -> {
                    switch (workInfo.getState()){
                        case SUCCEEDED:
                            if (listView == null) return;
                            listView.post(() -> {
                                updateList();
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

    public static class NewsLoader extends ReleaseLoadWorker {
        public NewsLoader(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        public Result Work(Data input) throws IOException {
            super.contentListState = AppBase.news.commonNewsList.state;

            Document document = Network.get("https://a-g.site/news?page=" + AppBase.news.commonNewsList.state.pagesLoaded);

            Element list = document.getElementsByClass("default_list_one").get(0);
            Elements items = list.getElementsByClass("dlo_item_row");

            Log.wtf("News", "Items: " + items.size());

            for (Element item: items) {
                NewsListItem newsItem = new NewsListItem();

                Element image_container = item.getElementsByClass("dlo_image").get(0);
                Element reference_container = image_container.getElementsByTag("a").get(0);
                newsItem.reference = reference_container.attributes().get("href");

                Element image = reference_container.getElementsByTag("img").get(0);
                newsItem.image = Network.getImageFromURL(image.attributes().get("src"));

                Element info_container = item.getElementsByClass("dlo_fields").get(0);

                Element title_container = info_container.getElementsByClass("dlo_fields_title").get(0);
                newsItem.title = title_container.getElementsByTag("a").get(0).html();

                Element date_container = info_container.getElementsByClass("dlo_date").get(0);
                newsItem.datetime = date_container.text();

                Elements description_check = info_container.getElementsByClass("dlo_teaser");
                if (description_check.size() != 0){
                    newsItem.description = description_check.get(0).html();
                }

                AppBase.news.commonNewsList.add(newsItem);
            }

            return null;
        }
    }
}