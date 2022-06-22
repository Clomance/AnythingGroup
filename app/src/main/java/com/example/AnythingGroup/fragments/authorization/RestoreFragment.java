package com.example.AnythingGroup.fragments.authorization;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.AnythingGroup.MainActivity;
import com.example.AnythingGroup.R;

import java.util.Objects;

public class RestoreFragment extends Fragment {
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.authorization_restore_fragment, container, false);

        // Установка заголовка страницы Восстановления
        MainActivity activity = Objects.requireNonNull((MainActivity) getActivity());
        activity.setToolbarTitle(null);

        return root;
    }
}