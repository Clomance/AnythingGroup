package com.example.AnythingGroup.fragments.releases;

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

public class ReleaseFragment extends Fragment {
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.release_fragment, container, false);

        ViewPager2 viewPager = root.findViewById(R.id.view_pager);
        TabLayout tabLayout = root.findViewById(R.id.tabLayout);

        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        TabPagesBuilder tabPagesBuilder = new TabPagesBuilder(this, tabLayout, viewPager);

        tabPagesBuilder.addPage("Аниме сериалы", new ReleaseSubfragment(0));
        tabPagesBuilder.addPage("OVA/ONA/Special", new ReleaseSubfragment(1));
        tabPagesBuilder.addPage("Movie", new ReleaseSubfragment(2));
        tabPagesBuilder.addPage("Полный метр", new ReleaseSubfragment(3));
        tabPagesBuilder.addPage("Документальные", new ReleaseSubfragment(4));
        tabPagesBuilder.addPage("Дорамы", new ReleaseSubfragment(5));

        tabPagesBuilder.build();

        return root;
    }
}