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

    /**
     * 현재 로그인한 유저의 UID를 반영한 고유의 진척도 SharedPreferences 장부를 반환하는 동적 헬퍼 메서드
     */
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

        // 기본 prefs 객체 연결 및 유저별 전용 동기화 준비
        prefs = getQuizPrefs();

        // fragment_left.xml의 1~8번 과목 카드 연결
        cardQuiz1 = view.findViewById(R.id.cardQuiz1);
        cardQuiz2 = view.findViewById(R.id.cardQuiz2);
        cardQuiz3 = view.findViewById(R.id.cardQuiz3);
        cardQuiz4 = view.findViewById(R.id.cardQuiz4);
        cardQuiz5 = view.findViewById(R.id.cardQuiz5);
        cardQuiz6 = view.findViewById(R.id.cardQuiz6);
        cardQuiz7 = view.findViewById(R.id.cardQuiz7);
        cardQuiz8 = view.findViewById(R.id.cardQuiz8);

        loadProgressFromFirebase();

        // [첫 번째 코드 신호] 퀴즈 결과 수신 시 카드 상태 최신화
        getParentFragmentManager().setFragmentResultListener("quiz_result", getViewLifecycleOwner(), (requestKey, bundle) -> {
            refreshCards();
        });

        // [두 번째 코드 신호] 결과창 등에서 새로고침 신호가 감지되면 UI와 장부를 강제 업데이트
        getParentFragmentManager().setFragmentResultListener("quiz_refresh_signal", getViewLifecycleOwner(), (requestKey, result) -> {
            loadProgressFromFirebase();
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // [두 번째 코드 기능] 화면의 뷰가 결합되는 순간 MainActivity 전역 변수에 주소값 등록
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).leftFragmentInstance = this;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // [두 번째 코드 기능] 메모리 누수 방지를 위해 인스턴스 참조 안전하게 해제
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

    /**
     * 전체 카드 상태를 갱신한다. (1~8과목 매칭)
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
     * 각 카드의 입장 및 자율 해금 여부를 설정한다. (첫 번째 코드의 핵심 기능 보존)
     */
    private void setupCard(LinearLayout card, int stageId) {
        if (card == null) {
            return;
        }

        boolean canPlay = canPlayStage(stageId);

        if (canPlay) {
            // 플레이 가능한 과목
            card.setAlpha(1.0f);
            card.setOnClickListener(v -> showDifficultyDialog(stageId));
        } else {
            // 아직 해금되지 않은 과목 판단 시작
            int tickets = getAvailableUnlockTickets();

            // MainActivity로부터 실시간 보유 골드 획득
            int currentGold = 0;
            if (getActivity() instanceof MainActivity) {
                currentGold = ((MainActivity) getActivity()).getGold();
            }

            // 해금권이 있거나, 혹은 300 골드 이상 있다면 해금 가능 상태(팝업 활성화)로 표시
            if (tickets > 0 || currentGold >= 300) {
                card.setAlpha(0.6f);
                card.setOnClickListener(v -> showUnlockOptionsDialog(stageId));
            } else {
                // 둘 다 없다면 잠금 안내 메시지 변경 및 어둡게 표시
                card.setAlpha(0.3f);
                card.setOnClickListener(v ->
                        Toast.makeText(
                                getContext(),
                                "사용 가능한 해금권이 없거나 골드가 부족합니다. (필요 골드: 300 G)",
                                Toast.LENGTH_SHORT
                        ).show()
                );
            }
        }
    }

    /**
     * 사용 가능한 해금권 개수를 실시간으로 계산합니다.
     */
    private int getAvailableUnlockTickets() {
        SharedPreferences quizPrefs = getQuizPrefs();
        int hardClearCount = 0;
        int manuallyUnlockedCount = 0;

        for (int i = 1; i <= 8; i++) {
            // 1. 상 난이도 클리어한 과목 수 체크
            if (quizPrefs.getInt("subject_" + i + "_hard_clear", 0) == 1) {
                hardClearCount++;
            }
            // 2. 5과목 이상 중 이미 해금되어 플레이 가능한 과목 수 체크
            if (i > 4 && quizPrefs.getInt("stage_" + i + "_before_clear", 0) == 1) {
                // 티켓으로 연 경우에만 해금권을 차감하도록 골드 해금 여부 필터링
                if (quizPrefs.getInt("stage_" + i + "_unlocked_with_gold", 0) == 0) {
                    manuallyUnlockedCount++;
                }
            }
        }
        return hardClearCount - manuallyUnlockedCount;
    }

    /**
     * 해금 선택 커스텀 다이얼로그 출력
     */
    private void showUnlockOptionsDialog(int stageId) {
        String subjectName = getSubjectNameById(stageId);
        int tickets = getAvailableUnlockTickets();

        int currentGold = 0;
        MainActivity mainActivity = null;

        if (getActivity() instanceof MainActivity) {
            mainActivity = (MainActivity) getActivity();
            currentGold = mainActivity.getGold();
        }

        final Dialog dialog = new Dialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_unlock_options, null);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvUnlockTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvUnlockMessage);
        TextView tvMyTickets = dialogView.findViewById(R.id.tvMyTickets);
        TextView tvMyGold = dialogView.findViewById(R.id.tvMyGold);
        Button btnTicket = dialogView.findViewById(R.id.btnUnlockWithTicket);
        Button btnGold = dialogView.findViewById(R.id.btnUnlockWithGold);
        TextView tvCancel = dialogView.findViewById(R.id.tvCancelUnlock);

        tvTitle.setText("[" + subjectName + "] 과목 해금");
        tvMyTickets.setText("• 보유 해금권: " + tickets + "개");
        tvMyGold.setText("• 보유 골드: " + currentGold + " G");

        if (tickets <= 0) {
            btnTicket.setVisibility(View.GONE);
            tvMessage.setText("현재 보유한 해금권이 없습니다.\n300 골드를 사용하여 해금할 수 있습니다.");
        }

        btnTicket.setOnClickListener(v -> {
            dialog.dismiss();
            unlockStageWithTicket(stageId, subjectName);
        });

        final MainActivity finalMainActivity = mainActivity;
        final int finalCurrentGold = currentGold;

        btnGold.setOnClickListener(v -> {
            if (finalMainActivity != null) {
                if (finalCurrentGold >= 300) {
                    dialog.dismiss();
                    finalMainActivity.spendGold(300);
                    int updatedGold = finalCurrentGold - 300;
                    unlockStageWithGold(stageId, subjectName, updatedGold);
                } else {
                    Toast.makeText(getContext(), "골드가 부족합니다. 퀴즈를 풀어 골드를 더 모아보세요!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        tvCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        Window shownWindow = dialog.getWindow();
        if (shownWindow != null) {
            shownWindow.setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            shownWindow.setDimAmount(0.55f);
            shownWindow.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
    }

    /**
     * 해금권을 사용한 방식을 처리
     */
    private void unlockStageWithTicket(int stageId, String subjectName) {
        boolean success = getQuizPrefs().edit().putInt("stage_" + stageId + "_before_clear", 1).commit();

        if (success) {
            saveStageUnlockToFirebase(stageId);
            refreshCards();
            Toast.makeText(getContext(), "[" + subjectName + "] 과목이 해금권으로 활성화되었습니다!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 300 골드를 소모한 방식을 처리
     */
    private void unlockStageWithGold(int stageId, String subjectName, int newGold) {
        getQuizPrefs().edit().putInt("stage_" + stageId + "_before_clear", 1)
                .putInt("stage_" + stageId + "_unlocked_with_gold", 1)
                .commit();

        com.google.firebase.auth.FirebaseAuth mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();

            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("unlocked_stage_" + stageId, true);
            updates.put("unlocked_with_gold_stage_" + stageId, true);
            updates.put("gold", newGold);

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).updateTopBar();
                        }
                    });
        }

        refreshCards();
        Toast.makeText(getContext(), "[" + subjectName + "] 과목이 300 골드로 활성화되었습니다!", Toast.LENGTH_SHORT).show();
    }

    private void saveStageUnlockToFirebase(int stageId) {
        com.google.firebase.auth.FirebaseAuth mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("unlocked_stage_" + stageId, true);
    }

    private String getSubjectNameById(int stageId) {
        switch (stageId) {
            case 1: return "C언어";
            case 2: return "객체지향프로그래밍";
            case 3: return "자료구조";
            case 4: return "운영체제";
            case 5: return "알고리즘";
            case 6: return "컴퓨터네트워크";
            case 7: return "인공지능개론";
            case 8: return "데이터과학";
            default: return "과목 " + stageId;
        }
    }

    /**
     * 난이도 선택 다이얼로그 (유저 티어 및 이전 난이도 클리어 여부 복합 검증 유지)
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

        String uid = "guest";
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            uid = auth.getCurrentUser().getUid();
        }

        SharedPreferences currentPrefs = getQuizPrefs();
        boolean isEasyCleared = currentPrefs.getInt("subject_" + stageId + "_easy_clear", 0) == 1;
        boolean isNormalCleared = currentPrefs.getInt("subject_" + stageId + "_normal_clear", 0) == 1;

        SharedPreferences userPrefs = requireContext().getSharedPreferences("user_" + uid, Context.MODE_PRIVATE);
        String userLevel = userPrefs.getString("level", "하수");

        // 1. '하(Easy)' 난이도는 프리패스
        btnEasy.setOnClickListener(v -> {
            dialog.dismiss();
            moveToQuizPlay(stageId, "easy");
        });

        // 2. '중(Normal)' 난이도 제어: '하' 클리어 혹은 유저 등급 '중수' 이상 오픈
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

        // 3. '상(Hard)' 난이도 제어: '중' 클리어 혹은 유저 등급 '고수'면 오픈
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
     * Firebase에서 해금 정보 가져오기 (첫 번째 코드의 누락 없는 동기화 루프 오류 수정본)
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

                            // 1~8단계 전체를 순회하며 해금 상태, 골드 해금 여부, 상 난이도 클리어 여부를 완벽히 동기화
                            for (int i = 1; i <= 8; i++) {
                                Boolean isUnlocked = doc.getBoolean("unlocked_stage_" + i);
                                if (isUnlocked != null && isUnlocked) {
                                    editor.putInt("stage_" + i + "_before_clear", 1);
                                }

                                Boolean isUnlockedWithGold = doc.getBoolean("unlocked_with_gold_stage_" + i);
                                if (isUnlockedWithGold != null && isUnlockedWithGold) {
                                    editor.putInt("stage_" + i + "_unlocked_with_gold", 1);
                                }

                                Boolean isHardCleared = doc.getBoolean("cleared_hard_stage_" + i);
                                if (isHardCleared != null && isHardCleared) {
                                    editor.putInt("subject_" + i + "_hard_clear", 1);
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
     * 해당 스테이지를 플레이할 수 있는지 확인한다. (1~4단계 기본 오픈 규칙 결합)
     */
    private boolean canPlayStage(int stageId) {
        if (stageId <= 4) {
            return true;
        }
        return getQuizPrefs().getInt("stage_" + stageId + "_before_clear", 0) == 1;
    }

    /**
     * 잠긴 카드 클릭 시 안내 메시지
     */
    private String getLockedMessage(int stageId) {
        if (stageId <= 4) {
            return "플레이 가능합니다.";
        }
        return (stageId - 1) + "단계를 먼저 클리어해야 합니다.";
    }

    /**
     * 퀴즈 화면으로 이동한다.
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

    /**
     * [두 번째 코드의 핵심 기능] 퀴즈가 끝나고 가림막이 걷힐 때 MainActivity가 직접 강제 호출하여 화면을 동기화하는 메서드
     */
    public void refreshUnlockedStages() {
        if (getContext() != null) {
            refreshCards();
            loadProgressFromFirebase();
        }
    }
}