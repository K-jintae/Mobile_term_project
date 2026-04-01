package com.example.term_project;

import android.os.Bundle;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class MainFragment extends Fragment {

    private ImageView characterImage;

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, container, false);

        characterImage = view.findViewById(R.id.characterImage);

        int[] characters = {
                R.drawable.sample1,
                R.drawable.sample2,
                R.drawable.sample3
        };

        Random random = new Random();
        int randomIndex = random.nextInt(characters.length);
        characterImage.setImageResource(characters[randomIndex]);

        return view;
    }
}