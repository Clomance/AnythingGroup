package com.example.AnythingGroup;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.work.Configuration;

import java.util.concurrent.Executors;

public class ExtendedApplication extends Application implements Configuration.Provider {
    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setExecutor(Executors.newFixedThreadPool(2))
                .build();
    }
}