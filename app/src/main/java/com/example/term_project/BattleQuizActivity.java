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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

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
    private boolean battleExitHandled = false;
    private Button btnOption1;
    private Button btnOption2;
    private Button btnOption3;
    private Button btnOption4;
    private Button btnExitBattle;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private FirebaseAuth auth;
    private DatabaseReference roomRef;

    private String battleId;
    private String myUid;
    private String playerA;
    private String playerB;
    private String hostUid;
    private String opponentUid;

    private boolean isHost = false;
    private boolean alreadyAnsweredThisRound = false;
    private boolean questionLoadRequested = false;
    private boolean finishSettledRequested = false;

    private int lastRenderedRound = -1;
    private int lastScheduledRevealRound = -1;

    private List<QuizQuestion> questions = new ArrayList<>();

    private Runnable timerRunnable;

    private static final int DEFAULT_BUTTON_COLOR = Color.parseColor("#F5EBE0");
    private static final int CORRECT_COLOR = Color.parseColor("#A5D6A7");
    private static final int MY_CHOICE_COLOR = Color.parseColor("#90CAF9");
    private static final int OPPONENT_CHOICE_COLOR = Color.parseColor("#FFCC80");

    // 대전용: 과목과 난이도를 섞어서 출제
    // 실제 DB 과목 번호가 1~7이 아니면 여기만 수정
    private static final int[] BATTLE_SUBJECT_IDS = {1, 2, 3, 4, 5, 6, 7};

    // 기존 LevelTestActivity에서 easy / normal / hard를 사용하고 있으므로 이 기준 사용
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

        btnExitBattle.setOnClickListener(v -> {
            leaveBattleRoomAndFinish();
        });
    }

    private void leaveBattleRoomAndFinish() {
        if (battleExitHandled) {
            return;
        }

        battleExitHandled = true;

        if (roomRef == null || myUid == null) {
            finish();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "cancelled");
        updates.put("roundState", "cancelled");
        updates.put("leftUid", myUid);
        updates.put("leftAt", System.currentTimeMillis());

        roomRef.updateChildren(updates)
                .addOnCompleteListener(task -> {
                    Toast.makeText(this, "대전을 나갔습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void listenBattleRoom() {
        roomRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
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
        });
    }

    private void renderRoom(DataSnapshot room) {
        playerA = room.child("playerA").getValue(String.class);
        playerB = room.child("playerB").getValue(String.class);
        hostUid = room.child("hostUid").getValue(String.class);

        String status = getStringValue(room, "status", "loading");
        String roundState = getStringValue(room, "roundState", "loading");

        // 방 기본 정보가 아직 준비되지 않은 경우
        if (playerA == null || playerB == null || hostUid == null) {
            showLoadingState("대전방 정보를 불러오는 중입니다.");
            return;
        }

        // 상대방 또는 내가 나가서 방이 취소된 경우
        if ("cancelled".equals(status) || "cancelled".equals(roundState)) {
            String leftUid = room.child("leftUid").getValue(String.class);

            stopTimer();
            setOptionButtonsEnabled(false);

            if (leftUid != null && !leftUid.equals(myUid)) {
                Toast.makeText(this, "상대방이 대전에서 나갔습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "대전이 종료되었습니다.", Toast.LENGTH_SHORT).show();
            }

            finish();
            return;
        }

        isHost = myUid != null && myUid.equals(hostUid);
        opponentUid = myUid != null && myUid.equals(playerA) ? playerB : playerA;

        int questionCount = getIntValue(room, "questionCount", 10);

        // 호스트만 문제를 불러와서 방에 저장
        if (isHost
                && ("loading".equals(status) || "loading".equals(roundState))
                && !questionLoadRequested) {

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
            alreadyAnsweredThisRound = hasMyAnswer(room, roundIndex);
            lastRenderedRound = roundIndex;
        } else {
            alreadyAnsweredThisRound = hasMyAnswer(room, roundIndex);
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

        } else if ("finished".equals(roundState) || "finished".equals(status)) {
            renderFinished(room);

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
                                // 일부 과목/난이도 조회 실패는 무시하고 나머지 문제로 진행
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
        updates.put("roundStartTime", System.currentTimeMillis());

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

        setOptionButtonsEnabled(myChoice < 0);

        long roundStartTime = getLongValue(room, "roundStartTime", System.currentTimeMillis());
        long roundDurationMs = getLongValue(room, "roundDurationMs", 10_000L);

        startLocalTimer(room, roundIndex, roundStartTime, roundDurationMs);

        if (isHost && bothAnswered(room, roundIndex)) {
            closeRoundByTransaction(roundIndex);
        }
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

        tvBattleTitle.setText("대전 종료");
        tvTimer.setText("끝");
        progressTimer.setProgress(100);
        tvQuestionCount.setText("최종 결과");

        String result;

        if (myScore > opponentScore) {
            result = "승리";
        } else if (myScore < opponentScore) {
            result = "패배";
        } else {
            result = "무승부";
        }

        tvQuestion.setText(
                "결과: " + result + "\n\n" +
                        "내 점수: " + myScore + "\n" +
                        "상대 점수: " + opponentScore + "\n" +
                        "베팅 골드: " + betGold
        );

        tvOpponentStatus.setText("골드 정산은 승패 기준으로 처리됩니다.");

        Boolean goldSettled = room.child("goldSettled").getValue(Boolean.class);

        if (isHost && !Boolean.TRUE.equals(goldSettled) && !finishSettledRequested) {
            finishSettledRequested = true;

            GoldManager.settleBattleGold(playerA, playerB, betGold, scoreA, scoreB, new GoldManager.GoldCallback() {
                @Override
                public void onSuccess() {
                    roomRef.child("goldSettled").setValue(true);
                }

                @Override
                public void onFailure(String message) {
                    Toast.makeText(BattleQuizActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
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

    @Override
    public void onBackPressed() {
        leaveBattleRoomAndFinish();
    }

    private void submitAnswer(int selectedIndex) {
        if (alreadyAnsweredThisRound) {
            return;
        }

        roomRef.get().addOnSuccessListener(room -> {
            String roundState = getStringValue(room, "roundState", "loading");

            if (!"answering".equals(roundState)) {
                return;
            }

            int roundIndex = getIntValue(room, "roundIndex", 0);

            if (roundIndex < 0 || roundIndex >= questions.size()) {
                return;
            }

            if (hasMyAnswer(room, roundIndex)) {
                alreadyAnsweredThisRound = true;
                setOptionButtonsEnabled(false);

                int myChoice = getSelectedIndex(room, roundIndex, myUid);
                if (myChoice >= 0) {
                    showMySelectedAnswer(myChoice);
                }

                return;
            }

            QuizQuestion question = questions.get(roundIndex);
            boolean isCorrect = selectedIndex == question.getCorrectAnswerIndex();

            Map<String, Object> answer = new HashMap<>();
            answer.put("selectedIndex", selectedIndex);
            answer.put("isCorrect", isCorrect);
            answer.put("timeout", false);
            answer.put("answeredAt", System.currentTimeMillis());

            roomRef.child("answers")
                    .child(String.valueOf(roundIndex))
                    .child(myUid)
                    .setValue(answer)
                    .addOnSuccessListener(unused -> {
                        alreadyAnsweredThisRound = true;
                        showMySelectedAnswer(selectedIndex);
                        setOptionButtonsEnabled(false);
                        tvOpponentStatus.setText("나: 선택 완료 / 상대 선택 대기 중");
                    });
        });
    }

    private void startLocalTimer(DataSnapshot room, int roundIndex, long roundStartTime, long roundDurationMs) {
        stopTimer();

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long elapsed = now - roundStartTime;
                long remaining = roundDurationMs - elapsed;

                if (remaining < 0) {
                    remaining = 0;
                }

                int seconds = (int) Math.ceil(remaining / 1000.0);
                tvTimer.setText(String.valueOf(seconds));

                int progress = (int) ((remaining * 100L) / roundDurationMs);
                progressTimer.setProgress(Math.max(0, Math.min(100, progress)));

                if (remaining <= 0) {
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
                String roundState = asString(currentData.child("roundState").getValue(), "loading");

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
                currentData.child("revealStartTime").setValue(System.currentTimeMillis());

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
        userAnswer.child("answeredAt").setValue(System.currentTimeMillis());
    }

    private void advanceToNextRound(int finishedRoundIndex) {
        roomRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                String roundState = asString(currentData.child("roundState").getValue(), "loading");

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
                    currentData.child("finishedAt").setValue(System.currentTimeMillis());
                } else {
                    currentData.child("roundIndex").setValue(currentRoundIndex + 1);
                    currentData.child("roundState").setValue("answering");
                    currentData.child("roundStartTime").setValue(System.currentTimeMillis());
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

    private boolean hasMyAnswer(DataSnapshot room, int roundIndex) {
        return room.child("answers")
                .child(String.valueOf(roundIndex))
                .child(myUid)
                .exists();
    }

    private boolean hasOpponentAnswer(DataSnapshot room, int roundIndex) {
        return room.child("answers")
                .child(String.valueOf(roundIndex))
                .child(opponentUid)
                .exists();
    }

    private boolean bothAnswered(DataSnapshot room, int roundIndex) {
        boolean aAnswered = room.child("answers")
                .child(String.valueOf(roundIndex))
                .child(playerA)
                .exists();

        boolean bAnswered = room.child("answers")
                .child(String.valueOf(roundIndex))
                .child(playerB)
                .exists();

        return aAnswered && bAnswered;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        handler.removeCallbacksAndMessages(null);
    }
}