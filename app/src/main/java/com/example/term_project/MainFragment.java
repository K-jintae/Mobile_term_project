package com.example.term_project;

import android.content.Context;
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
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.Random;
import android.widget.FrameLayout;
import android.app.Dialog;
import android.widget.Button;
import android.view.ViewGroup;
import android.view.Window;

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
    // 소리 설정 팝업 관련 변수
    private LinearLayout soundSettingsPopup;
    private FrameLayout btnSoundOn;
    private FrameLayout btnSoundMute;
    private boolean isSoundOn = true; // 소리 설정 상태
    // 닉네임 수정 팝업 관련 변수
    private LinearLayout nicknamePopup;
    private EditText etNickname;

    // 도움말 팝업 관련 변수
    private LinearLayout helpPopup;
    private ImageButton btnOpenFriend;
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
        View btnContinueQuiz = view.findViewById(R.id.quiz_container);

        if (btnContinueQuiz != null) {
            applyPressAnimation(btnContinueQuiz);
            btnContinueQuiz.setOnClickListener(v -> showContinueQuizDialog());
        }

        ImageButton btnSettings = view.findViewById(R.id.btnSettings);
        dimView = view.findViewById(R.id.dimView);
        settingsPopup = view.findViewById(R.id.settingsPopup);
        Button btnEditNickname = view.findViewById(R.id.btnEditNickname);

        Button btnSound = view.findViewById(R.id.btnSound);
        Button btnHelp = view.findViewById(R.id.btnHelp);
        Button btnClosePopup = view.findViewById(R.id.btnClosePopup);
        Button logoutBtn = view.findViewById(R.id.logoutBtn);

        // 소리 설정 팝업 관련 findViewById
        soundSettingsPopup = view.findViewById(R.id.soundSettingsPopup);
        btnSoundOn = view.findViewById(R.id.btnSoundOn);
        btnSoundMute = view.findViewById(R.id.btnSoundMute);
        Button btnCloseSoundPopup = view.findViewById(R.id.btnCloseSoundPopup);

        // 도움말 팝업 관련 findViewById
        helpPopup = view.findViewById(R.id.helpPopup);
        Button btnCloseHelpPopup = view.findViewById(R.id.btnCloseHelpPopup);
        nicknamePopup = view.findViewById(R.id.nicknamePopup);
        etNickname = view.findViewById(R.id.etNickname);
        ImageButton btnClearNickname = view.findViewById(R.id.btnClearNickname);
        btnClearNickname.setOnClickListener(v -> etNickname.setText(""));

        Button btnSaveNickname = view.findViewById(R.id.btnSaveNickname);
        Button btnCloseNicknamePopup = view.findViewById(R.id.btnCloseNicknamePopup);

        btnOpenFriend = view.findViewById(R.id.btnOpenFriend);
        if(btnOpenFriend != null){
            applyPressAnimation(btnOpenFriend);

            btnOpenFriend.setOnClickListener(v ->{
                Intent intent = new Intent(getActivity(), FriendActivity.class);
                startActivity(intent);
            });
        }

        isSoundOn = ((MainActivity) getActivity()).isSoundOn();
        updateSoundButtonUI();

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



        btnSettings.setOnClickListener(v -> showSettingsPopup());

        logoutBtn.setOnClickListener(v -> {
            Dialog logoutDialog = new Dialog(requireContext());
            logoutDialog.setContentView(R.layout.dialog_logout);
            logoutDialog.setCancelable(true);

            Button btnCancel = logoutDialog.findViewById(R.id.btnCancel);
            Button btnConfirm = logoutDialog.findViewById(R.id.btnConfirm);

            btnCancel.setOnClickListener(v1 -> logoutDialog.dismiss());

            btnConfirm.setOnClickListener(v1 -> {
                hideSettingsPopup(() -> {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut();

                    SharedPreferences pref = requireActivity()
                            .getSharedPreferences("user", requireContext().MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putBoolean("isLogin", false);
                    editor.apply();

                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                });

                logoutDialog.dismiss();
            });

            logoutDialog.show();

            if (logoutDialog.getWindow() != null) {
                logoutDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                logoutDialog.getWindow().setGravity(android.view.Gravity.CENTER);
                logoutDialog.getWindow().setLayout(
                        (int) (getResources().getDisplayMetrics().widthPixels * 0.78),
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
            }
        });


        btnClosePopup.setOnClickListener(v -> hideSettingsPopup(null));

        dimView.setOnClickListener(v -> hideSettingsPopup(null));
        // 팝업 바깥 부분을 클릭하면 열려있는 모든 팝업 닫기
        dimView.setOnClickListener(v -> {
            // 소리 설정 팝업이 열려있으면 닫기
            if (soundSettingsPopup.getVisibility() == View.VISIBLE) {
                hideSoundSettingsPopup(null);
            }
            // 도움말 팝업이 열려있으면 닫기
            else if (helpPopup.getVisibility() == View.VISIBLE) {
                hideHelpPopup(null);
            }
            else if (nicknamePopup.getVisibility() == View.VISIBLE) {
                hideNicknamePopup(null);
            }
            // 설정 팝업이 열려있으면 닫기
            else if (settingsPopup.getVisibility() == View.VISIBLE) {
                hideSettingsPopup(null);
            }
        });

        // 닉네임 수정 버튼 클릭 (설정창 닫고 닉네임 창 열기)
        btnEditNickname.setOnClickListener(v -> {
            hideSettingsPopup(() -> {
                // 기존 저장된 닉네임이 있다면 불러와서 EditText에 표시
                SharedPreferences pref = requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE);
                String currentNickname = pref.getString("nickname", "");
                etNickname.setText(currentNickname);

                showNicknamePopup();
            });
        });

        // 닉네임 수정 버튼 클릭 (설정창 닫고 닉네임 창 열기)
        btnEditNickname.setOnClickListener(v -> {
            hideSettingsPopup(() -> {
                // MainActivity와 동일하게 "name" 키를 사용하여 불러옵니다.
                SharedPreferences pref = requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE);
                String currentNickname = pref.getString("name", "");
                etNickname.setText(currentNickname);

                showNicknamePopup();
            });
        });

        // 닉네임 저장 버튼 클릭 (SharedPreferences에 저장)
        btnSaveNickname.setOnClickListener(v -> {
            String newNickname = etNickname.getText().toString().trim();
            if (!newNickname.isEmpty()) {
                // MainActivity와 동일하게 "name" 키를 사용하여 저장합니다.
                SharedPreferences pref = requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("name", newNickname);
                editor.apply();

                // MainActivity의 updatePlayerName 메서드를 호출하여 상단 UI 즉시 변경
                if(getActivity() instanceof MainActivity){
                    MainActivity mainActivity = (MainActivity) getActivity();
                    TextView tvMainPlayerName = mainActivity.findViewById(R.id.tvPlayerName);
                    if(tvMainPlayerName != null){
                        tvMainPlayerName.setText(newNickname);
                    }
                }

                // 파이어베이스 FireStore 서버에 실시간 원격 저장
                com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
                if (auth.getCurrentUser() != null) {
                    String uid = auth.getCurrentUser().getUid();
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users").document(uid)
                            .update("name", newNickname) // users -> UID 문서의 name 필드를 새 닉네임으로 변경
                            .addOnSuccessListener(aVoid -> {
                                // 서버 저장 성공 시 디버그 로그 기록
                                android.util.Log.d("Firebase", "닉네임 서버 저장 완료: " + newNickname);
                            })
                            .addOnFailureListener(e -> {
                                // 실패 시 기록
                                android.util.Log.e("Firebase", "닉네임 서버 저장 실패", e);
                            });
                }

                Toast.makeText(getActivity(), "닉네임이 '" + newNickname + "'(으)로 변경되었습니다.", Toast.LENGTH_SHORT).show();
                hideNicknamePopup(null);
            } else {
                Toast.makeText(getActivity(), "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        // 닉네임 팝업 닫기 버튼
        btnCloseNicknamePopup.setOnClickListener(v -> hideNicknamePopup(null));


        // 소리 설정 버튼 클릭
        btnSound.setOnClickListener(v -> {
            hideSettingsPopup(() -> showSoundSettingsPopup());
        });

        // 도움말 버튼 클릭
        btnHelp.setOnClickListener(v -> {
            hideSettingsPopup(() -> showHelpPopup());
        });

        // 소리 ON 버튼 클릭
        btnSoundOn.setOnClickListener(v -> {
            isSoundOn = true;              //상태 변경
            updateSoundButtonUI();         //UI 반영

            ((MainActivity) getActivity()).setSound(true);
            Toast.makeText(getActivity(), "음성이 켜졌습니다", Toast.LENGTH_SHORT).show();
        });

        //소리 mute
        btnSoundMute.setOnClickListener(v -> {
            isSoundOn = false;
            updateSoundButtonUI();

            ((MainActivity) getActivity()).setSound(false);
            Toast.makeText(getActivity(), "음성이 꺼졌습니다", Toast.LENGTH_SHORT).show();
        });

        // 소리 설정 팝업 닫기 버튼
        btnCloseSoundPopup.setOnClickListener(v -> hideSoundSettingsPopup(null));

        // 도움말 팝업 닫기 버튼
        btnCloseHelpPopup.setOnClickListener(v -> hideHelpPopup(null));


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

    // ===== 소리 설정 팝업 메서드 =====
    private void showSoundSettingsPopup() {
        dimView.setAlpha(0f);
        dimView.setVisibility(View.VISIBLE);
        dimView.animate()
                .alpha(1f)
                .setDuration(180)
                .start();

        soundSettingsPopup.setVisibility(View.VISIBLE);
        soundSettingsPopup.setAlpha(0f);
        soundSettingsPopup.setScaleX(0.88f);
        soundSettingsPopup.setScaleY(0.88f);
        soundSettingsPopup.setTranslationY(20f);

        soundSettingsPopup.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(220)
                .start();
    }

    private void hideSoundSettingsPopup(Runnable endAction) {
        dimView.animate()
                .alpha(0f)
                .setDuration(160)
                .withEndAction(() -> {
                    dimView.setVisibility(View.GONE);
                    dimView.setAlpha(1f);
                })
                .start();

        soundSettingsPopup.animate()
                .alpha(0f)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .translationY(16f)
                .setDuration(180)
                .withEndAction(() -> {
                    soundSettingsPopup.setVisibility(View.GONE);
                    soundSettingsPopup.setAlpha(1f);
                    soundSettingsPopup.setScaleX(1f);
                    soundSettingsPopup.setScaleY(1f);
                    soundSettingsPopup.setTranslationY(0f);

                    if (endAction != null) {
                        endAction.run();
                    }
                })
                .start();
    }

    private void updateSoundButtonUI() {
        if (isSoundOn) {
            btnSoundOn.setAlpha(1.0f);
            btnSoundMute.setAlpha(0.6f);
        } else {
            btnSoundOn.setAlpha(0.6f);
            btnSoundMute.setAlpha(1.0f);
        }
    }

    // ===== 도움말 팝업 메서드 =====
    private void showHelpPopup() {
        dimView.setAlpha(0f);
        dimView.setVisibility(View.VISIBLE);
        dimView.animate()
                .alpha(1f)
                .setDuration(180)
                .start();

        helpPopup.setVisibility(View.VISIBLE);
        helpPopup.setAlpha(0f);
        helpPopup.setScaleX(0.88f);
        helpPopup.setScaleY(0.88f);
        helpPopup.setTranslationY(20f);

        helpPopup.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(220)
                .start();
    }

    private void hideHelpPopup(Runnable endAction) {
        dimView.animate()
                .alpha(0f)
                .setDuration(160)
                .withEndAction(() -> {
                    dimView.setVisibility(View.GONE);
                    dimView.setAlpha(1f);
                })
                .start();

        helpPopup.animate()
                .alpha(0f)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .translationY(16f)
                .setDuration(180)
                .withEndAction(() -> {
                    helpPopup.setVisibility(View.GONE);
                    helpPopup.setAlpha(1f);
                    helpPopup.setScaleX(1f);
                    helpPopup.setScaleY(1f);
                    helpPopup.setTranslationY(0f);

                    if (endAction != null) {
                        endAction.run();
                    }
                })
                .start();
    }

    // ===== 닉네임 팝업 메서드 =====
    private void showNicknamePopup() {
        dimView.setAlpha(0f);
        dimView.setVisibility(View.VISIBLE);
        dimView.animate()
                .alpha(1f)
                .setDuration(180)
                .start();

        nicknamePopup.setVisibility(View.VISIBLE);
        nicknamePopup.setAlpha(0f);
        nicknamePopup.setScaleX(0.88f);
        nicknamePopup.setScaleY(0.88f);
        nicknamePopup.setTranslationY(20f);

        nicknamePopup.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(220)
                .start();
    }

    private void hideNicknamePopup(Runnable endAction) {
        dimView.animate()
                .alpha(0f)
                .setDuration(160)
                .withEndAction(() -> {
                    dimView.setVisibility(View.GONE);
                    dimView.setAlpha(1f);
                })
                .start();

        nicknamePopup.animate()
                .alpha(0f)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .translationY(16f)
                .setDuration(180)
                .withEndAction(() -> {
                    nicknamePopup.setVisibility(View.GONE);
                    nicknamePopup.setAlpha(1f);
                    nicknamePopup.setScaleX(1f);
                    nicknamePopup.setScaleY(1f);
                    nicknamePopup.setTranslationY(0f);

                    if (endAction != null) {
                        endAction.run();
                    }
                })
                .start();
    }

    private void updateMessage(CharacterState state) {

        // 48시간 미접속 상태면 무조건 이 메시지 출력
        if (getActivity() instanceof MainActivity
                && ((MainActivity) getActivity()).isNeedQuizRecovery()) {

            if (tvMessage != null) {
                tvMessage.setText("오랜만이야... 퀴즈 하나 풀면\n기분이 나아질 것 같아.");
            }
            return;
        }

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
    private void continueQuiz() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("continue_quiz", Context.MODE_PRIVATE);

        boolean hasContinue = prefs.getBoolean("has_continue", false);

        if (!hasContinue) {
            Toast.makeText(getActivity(), "이어풀 문제가 없어요.", Toast.LENGTH_SHORT).show();
            return;
        }

        int subjectId = prefs.getInt("subject_id", 1);
        String difficultyLevel = prefs.getString("difficulty_level", "easy");

        Bundle bundle = new Bundle();
        bundle.putInt("subject_id", subjectId);
        bundle.putString("difficulty_level", difficultyLevel);
        bundle.putBoolean("continue_mode", true);

        QuizPlayFragment fragment = new QuizPlayFragment();
        fragment.setArguments(bundle);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openFragment(fragment);
        }
    }
    private void showContinueQuizDialog() {
        Dialog dialog = new Dialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_continue_quiz, null);

        dialog.setContentView(dialogView);
        dialog.setCancelable(true);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            continueQuiz();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.82),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
}
