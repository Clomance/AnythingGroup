package com.example.AnythingGroup.fragments;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;

// Строит элементы из страниц со вкладками.
public class TabPagesBuilder {
    private final ArrayList<String> tabNames = new ArrayList<>();

    private final TabLayout tabLayout;
    private final ViewPager2 viewPager;

    private final ViewPagesAdapter pageAdapter;

    private ViewPager2.OnPageChangeCallback pageChangeListener;

    public TabPagesBuilder(Fragment fragment, TabLayout tabLayout, ViewPager2 viewPager){
        pageAdapter = new ViewPagesAdapter(fragment);

        this.tabLayout = tabLayout;
        this.viewPager = viewPager;
    }

    // Добавление страницы.
    public void addPage(String name, Fragment page){
        tabNames.add(name);
        pageAdapter.addPage(page);
    }

    public Fragment getPage(int position){
        return pageAdapter.getPage(position);
    }

    public void setOnPageChangeListener(ViewPager2.OnPageChangeCallback pageChangeListener){
        this.pageChangeListener = pageChangeListener;
    }

    public void build(){
        viewPager.setAdapter(pageAdapter);

        if (pageChangeListener != null){
            viewPager.registerOnPageChangeCallback(pageChangeListener);
        }

        new TabLayoutMediator(
                tabLayout,
                viewPager,
                // Настройки вкладок (все прошлые действия со вкладками сбрасываются,
                // кроме самой настройки панели вкладок)
                (TabLayout.Tab tab, int position) -> tab.setText(tabNames.get(position))
        ).attach();
    }

    public static class ViewPagesAdapter extends FragmentStateAdapter {
        ArrayList<Fragment> pages = new ArrayList<>(2);

        public ViewPagesAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return this.pages.get(position);
        }

        @Override
        public int getItemCount() {
            return this.pages.size();
        }

        public void addPage(Fragment page){
            this.pages.add(page);
        }

        public Fragment getPage(int position){
            return pages.get(position);
        }
    }
}
