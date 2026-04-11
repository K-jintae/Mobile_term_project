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

// 실제 문제를 푸는 Fragment
public class QuizPlayFragment extends Fragment {

    //정답/오답을 보여줄 변수
    private com.airbnb.lottie.LottieAnimationView lottieEffect; //lottie effect를 사용하기 위해 데려옴
    private View layoutResult; //lottie effect 자체가 원래 화면에 레이어 쌓는 것
    private TextView tvResultStatus;
    // 문제 문장을 보여줄 TextView
    private TextView tvQuestion;

    // 보기들을 묶는 RadioGroup
    private RadioGroup radioGroup;

    // 보기 4개
    private RadioButton option1, option2, option3, option4;

    // 제출 버튼
    private Button btnSubmit;

    // 문제 데이터를 가져오기 위한 Repository
    private QuizRepository repository;

    // 현재 화면에 표시 중인 문제 객체
    private QuizQuestion currentQuestion;

    public QuizPlayFragment() {
        // 기본 생성자
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // fragment_quiz_play.xml 연결
        View view = inflater.inflate(R.layout.fragment_quiz_play, container, false);

        // 화면 요소들 연결
        tvQuestion = view.findViewById(R.id.tvQuestion);
        radioGroup = view.findViewById(R.id.radioGroupOptions);
        option1 = view.findViewById(R.id.option1);
        option2 = view.findViewById(R.id.option2);
        option3 = view.findViewById(R.id.option3);
        option4 = view.findViewById(R.id.option4);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        lottieEffect = view.findViewById(R.id.lottieEffect);
        layoutResult = view.findViewById(R.id.layoutResult);
        tvResultStatus = view.findViewById(R.id.tvResultStatus);



        // Repository 생성
        repository = new QuizRepository();

        // 이전 화면(QuizSelectFragment)에서 넘겨준 quiz_id 받기
        if (getArguments() != null) {
            int quizId = getArguments().getInt("quiz_id");

            // quiz_id에 해당하는 문제 가져오기
            currentQuestion = repository.getQuestionByQuizId(quizId);

            // 가져온 문제를 화면에 표시
            bindQuestion(currentQuestion);
        }

        // 제출 버튼 클릭 시 정답 체크
        btnSubmit.setOnClickListener(v -> checkAnswer());

        return view;
    }

    // lottie files에서 받아온 이펙트 사용
    private void playEffect(int rawResId, String statusText){
        layoutResult.setVisibility(View.VISIBLE);
        tvResultStatus.setText(statusText);

        lottieEffect.setAnimation(rawResId);
        lottieEffect.playAnimation();

        lottieEffect.addAnimatorListener(new android.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation){
                layoutResult.postDelayed(() -> layoutResult.setVisibility(View.GONE), 500);
            }
            @Override public void onAnimationStart(android.animation.Animator animation) {}
            @Override public void onAnimationCancel(android.animation.Animator animation){}
            @Override public void onAnimationRepeat(android.animation.Animator animation){}
        });
    }

    // 문제 객체의 내용을 화면에 표시하는 함수
    private void bindQuestion(QuizQuestion question) {

        // 문제 문장 표시
        tvQuestion.setText(question.getQuestion());

        // 보기 배열 가져오기
        String[] options = question.getOptions();

        // 4개의 보기 버튼에 텍스트 설정
        option1.setText(options[0]);
        option2.setText(options[1]);
        option3.setText(options[2]);
        option4.setText(options[3]);
    }

    // 사용자가 선택한 답이 정답인지 확인하는 함수
    private void checkAnswer() {

        // 현재 선택된 RadioButton의 id 가져오기
        int checkedId = radioGroup.getCheckedRadioButtonId();

        // 아무것도 선택하지 않았으면 안내 메시지 출력 후 종료
        if (checkedId == -1) {
            Toast.makeText(getContext(), "정답을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 사용자가 고른 보기 번호를 0~3 인덱스로 변환
        int selectedIndex = -1;

        if (checkedId == R.id.option1) {
            selectedIndex = 0;
        } else if (checkedId == R.id.option2) {
            selectedIndex = 1;
        } else if (checkedId == R.id.option3) {
            selectedIndex = 2;
        } else if (checkedId == R.id.option4) {
            selectedIndex = 3;
        }

        // 사용자가 고른 답과 실제 정답 비교
        if (selectedIndex == currentQuestion.getCorrectAnswerIndex()) {
            playEffect(R.raw.success, "정답입니다!");

            // 나중에 여기에 코인 지급, 문제 해금, 다음 문제 이동 같은 기능 추가 가능
            // 예: addCoin(10);
            // 예: unlockNextQuiz();

        } else {
            playEffect(R.raw.fail, "오답입니다");
        }
    }
}
