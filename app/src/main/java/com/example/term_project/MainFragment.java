package com.example.term_project;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.Toast;

public class MainFragment extends Fragment {

    // 기본 캐릭터
    private ImageView characterImage;

    // 꾸미기 레이어
    private ImageView clothesImage;
    private ImageView faceImage;
    private ImageView hatImage;

    // ViewModel
    private CharacterViewModel viewModel;

    public MainFragment() {
        // 필수 생성자
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, container, false);

        // 이미지 연결
        characterImage = view.findViewById(R.id.characterImage);
        clothesImage = view.findViewById(R.id.clothes_image);
        faceImage = view.findViewById(R.id.face_image);
        hatImage = view.findViewById(R.id.hat_image);

        // ViewModel 연결 (Activity 공유)
        viewModel = new ViewModelProvider(requireActivity()).get(CharacterViewModel.class);

        // 캐릭터 몸체
        viewModel.getCharacter().observe(getViewLifecycleOwner(), resId -> {
            characterImage.setImageResource(resId);
        });

        // 얼굴
        viewModel.getFace().observe(getViewLifecycleOwner(), resId -> {
            if (resId != 0) {
                faceImage.setImageResource(resId);
            } else {
                faceImage.setImageDrawable(null);
            }
        });

        // 모자
        viewModel.getHat().observe(getViewLifecycleOwner(), resId -> {
            if (resId != 0) {
                hatImage.setImageResource(resId);
            } else {
                hatImage.setImageDrawable(null);
            }
        });

        // 옷
        viewModel.getClothes().observe(getViewLifecycleOwner(), resId -> {
            if (resId != 0) {
                clothesImage.setImageResource(resId);
            } else {
                clothesImage.setImageDrawable(null);
            }
        });

        // 설정 UI
        ImageButton btnSettings = view.findViewById(R.id.btnSettings);
        View dimView = view.findViewById(R.id.dimView);
        LinearLayout settingsPopup = view.findViewById(R.id.settingsPopup);

        Button btnSound = view.findViewById(R.id.btnSound);
        Button btnHelp = view.findViewById(R.id.btnHelp);
        Button btnClosePopup = view.findViewById(R.id.btnClosePopup);

        // 설정 열기
        btnSettings.setOnClickListener(v -> {
            dimView.setVisibility(View.VISIBLE);
            settingsPopup.setVisibility(View.VISIBLE);
        });

        // 닫기 버튼
        btnClosePopup.setOnClickListener(v -> {
            dimView.setVisibility(View.GONE);
            settingsPopup.setVisibility(View.GONE);
        });

        // 바깥 클릭 닫기
        dimView.setOnClickListener(v -> {
            dimView.setVisibility(View.GONE);
            settingsPopup.setVisibility(View.GONE);
        });

        // 테스트 기능
        btnSound.setOnClickListener(v ->
                Toast.makeText(getActivity(), "소리 설정", Toast.LENGTH_SHORT).show());

        btnHelp.setOnClickListener(v ->
                Toast.makeText(getActivity(), "도움말", Toast.LENGTH_SHORT).show());

        return view;
    }
}