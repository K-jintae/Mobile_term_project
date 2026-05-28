package com.example.term_project;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class BattleQuizActivity extends AppCompatActivity {

    private TextView tvBattleTitle;
    private TextView tvTimer;
    private ProgressBar progressTimer;
    private TextView tvQuestionCount;
    private TextView tvQuestion;
    private TextView tvMyScore;
    private TextView tvOpponentScore;
    private TextView tvOpponentStatus;

    private Button btnOption1;
    private Button btnOption2;
    private Button btnOption3;
    private Button btnOption4;
    private Button btnExitBattle;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private FirebaseAuth auth;
    private DatabaseReference roomRef;

    private ValueEventListener roomListener;
    private ValueEventListener serverTimeOffsetListener;

    private String battleId;
    private String myUid;
    private String playerA;
    private String playerB;
    private String hostUid;
    private String opponentUid;

    private boolean isHost = false;
    private boolean battleExitHandled = false;
    private boolean questionLoadRequested = false;
    private boolean finishSettledRequested = false;

    private int lastRenderedRound = -1;
    private int lastScheduledRevealRound = -1;

    private long serverTimeOffset = 0L;

    private final List<QuizQuestion> questions = new ArrayList<>();
    private Runnable timerRunnable;

    private static final int DEFAULT_BUTTON_COLOR = Color.parseColor("#F5EBE0");
    private static final int CORRECT_COLOR = Color.parseColor("#A5D6A7");
    private static final int MY_CHOICE_COLOR = Color.parseColor("#90CAF9");
    private static final int OPPONENT_CHOICE_COLOR = Color.parseColor("#FFCC80");

    // 실제 DB 과목 번호가 다르면 여기만 수정
    private static final int[] BATTLE_SUBJECT_IDS = {1, 2, 3, 4, 5, 6, 7};

    // 기존 LevelTestActivity 기준
    private static final String[] BATTLE_DIFFICULTIES = {"easy", "normal", "hard"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle_quiz);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        myUid = auth.getCurrentUser().getUid();

        battleId = getIntent().getStringExtra("battleId");
        if (battleId == null || battleId.trim().isEmpty()) {
            Toast.makeText(this, "대전 방 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        roomRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("battle_rooms")
                .child(battleId);

        bindViews();
        setButtonEvents();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                leaveBattleRoomAndFinish();
            }
        });

        listenServerTimeOffset();
        listenBattleRoom();
    }

    private void bindViews() {
        tvBattleTitle = findViewById(R.id.tvBattleTitle);
        tvTimer = findViewById(R.id.tvTimer);
        progressTimer = findViewById(R.id.progressTimer);
        tvQuestionCount = findViewById(R.id.tvQuestionCount);
        tvQuestion = findViewById(R.id.tvQuestion);
        tvMyScore = findViewById(R.id.tvMyScore);
        tvOpponentScore = findViewById(R.id.tvOpponentScore);
        tvOpponentStatus = findViewById(R.id.tvOpponentStatus);

        btnOption1 = findViewById(R.id.btnOption1);
        btnOption2 = findViewById(R.id.btnOption2);
        btnOption3 = findViewById(R.id.btnOption3);
        btnOption4 = findViewById(R.id.btnOption4);
        btnExitBattle = findViewById(R.id.btnExitBattle);
    }

    private void setButtonEvents() {
        btnOption1.setOnClickListener(v -> submitAnswer(0));
        btnOption2.setOnClickListener(v -> submitAnswer(1));
        btnOption3.setOnClickListener(v -> submitAnswer(2));
        btnOption4.setOnClickListener(v -> submitAnswer(3));

        btnExitBattle.setOnClickListener(v -> leaveBattleRoomAndFinish());
    }

    private void listenServerTimeOffset() {
        DatabaseReference offsetRef = FirebaseDatabase.getInstance()
                .getReference(".info/serverTimeOffset");

        serverTimeOffsetListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long offset = snapshot.getValue(Long.class);
                if (offset != null) {
                    serverTimeOffset = offset;
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                // 실패하면 로컬 시간 기준 fallback
            }
        };

        offsetRef.addValueEventListener(serverTimeOffsetListener);
    }

    private long getServerNow() {
        return System.currentTimeMillis() + serverTimeOffset;
    }

    private void listenBattleRoom() {
        roomListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(BattleQuizActivity.this, "대전 방이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                renderRoom(snapshot);
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                Toast.makeText(BattleQuizActivity.this, "대전 정보 로드 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        roomRef.addValueEventListener(roomListener);
    }

    private void renderRoom(DataSnapshot room) {
        playerA = room.child("playerA").getValue(String.class);
        playerB = room.child("playerB").getValue(String.class);
        hostUid = room.child("hostUid").getValue(String.class);

        String status = getStringValue(room, "status", "loading");
        String roundState = getStringValue(room, "roundState", "loading");

        if (playerA == null || playerB == null || hostUid == null) {
            showLoadingState("대전방 정보를 불러오는 중입니다.");
            return;
        }

        isHost = myUid != null && myUid.equals(hostUid);
        opponentUid = myUid != null && myUid.equals(playerA) ? playerB : playerA;

        if ("cancelled".equals(status) || "cancelled".equals(roundState)) {
            stopTimer();
            setOptionButtonsEnabled(false);
            Toast.makeText(this, "대전이 취소되었습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if ("finished".equals(status) || "finished".equals(roundState)) {
            loadQuestionsFromSnapshot(room);
            updateScoreViews(room);
            renderFinished(room);
            return;
        }

        int questionCount = getIntValue(room, "questionCount", 10);

        if (isHost && ("loading".equals(status) || "loading".equals(roundState)) && !questionLoadRequested) {
            questionLoadRequested = true;
            loadMixedQuestionsAndStart(questionCount);
            showLoadingState("문제를 불러오는 중입니다.");
            return;
        }

        loadQuestionsFromSnapshot(room);

        if (questions.isEmpty()) {
            showLoadingState("상대가 문제를 준비하는 중입니다.");
            return;
        }

        int roundIndex = getIntValue(room, "roundIndex", 0);

        if (roundIndex != lastRenderedRound) {
            lastRenderedRound = roundIndex;
        }

        updateScoreViews(room);

        if ("answering".equals(roundState)) {
            renderAnswering(room, roundIndex);
        } else if ("revealing".equals(roundState)) {
            renderRevealing(room, roundIndex);

            if (isHost && lastScheduledRevealRound != roundIndex) {
                lastScheduledRevealRound = roundIndex;
                long revealDurationMs = getLongValue(room, "revealDurationMs", 2000L);

                handler.postDelayed(() -> advanceToNextRound(roundIndex), revealDurationMs);
            }
        } else {
            showLoadingState("대전 상태를 준비하는 중입니다.");
        }
    }

    private void showLoadingState(String message) {
        tvBattleTitle.setText("실시간 퀴즈 대전");
        tvTimer.setText("-");
        progressTimer.setProgress(0);
        tvQuestionCount.setText("");
        tvQuestion.setText(message);
        tvOpponentStatus.setText("");
        setOptionButtonsEnabled(false);
        resetButtonColors();
    }

    private void loadMixedQuestionsAndStart(int questionCount) {
        QuizRepository repository = new QuizRepository();
        ArrayList<QuizQuestion> allQuestions = new ArrayList<>();

        final int[] pendingCount = {BATTLE_SUBJECT_IDS.length * BATTLE_DIFFICULTIES.length};

        for (int subjectId : BATTLE_SUBJECT_IDS) {
            for (String difficulty : BATTLE_DIFFICULTIES) {
                repository.getQuizQuestionsFromFirestore(
                        subjectId,
                        difficulty,
                        100,
                        new QuizRepository.OnQuestionsFetchedListener() {
                            @Override
                            public void onSuccess(List<QuizQuestion> fetchedQuestions) {
                                if (fetchedQuestions != null) {
                                    allQuestions.addAll(fetchedQuestions);
                                }

                                pendingCount[0]--;

                                if (pendingCount[0] == 0) {
                                    saveMixedQuestionsToRoom(allQuestions, questionCount);
                                }
                            }

                            @Override
                            public void onFailure(Exception e) {
                                pendingCount[0]--;

                                if (pendingCount[0] == 0) {
                                    saveMixedQuestionsToRoom(allQuestions, questionCount);
                                }
                            }
                        }
                );
            }
        }
    }

    private void saveMixedQuestionsToRoom(List<QuizQuestion> allQuestions, int questionCount) {
        if (allQuestions == null || allQuestions.isEmpty()) {
            Toast.makeText(BattleQuizActivity.this, "출제 가능한 문제가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Collections.shuffle(allQuestions);

        int finalCount = Math.min(questionCount, allQuestions.size());

        Map<String, Object> updates = new HashMap<>();
        Map<String, Object> questionMap = new HashMap<>();

        for (int i = 0; i < finalCount; i++) {
            questionMap.put(String.valueOf(i), allQuestions.get(i).toBattleMap());
        }

        updates.put("questions", questionMap);
        updates.put("questionCount", finalCount);
        updates.put("status", "playing");
        updates.put("roundState", "answering");
        updates.put("roundIndex", 0);
        updates.put("roundStartTime", getServerNow());
        updates.put("roundDurationMs", 10_000L);
        updates.put("revealDurationMs", 2000L);
        updates.put("goldSettled", false);
        updates.put("goldSettlementInProgress", false);

        roomRef.updateChildren(updates);
    }

    private void loadQuestionsFromSnapshot(DataSnapshot room) {
        questions.clear();

        DataSnapshot questionSnap = room.child("questions");

        for (DataSnapshot child : questionSnap.getChildren()) {
            QuizQuestion q = snapshotToQuizQuestion(child);

            if (q != null) {
                questions.add(q);
            }
        }
    }

    private QuizQuestion snapshotToQuizQuestion(DataSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }

        Long quizIdLong = snapshot.child("quizId").getValue(Long.class);
        String questionText = snapshot.child("question").getValue(String.class);
        Long correctIndexLong = snapshot.child("correctAnswerIndex").getValue(Long.class);
        String difficultyLevel = snapshot.child("difficultyLevel").getValue(String.class);

        if (questionText == null) {
            return null;
        }

        int quizId = quizIdLong != null ? quizIdLong.intValue() : 0;
        int correctAnswerIndex = correctIndexLong != null ? correctIndexLong.intValue() : 0;

        ArrayList<String> optionList = new ArrayList<>();
        DataSnapshot optionsSnap = snapshot.child("options");

        for (DataSnapshot optionChild : optionsSnap.getChildren()) {
            String option = optionChild.getValue(String.class);

            if (option != null) {
                optionList.add(option);
            }
        }

        String[] options = optionList.toArray(new String[0]);

        return new QuizQuestion(
                quizId,
                questionText,
                options,
                correctAnswerIndex,
                difficultyLevel
        );
    }

    private void renderAnswering(DataSnapshot room, int roundIndex) {
        if (roundIndex < 0 || roundIndex >= questions.size()) {
            return;
        }

        QuizQuestion question = questions.get(roundIndex);

        tvBattleTitle.setText("실시간 퀴즈 대전");
        tvQuestionCount.setText((roundIndex + 1) + " / " + questions.size());
        tvQuestion.setText(question.getQuestion());

        setOptions(question);
        resetButtonColors();

        int myChoice = getSelectedIndex(room, roundIndex, myUid);
        boolean opponentAnswered = hasOpponentAnswer(room, roundIndex);

        if (myChoice >= 0) {
            showMySelectedAnswer(myChoice);
        }

        if (myChoice >= 0 && opponentAnswered) {
            tvOpponentStatus.setText("나: 선택 완료 / 상대: 선택 완료");
        } else if (myChoice >= 0) {
            tvOpponentStatus.setText("나: 선택 완료 / 상대: 선택 중");
        } else if (opponentAnswered) {
            tvOpponentStatus.setText("나: 선택 중 / 상대: 선택 완료");
        } else {
            tvOpponentStatus.setText("나: 선택 중 / 상대: 선택 중");
        }

        // 핵심 수정:
        // 선택했더라도 제한 시간 전까지 다시 선택 가능하게 유지
        setOptionButtonsEnabled(true);

        long roundStartTime = getLongValue(room, "roundStartTime", getServerNow());
        long roundDurationMs = getLongValue(room, "roundDurationMs", 10_000L);

        startLocalTimer(roundIndex, roundStartTime, roundDurationMs);

        // 핵심 수정:
        // 양쪽이 선택해도 즉시 라운드 종료하지 않음.
        // 타이머가 0이 되었을 때 host만 closeRoundByTransaction() 실행.
    }

    private void renderRevealing(DataSnapshot room, int roundIndex) {
        stopTimer();

        if (roundIndex < 0 || roundIndex >= questions.size()) {
            return;
        }

        QuizQuestion question = questions.get(roundIndex);

        tvBattleTitle.setText("정답 공개");
        tvTimer.setText("공개");
        progressTimer.setProgress(100);
        tvQuestionCount.setText((roundIndex + 1) + " / " + questions.size());
        tvQuestion.setText(question.getQuestion());

        setOptions(question);
        setOptionButtonsEnabled(false);
        resetButtonColors();

        int correctIndex = question.getCorrectAnswerIndex();
        int myChoice = getSelectedIndex(room, roundIndex, myUid);
        int opponentChoice = getSelectedIndex(room, roundIndex, opponentUid);

        Button[] buttons = getOptionButtons();

        if (correctIndex >= 0 && correctIndex < buttons.length) {
            buttons[correctIndex].setBackgroundColor(CORRECT_COLOR);
        }

        if (myChoice >= 0 && myChoice < buttons.length && myChoice != correctIndex) {
            buttons[myChoice].setBackgroundColor(MY_CHOICE_COLOR);
        }

        if (opponentChoice >= 0 && opponentChoice < buttons.length && opponentChoice != correctIndex) {
            buttons[opponentChoice].setBackgroundColor(OPPONENT_CHOICE_COLOR);
        }

        String myText = myChoice == -1 ? "나: 시간 초과" : "나: " + (myChoice + 1) + "번";
        String opponentText = opponentChoice == -1 ? "상대: 시간 초과" : "상대: " + (opponentChoice + 1) + "번";

        tvOpponentStatus.setText(myText + " / " + opponentText);
    }

    private void renderFinished(DataSnapshot room) {
        stopTimer();
        setOptionButtonsEnabled(false);

        int scoreA = getScore(room, playerA);
        int scoreB = getScore(room, playerB);

        int myScore = getScore(room, myUid);
        int opponentScore = getScore(room, opponentUid);

        int betGold = getIntValue(room, "betGold", 0);

        String endReason = room.child("endReason").getValue(String.class);
        String forfeitWinnerUid = room.child("forfeitWinnerUid").getValue(String.class);
        String leftUid = room.child("leftUid").getValue(String.class);

        tvBattleTitle.setText("대전 종료");
        tvTimer.setText("끝");
        progressTimer.setProgress(100);
        tvQuestionCount.setText("최종 결과");

        String result;

        if ("forfeit".equals(endReason) && forfeitWinnerUid != null) {
            if (forfeitWinnerUid.equals(myUid)) {
                result = "승리";
            } else {
                result = "패배";
            }
        } else {
            if (myScore > opponentScore) {
                result = "승리";
            } else if (myScore < opponentScore) {
                result = "패배";
            } else {
                result = "무승부";
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("결과: ").append(result).append("\n\n");
        sb.append("내 점수: ").append(myScore).append("\n");
        sb.append("상대 점수: ").append(opponentScore).append("\n");
        sb.append("베팅 골드: ").append(betGold);

        if ("forfeit".equals(endReason)) {
            sb.append("\n\n상대 또는 내가 대전 중 나가서 몰수패 처리되었습니다.");
        }

        tvQuestion.setText(sb.toString());

        Boolean goldSettled = room.child("goldSettled").getValue(Boolean.class);
        Boolean goldSettlementInProgress = room.child("goldSettlementInProgress").getValue(Boolean.class);

        if (Boolean.TRUE.equals(goldSettled)) {
            tvOpponentStatus.setText("골드 정산 완료");
        } else if (Boolean.TRUE.equals(goldSettlementInProgress)) {
            tvOpponentStatus.setText("골드 정산 중");
        } else {
            tvOpponentStatus.setText("골드 정산 대기 중");
        }

        if (!Boolean.TRUE.equals(goldSettled)
                && !Boolean.TRUE.equals(goldSettlementInProgress)
                && !finishSettledRequested) {

            finishSettledRequested = true;

            if ("forfeit".equals(endReason) && forfeitWinnerUid != null && leftUid != null) {
                requestForfeitGoldSettlement(forfeitWinnerUid, leftUid, betGold);
            } else {
                requestScoreGoldSettlement(playerA, playerB, betGold, scoreA, scoreB);
            }
        }
    }

    private void requestScoreGoldSettlement(
            String playerA,
            String playerB,
            int betGold,
            int scoreA,
            int scoreB
    ) {
        claimGoldSettlement(() -> {
            GoldManager.settleBattleGold(playerA, playerB, betGold, scoreA, scoreB, new GoldManager.GoldCallback() {
                @Override
                public void onSuccess() {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("goldSettled", true);
                    updates.put("goldSettlementInProgress", false);
                    updates.put("goldSettledAt", getServerNow());
                    roomRef.updateChildren(updates);
                }

                @Override
                public void onFailure(String message) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("goldSettled", false);
                    updates.put("goldSettlementInProgress", false);
                    updates.put("goldSettlementError", message);
                    roomRef.updateChildren(updates);

                    Toast.makeText(BattleQuizActivity.this, message, Toast.LENGTH_SHORT).show();
                    finishSettledRequested = false;
                }
            });
        });
    }

    private void requestForfeitGoldSettlement(
            String winnerUid,
            String loserUid,
            int betGold
    ) {
        claimGoldSettlement(() -> {
            GoldManager.settleBattleGoldByWinner(winnerUid, loserUid, betGold, new GoldManager.GoldCallback() {
                @Override
                public void onSuccess() {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("goldSettled", true);
                    updates.put("goldSettlementInProgress", false);
                    updates.put("goldSettledAt", getServerNow());
                    roomRef.updateChildren(updates);
                }

                @Override
                public void onFailure(String message) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("goldSettled", false);
                    updates.put("goldSettlementInProgress", false);
                    updates.put("goldSettlementError", message);
                    roomRef.updateChildren(updates);

                    Toast.makeText(BattleQuizActivity.this, message, Toast.LENGTH_SHORT).show();
                    finishSettledRequested = false;
                }
            });
        });
    }

    private void claimGoldSettlement(Runnable onClaimed) {
        roomRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Boolean goldSettled = asBooleanObject(currentData.child("goldSettled").getValue());
                Boolean inProgress = asBooleanObject(currentData.child("goldSettlementInProgress").getValue());

                if (Boolean.TRUE.equals(goldSettled) || Boolean.TRUE.equals(inProgress)) {
                    return Transaction.abort();
                }

                currentData.child("goldSettlementInProgress").setValue(true);
                currentData.child("goldSettlementStartedBy").setValue(myUid);
                currentData.child("goldSettlementStartedAt").setValue(getServerNow());

                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(
                    com.google.firebase.database.DatabaseError error,
                    boolean committed,
                    DataSnapshot currentData
            ) {
                if (error != null) {
                    finishSettledRequested = false;
                    Toast.makeText(BattleQuizActivity.this, "골드 정산 요청 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (committed && onClaimed != null) {
                    onClaimed.run();
                }
            }
        });
    }

    private void setOptions(QuizQuestion question) {
        String[] options = question.getOptions();
        Button[] buttons = getOptionButtons();

        for (int i = 0; i < buttons.length; i++) {
            if (options != null && i < options.length) {
                buttons[i].setText((i + 1) + ". " + options[i]);
                buttons[i].setVisibility(View.VISIBLE);
            } else {
                buttons[i].setVisibility(View.GONE);
            }
        }
    }

    private void showMySelectedAnswer(int selectedIndex) {
        Button[] buttons = getOptionButtons();

        for (Button button : buttons) {
            button.setBackgroundColor(DEFAULT_BUTTON_COLOR);
            button.setTextColor(Color.BLACK);
        }

        if (selectedIndex >= 0 && selectedIndex < buttons.length) {
            buttons[selectedIndex].setBackgroundColor(MY_CHOICE_COLOR);
            buttons[selectedIndex].setTextColor(Color.BLACK);
        }
    }

    private void submitAnswer(int selectedIndex) {
        roomRef.get().addOnSuccessListener(room -> {
            String roundState = getStringValue(room, "roundState", "loading");

            if (!"answering".equals(roundState)) {
                return;
            }

            int roundIndex = getIntValue(room, "roundIndex", 0);

            if (roundIndex < 0 || roundIndex >= questions.size()) {
                return;
            }

            long roundStartTime = getLongValue(room, "roundStartTime", getServerNow());
            long roundDurationMs = getLongValue(room, "roundDurationMs", 10_000L);
            long remaining = roundDurationMs - (getServerNow() - roundStartTime);

            if (remaining <= 0) {
                setOptionButtonsEnabled(false);
                return;
            }

            QuizQuestion question = questions.get(roundIndex);
            boolean isCorrect = selectedIndex == question.getCorrectAnswerIndex();

            Map<String, Object> answer = new HashMap<>();
            answer.put("selectedIndex", selectedIndex);
            answer.put("isCorrect", isCorrect);
            answer.put("timeout", false);
            answer.put("answeredAt", getServerNow());

            roomRef.child("answers")
                    .child(String.valueOf(roundIndex))
                    .child(myUid)
                    .setValue(answer)
                    .addOnSuccessListener(unused -> {
                        showMySelectedAnswer(selectedIndex);
                        setOptionButtonsEnabled(true);
                        tvOpponentStatus.setText("나: 선택 완료 / 제한 시간 전까지 변경 가능");
                    });
        });
    }

    private void startLocalTimer(int roundIndex, long roundStartTime, long roundDurationMs) {
        stopTimer();

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long now = getServerNow();
                long elapsed = now - roundStartTime;
                long remaining = roundDurationMs - elapsed;

                if (remaining < 0) {
                    remaining = 0;
                }

                int seconds = (int) Math.ceil(remaining / 1000.0);
                tvTimer.setText(String.valueOf(seconds));

                int progress = 0;

                if (roundDurationMs > 0) {
                    progress = (int) ((remaining * 100L) / roundDurationMs);
                }

                progressTimer.setProgress(Math.max(0, Math.min(100, progress)));

                if (remaining <= 0) {
                    setOptionButtonsEnabled(false);

                    if (isHost) {
                        closeRoundByTransaction(roundIndex);
                    }

                    return;
                }

                handler.postDelayed(this, 200L);
            }
        };

        handler.post(timerRunnable);
    }

    private void stopTimer() {
        if (timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }

    private void closeRoundByTransaction(int targetRoundIndex) {
        roomRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                String status = asString(currentData.child("status").getValue(), "loading");
                String roundState = asString(currentData.child("roundState").getValue(), "loading");

                if ("finished".equals(status) || "cancelled".equals(status)) {
                    return Transaction.success(currentData);
                }

                if (!"answering".equals(roundState)) {
                    return Transaction.success(currentData);
                }

                int roundIndex = asInt(currentData.child("roundIndex").getValue(), 0);

                if (roundIndex != targetRoundIndex) {
                    return Transaction.success(currentData);
                }

                int correctIndex = -999;

                MutableData questionData = currentData.child("questions").child(String.valueOf(roundIndex));
                Object correctObj = questionData.child("correctAnswerIndex").getValue();

                if (correctObj instanceof Number) {
                    correctIndex = ((Number) correctObj).intValue();
                }

                MutableData answersData = currentData.child("answers").child(String.valueOf(roundIndex));

                ensureAnswerExists(answersData, playerA, correctIndex);
                ensureAnswerExists(answersData, playerB, correctIndex);

                boolean aCorrect = asBoolean(answersData.child(playerA).child("isCorrect").getValue(), false);
                boolean bCorrect = asBoolean(answersData.child(playerB).child("isCorrect").getValue(), false);

                int currentScoreA = asInt(currentData.child("scores").child(playerA).getValue(), 0);
                int currentScoreB = asInt(currentData.child("scores").child(playerB).getValue(), 0);

                if (aCorrect) {
                    currentData.child("scores").child(playerA).setValue(currentScoreA + 1);
                }

                if (bCorrect) {
                    currentData.child("scores").child(playerB).setValue(currentScoreB + 1);
                }

                currentData.child("roundState").setValue("revealing");
                currentData.child("revealStartTime").setValue(getServerNow());

                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(
                    com.google.firebase.database.DatabaseError error,
                    boolean committed,
                    DataSnapshot currentData
            ) {
                if (error != null) {
                    Toast.makeText(BattleQuizActivity.this, "라운드 종료 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void ensureAnswerExists(MutableData answersData, String uid, int correctIndex) {
        if (uid == null) {
            return;
        }

        MutableData userAnswer = answersData.child(uid);

        if (userAnswer.getValue() != null) {
            return;
        }

        userAnswer.child("selectedIndex").setValue(-1);
        userAnswer.child("isCorrect").setValue(false);
        userAnswer.child("timeout").setValue(true);
        userAnswer.child("answeredAt").setValue(getServerNow());
    }

    private void advanceToNextRound(int finishedRoundIndex) {
        roomRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                String status = asString(currentData.child("status").getValue(), "loading");
                String roundState = asString(currentData.child("roundState").getValue(), "loading");

                if ("finished".equals(status) || "cancelled".equals(status)) {
                    return Transaction.success(currentData);
                }

                if (!"revealing".equals(roundState)) {
                    return Transaction.success(currentData);
                }

                int currentRoundIndex = asInt(currentData.child("roundIndex").getValue(), 0);

                if (currentRoundIndex != finishedRoundIndex) {
                    return Transaction.success(currentData);
                }

                int questionCount = asInt(currentData.child("questionCount").getValue(), 10);

                if (currentRoundIndex + 1 >= questionCount) {
                    currentData.child("roundState").setValue("finished");
                    currentData.child("status").setValue("finished");
                    currentData.child("finishedAt").setValue(getServerNow());
                } else {
                    currentData.child("roundIndex").setValue(currentRoundIndex + 1);
                    currentData.child("roundState").setValue("answering");
                    currentData.child("roundStartTime").setValue(getServerNow());
                }

                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(
                    com.google.firebase.database.DatabaseError error,
                    boolean committed,
                    DataSnapshot currentData
            ) {
                if (error != null) {
                    Toast.makeText(BattleQuizActivity.this, "다음 문제 이동 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void leaveBattleRoomAndFinish() {
        if (battleExitHandled) {
            return;
        }

        battleExitHandled = true;

        stopTimer();
        setOptionButtonsEnabled(false);

        if (roomRef == null || myUid == null) {
            finish();
            return;
        }

        roomRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                String status = asString(currentData.child("status").getValue(), "loading");
                String roundState = asString(currentData.child("roundState").getValue(), "loading");

                if ("finished".equals(status) || "finished".equals(roundState)) {
                    return Transaction.success(currentData);
                }

                String a = asString(currentData.child("playerA").getValue(), null);
                String b = asString(currentData.child("playerB").getValue(), null);

                String winnerUid = null;

                if (myUid.equals(a)) {
                    winnerUid = b;
                } else if (myUid.equals(b)) {
                    winnerUid = a;
                }

                if (winnerUid == null) {
                    currentData.child("status").setValue("cancelled");
                    currentData.child("roundState").setValue("cancelled");
                    currentData.child("leftUid").setValue(myUid);
                    currentData.child("leftAt").setValue(getServerNow());
                    return Transaction.success(currentData);
                }

                currentData.child("status").setValue("finished");
                currentData.child("roundState").setValue("finished");
                currentData.child("endReason").setValue("forfeit");
                currentData.child("leftUid").setValue(myUid);
                currentData.child("forfeitWinnerUid").setValue(winnerUid);
                currentData.child("leftAt").setValue(getServerNow());
                currentData.child("finishedAt").setValue(getServerNow());

                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(
                    com.google.firebase.database.DatabaseError error,
                    boolean committed,
                    DataSnapshot currentData
            ) {
                if (error != null) {
                    battleExitHandled = false;
                    setOptionButtonsEnabled(true);
                    Toast.makeText(BattleQuizActivity.this, "대전 나가기 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(BattleQuizActivity.this, "대전을 나갔습니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void updateScoreViews(DataSnapshot room) {
        int myScore = getScore(room, myUid);
        int opponentScore = getScore(room, opponentUid);

        tvMyScore.setText("나: " + myScore);
        tvOpponentScore.setText("상대: " + opponentScore);
    }

    private int getScore(DataSnapshot room, String uid) {
        if (uid == null) {
            return 0;
        }

        Long value = room.child("scores").child(uid).getValue(Long.class);
        return value != null ? value.intValue() : 0;
    }

    private boolean hasOpponentAnswer(DataSnapshot room, int roundIndex) {
        if (opponentUid == null) {
            return false;
        }

        return room.child("answers")
                .child(String.valueOf(roundIndex))
                .child(opponentUid)
                .exists();
    }

    private int getSelectedIndex(DataSnapshot room, int roundIndex, String uid) {
        if (uid == null) {
            return -1;
        }

        Long value = room.child("answers")
                .child(String.valueOf(roundIndex))
                .child(uid)
                .child("selectedIndex")
                .getValue(Long.class);

        return value != null ? value.intValue() : -1;
    }

    private void setOptionButtonsEnabled(boolean enabled) {
        btnOption1.setEnabled(enabled);
        btnOption2.setEnabled(enabled);
        btnOption3.setEnabled(enabled);
        btnOption4.setEnabled(enabled);
    }

    private Button[] getOptionButtons() {
        return new Button[]{btnOption1, btnOption2, btnOption3, btnOption4};
    }

    private void resetButtonColors() {
        Button[] buttons = getOptionButtons();

        for (Button button : buttons) {
            button.setBackgroundColor(DEFAULT_BUTTON_COLOR);
            button.setTextColor(Color.BLACK);
        }
    }

    private String getStringValue(DataSnapshot snapshot, String key, String defaultValue) {
        String value = snapshot.child(key).getValue(String.class);
        return value != null ? value : defaultValue;
    }

    private int getIntValue(DataSnapshot snapshot, String key, int defaultValue) {
        Long value = snapshot.child(key).getValue(Long.class);
        return value != null ? value.intValue() : defaultValue;
    }

    private long getLongValue(DataSnapshot snapshot, String key, long defaultValue) {
        Long value = snapshot.child(key).getValue(Long.class);
        return value != null ? value : defaultValue;
    }

    private String asString(Object value, String defaultValue) {
        return value instanceof String ? (String) value : defaultValue;
    }

    private int asInt(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        return defaultValue;
    }

    private boolean asBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        return defaultValue;
    }

    private Boolean asBooleanObject(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopTimer();
        handler.removeCallbacksAndMessages(null);

        if (roomRef != null && roomListener != null) {
            roomRef.removeEventListener(roomListener);
        }

        if (serverTimeOffsetListener != null) {
            FirebaseDatabase.getInstance()
                    .getReference(".info/serverTimeOffset")
                    .removeEventListener(serverTimeOffsetListener);
        }
    }
}