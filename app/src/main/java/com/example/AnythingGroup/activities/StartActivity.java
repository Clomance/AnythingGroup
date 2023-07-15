package com.example.AnythingGroup.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.util.Calendar;

import com.example.AnythingGroup.AppBase;
import com.example.AnythingGroup.R;
import com.example.AnythingGroup.fragments.authorization.SignInSubfragment;

/// Стартовая активность.
/// Загружает необходымые данные.
/// Отвечает за автоматический вход.
public class StartActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        ImageView start_image = findViewById(R.id.start_image);

        // Определение текущего сезона и
        // установка соответствующего лого
        Calendar currentTime = Calendar.getInstance();
        // Начинается с нуля
        int month = currentTime.get(Calendar.MONTH);
        // Начинается с единицы
        int day = currentTime.get(Calendar.DAY_OF_MONTH);

        switch (month){
            // Зимний сезон
            case 0:
                if (day > 7) {
                    break;
                }
            case 11:
                if (day < 24){
                    break;
                }
                AppBase.logoOption = AppBase.LogoOption.NewYear;
                start_image.setImageResource(R.drawable.logo_new_year);
                break;
            case 1:
                break;

            // Весенний сезон
            case 3:
                // День космонавтики (12 апреля)
                if (day == 12){
                    AppBase.logoOption = AppBase.LogoOption.Cosmos;
                    start_image.setImageResource(R.drawable.logo_cosmos);
                    break;
                }
            case 2: case 4:
                AppBase.logoOption = AppBase.LogoOption.Spring;
                start_image.setImageResource(R.drawable.logo_spring);
                break;

            // Летний сезон
            case 5: case 6: case 7:
                AppBase.logoOption = AppBase.LogoOption.Default;
                start_image.setImageResource(R.drawable.logo_default);
                break;

            // Осенний сезон
            case 8:
                break;
            case 9:
                // Хеллоуин
                if (day == 31){
                    AppBase.logoOption = AppBase.LogoOption.Halloween;
                    start_image.setImageResource(R.drawable.logo_halloween);
                }
                break;
            case 10:
                // Хеллоуин
                if (day == 1){
                    AppBase.logoOption = AppBase.LogoOption.Halloween;
                    start_image.setImageResource(R.drawable.logo_halloween);
                }
                break;

            default:
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // Добавление ссылок в матрицу релизов
        AppBase.releases.releaseMatrix.add(AppBase.releases.animeReleaseList);
        AppBase.releases.releaseMatrix.add(AppBase.releases.OVAONASpecialReleaseList);
        AppBase.releases.releaseMatrix.add(AppBase.releases.movieReleaseList);
        AppBase.releases.releaseMatrix.add(AppBase.releases.polnyiMetrReleaseList);
        AppBase.releases.releaseMatrix.add(AppBase.releases.documentaryReleaseList);
        AppBase.releases.releaseMatrix.add(AppBase.releases.doramaReleaseList);

        // Получение ссылки на локальный файл настроек
        AppBase.getSharedPreferences(this);
        // Проверка разрешений
        AppBase.checkPermissions(this);

        if (!AppBase.loadLoginPassword()){
            StartActivity.this.startActivity(new Intent(StartActivity.this, MainActivity.class));
            StartActivity.this.finish();
            return;
        }

        Log.wtf("Start", "Auto sign in");

        Data.Builder args = new Data.Builder();
        args.putString("email", AppBase.user.email);
        args.putString("password", AppBase.user.password);

        // Создание процедуры для авторизации
        WorkRequest startWorkRequest = new OneTimeWorkRequest
                .Builder(SignInSubfragment.SignInWorker.class)
                .setInputData(args.build())
                .build();

        WorkManager workManager = WorkManager.getInstance(this);
        // Добавление процедуры в очередь выполнения
        workManager.enqueue(startWorkRequest);

        // Подключение функции для ожидания завершения загрузки
        workManager.getWorkInfoByIdLiveData(startWorkRequest.getId())
                .observe(this, workInfo -> {
                    if (workInfo.getState() == WorkInfo.State.SUCCEEDED || workInfo.getState() == WorkInfo.State.FAILED) {
                        StartActivity.this.startActivity(new Intent(StartActivity.this, MainActivity.class));
                        StartActivity.this.finish();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        AppBase.onRequestPermissionsResult(requestCode, grantResults);
    }
}