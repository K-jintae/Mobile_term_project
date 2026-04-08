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
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Random;

public class MainFragment extends Fragment {

    // 기본 캐릭터
    private ImageView characterImage;

    // 꾸미기 레이어
    private ImageView clothesImage;
    private ImageView faceImage;
    private ImageView hatImage;

    // 말풍선 텍스트
    private TextView tvMessage;

    // ViewModel
    private CharacterViewModel viewModel;

    // 캐릭터 상태
    enum CharacterState {
        NORMAL,
        HUNGRY,
        NEW_CLOTHES
    }

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

        // 말풍선 연결
        tvMessage = view.findViewById(R.id.tv_message);

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

                // 옷이 바뀌었을 때 메세지 출력
                updateMessage(CharacterState.NEW_CLOTHES);
            } else {
                clothesImage.setImageDrawable(null);
            }
        });

        // 처음 화면 들어왔을 때 기본 메세지 출력
        updateMessage(CharacterState.NORMAL);

        // 설정 UI
        ImageButton btnSettings = view.findViewById(R.id.btnSettings);
        View dimView = view.findViewById(R.id.dimView);
        LinearLayout settingsPopup = view.findViewById(R.id.settingsPopup);

        Button btnSound = view.findViewById(R.id.btnSound);
        Button btnHelp = view.findViewById(R.id.btnHelp);
        Button btnClosePopup = view.findViewById(R.id.btnClosePopup);
        Button logoutBtn = view.findViewById(R.id.logoutBtn);

        // 설정 열기
        btnSettings.setOnClickListener(v -> {
            dimView.setVisibility(View.VISIBLE);
            settingsPopup.setVisibility(View.VISIBLE);
        });

        // 로그아웃 버튼(팝업으로 한 번 더 확인 후 로그아웃)
        logoutBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("로그아웃")
                    .setMessage("정말 로그아웃 하시겠습니까?")
                    .setPositiveButton("예", (dialog, which) -> {

                        // 자동으로 팝업 닫고 로그아웃
                        dimView.setVisibility(View.GONE);
                        settingsPopup.setVisibility(View.GONE);

                        SharedPreferences pref = requireActivity()
                                .getSharedPreferences("user", getContext().MODE_PRIVATE);
                        SharedPreferences.Editor editor = pref.edit();

                        editor.putBoolean("isLogin", false);
                        editor.apply();

                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        startActivity(intent);
                        requireActivity().finish();
                    })
                    .setNegativeButton("아니오", null)
                    .show();
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

    // 캐릭터 상태에 따라 말풍선 메세지 변경
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