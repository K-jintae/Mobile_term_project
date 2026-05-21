package com.example.term_project;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LevelTestActivity extends AppCompatActivity {

    private RadioGroup radioGroupOptions;

    private RadioButton LT_option1;
    private RadioButton LT_option2;
    private RadioButton LT_option3;
    private RadioButton LT_option4;

    private Button LT_btnSubmit;

    private TextView tvQuizQuestion;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private QuizRepository repository;

    private QuizQuestion currentQuiz;

    private List<QuizQuestion> testQuestions = new ArrayList<>();

    private int currentQuestionIndex = 0;

    private int testCorrectCount = 0;

    private final int TEST_TOTAL_COUNT = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_level_test);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        repository = new QuizRepository();

        tvQuizQuestion = findViewById(R.id.tvQuizQuestion);

        radioGroupOptions = findViewById(R.id.radioGroupOptions);

        LT_option1 = findViewById(R.id.LT_option1);
        LT_option2 = findViewById(R.id.LT_option2);
        LT_option3 = findViewById(R.id.LT_option3);
        LT_option4 = findViewById(R.id.LT_option4);

        LT_btnSubmit = findViewById(R.id.LT_btnSubmit);

        fetchQuestionsAndStartTest(1);

        LT_btnSubmit.setOnClickListener(v -> checkLevelTestAnswer());

        radioGroupOptions.setOnCheckedChangeListener(
                (group, checkedId) -> updateOptionBackgrounds()
        );
    }

    /*
     * 레벨 테스트용 문제 불러오기
     *
     * Easy / Normal / Hard 문제를 모두 가져온 뒤
     * quizId 기준으로 중복 제거 후
     * Knapsack 알고리즘 적용
     */
    private void fetchQuestionsAndStartTest(int subjectId) {

        /*
         * key   = quizId
         * value = QuizQuestion
         * 같은 quizId가 들어오면 자동으로 덮어쓰기되므로
         * 중복 문제 제거 가능
         */
        Map<Integer, QuizQuestion> uniqueQuestions = new HashMap<>();

        repository.getQuizQuestionsFromFirestore(
                subjectId,
                "easy",
                100,
                new QuizRepository.OnQuestionsFetchedListener() {

                    @Override
                    public void onSuccess(List<QuizQuestion> easyQuestions) {

                        for (QuizQuestion q : easyQuestions) {
                            uniqueQuestions.put(q.getQuizId(), q);
                        }

                        repository.getQuizQuestionsFromFirestore(
                                subjectId,
                                "normal",
                                100,
                                new QuizRepository.OnQuestionsFetchedListener() {

                                    @Override
                                    public void onSuccess(List<QuizQuestion> normalQuestions) {

                                        for (QuizQuestion q : normalQuestions) {
                                            uniqueQuestions.put(q.getQuizId(), q);
                                        }

                                        repository.getQuizQuestionsFromFirestore(
                                                subjectId,
                                                "hard",
                                                100,
                                                new QuizRepository.OnQuestionsFetchedListener() {

                                                    @Override
                                                    public void onSuccess(List<QuizQuestion> hardQuestions) {

                                                        for (QuizQuestion q : hardQuestions) {
                                                            uniqueQuestions.put(q.getQuizId(), q);
                                                        }

                                                        /*
                                                         * Map → List 변환
                                                         */
                                                        List<QuizQuestion> mergedQuestions =
                                                                new ArrayList<>(
                                                                        uniqueQuestions.values()
                                                                );

                                                        Log.d(
                                                                "LevelTest",
                                                                "중복 제거 후 문제 수 = "
                                                                        + mergedQuestions.size()
                                                        );

                                                        for (QuizQuestion q : mergedQuestions) {

                                                            Log.d(
                                                                    "LevelTest",
                                                                    "문제: "
                                                                            + q.getQuestion()
                                                                            + " / 난이도: "
                                                                            + q.getDifficultyLevel()
                                                            );
                                                        }

                                                        /*
                                                         * 현재 목표(DB에 문제 추가 후에 수정 해야함)‼️‼️‼️:
                                                         * Easy 5개 (50점)
                                                         * Hard 2개 (60점)
                                                         * 총 110점
                                                         */
                                                        testQuestions =
                                                                KnapsackQuizSelector.selectQuestions(
                                                                        mergedQuestions,
                                                                        110,
                                                                        5,
                                                                        0,
                                                                        2,
                                                                        TEST_TOTAL_COUNT
                                                                );

                                                        Log.d(
                                                                "LevelTest",
                                                                "최종 선택 문제 수 = "
                                                                        + testQuestions.size()
                                                        );

                                                        if (testQuestions.size()
                                                                < TEST_TOTAL_COUNT) {

                                                            showErrorDialogAndGoBack(
                                                                    "조건에 맞는 문제가 부족합니다."
                                                            );

                                                            return;
                                                        }

                                                        currentQuestionIndex = 0;

                                                        showQuestion(currentQuestionIndex);
                                                    }

                                                    @Override
                                                    public void onFailure(Exception e) {

                                                        showErrorDialogAndGoBack(
                                                                "Hard 문제를 불러오지 못했습니다.\n"
                                                                        + e.getMessage()
                                                        );
                                                    }
                                                }
                                        );
                                    }

                                    @Override
                                    public void onFailure(Exception e) {

                                        showErrorDialogAndGoBack(
                                                "Normal 문제를 불러오지 못했습니다.\n"
                                                        + e.getMessage()
                                        );
                                    }
                                }
                        );
                    }

                    @Override
                    public void onFailure(Exception e) {

                        showErrorDialogAndGoBack(
                                "Easy 문제를 불러오지 못했습니다.\n"
                                        + e.getMessage()
                        );
                    }
                }
        );
    }

    private void showErrorDialogAndGoBack(String message) {

        new AlertDialog.Builder(this)
                .setTitle("안내")
                .setMessage(message)
                .setPositiveButton(
                        "확인",
                        (dialog, which) -> finish()
                )
                .setCancelable(false)
                .show();
    }

    private void showQuestion(int index) {

        currentQuiz = testQuestions.get(index);

        tvQuizQuestion.setText(
                "Q" + (index + 1) + ". " + currentQuiz.getQuestion()
        );

        String[] options = currentQuiz.getOptions();

        LT_option1.setVisibility(View.GONE);
        LT_option2.setVisibility(View.GONE);
        LT_option3.setVisibility(View.GONE);
        LT_option4.setVisibility(View.GONE);

        if (options != null) {

            if (options.length > 0) {

                LT_option1.setText(options[0]);
                LT_option1.setVisibility(View.VISIBLE);
            }

            if (options.length > 1) {

                LT_option2.setText(options[1]);
                LT_option2.setVisibility(View.VISIBLE);
            }

            if (options.length > 2) {

                LT_option3.setText(options[2]);
                LT_option3.setVisibility(View.VISIBLE);
            }

            if (options.length > 3) {

                LT_option4.setText(options[3]);
                LT_option4.setVisibility(View.VISIBLE);
            }
        }

        radioGroupOptions.clearCheck();

        updateOptionBackgrounds();
    }

    private void checkLevelTestAnswer() {

        if (currentQuiz == null) {
            return;
        }

        int checkedId = radioGroupOptions.getCheckedRadioButtonId();

        if (checkedId == -1) {

            Toast.makeText(
                    this,
                    "정답을 선택해주세요.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        int selectedIndex;

        if (checkedId == R.id.LT_option1) {

            selectedIndex = 0;

        } else if (checkedId == R.id.LT_option2) {

            selectedIndex = 1;

        } else if (checkedId == R.id.LT_option3) {

            selectedIndex = 2;

        } else {

            selectedIndex = 3;
        }

        if (selectedIndex == currentQuiz.getCorrectAnswerIndex()) {

            testCorrectCount++;
        }

        currentQuestionIndex++;

        if (currentQuestionIndex >= TEST_TOTAL_COUNT) {

            finishLevelTest(
                    TEST_TOTAL_COUNT,
                    testCorrectCount
            );

        } else {

            showQuestion(currentQuestionIndex);
        }
    }

    private void finishLevelTest(int totalSolved, int correct) {

        String userLevel;

        if (correct >= 6) {

            userLevel = "고수";

        } else if (correct >= 5) {

            userLevel = "중수";

        } else {

            userLevel = "하수";
        }

        String alertMessage;

        if (correct >= 6) {

            alertMessage = "축하해요!! 당신은 고수의 실력을 가졌군요!";

        } else if (correct >= 5) {

            alertMessage = "오호라... 중수 레벨이라니...";

        } else {

            alertMessage = "테스트 완료!! 같이 차차 공부해봐요!!";
        }

        int setKnapsackCapacityScore;

        if (userLevel.equals("고수")) {

            setKnapsackCapacityScore = 350;

        } else if (userLevel.equals("중수")) {

            setKnapsackCapacityScore = 250;

        } else {

            setKnapsackCapacityScore = 150;
        }

        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> updates = new HashMap<>();

        updates.put("isTested", true);

        updates.put(
                "knapsack_capacity_score",
                setKnapsackCapacityScore
        );

        db.collection("users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {

                    AlertDialog dialog =
                            new AlertDialog.Builder(this)
                                    .setTitle("레벨 테스트 결과")
                                    .setMessage(alertMessage)
                                    .setPositiveButton(
                                            "확인",
                                            (d, which) -> moveToMain()
                                    )
                                    .create();

                    dialog.setCancelable(false);

                    dialog.show();
                });
    }

    private void moveToMain() {

        startActivity(
                new Intent(this, MainActivity.class)
        );

        finish();
    }

    private void updateOptionBackgrounds() {

        LT_option1.setBackgroundResource(
                LT_option1.isChecked()
                        ? R.drawable.bg_quiz_option_selected
                        : R.drawable.bg_option_default
        );

        LT_option2.setBackgroundResource(
                LT_option2.isChecked()
                        ? R.drawable.bg_quiz_option_selected
                        : R.drawable.bg_option_default
        );

        LT_option3.setBackgroundResource(
                LT_option3.isChecked()
                        ? R.drawable.bg_quiz_option_selected
                        : R.drawable.bg_option_default
        );

        LT_option4.setBackgroundResource(
                LT_option4.isChecked()
                        ? R.drawable.bg_quiz_option_selected
                        : R.drawable.bg_option_default
        );
    }
}