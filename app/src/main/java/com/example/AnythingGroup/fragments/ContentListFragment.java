package com.example.AnythingGroup.fragments;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.WorkManager;

import java.util.UUID;

public abstract class ContentListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    protected WorkManager workManager;

    protected UUID load_worker_id = null;

    // Флаг, показывающий, что все новости загружены
    protected boolean loaded_all = false;

    protected SwipeRefreshLayout refresh;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        workManager = WorkManager.getInstance(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        // При возобновлении работы (при переходе или после сворачивания) страницы проверяется, идёт ли загрузка записей (постов)
        // Если нет, то проверяется, пуст ли список записей
        if (load_worker_id != null) {
            // Баг со съезжанием иконки загрузки; решение взято отсюда -
            // https://stackoverflow.com/questions/41854351/visual-bug-when-using-swipe-refresh-layout
            refresh.setProgressViewOffset(false, 0, 50);
            refresh.setRefreshing(true);
        }
        else {
            refresh.setRefreshing(false);
        }
    }

    @Override
    public void onDestroy() {
        Log.i("ContentListFragment", "onDestroy");

        if (this.load_worker_id != null) {
            workManager.cancelWorkById(this.load_worker_id);
            this.load_worker_id = null;
        }
        super.onDestroy();
    }
}