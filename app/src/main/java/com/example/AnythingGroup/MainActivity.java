package com.example.AnythingGroup;

import android.os.Bundle;
import android.view.Menu;
import android.widget.ImageView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private AppBarConfiguration mAppBarConfiguration;
    // Управление навигацией по страницам
    public NavController navController;
    public DrawerLayout drawer;

    public Toolbar toolbar;

    /// Слой поверх всего интерфейса,
    /// который блокирует нажатия и свайпы.
    /// На нём расположена иконка загрузки.
    /// Используется при попытке авторизации.
    public ConstraintLayout authorization_overlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);

        // Установка лого текущего времени года
        ImageView navigation_header_logo = navigationView
                .getHeaderView(0)
                .findViewById(R.id.navigation_header_logo);
        switch (AppBase.logoOption){
            case Default:
                navigation_header_logo.setImageResource(R.drawable.logo_default);
                break;
            case NewYear:
                navigation_header_logo.setImageResource(R.drawable.logo_new_year);
                break;
            case Spring:
                navigation_header_logo.setImageResource(R.drawable.logo_spring);
                break;
            case Cosmos:
                navigation_header_logo.setImageResource(R.drawable.logo_cosmos);
                break;
            case Halloween:
                navigation_header_logo.setImageResource(R.drawable.logo_halloween);
                break;
        }

        // Добавляем все страницы для возможности перехода
        mAppBarConfiguration = new AppBarConfiguration.Builder(
//                R.id.fragment_my_profile,
                R.id.fragment_news,
                R.id.fragment_releases,
                R.id.fragment_authorization,
                R.id.fragment_search
        ).setOpenableLayout(drawer).build();

        NavHostFragment hostFragment = (NavHostFragment) this
                .getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = Objects.requireNonNull(hostFragment).getNavController();
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_app_bar, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
//        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    /// Настраивает и включает заголовок на панели управления.
    /// Выключает выпадающий список.
    /// Если значение null, то остаётся старое значение.
    public void setToolbarTitle(String title){
        ActionBar bar = Objects.requireNonNull(getSupportActionBar());
        bar.setDisplayShowTitleEnabled(true);

        if (title != null){
            bar.setTitle(title);
        }
    }
}