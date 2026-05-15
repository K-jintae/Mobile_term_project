package com.example.term_project;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LevelTestActivity extends AppCompatActivity {

    private RadioGroup radioGroupOptions;
    private RadioButton LT_option1, LT_option2, LT_option3, LT_option4;
    private Button LT_btnSubmit;
    private TextView tvQuizQuestion;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private QuizRepository repository;
    private QuizQuestion currentQuiz;

    private int testCorrectCount = 0;
    private int testCurrentIndex = 1;
    private final int TEST_TOTAL_COUNT = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState){
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

        loadRandomQuiz(0, testCurrentIndex);

        LT_btnSubmit.setOnClickListener(v -> checkLevelTestAnswer());
        radioGroupOptions.setOnCheckedChangeListener((group, checkedId) -> updateOptionBackgrounds());
    }

    private void loadRandomQuiz(int subjectId, int questionId){
        repository.getQuizQuestionFromFirestore(0, questionId, new QuizRepository.OnQuestionFetchedListener() {
            @Override
            public void onSuccess(QuizQuestion question) {
                currentQuiz = question;
                tvQuizQuestion.setText("Q." + question.getQuestion());
                String[] options = question.getOptions();
                LT_option1.setText(options[0]);
                LT_option2.setText(options[1]);
                LT_option3.setText(options[2]);
                LT_option4.setText(options[3]);
                radioGroupOptions.clearCheck();
                updateOptionBackgrounds();
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(LevelTestActivity.this, "문제를 불러오지 못했습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkLevelTestAnswer(){
        if(currentQuiz == null){
            return;
        }
        int checkedId = radioGroupOptions.getCheckedRadioButtonId();
        if(checkedId == -1){
            Toast.makeText(this, "정답을 골라주세요!", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedIndex = (checkedId == R.id.LT_option1) ? 0 : (checkedId == R.id.LT_option2) ? 1 : (checkedId == R.id.LT_option3) ? 2 : 3;

        if(selectedIndex == currentQuiz.getCorrectAnswerIndex()){
            testCorrectCount++;
            testCurrentIndex++;
            loadRandomQuiz(0, testCurrentIndex);
        } else {
            finishLevelTest(TEST_TOTAL_COUNT, testCorrectCount);
        }
    }

    private void finishLevelTest(int totalSolved, int correct){
        String userlevel = (correct >= 5) ? "고수" : (correct >= 3) ? "중수" : "하수";;
        String alertMessage = (correct >= 5) ? "축하해요!! 당신은 고수의 실력을 가졌군요!" : (correct >= 3) ? "오호라...중수 레벨이라니..." : "테스트 완료!! 같이 차차 공부해봐요!!";

        String uid = mAuth.getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("isTested", true);
        updates.put("level", userlevel);
        updates.put("score", correct);

        db.collection("users").document(uid).update(updates).addOnSuccessListener(aVoid -> {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("레벨테스트 결과!")
                    .setMessage(alertMessage)
                    .setPositiveButton("확인", (d, which) -> moveToMain())
                    .create();
            dialog.setCancelable(true);
            dialog.setOnCancelListener(d -> moveToMain());
            dialog.show();
        });
    }

    private void moveToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
    private void updateOptionBackgrounds(){

    }
}
