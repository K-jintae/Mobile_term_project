package com.example.term_project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class RightFragment extends Fragment {

    public RightFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // ⭐ view 먼저 받아야 함
        View view = inflater.inflate(R.layout.fragment_right, container, false);

        Button logoutBtn = view.findViewById(R.id.logoutBtn);

        logoutBtn.setOnClickListener(v -> {

            SharedPreferences pref = requireActivity().getSharedPreferences("user", getContext().MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();

            editor.putBoolean("isLogin", false);
            editor.apply();

            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);

            requireActivity().finish();
        });

        return view;
    }
}