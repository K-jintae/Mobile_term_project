package com.example.term_project;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;

public class LeftFragment extends Fragment {

    private SharedPreferences prefs;

    private LinearLayout cardQuiz1;
    private LinearLayout cardQuiz2;
    private LinearLayout cardQuiz3;

    // --- 추가/수정된 부분 시작: 새 과목 변수 선언 ---
    private LinearLayout cardQuiz4;
    private LinearLayout cardQuiz5;
    private LinearLayout cardQuiz6;
    private LinearLayout cardQuiz7;
    private LinearLayout cardQuiz8;
    private LinearLayout cardQuiz9;
    //-------------------------------

    public LeftFragment() {
        // 기본 생성자
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_left, container, false);

        prefs = requireContext().getSharedPreferences("quiz_progress", Context.MODE_PRIVATE);

        cardQuiz1 = view.findViewById(R.id.cardQuiz1);
        cardQuiz2 = view.findViewById(R.id.cardQuiz2);
        cardQuiz3 = view.findViewById(R.id.cardQuiz3);
        // --- 추가/수정된 부분 시작: XML ID 연결 ---
        cardQuiz4 = view.findViewById(R.id.cardQuiz4);
        cardQuiz5 = view.findViewById(R.id.cardQuiz5);
        cardQuiz6 = view.findViewById(R.id.cardQuiz6);
        cardQuiz7 = view.findViewById(R.id.cardQuiz7);
        cardQuiz8 = view.findViewById(R.id.cardQuiz8);
        cardQuiz9 = view.findViewById(R.id.cardQuiz9);
        // ------------

        loadProgressFromFirebase();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCards();
    }

    private void refreshCards() {
        setupCard(cardQuiz1, 1);
        setupCard(cardQuiz2, 2);
        setupCard(cardQuiz3, 3);
        setupCard(cardQuiz4, 4);
        setupCard(cardQuiz5, 5);
        setupCard(cardQuiz6, 6);
        setupCard(cardQuiz7, 7);
        setupCard(cardQuiz8, 8);
        setupCard(cardQuiz9, 9);
        //--------------------------------
    }

    private void setupCard(LinearLayout card, int subjectId) {
        if (card == null) {
            return;
        }

        boolean canPlay = canPlayStage(subjectId);

        // 추가 된 부분
        View iconView = card.getChildAt(0);

        if (canPlay) {
            card.setAlpha(1.0f);

            // 핵심 변경:
            // 과목 카드를 누르면 바로 문제로 가지 않고 난이도 선택창을 띄운다.
            card.setOnClickListener(v -> showDifficultyDialog(subjectId));



        } else {
            card.setAlpha(0.5f);
            card.setOnClickListener(v ->
                    Toast.makeText(
                            getContext(),
                            getLockedMessage(subjectId),
                            Toast.LENGTH_SHORT
                    ).show()
            );
        }
    }

    private void showDifficultyDialog(int subjectId) {
        final android.app.Dialog dialog = new android.app.Dialog(requireContext());

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_difficulty_select, null);

        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            );
        }

        Button btnEasy = dialogView.findViewById(R.id.btnEasy);
        Button btnNormal = dialogView.findViewById(R.id.btnNormal);
        Button btnHard = dialogView.findViewById(R.id.btnHard);
        TextView btnCancel = dialogView.findViewById(R.id.btnCancelDifficulty);

        btnEasy.setOnClickListener(v -> {
            dialog.dismiss();
            moveToQuizPlay(subjectId, "easy");
        });

        btnNormal.setOnClickListener(v -> {
            dialog.dismiss();
            moveToQuizPlay(subjectId, "normal");
        });

        btnHard.setOnClickListener(v -> {
            dialog.dismiss();
            moveToQuizPlay(subjectId, "hard");
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            dialog.getWindow().setDimAmount(0.55f);
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
    }

    private void loadProgressFromFirebase() {
        com.google.firebase.auth.FirebaseAuth mAuth =
                com.google.firebase.auth.FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && isAdded()) {
                            SharedPreferences.Editor editor = prefs.edit();

                            //변경: 11단계까지 확인하도록 범위를 늘림
                            for (int i = 2; i <= 11; i++) {
                                Boolean isUnlocked = doc.getBoolean("unlocked_stage_" + i);

                                if (isUnlocked != null && isUnlocked) {
                                    editor.putInt("stage_" + i + "_before_clear", 1);
                                }
                            }

                            editor.apply();
                            refreshCards();
                        }
                    });
        } else {
            refreshCards();
        }
    }

    private boolean canPlayStage(int subjectId) {
        if (subjectId == 1) {
            return true;
        }

        return prefs.getInt("stage_" + subjectId + "_before_clear", 0) == 1;
    }

    private String getLockedMessage(int subjectId) {
        if (subjectId == 1) {
            return "플레이 가능합니다.";
        }

        return (subjectId - 1) + "단계를 먼저 클리어해야 합니다.";
    }

    private void moveToQuizPlay(int subjectId, String difficultyLevel) {
        Bundle bundle = new Bundle();

        // 과목 번호
        bundle.putInt("subject_id", subjectId);

        // 과목 안에서 선택한 난이도
        bundle.putString("difficulty_level", difficultyLevel);

        QuizPlayFragment fragment = new QuizPlayFragment();
        fragment.setArguments(bundle);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openFragment(fragment);
        }
    }
}
