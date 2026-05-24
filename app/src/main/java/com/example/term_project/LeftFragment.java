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

        getParentFragmentManager().setFragmentResultListener("quiz_result", getViewLifecycleOwner(), (requestKey, bundle) -> {
            // 신호를 받으면 즉시 보유 티켓 수와 카드 상태를 최신화
            refreshCards();
        });

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
     * 각 카드의 입장 및 자율 해금 여부를 설정한다.
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
            // 아직 해금되지 않은 과목
            int tickets = getAvailableUnlockTickets();

            // MainActivity로부터 실시간 보유 골드 획득
            int currentGold = 0;
            if (getActivity() instanceof MainActivity) {
                currentGold = ((MainActivity) getActivity()).getGold();
            }

            // 해금권이 있거나, 혹은 3000 골드 이상 있다면 해금 가능 상태로 표시
            if (tickets > 0 || currentGold >= 300) {
                card.setAlpha(0.6f);
                card.setOnClickListener(v -> showUnlockOptionsDialog(stageId));
            } else {
                //  둘 다 없다면 잠금 안내 메시지 변경 및 어둡게 표시
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
        SharedPreferences prefs = getQuizPrefs();
        int hardClearCount = 0;
        int manuallyUnlockedCount = 0;

        for (int i = 1; i <= 8; i++) {
            // 1. 상 난이도 클리어한 과목 수 체크
            if (prefs.getInt("subject_" + i + "_hard_clear", 0) == 1) {
                hardClearCount++;
            }
            // 2. 5과목 이상 중 이미 해금되어 플레이 가능한 과목 수 체크
            if (i > 4 && prefs.getInt("stage_" + i + "_before_clear", 0) == 1) {
                // 티켓으로 연 경우에만 해금권을 차감하도록 골드 해금 여부 필터링
                if (prefs.getInt("stage_" + i + "_unlocked_with_gold", 0) == 0) {
                    manuallyUnlockedCount++;
                }
            }
        }
        return hardClearCount - manuallyUnlockedCount;
    }

    private void showUnlockOptionsDialog(int stageId) {
        String subjectName = getSubjectNameById(stageId);
        int tickets = getAvailableUnlockTickets();

        int currentGold = 0;
        MainActivity mainActivity = null;

        if (getActivity() instanceof MainActivity) {
            mainActivity = (MainActivity) getActivity();
            currentGold = mainActivity.getGold();
        }

        // 💡 1. AlertDialog 대신 일반 Dialog 사용 및 커스텀 레이아웃 인플레이트
        final Dialog dialog = new Dialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_unlock_options, null);
        dialog.setContentView(dialogView);

        // 배경 투명화 (둥근 모서리 커스텀 배경이 제대로 보이게 하기 위함)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // 💡 2. 뷰 연결
        TextView tvTitle = dialogView.findViewById(R.id.tvUnlockTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvUnlockMessage);
        TextView tvMyTickets = dialogView.findViewById(R.id.tvMyTickets);
        TextView tvMyGold = dialogView.findViewById(R.id.tvMyGold);
        Button btnTicket = dialogView.findViewById(R.id.btnUnlockWithTicket);
        Button btnGold = dialogView.findViewById(R.id.btnUnlockWithGold);
        TextView tvCancel = dialogView.findViewById(R.id.tvCancelUnlock);

        // 💡 3. 데이터 셋팅
        tvTitle.setText("[" + subjectName + "] 과목 해금");
        tvMyTickets.setText("• 보유 해금권: " + tickets + "개");
        tvMyGold.setText("• 보유 골드: " + currentGold + " G");

        // 상황별 UI 예외 처리 (해금권이 없으면 버튼 비활성화 또는 숨김)
        if (tickets <= 0) {
            btnTicket.setVisibility(View.GONE); // 티켓 없으면 버튼을 아예 숨기거나 비활성화
            tvMessage.setText("현재 보유한 해금권이 없습니다.\n300 골드를 사용하여 해금할 수 있습니다.");
        }

        // QuizPlayFragment에 있는 버튼 꾹 누르기 애니메이션이 있다면 여기도 적용 가능
        if (mainActivity != null) {
            // applyPressAnimation(btnTicket);
            // applyPressAnimation(btnGold);
        }

        // 💡 4. 이벤트 리스너 정의
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

        // 💡 팝업창 크기 및 뒷배경 흐림(Dim) 처리 조정
        Window shownWindow = dialog.getWindow();
        if (shownWindow != null) {
            shownWindow.setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.85), // 화면 너비의 85%
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            shownWindow.setDimAmount(0.55f);
            shownWindow.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
    }

    /**
     * 💡  해금권을 사용한 방식을 처리
     */
    private void unlockStageWithTicket(int stageId, String subjectName) {
        boolean success = getQuizPrefs().edit().putInt("stage_" + stageId + "_before_clear", 1).commit();

        if (success) {
            saveStageUnlockToFirebase(stageId);
            refreshCards(); // 이제 무조건 해금된 데이터(1)를 읽어 화면을 갱신
            Toast.makeText(getContext(), "[" + subjectName + "] 과목이 해금권으로 활성화되었습니다!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 💡  3000 골드를 소모한 방식을 처리
     */
    private void unlockStageWithGold(int stageId, String subjectName, int newGold) {
        // 로컬 SharedPreferences에 해금 기록 저장
        getQuizPrefs().edit().putInt("stage_" + stageId + "_before_clear", 1)
                .putInt("stage_" + stageId + "_unlocked_with_gold", 1)
                .commit();

        //  파이어베이스 서버에 해금 상태 및 차감된 골드 데이터를 동시에 업데이트
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
                        // 서버 저장이 완료되면 상단바 텍스트 리프레시
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

                            //  1~8단계 전체를 확인하며 해금 정보와 상 난이도 클리어 정보 동기화
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
