package com.example.AnythingGroup.fragments.authorization;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.WorkerParameters;

import com.example.AnythingGroup.AppBase;
import com.example.AnythingGroup.LoadWorker;
import com.example.AnythingGroup.activities.MainActivity;
import com.example.AnythingGroup.Network;
import com.example.AnythingGroup.R;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class SignInSubfragment extends Fragment {
    private UUID sign_in_worker_id = null;

    private TextView errorView;

    private EditText emailView;
    private EditText passwordView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.authorization_sign_in_subfragment, container, false);

        errorView = root.findViewById(R.id.sign_in_error_view);

        emailView = root.findViewById(R.id.sign_in_email);
        passwordView = root.findViewById(R.id.sign_in_password);

        Button signInButton = root.findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(this::signIn);

        Button signInButtonRestore = root.findViewById(R.id.sign_in_button_restore);
        signInButtonRestore.setOnClickListener(this::restore);

        return root;
    }

    @Override
    public void onDestroy() {
        // Отмена загрузки данных тайтла при закрытии фрагмента
        if (sign_in_worker_id != null) {
            WorkManager.getInstance(requireContext()).cancelWorkById(sign_in_worker_id);
            sign_in_worker_id = null;
        }
        super.onDestroy();
    }

    public void signIn(View view) {
        errorView.setVisibility(View.GONE);
        MainActivity activity = (MainActivity) requireActivity();
        activity.authorization_overlay.setVisibility(View.VISIBLE);

        InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);

        String email = emailView.getText().toString().trim();
        String password = passwordView.getText().toString().trim();

        Data.Builder args = new Data.Builder();
        args.putString("email", email);
        args.putString("password", password);

        // Создание процедуры для загрузки релизов
        WorkRequest signInWorkRequest = new OneTimeWorkRequest
                .Builder(SignInWorker.class)
                .setInputData(args.build())
                .build();

        sign_in_worker_id = signInWorkRequest.getId();

        WorkManager.getInstance(requireContext()).enqueue(signInWorkRequest);

        WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(sign_in_worker_id)
                .observe(getViewLifecycleOwner(), workInfo -> {
                    switch (workInfo.getState()) {
                        case SUCCEEDED:
                            activity.navController.popBackStack();
                            break;

                        case FAILED:
                            String error = workInfo.getOutputData().getString("error");

                            activity.authorization_overlay.setVisibility(View.GONE);

                            errorView.setText(error);
                            errorView.setVisibility(View.VISIBLE);
                            break;

                        case CANCELLED:
                            AppBase.user.authorized = false;
                        default:
                            return;
                    }

                    sign_in_worker_id = null;
                });
    }

    /// Переход к странице восстановления.
    public void restore(View view) {
        MainActivity activity = (MainActivity) getActivity();
        Objects.requireNonNull(activity).navController.navigate(R.id.fragment_authorization_restore);
    }

    public static class SignInWorker extends LoadWorker {
        public SignInWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        public Result Work(Data input) throws IOException {
            String email = input.getString("email");
            String password = input.getString("password");
            assert email != null;
            assert password != null;

            Connection.Response response = Jsoup.connect("https://a-g.site/auth/login")
                    .followRedirects(true)
                    .method(Connection.Method.GET)
                    .execute();

            Network.cookies.putAll(response.cookies());

            Document document = response.parse();

            Elements forms = document.getElementsByTag("meta");

            Network.token = "";
            for (Element form: forms){
                Attributes attributes = form.attributes();
                if (attributes.get("name").equals("csrf-token")){
                    Network.token = attributes.get("content");
                    break;
                }
            }
            Log.wtf("token", Network.token);

            response = Jsoup.connect("https://a-g.site/auth/login")
                    .referrer("https://a-g.site/auth/login")
                    .data("login_email", email)
                    .data("login_password", password)
                    .data("remember", "1")
                    .data("csrf_token", Network.token)
                    .data("submit", "Войти")
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .cookies(Network.cookies)
                    .method(Connection.Method.POST)
                    .execute();

            Network.cookies.putAll(response.cookies());

            document = response.parse();

            Elements check_sign_in = document.getElementsByClass("widget_user_avatar");
            if (check_sign_in.size() != 0){
                Log.wtf("SignIn", "Ok");

                AppBase.user.authorized = true;
                AppBase.user.email = email;
                AppBase.user.password = password;
                AppBase.saveLoginPassword();
            }
            else{
                Log.wtf("SignIn", "Err");
                Data.Builder output = new Data.Builder();
                output.putString("error", "Вход не выполнен. Проверьте правильность адреса e-mail и пароля.");
                return Result.failure(output.build());
            }

            Element profile_info_container = check_sign_in.get(0);

            // ID профиля
            Element profile_id_container = profile_info_container.getElementsByClass("profile").get(0);
            String profile_reference = profile_id_container.getElementsByTag("a").get(0).attributes().get("href");
            int profile_id_divider = profile_reference.lastIndexOf("/");
            String profile_id = profile_reference.substring(profile_id_divider + 1);
            AppBase.user.mainInfo.id = Integer.parseInt(profile_id);

            Log.wtf("ProfileId", profile_id);

            // Имя профиля
            Element profile_name_container = profile_info_container.getElementsByClass("name").get(0);
            AppBase.user.mainInfo.name = profile_name_container.getElementsByTag("a").html().trim();

            return null;
        }
    }
}