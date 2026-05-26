package com.example.term_project;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class QuizDifficultyFragment extends Fragment {

    private int subjectId;

    // 난이도 이름
    private static final String EASY = "easy";
    private static final String NORMAL = "normal";
    private static final String HARD = "hard";

    // 난이도별 목표 점수
    private static final int EASY_TARGET_SCORE = 50;
    private static final int NORMAL_TARGET_SCORE = 80;
    private static final int HARD_TARGET_SCORE = 120;

    // 🎯 실시간 유저 티어 보관용 변수 (기본값 하수 세팅)
    private String userLevel = "하수";

    private Button btnEasy;
    private Button btnNormal;
    private Button btnHard;

    public QuizDifficultyFragment() {
        // 기본 생성자
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_quiz_difficulty, container, false);

        if (getArguments() != null) {
            subjectId = getArguments().getInt("subject_id");
        }

        // 🎯 태응님이 수정하신 XML 속 ID(btnEasy, btnNormal, btnHard)와 정확하게 바인딩 매칭
        btnEasy = view.findViewById(R.id.btnEasy);
        btnNormal = view.findViewById(R.id.btnNormal);
        btnHard = view.findViewById(R.id.btnHard);

        // 🔒 [강력 차단] 파이어스토어에서 등급 장부를 원격으로 긁어오기 전까지는
        // 화면이 뜨자마자 우선 무조건 모든 버튼을 회색 패드로 완전히 잠가버립니다.
        lockButtonInstantly(btnEasy);
        lockButtonInstantly(btnNormal);
        lockButtonInstantly(btnHard);

        // 🎯 서버에서 내 진짜 티어("하수", "중수", "고수") 체킹하러 출발
        checkUserLevelAndRestrictAccess();

        // 클릭 리스너 연결
        btnEasy.setOnClickListener(v -> moveToQuizPlay(EASY, EASY_TARGET_SCORE));
        btnNormal.setOnClickListener(v -> moveToQuizPlay(NORMAL, NORMAL_TARGET_SCORE));
        btnHard.setOnClickListener(v -> moveToQuizPlay(HARD, HARD_TARGET_SCORE));

        return view;
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
                            userLevel = lvl.trim(); // 🎯 미세한 뒤쪽 공백 찌꺼기 전면 제거 (.trim())
                        } else {
                            userLevel = "하수";
                        }
                    } else {
                        userLevel = "하수";
                    }

                    // 디버깅용 실시간 로그캣 출력
                    Log.d("UserLevelCheck", "파이어스토어에서 긁어온 실제 유저 티어: " + userLevel);

                    // 🎯 확보한 티어 정보에 근거하여 자물쇠 해제 신호를 쏩니다.
                    applyLevelRestrictionUI();
                })
                .addOnFailureListener(e -> {
                    Log.e("UserLevelCheck", "파이어스토어 로드 실패", e);
                    userLevel = "하수";
                    applyLevelRestrictionUI();
                });
    }

    private void applyLevelRestrictionUI() {
        if (!isAdded() || btnEasy == null || btnNormal == null || btnHard == null) return;

        // 🎯 [핵심 알고리즘 분기] 안드로이드 테마 간섭을 완전 배제하기 위해 조건별 락/언락 처리를 매치합니다.
        if (userLevel.contains("하수")) {
            // 하수는 오직 '하'만 활성화
            unlockButton(btnEasy, "#4CAF50"); // 원래 초록색 복구
            lockButtonInstantly(btnNormal);
            lockButtonInstantly(btnHard);

        } else if (userLevel.contains("중수")) {
            // 중수는 '하'와 '중' 활성화
            unlockButton(btnEasy, "#4CAF50");
            unlockButton(btnNormal, "#FF9800"); // 원래 주황색 복구
            lockButtonInstantly(btnHard);

        } else if (userLevel.contains("고수")) {
            // 고수는 만렙 프리패스 완전 개방
            unlockButton(btnEasy, "#4CAF50");
            unlockButton(btnNormal, "#FF9800");
            unlockButton(btnHard, "#E91E63"); // 원래 핑크색 복구

        } else {
            // 알 수 없는 데이터 예외 폴백 (안전하게 하수 처리)
            unlockButton(btnEasy, "#4CAF50");
            lockButtonInstantly(btnNormal);
            lockButtonInstantly(btnHard);
        }
    }

    /**
     * 🔒 버튼을 강제로 어두운 회색 상자로 물들이고 터치 입력을 차단하는 물리 자물쇠 메서드
     */
    private void lockButtonInstantly(Button button) {
        if (button == null) return;
        button.setEnabled(false);
        button.setClickable(false);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#A8A8A8"))); // 잠금 회색
        button.setTextColor(Color.parseColor("#E0E0E0")); // 글자 흐리게
    }

    /**
     * 🔓 유저 실력 조건 충족 시 자물쇠를 풀고 원래 XML에 세팅해둔 화사한 색상으로 불을 켜주는 메서드
     */
    private void unlockButton(Button button, String activeColorHex) {
        if (button == null) return;
        button.setEnabled(true);
        button.setClickable(true);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(activeColorHex))); // 지정 컬러 복구
        button.setTextColor(Color.parseColor("#FFFFFF")); // 글자 흰색 복구
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