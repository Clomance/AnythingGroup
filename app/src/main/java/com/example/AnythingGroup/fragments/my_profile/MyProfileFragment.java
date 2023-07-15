package com.example.AnythingGroup.fragments.my_profile;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.UUID;

public class MyProfileFragment extends Fragment {
    private UUID profile_load_worker_id = null;

    private TextView profileError;

    private ImageView profileImage;
    private TextView profileRating;
    private TextView profileReputation;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.my_profile_fragment, container, false);

        if (AppBase.user.authorized){
            profileError = root.findViewById(R.id.ProfileErrorView);

            profileImage = root.findViewById(R.id.ProfileImage);
            profileRating = root.findViewById(R.id.ProfileRating);
            profileReputation = root.findViewById(R.id.ProfileReputation);

            Button signOut = root.findViewById(R.id.ProfileSignOut);
            signOut.setOnClickListener(this::signOut);

            if (AppBase.user.additionalInfo != null){
                setProfileInfo();
            }
        }

        return root;
    }

    @Override
    public void onStart(){
        super.onStart();

        MainActivity activity = (MainActivity) requireActivity();
        if (!AppBase.user.authorized) {
            activity.navController.navigate(R.id.fragment_authorization);
            return;
        }
        else{
            activity.setToolbarTitle(AppBase.user.mainInfo.name);
        }

        if (AppBase.user.additionalInfo == null) {
            WorkRequest loadWorkRequest = new OneTimeWorkRequest
                    .Builder(ProfileLoadWorker.class)
                    .build();

            profile_load_worker_id = loadWorkRequest.getId();

            WorkManager.getInstance(requireContext()).enqueue(loadWorkRequest);

            WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(profile_load_worker_id)
                    .observe(getViewLifecycleOwner(), workInfo -> {
                        switch (workInfo.getState()) {
                            case SUCCEEDED:
                                setProfileInfo();
                                break;

                            case FAILED:
                                String error = workInfo.getOutputData().getString("error");

                                profileError.setText(error);
                                profileError.setVisibility(View.VISIBLE);
                                break;

                            default:
                                return;
                        }

                        profile_load_worker_id = null;
                    });
        }
    }

    @Override
    public void onDestroy() {
        if (profile_load_worker_id != null) {
            WorkManager.getInstance(requireContext()).cancelWorkById(profile_load_worker_id);
            profile_load_worker_id = null;
        }
        super.onDestroy();
    }

    public void setProfileInfo(){
        // Изображение
        profileImage.setImageBitmap(AppBase.user.additionalInfo.image);

        // Райтинг
        String text = "Рейтинг: " + AppBase.user.additionalInfo.rating;
        profileRating.setText(text);

        // Репутация
        text = "Репутация: " + AppBase.user.additionalInfo.reputation;
        profileReputation.setText(text);
    }

    public void signOut(View view){
        AppBase.setAutoAuthorisation(false);

        AppBase.user.authorized = false;

        MainActivity activity = (MainActivity) requireActivity();
        activity.navController.navigate(R.id.fragment_authorization);
    }

    public static class ProfileLoadWorker extends LoadWorker {
        public ProfileLoadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        public Result Work(Data input) throws IOException {
            ProfileAdditionalInfo ProfileAdditional = new ProfileAdditionalInfo();

            Connection.Response response = Jsoup.connect("https://a-g.site/users/" + AppBase.user.mainInfo.id)
                    .followRedirects(true)
                    .method(Connection.Method.GET)
                    .cookies(Network.cookies)
                    .execute();

            Network.cookies.putAll(response.cookies());

            Document document = response.parse();

            // Изображение
            Element user_profile = document.getElementById("user_profile");
            assert user_profile != null;
            Element profile_image = user_profile.getElementsByClass("img-fluid").get(0);
            String profile_image_reference = profile_image.attributes().get("src");
            ProfileAdditional.image = Network.getImageFromURL(profile_image_reference);

            // Рейтинг
            Element profile_rating_container = document.getElementById("user_profile_ratings");
            assert profile_rating_container != null;
            Element profile_rating_element = profile_rating_container.getElementsByTag("span").get(0);
            ProfileAdditional.rating = Integer.parseInt(profile_rating_element.html().trim());

            // Репутация
            Element profile_reputation_container = document.getElementById("user_profile_rates");
            assert profile_reputation_container != null;
            Element profile_reputation_element = profile_reputation_container.getElementsByTag("span").get(0);
            ProfileAdditional.reputation = Integer.parseInt(profile_reputation_element.html().trim());



            AppBase.user.additionalInfo = ProfileAdditional;
            return null;
        }
    }
}