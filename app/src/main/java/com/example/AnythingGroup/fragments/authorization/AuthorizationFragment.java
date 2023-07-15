package com.example.AnythingGroup.fragments.authorization;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.AnythingGroup.activities.MainActivity;
import com.example.AnythingGroup.fragments.TabPagesBuilder;
import com.example.AnythingGroup.R;
import com.google.android.material.tabs.TabLayout;

public class AuthorizationFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.authorization_fragment, container, false);

        MainActivity activity = (MainActivity) requireActivity();
        activity.authorization_overlay = root.findViewById(R.id.authorization_overlay);

        ViewPager2 viewPager = root.findViewById(R.id.view_pager);
        TabLayout tabLayout = root.findViewById(R.id.tabLayout);

        TabPagesBuilder tabPagesBuilder = new TabPagesBuilder(this, tabLayout, viewPager);

        // (Какой-то баг с определением длины слова "Вход",
        // поэтому там пробел в конце (может стоит сменить шрифт?))
        tabPagesBuilder.addPage("Вход ", new SignInSubfragment());
//        tabPagesBuilder.addPage("Регистрация", new SignUpSubfragment(overlay));

        tabPagesBuilder.build();

        return root;
    }
}