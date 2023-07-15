package com.example.AnythingGroup.fragments;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.WorkManager;

import com.example.AnythingGroup.extendedUI.ExtendedScrollView;
import com.example.AnythingGroup.fragments.releases.ReleaseContentListParser;


public abstract class ContentListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, ExtendedScrollView.OnScrollListener {
    protected WorkManager workManager;

    protected TextView errorView;
    protected SwipeRefreshLayout refreshLayout;
    protected ExtendedScrollView scrollView;

    protected ReleaseContentListParser.ContentListState contentListState;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        workManager = WorkManager.getInstance(context);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Пролистывание новостей
        scrollView.setOnScrollListener(this);

        // Обновление новостей при свайпе
        refreshLayout.setOnRefreshListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // При возобновлении работы (при переходе или после сворачивания) страницы проверяется, идёт ли загрузка записей (постов)
        // Если нет, то проверяется, пуст ли список записей
        if (contentListState.state == ReleaseContentListParser.ContentState.Loading) {
            // Баг со съезжанием иконки загрузки; решение взято отсюда -
            // https://stackoverflow.com/questions/41854351/visual-bug-when-using-swipe-refresh-layout
            refreshLayout.setProgressViewOffset(false, 0, 50);
            refreshLayout.setRefreshing(true);
        }
        else {
            refreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onDestroy() {
        if (contentListState.state == ReleaseContentListParser.ContentState.Loading) {
            workManager.cancelWorkById(contentListState.workerId);
        }
        super.onDestroy();
    }
}