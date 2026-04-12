package com.example.term_project;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class QuizPlayFragment extends Fragment {

    private TextView tvQuestion;
    private RadioGroup radioGroup;
    private RadioButton option1, option2, option3, option4;
    private Button btnSubmit;

    private QuizRepository repository;
    private QuizQuestion currentQuestion;

    // 현재 과목과 문제 순서를 기억하는 변수
    private int currentSubjectId;
    private int currentQuestionId = 1; // 1번부터 (나중에 랜덤으로)

    public QuizPlayFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quiz_play, container, false);

        tvQuestion = view.findViewById(R.id.tvQuestion);
        radioGroup = view.findViewById(R.id.radioGroupOptions);
        option1 = view.findViewById(R.id.option1);
        option2 = view.findViewById(R.id.option2);
        option3 = view.findViewById(R.id.option3);
        option4 = view.findViewById(R.id.option4);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        repository = new QuizRepository();

        if (getArguments() != null) {
            // LeftFragment에서 넘겨준 과목 번호(subject_id) 받기
            currentSubjectId = getArguments().getInt("subject_id");
            tvQuestion.setText("문제를 불러오는 중입니다...\n잠시만 기다려주세요.");
            // 문제부터 시작
            loadQuestion(currentSubjectId, currentQuestionId);
        }

        btnSubmit.setOnClickListener(v -> checkAnswer());
        return view;
    }

    // (과목, 문제) 서버에서 불러오기
    private void loadQuestion(int subjectId, int questionId) {
        repository.getQuizQuestionFromFirestore(subjectId, questionId, new QuizRepository.OnQuestionFetchedListener() {
            @Override
            public void onSuccess(QuizQuestion question) {
                currentQuestion = question;
                bindQuestion(question);
                radioGroup.clearCheck(); // 다음 문제를 위해 체크 해제
            }

            @Override
            public void onFailure(Exception e) {
                if (currentQuestionId == 1) {
                    tvQuestion.setText("🚨 문제를 찾을 수 없습니다.\n\n원인: " + e.getMessage());
                } else {
                    // 문제를 풀다가 실패한 거라면 '클리어'로 처리
                    Toast.makeText(getContext(), "과목을 모두 클리어했습니다! 🎉", Toast.LENGTH_SHORT).show();
                }
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).closeCurrentFragment();
                }
            }
        });
    }

    private void bindQuestion(QuizQuestion question) {
        if (question == null) return;
        tvQuestion.setText(question.getQuestion());
        String[] options = question.getOptions();

        // 꼬이지 않게 일단 보기를 다 화면에서 치웁니다.
        option1.setVisibility(View.GONE);
        option2.setVisibility(View.GONE);
        option3.setVisibility(View.GONE);
        option4.setVisibility(View.GONE);

        // 파이어베이스에서 가져온 보기 개수만큼만 글자를 채우고 다시 보여줍니다.
        if (options != null) {
            if (options.length > 0) { option1.setText(options[0]); option1.setVisibility(View.VISIBLE); }
            if (options.length > 1) { option2.setText(options[1]); option2.setVisibility(View.VISIBLE); }
            if (options.length > 2) { option3.setText(options[2]); option3.setVisibility(View.VISIBLE); }
            if (options.length > 3) { option4.setText(options[3]); option4.setVisibility(View.VISIBLE); }
        }
    }

    private void checkAnswer() {
        if (currentQuestion == null) return;

        int checkedId = radioGroup.getCheckedRadioButtonId();
        if (checkedId == -1) {
            Toast.makeText(getContext(), "정답을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedIndex = -1;
        if (checkedId == R.id.option1) selectedIndex = 0;
        else if (checkedId == R.id.option2) selectedIndex = 1;
        else if (checkedId == R.id.option3) selectedIndex = 2;
        else if (checkedId == R.id.option4) selectedIndex = 3;

        if (selectedIndex == currentQuestion.getCorrectAnswerIndex()) {

            // 난이도별 골드 계산
            int goldToAdd = 10; // easy 기본값
            String level = currentQuestion.getDifficultyLevel();
            if ("normal".equals(level)) goldToAdd = 20;
            else if ("hard".equals(level)) goldToAdd = 30;

            Toast.makeText(getContext(), "정답입니다! +" + goldToAdd + " 골드", Toast.LENGTH_SHORT).show();

            // db에 골드 추가
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).updateUserGold(goldToAdd);
            }

            // 다음 문제 불러오기
            currentQuestionId++;
            loadQuestion(currentSubjectId, currentQuestionId);

        } else {
            Toast.makeText(getContext(), "오답입니다. 다시 시도해보세요!", Toast.LENGTH_SHORT).show();
        }
    }
}