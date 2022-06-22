package com.example.AnythingGroup.fragments.search;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.WorkerParameters;

import com.example.AnythingGroup.AppBase;
import com.example.AnythingGroup.LoadWorker;
import com.example.AnythingGroup.MainActivity;
import com.example.AnythingGroup.R;
import com.example.AnythingGroup.fragments.title.TitleMain;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class SearchFragment extends Fragment {
    private UUID load_worker_id = null;

    private TextView errorView;

    private ProgressBar progress;

    private TextView search_text;

    private LinearLayout list;

    private static final ArrayList<SearchReleaseItem> searchResultList =  new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.search_fragment, container, false);

        errorView = root.findViewById(R.id.errorView);

        progress = root.findViewById(R.id.search_fragment_progress);

        search_text = root.findViewById(R.id.search_fragment_text);

        search_text.setOnEditorActionListener((v, actionId, event) -> {
            boolean handled = false;
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                Log.wtf("Search", "Enter");
                search();

                InputMethodManager inputManager = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(SearchFragment.this.requireActivity().getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
                handled = true;
            }
            return handled;
        });

        list = root.findViewById(R.id.search_fragment_list);

        ImageView search_button = root.findViewById(R.id.search_fragment_button);
        search_button.setOnClickListener(this::searchButton);

        return root;
    }

    @Override
    public void onDestroy() {
        if (load_worker_id != null) {
            WorkManager.getInstance(requireContext()).cancelWorkById(load_worker_id);
            load_worker_id = null;
        }
        super.onDestroy();
    }

    public void updateList(){
        list.removeAllViews();
        for (SearchReleaseItem item: searchResultList) {
            Context context = this.getContext();
            if (context != null) {
                View view = View.inflate(context, R.layout.search_release_item, null);

                ImageView image = view.findViewById(R.id.image);
                image.setImageBitmap(item.image);

                TextView title = view.findViewById(R.id.title);

                title.setText(AppBase.textFormatter(context, item.title, null));

                TextView description = view.findViewById(R.id.description);
                if (item.description != null) {
                    description.setText(item.description);
                }

                View.OnClickListener onClickListener = view1 -> {
                    TitleMain titleMainArguments = new TitleMain();
                    titleMainArguments.reference = item.reference;

                    // Упаковка агрументов
                    Bundle args = new Bundle();
                    args.putParcelable("title_main", titleMainArguments);

                    // Переход к странице тайтла
                    MainActivity activity = (MainActivity) requireActivity();
                    activity.navController.navigate(R.id.fragment_title, args);
                };
                view.setOnClickListener(onClickListener);

                list.addView(view);
            }
        }
    }

    public void searchButton(View view){
        if (load_worker_id == null) {
            InputMethodManager inputManager = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(SearchFragment.this.requireActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            search();
        }
    }

    public void search(){
        progress.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);

        String searchRequestText = search_text.getText().toString();
        String[] splitSearchRequestText = searchRequestText.split(" ");
        StringBuilder searchRequest = new StringBuilder("https://a-g.site/search/relise?q=");

        for (int i = 0; i < splitSearchRequestText.length - 1; i++){
            searchRequest.append(splitSearchRequestText[i]);
            searchRequest.append("+");
        }
        searchRequest.append(splitSearchRequestText[splitSearchRequestText.length - 1]);

        searchResultList.clear();

        WorkRequest loadWorkRequest = new OneTimeWorkRequest
                .Builder(SearchResultLoadWorker.class)
                .setInputData(new Data.Builder().putString("search_request", searchRequest.toString()).build())
                .build();

        WorkManager workManager = WorkManager.getInstance(requireContext());
        // Добавление процедуры в очередь выполнения
        workManager.enqueue(loadWorkRequest);

        load_worker_id = loadWorkRequest.getId();

        // Подключение функции для ожидания завершения загрузки
        workManager.getWorkInfoByIdLiveData(load_worker_id)
                .observe(getViewLifecycleOwner(), workInfo -> {
                    switch (workInfo.getState()){
                        case SUCCEEDED:
                            updateList();
                            break;

                        case FAILED:
                            searchResultList.clear();
                            String error = workInfo.getOutputData().getString("error");
                            errorView.setText(error);
                            errorView.setVisibility(View.VISIBLE);
                            updateList();
                            break;

                        case CANCELLED:
                            break;

                        default:
                            return;
                    }

                    progress.setVisibility(View.GONE);
                    load_worker_id = null;
                });
    }

    public static class SearchResultLoadWorker extends LoadWorker {
        public SearchResultLoadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        public Result Work(Data input) throws IOException{
            String searchRequest = input.getString("search_request");

            Log.wtf("search_request", searchRequest);

            Data.Builder output = new Data.Builder();
            assert searchRequest != null;
            Document document = Jsoup.connect(searchRequest).get();

            Elements list_check = document.getElementsByClass("widget_content_list");
            if (list_check.size() == 0){
                output.putString("error", "Ничего не найдено");
                return Result.failure(output.build());
            }
            Element list = list_check.get(0);
            Elements items = list.getElementsByClass("item");

            for (Element item: items) {
                SearchReleaseItem searchItem = new SearchReleaseItem();

                Element image_container = item.getElementsByClass("image").get(0);
                Element reference_container = image_container.getElementsByTag("a").get(0);
                searchItem.reference = reference_container.attributes().get("href");

                Element image = reference_container.getElementsByTag("img").get(0);
                searchItem.image = AppBase.loadImageFromURL(image.attributes().get("src"));

                Element info_container = item.getElementsByClass("info").get(0);

                Element title_container = info_container.getElementsByClass("title").get(0);
                searchItem.title = title_container.getElementsByTag("a").get(0).html();

                Elements description_check = info_container.getElementsByClass("teaser");
                if (description_check.size() != 0){
                    searchItem.description = description_check.get(0).html();
                }

                searchResultList.add(searchItem);
            }

            return null;
        }
    }
}