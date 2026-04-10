package com.example.term_project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.Random;

public class MainFragment extends Fragment {

    private ImageView characterImage;
    private ImageView clothesImage;
    private ImageView faceImage;
    private ImageView hatImage;
    private ImageView bgInterior;

    private TextView tvMessage;

    private CharacterViewModel viewModel;

    private View dimView;
    private LinearLayout settingsPopup;

    enum CharacterState {
        NORMAL,
        HUNGRY,
        NEW_CLOTHES
    }

    public MainFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, container, false);

        bgInterior = view.findViewById(R.id.bgInterior);
        characterImage = view.findViewById(R.id.characterImage);
        clothesImage = view.findViewById(R.id.clothes_image);
        faceImage = view.findViewById(R.id.face_image);
        hatImage = view.findViewById(R.id.hat_image);
        tvMessage = view.findViewById(R.id.tv_message);

        ImageButton btnSettings = view.findViewById(R.id.btnSettings);
        dimView = view.findViewById(R.id.dimView);
        settingsPopup = view.findViewById(R.id.settingsPopup);

        Button btnSound = view.findViewById(R.id.btnSound);
        Button btnHelp = view.findViewById(R.id.btnHelp);
        Button btnClosePopup = view.findViewById(R.id.btnClosePopup);
        Button logoutBtn = view.findViewById(R.id.logoutBtn);

        applyPressAnimation(btnSettings);

        viewModel = new ViewModelProvider(requireActivity()).get(CharacterViewModel.class);

        viewModel.getInterior().observe(getViewLifecycleOwner(), resId -> {
            bgInterior.setImageResource(resId);
        });

        viewModel.getCharacter().observe(getViewLifecycleOwner(), resId -> {
            characterImage.setImageResource(resId);
        });

        viewModel.getFace().observe(getViewLifecycleOwner(), resId -> {
            if (resId != 0) {
                faceImage.setImageResource(resId);
            } else {
                faceImage.setImageDrawable(null);
            }
        });

        viewModel.getHat().observe(getViewLifecycleOwner(), resId -> {
            if (resId != 0) {
                hatImage.setImageResource(resId);
            } else {
                hatImage.setImageDrawable(null);
            }
        });

        viewModel.getClothes().observe(getViewLifecycleOwner(), resId -> {
            if (resId != 0) {
                clothesImage.setImageResource(resId);
                updateMessage(CharacterState.NEW_CLOTHES);
            } else {
                clothesImage.setImageDrawable(null);
            }
        });

        updateMessage(CharacterState.NORMAL);

        btnSettings.setOnClickListener(v -> showSettingsPopup());

        logoutBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("로그아웃")
                    .setMessage("정말 로그아웃 하시겠습니까?")
                    .setPositiveButton("예", (dialog, which) -> {
                        hideSettingsPopup(() -> {
                            SharedPreferences pref = requireActivity()
                                    .getSharedPreferences("user", requireContext().MODE_PRIVATE);
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putBoolean("isLogin", false);
                            editor.apply();

                            Intent intent = new Intent(getActivity(), LoginActivity.class);
                            startActivity(intent);
                            requireActivity().finish();
                        });
                    })
                    .setNegativeButton("아니오", null)
                    .show();
        });

        btnClosePopup.setOnClickListener(v -> hideSettingsPopup(null));

        dimView.setOnClickListener(v -> hideSettingsPopup(null));

        btnSound.setOnClickListener(v ->
                Toast.makeText(getActivity(), "소리 설정", Toast.LENGTH_SHORT).show());

        btnHelp.setOnClickListener(v ->
                Toast.makeText(getActivity(), "도움말", Toast.LENGTH_SHORT).show());

        return view;
    }

    private void applyPressAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate()
                            .scaleX(0.92f)
                            .scaleY(0.92f)
                            .setDuration(80)
                            .start();
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(80)
                            .start();
                    break;
            }
            return false;
        });
    }

    private void showSettingsPopup() {
        dimView.setAlpha(0f);
        dimView.setVisibility(View.VISIBLE);
        dimView.animate()
                .alpha(1f)
                .setDuration(180)
                .start();

        settingsPopup.setVisibility(View.VISIBLE);
        settingsPopup.setAlpha(0f);
        settingsPopup.setScaleX(0.88f);
        settingsPopup.setScaleY(0.88f);
        settingsPopup.setTranslationY(20f);

        settingsPopup.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(220)
                .start();
    }

    private void hideSettingsPopup(Runnable endAction) {
        dimView.animate()
                .alpha(0f)
                .setDuration(160)
                .withEndAction(() -> {
                    dimView.setVisibility(View.GONE);
                    dimView.setAlpha(1f);
                })
                .start();

        settingsPopup.animate()
                .alpha(0f)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .translationY(16f)
                .setDuration(180)
                .withEndAction(() -> {
                    settingsPopup.setVisibility(View.GONE);
                    settingsPopup.setAlpha(1f);
                    settingsPopup.setScaleX(1f);
                    settingsPopup.setScaleY(1f);
                    settingsPopup.setTranslationY(0f);

                    if (endAction != null) {
                        endAction.run();
                    }
                })
                .start();
    }

    private void updateMessage(CharacterState state) {
        String message = "";
        Random random = new Random();

        switch (state) {
            case NORMAL:
                String[] normalMessage = {
                        "안녕! 반가워!",
                        "안녕~! 오늘 하루 잘 보냈어?",
                        "안녕!! 보고싶었어!",
                        "오늘도 좋은 하루!"
                };
                message = normalMessage[random.nextInt(normalMessage.length)];
                break;

            case HUNGRY:
                message = "나 배고파...";
                break;

            case NEW_CLOTHES:
                String[] clothMessage = {
                        "우와! 이거 멋있다!!",
                        "나 이거 맘에 들어!"
                };
                message = clothMessage[random.nextInt(clothMessage.length)];
                break;
        }

        if (tvMessage != null) {
            tvMessage.setText(message);
        }
    }
}