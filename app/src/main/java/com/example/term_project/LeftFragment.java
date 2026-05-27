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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class LeftFragment extends Fragment {

    private SharedPreferences prefs;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_left, container, false);

        com.google.firebase.auth.FirebaseAuth mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
        String uid = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : "guest";
        prefs = requireContext().getSharedPreferences("quiz_progress_" + uid, Context.MODE_PRIVATE);

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

        getParentFragmentManager().setFragmentResultListener("quiz_refresh_signal", getViewLifecycleOwner(), (requestKey, result) -> {
            // 신호가 감지되면 서버에서 최신 해금 장부를 다시 긁어오고 UI를 강제 새로고침합니다.
            loadProgressFromFirebase();
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // [신규 추가] 화면의 뷰가 완전히 결합되는 순간 MainActivity 전역 변수에 내 주소값을 직접 등록합니다.
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).leftFragmentInstance = this;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // [신규 추가] 뷰 레이아웃이 파괴되어 나갈 때 메모리 누수 방지를 위해 안전하게 변수를 비워줍니다.
        if (getActivity() instanceof MainActivity) {
            if (((MainActivity) getActivity()).leftFragmentInstance == this) {
                ((MainActivity) getActivity()).leftFragmentInstance = null;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCards();
    }

    /*
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

    /*
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
        // 과목 ID를 번들에 담아 새로 만든 난이도 프래그먼트로 전달합니다.
        Bundle bundle = new Bundle();
        bundle.putInt("subject_id", stageId);

        QuizDifficultyFragment fragment = new QuizDifficultyFragment();
        fragment.setArguments(bundle);

        // MainActivity의 openFragment를 통해 정식으로 화면을 띄웁니다.
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openFragment(fragment);
        }
    }

    /*
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
                            SharedPreferences.Editor editor = prefs.edit();

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

    /*
     * 해당 스테이지를 플레이할 수 있는지 확인한다.
     * 1단계는 항상 플레이 가능.
     * 2단계 이상은 stage_N_before_clear 값이 1이어야 가능.
     */
    private boolean canPlayStage(int stageId) {
        if (stageId == 1) {
            return true;
        }

        return prefs.getInt("stage_" + stageId + "_before_clear", 0) == 1;
    }

    /*
     * 잠긴 카드 클릭 시 안내 메시지.
     */
    private String getLockedMessage(int stageId) {
        if (stageId == 1) {
            return "플레이 가능합니다.";
        }

        return (stageId - 1) + "단계를 먼저 클리어해야 합니다.";
    }

    /*
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

    /*
     * 퀴즈가 끝나고 화면 가림막이 걷힐 때 MainActivity에 의해 다이렉트로 강제 호출되는 메서드
     */
    public void refreshUnlockedStages() {
        if (getContext() != null) {
            // 로컬에 누적된 스테이지 클리어 정보를 UI 카드에 반영합니다.
            refreshCards();
            // 파이어베이스 최신 해금 장부도 다시 한번 백그라운드 동기화합니다.
            loadProgressFromFirebase();
        }
    }
}