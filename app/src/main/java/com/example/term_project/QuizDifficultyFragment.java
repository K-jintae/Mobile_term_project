package com.example.term_project;

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
        // onCreateView에서는 오직 레이아웃을 인플레이트하는 역할만 수행합니다.
        return inflater.inflate(R.layout.fragment_quiz_difficulty, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("UserLevelCheck", "onViewCreated 진입 - 뷰 초기화 및 리스너 등록 시작");

        if (getArguments() != null) {
            subjectId = getArguments().getInt("subject_id");
        }

        // 뷰 바인딩
        btnEasy = view.findViewById(R.id.btnEasy);
        btnNormal = view.findViewById(R.id.btnNormal);
        btnHard = view.findViewById(R.id.btnHard);
        tvClose = view.findViewById(R.id.tvClose);

        // [1단계] 클릭 리스너 연결
        btnEasy.setOnClickListener(v -> {
            if (userLevel.contains("하수") || userLevel.contains("중수") || userLevel.contains("고수")) {
                moveToQuizPlay(EASY, EASY_TARGET_SCORE);
            }
        });

        btnNormal.setOnClickListener(v -> {
            if (userLevel.contains("중수") || userLevel.contains("고수")) {
                moveToQuizPlay(NORMAL, NORMAL_TARGET_SCORE);
            } else {
                Toast.makeText(getContext(), "중수 등급 이상만 도전할 수 있습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        btnHard.setOnClickListener(v -> {
            if (userLevel.contains("고수")) {
                moveToQuizPlay(HARD, HARD_TARGET_SCORE);
            } else {
                Toast.makeText(getContext(), "고수 등급만 도전할 수 있습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        if (tvClose != null) {
            tvClose.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    // MainActivity에 정석으로 구현된 프래그먼트 종료 메서드를 호출합니다.
                    // 이 메서드가 백스택을 비우고 컨테이너 레이아웃을 GONE 처리하여 화면을 완전히 치웁니다.
                    ((MainActivity) getActivity()).closeCurrentFragment();
                }
            });
        }

        // [2단계] 화면이 뜨자마자 우선 무조건 모든 버튼을 잠금(연한 상태) 처리합니다.
        lockButtonInstantly(btnEasy, "하");
        lockButtonInstantly(btnNormal, "중");
        lockButtonInstantly(btnHard, "상");

        // [3단계] 서버에서 내 진짜 티어 체킹하러 출발
        checkUserLevelAndRestrictAccess();
    }

    private void checkUserLevelAndRestrictAccess() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            // 로그인 안 된 상태도 추적 가능하도록 로그 추가
            userLevel = "하수";
            Log.d("UserLevelCheck", "현재 로그인된 유저가 없습니다. 기본값 [하수]로 제한을 적용합니다.");
            applyLevelRestrictionUI();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        Log.d("UserLevelCheck", "로그인 유저 UID 확인됨: " + uid + " -> Firestore 호출합니다.");

        FirebaseFirestore.getInstance()
                .collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String lvl = documentSnapshot.getString("level");
                        if (lvl != null && !lvl.isEmpty()) {
                            userLevel = lvl.trim();
                        } else {
                            userLevel = "하수";
                        }
                    } else {
                        userLevel = "하수";
                    }

                    Log.d("UserLevelCheck", "파이어스토어 로드 성공! 실제 유저 티어: [" + userLevel + "]");
                    applyLevelRestrictionUI();
                })
                .addOnFailureListener(e -> {
                    Log.e("UserLevelCheck", "파이어스토어 로드 실패 (통신 에러)", e);
                    userLevel = "하수";
                    applyLevelRestrictionUI();
                });
    }

    private void applyLevelRestrictionUI() {
        if (btnEasy == null || btnNormal == null || btnHard == null) {
            Log.e("UserLevelCheck", "applyLevelRestrictionUI 에러: 버튼 뷰가 null입니다.");
            return;
        }

        Log.d("UserLevelCheck", "applyLevelRestrictionUI 실행 처리 시작 - 현재 티어: " + userLevel);

        if (userLevel.contains("하수")) {
            Log.d("UserLevelCheck", "결과: 하수 UI 적용 (하 진하게 / 중,상 연하게)");
            unlockButton(btnEasy, "하");
            lockButtonInstantly(btnNormal, "중");
            lockButtonInstantly(btnHard, "상");

        } else if (userLevel.contains("중수")) {
            Log.d("UserLevelCheck", "결과: 중수 UI 적용 (하,중 진하게 / 상 연하게)");
            unlockButton(btnEasy, "하");
            unlockButton(btnNormal, "중");
            lockButtonInstantly(btnHard, "상");

        } else if (userLevel.contains("고수")) {
            Log.d("UserLevelCheck", "결과: 고수 UI 적용 (모든 난이도 프리패스 진하게)");
            unlockButton(btnEasy, "하");
            unlockButton(btnNormal, "중");
            unlockButton(btnHard, "상");

        } else {
            Log.d("UserLevelCheck", "결과: 예외 등급 감지 -> 하수 UI로 대체");
            unlockButton(btnEasy, "하");
            lockButtonInstantly(btnNormal, "중");
            lockButtonInstantly(btnHard, "상");
        }
    }

    private void lockButtonInstantly(Button button, String text) {
        if (button == null) return;
        button.setText(text);
        // ARGB 알파 투명도(4C)를 결합하여 완벽하게 흐린 갈색으로 강제 고정
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CDCD3C9")));
        button.setTextColor(Color.parseColor("#4C4A3B32"));
    }

    private void unlockButton(Button button, String text) {
        if (button == null) return;
        button.setText(text);
        // 원본의 진하고 선명한 베이지 및 밤색 복구
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#DCD3C9")));
        button.setTextColor(Color.parseColor("#4A3B32"));
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