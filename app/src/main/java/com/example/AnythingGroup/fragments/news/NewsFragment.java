package com.example.AnythingGroup.fragments.news;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.AnythingGroup.fragments.TabPagesBuilder;
import com.example.AnythingGroup.R;
import com.google.android.material.tabs.TabLayout;

public class NewsFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.news_fragment, container, false);

        ViewPager2 viewPager = root.findViewById(R.id.view_pager);
        TabLayout tabLayout = root.findViewById(R.id.tabLayout);

        TabPagesBuilder tabPagesBuilder = new TabPagesBuilder(this, tabLayout, viewPager);

        tabPagesBuilder.addPage("Новости", new NewsSubfragment());
        tabPagesBuilder.addPage("Релизы", new NewsReleasesSubfragment());

        tabPagesBuilder.build();

        return root;
    }
}