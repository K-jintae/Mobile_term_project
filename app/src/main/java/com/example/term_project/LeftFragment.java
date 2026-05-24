package com.example.term_project;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class LeftFragment extends Fragment {


    private LinearLayout cardQuiz1;
    private LinearLayout cardQuiz2;
    private LinearLayout cardQuiz3;
    private LinearLayout cardQuiz4;
    private LinearLayout cardQuiz5;
    private LinearLayout cardQuiz6;
    private LinearLayout cardQuiz7;
    private LinearLayout cardQuiz8;

    public LeftFragment() {
        // 기본 생성자
    }

    private SharedPreferences getQuizPrefs() {
        String uid = "guest";
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            uid = auth.getCurrentUser().getUid();
        }
        return requireContext().getSharedPreferences("quiz_progress_" + uid, Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_left, container, false);

        //현재 로그인한 유저의 uid를 획득하여 전용 진척도 파일 연결
        String uid = "guest";
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            uid = auth.getCurrentUser().getUid();
        }


        // fragment_left.xml의 1~9번 과목 카드 연결
        cardQuiz1 = view.findViewById(R.id.cardQuiz1);
        cardQuiz2 = view.findViewById(R.id.cardQuiz2);
        cardQuiz3 = view.findViewById(R.id.cardQuiz3);
        cardQuiz4 = view.findViewById(R.id.cardQuiz4);
        cardQuiz5 = view.findViewById(R.id.cardQuiz5);
        cardQuiz6 = view.findViewById(R.id.cardQuiz6);
        cardQuiz7 = view.findViewById(R.id.cardQuiz7);
        cardQuiz8 = view.findViewById(R.id.cardQuiz8);

        loadProgressFromFirebase();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCards();
    }

    /**
     * 전체 카드 상태를 갱신한다.
     * XML에 cardQuiz1~cardQuiz9가 있으므로 9개 모두 관리한다.
     */
    private void refreshCards() {
        setupCard(cardQuiz1, 1);
        setupCard(cardQuiz2, 2);
        setupCard(cardQuiz3, 3);
        setupCard(cardQuiz4, 4);
        setupCard(cardQuiz5, 5);
        setupCard(cardQuiz6, 6);
        setupCard(cardQuiz7, 7);
        setupCard(cardQuiz8, 8);
    }

    /**
     * 각 카드의 입장 가능 여부를 설정한다.
     * 해금된 카드는 난이도 선택창을 띄우고,
     * 잠긴 카드는 안내 메시지만 띄운다.
     */
    private void setupCard(LinearLayout card, int stageId) {
        if (card == null) {
            return;
        }

        boolean canPlay = canPlayStage(stageId);

        if (canPlay) {
            card.setAlpha(1.0f);
            card.setOnClickListener(v -> showDifficultyDialog(stageId));
        } else {
            card.setAlpha(0.5f);
            card.setOnClickListener(v ->
                    Toast.makeText(
                            getContext(),
                            getLockedMessage(stageId),
                            Toast.LENGTH_SHORT
                    ).show()
            );
        }
    }

    /**
     * 난이도 선택 다이얼로그를 띄운다.
     */
    private void showDifficultyDialog(int stageId) {
        final Dialog dialog = new Dialog(requireContext());

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_difficulty_select, null);

        dialog.setContentView(dialogView);
        dialog.setCancelable(true);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button btnEasy = dialogView.findViewById(R.id.btnEasy);
        Button btnNormal = dialogView.findViewById(R.id.btnNormal);
        Button btnHard = dialogView.findViewById(R.id.btnHard);
        TextView btnCancel = dialogView.findViewById(R.id.btnCancelDifficulty);

        // 안전하게 현재 로그인한 유저의 UID 가져오기
        String uid = "guest";
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            uid = auth.getCurrentUser().getUid();
        }

        // 각 난이도별 클리어 기록 불러오기 (순차 해금용)
        SharedPreferences currentPrefs = getQuizPrefs();
        boolean isEasyCleared = currentPrefs.getInt("subject_" + stageId + "_easy_clear", 0) == 1;
        boolean isNormalCleared = currentPrefs.getInt("subject_" + stageId + "_normal_clear", 0) == 1;

        //  유저의 등급 불러오기 (기본 해금용)
        SharedPreferences userPrefs = requireContext().getSharedPreferences("user_" + uid, Context.MODE_PRIVATE);
        String userLevel = userPrefs.getString("level", "하수");


        // 1. '하(Easy)' 난이도는 누구나 입장 가능
        btnEasy.setOnClickListener(v -> {
            dialog.dismiss();
            moveToQuizPlay(stageId, "easy");
        });

        // 2. '중(Normal)' 난이도 제어:
        // 해당 과목의 '하' 난이도를 깼거나 OR 유저 레벨이 '중수', '고수'면 오픈
        if (isEasyCleared || "중수".equals(userLevel) || "고수".equals(userLevel)) {
            btnNormal.setAlpha(1.0f);
            btnNormal.setOnClickListener(v -> {
                dialog.dismiss();
                moveToQuizPlay(stageId, "normal");
            });
        } else {
            btnNormal.setAlpha(0.4f);
            btnNormal.setOnClickListener(v ->
                    Toast.makeText(getContext(), "먼저 '하' 난이도를 클리어하거나 중수 레벨이 되어야 합니다.", Toast.LENGTH_SHORT).show()
            );
        }

        // 3. '상(Hard)' 난이도 제어:
        // 해당 과목의 '중' 난이도를 깼거나 OR 유저 레벨이 '고수'면 오픈
        if (isNormalCleared || "고수".equals(userLevel)) {
            btnHard.setAlpha(1.0f);
            btnHard.setOnClickListener(v -> {
                dialog.dismiss();
                moveToQuizPlay(stageId, "hard");
            });
        } else {
            btnHard.setAlpha(0.4f);
            btnHard.setOnClickListener(v ->
                    Toast.makeText(getContext(), "먼저 '중' 난이도를 클리어하거나 고수 레벨이 되어야 합니다.", Toast.LENGTH_SHORT).show()
            );
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        Window shownWindow = dialog.getWindow();
        if (shownWindow != null) {
            shownWindow.setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            shownWindow.setDimAmount(0.55f);
            shownWindow.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            shownWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    /**
     * Firebase에서 해금 정보 가져오기.
     * 현재 XML은 9단계까지 있으므로 2~9단계까지 확인한다.
     */
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
                            SharedPreferences.Editor editor = getQuizPrefs().edit();

                            for (int i = 2; i <= 9; i++) {
                                Boolean isUnlocked = doc.getBoolean("unlocked_stage_" + i);

                                if (isUnlocked != null && isUnlocked) {
                                    editor.putInt("stage_" + i + "_before_clear", 1);
                                }
                            }

                            editor.apply();
                            refreshCards();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            refreshCards();
                        }
                    });
        } else {
            refreshCards();
        }
    }

    /**
     * 해당 스테이지를 플레이할 수 있는지 확인한다.
     * 1단계는 항상 플레이 가능.
     * 2단계 이상은 stage_N_before_clear 값이 1이어야 가능.
     */
    private boolean canPlayStage(int stageId) {
        if (stageId <= 4) {
            return true;
        }

        // 동적 헬퍼 메서드로 교체
        return getQuizPrefs().getInt("stage_" + stageId + "_before_clear", 0) == 1;
    }

    /**
     * 잠긴 카드 클릭 시 안내 메시지.
     */
    private String getLockedMessage(int stageId) {
        if (stageId <= 4) {
            return "플레이 가능합니다.";
        }

        return (stageId - 1) + "단계를 먼저 클리어해야 합니다.";
    }

    /**
     * 퀴즈 화면으로 이동한다.
     * subject_id와 difficulty_level을 같이 넘긴다.
     */
    private void moveToQuizPlay(int stageId, String difficultyLevel) {
        Bundle bundle = new Bundle();

        bundle.putInt("subject_id", stageId);
        bundle.putString("difficulty_level", difficultyLevel);

        QuizPlayFragment fragment = new QuizPlayFragment();
        fragment.setArguments(bundle);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openFragment(fragment);
        }
    }
}
