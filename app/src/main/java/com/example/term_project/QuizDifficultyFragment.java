package com.example.term_project;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class QuizDifficultyFragment extends Fragment {

    private int subjectId;
    private SharedPreferences prefs;

    private static final String EASY = "easy";
    private static final String NORMAL = "normal";
    private static final String HARD = "hard";

    private static final int EASY_TARGET_SCORE = 50;
    private static final int NORMAL_TARGET_SCORE = 80;
    private static final int HARD_TARGET_SCORE = 120;

    private String userLevel = "하수";

    private Button btnEasy;
    private Button btnNormal;
    private Button btnHard;
    private TextView tvClose;

    public QuizDifficultyFragment() {
        // 기본 생성자
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz_difficulty, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("UserLevelCheck", "onViewCreated 진입 - 규칙 통합 초기화 시작");

        if (getArguments() != null) {
            subjectId = getArguments().getInt("subject_id");
        }

        // 로컬 클리어 진척도 장부 및 등급 캐시 연결
        com.google.firebase.auth.FirebaseAuth mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
        String uid = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : "guest";
        prefs = requireContext().getSharedPreferences("quiz_progress_" + uid, Context.MODE_PRIVATE);

        // 뷰 바인딩
        btnEasy = view.findViewById(R.id.btnEasy);
        btnNormal = view.findViewById(R.id.btnNormal);
        btnHard = view.findViewById(R.id.btnHard);
        tvClose = view.findViewById(R.id.tvClose);

        // 규칙 1 및 규칙 2를 반영한 버튼 클릭 리스너 연결
        btnEasy.setOnClickListener(v -> {
            moveToQuizPlay(EASY, EASY_TARGET_SCORE);
        });

        btnNormal.setOnClickListener(v -> {
            boolean isEasyClear = prefs.getInt("subject_" + subjectId + "_easy_clear", 0) == 1;

            // 규칙 1(중수 이상 등급) 또는 규칙 2(현재 과목의 하 난이도 클리어) 중 하나라도 만족하면 입장 가능
            if (userLevel.contains("중수") || userLevel.contains("고수") || isEasyClear) {
                moveToQuizPlay(NORMAL, NORMAL_TARGET_SCORE);
            } else {
                Toast.makeText(getContext(), "하 난이도를 먼저 클리어하거나 중수 등급 이상이어야 합니다.", Toast.LENGTH_SHORT).show();
            }
        });

        btnHard.setOnClickListener(v -> {
            boolean isNormalClear = prefs.getInt("subject_" + subjectId + "_normal_clear", 0) == 1;

            // 규칙 1(고수 등급) 또는 규칙 2(현재 과목의 중 난이도 클리어) 중 하나라도 만족하면 입장 가능
            if (userLevel.contains("고수") || isNormalClear) {
                moveToQuizPlay(HARD, HARD_TARGET_SCORE);
            } else {
                Toast.makeText(getContext(), "중 난이도를 먼저 클리어하거나 고수 등급이어야 합니다.", Toast.LENGTH_SHORT).show();
            }
        });

        if (tvClose != null) {
            tvClose.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).closeCurrentFragment();
                }
            });
        }

        // 비동기 깜빡임 현상을 방지하기 위해, 먼저 로컬에 저장되어 있던 기존 등급으로 UI를 즉시 그려줍니다.
        userLevel = prefs.getString("cached_user_level", "하수");
        applyLevelRestrictionUI();

        // 이어서 서버의 최신 유저 등급 장부(규칙 3 승급 반영용)를 동기화하러 출발합니다.
        checkUserLevelAndRestrictAccess();
    }

    private void checkUserLevelAndRestrictAccess() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            userLevel = "하수";
            applyLevelRestrictionUI();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String lvl = documentSnapshot.getString("level");
                        if (lvl != null && !lvl.isEmpty()) {
                            userLevel = lvl.trim();

                            // 규칙 3에 의해 등급이 상승했다면, 다음 진입 시 깜빡이지 않도록 로컬 장부도 동기화합니다.
                            prefs.edit().putString("cached_user_level", userLevel).apply();
                        } else {
                            userLevel = "하수";
                        }
                    } else {
                        userLevel = "하수";
                    }

                    Log.d("UserLevelCheck", "파이어스토어 동기화 완료 - 현재 등급: [" + userLevel + "]");
                    applyLevelRestrictionUI();
                })
                .addOnFailureListener(e -> {
                    Log.e("UserLevelCheck", "파이어스토어 로드 실패", e);
                    applyLevelRestrictionUI();
                });
    }

    /*
     * 정해진 규칙 순서대로 UI 버튼의 활성화/투명도 상태를 매칭하는 핵심 메서드입니다.
     */
    private void applyLevelRestrictionUI() {
        if (btnEasy == null || btnNormal == null || btnHard == null) {
            return;
        }

        // 현재 과목의 하, 중 난이도 개별 클리어 여부를 장부에서 가져옵니다.
        boolean isEasyClear = prefs.getInt("subject_" + subjectId + "_easy_clear", 0) == 1;
        boolean isNormalClear = prefs.getInt("subject_" + subjectId + "_normal_clear", 0) == 1;

        // 하 난이도는 기본 개방 상태이므로 상시 활성화
        unlockButton(btnEasy, "하");

        // 중 난이도 판정 규칙: 유저 등급이 중수/고수이거나, 현재 과목의 하 단계를 깼을 때 해금
        if (userLevel.contains("중수") || userLevel.contains("고수") || isEasyClear) {
            unlockButton(btnNormal, "중");
        } else {
            lockButtonInstantly(btnNormal, "중");
        }

        // 상 난이도 판정 규칙: 유저 등급이 고수이거나, 현재 과목의 중 단계를 깼을 때 해금
        if (userLevel.contains("고수") || isNormalClear) {
            unlockButton(btnHard, "상");
        } else {
            lockButtonInstantly(btnHard, "상");
        }
    }

    private void lockButtonInstantly(Button button, String text) {
        if (button == null) return;
        button.setText(text);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CDCD3C9")));
        button.setTextColor(Color.parseColor("#4C4A3B32"));
        button.setAlpha(0.4f);
    }

    private void unlockButton(Button button, String text) {
        if (button == null) return;
        button.setText(text);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#DCD3C9")));
        button.setTextColor(Color.parseColor("#4A3B32"));
        button.setAlpha(1.0f);
    }

    private void moveToQuizPlay(String difficulty, int targetScore) {
        Bundle bundle = new Bundle();
        bundle.putInt("subject_id", subjectId);
        bundle.putString("difficulty", difficulty);
        bundle.putInt("target_score", targetScore);

        QuizPlayFragment fragment = new QuizPlayFragment();
        fragment.setArguments(bundle);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openFragment(fragment);
        }
    }
}