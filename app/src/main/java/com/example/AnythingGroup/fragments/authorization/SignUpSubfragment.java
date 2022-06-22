package com.example.AnythingGroup.fragments.authorization;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.AnythingGroup.R;

public class SignUpSubfragment extends Fragment {
    EditText name;
    EditText email;
    EditText password;
    EditText confirm_password;
    Button signUpButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.authorization_sign_up_subfragment, container, false);

        signUpButton = root.findViewById(R.id.sign_up_button);
        signUpButton.setOnClickListener(this::signUp);

        name = root.findViewById(R.id.sign_up_name);
        email = root.findViewById(R.id.sign_up_email);
        password = root.findViewById(R.id.sign_up_password);
        confirm_password = root.findViewById(R.id.sign_up_confirm_password);

        return root;
    }

    public void signUp(View view){

    }
}