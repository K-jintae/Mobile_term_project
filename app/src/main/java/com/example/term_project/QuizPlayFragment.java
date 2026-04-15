package com.example.term_project;

import android.animation.Animator;
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

    private com.airbnb.lottie.LottieAnimationView lottieEffect;
    private View layoutResult;
    private TextView tvResultStatus;

    private QuizRepository repository;
    private QuizQuestion currentQuestion;
    private int currentSubjectId;
    private int currentQuestionId = 1;

    public QuizPlayFragment() {
    }

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
        lottieEffect = view.findViewById(R.id.lottieEffect);
        layoutResult = view.findViewById(R.id.layoutResult);
        tvResultStatus = view.findViewById(R.id.tvResultStatus);

        repository = new QuizRepository();

        if (getArguments() != null) {
            currentSubjectId = getArguments().getInt("subject_id");
            loadQuestion(currentSubjectId, currentQuestionId);
        }

        btnSubmit.setOnClickListener(v -> checkAnswer());

        return view;
    }

    private void loadQuestion(int subjectId, int questionId) {
        repository.getQuizQuestionFromFirestore(subjectId, questionId, new QuizRepository.OnQuestionFetchedListener() {
            @Override
            public void onSuccess(QuizQuestion question) {
                if (!isAdded()) return;
                currentQuestion = question;
                bindQuestion(question);
                radioGroup.clearCheck();
            }

            @Override
            public void onFailure(Exception e) {
                if (currentQuestionId == 1) {
                    tvQuestion.setText("문제를 찾을 수 없습니다");
                } else {
                    Toast.makeText(getContext(), "모든 문제를 클리어했습니다!", Toast.LENGTH_SHORT).show();
                }
                closeFragment();
            }
        });
    }

    private void bindQuestion(QuizQuestion question) {
        if (question == null) {
            return;
        }
        tvQuestion.setText(question.getQuestion());
        String[] options = question.getOptions();

        option1.setVisibility(View.GONE);
        option2.setVisibility(View.GONE);
        option3.setVisibility(View.GONE);
        option4.setVisibility(View.GONE);

        if (options != null) {
            if (options.length > 0) {
                option1.setText(options[0]);
                option1.setVisibility(View.VISIBLE);
            }
            if (options.length > 1) {
                option2.setText(options[1]);
                option2.setVisibility(View.VISIBLE);
            }
            if (options.length > 2) {
                option3.setText(options[2]);
                option3.setVisibility(View.VISIBLE);
            }
            if (options.length > 3) {
                option4.setText(options[3]);
                option4.setVisibility(View.VISIBLE);
            }
        }
    }
    private void checkAnswer(){
        if(currentQuestion == null){
            return;
        }
        int checkedId = radioGroup.getCheckedRadioButtonId();
        if(checkedId == -1) {
            Toast.makeText(getContext(), "정답을 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        int selectedIndex = -1;
        if(checkedId == R.id.option1){
            selectedIndex = 0;
        }
        else if (checkedId == R.id.option2) {
            selectedIndex = 1;
        }
        else if(checkedId == R.id.option3){
            selectedIndex = 2;
        }
        else if(checkedId == R.id.option4){
            selectedIndex =3 ;
        }

        if(selectedIndex == currentQuestion.getCorrectAnswerIndex()){
            handleResult(true);
        } else{
            handleResult(false);
        }
    }

    private void handleResult(boolean isCorrect){
        btnSubmit.setEnabled(false);
        if(isCorrect){
            int goldToAdd = calculatedGold();
            if(getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).updateUserGold(goldToAdd);
            }
            playEffect(R.raw.success, "정답입니다! \n" + goldToAdd + "G", true);
        }else {
            playEffect(R.raw.fail, "오답입니다 \n 다시 해보세요!", false);
        }
    }

    private void playEffect(int rawResId, String statusText, boolean nextQuestion){
        layoutResult.setVisibility(View.VISIBLE);
        tvResultStatus.setText(statusText);
        lottieEffect.setAnimation(rawResId);
        lottieEffect.playAnimation();

        lottieEffect.addAnimatorListener(new android.animation.Animator.AnimatorListener(){
            @Override
            public void onAnimationEnd(android.animation.Animator animation){
                lottieEffect.removeAllAnimatorListeners();
                layoutResult.postDelayed(() -> {
                    layoutResult.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);

                    if(nextQuestion){
                        currentQuestionId++;
                        loadQuestion(currentSubjectId, currentQuestionId);
                    }
                }, 500);
            }
            @Override public void onAnimationStart(android.animation.Animator animation) {}
            @Override public void onAnimationCancel(android.animation.Animator animation) {}
            @Override public void onAnimationRepeat(android.animation.Animator animation) {}
        });
    }

    private int calculatedGold(){
        String level = currentQuestion.getDifficultyLevel();
        if("hard".equals(level)){
            return 30;
        }
        if("normal".equals(level)){
            return 20;
        }
        return 10;
    }

    private void closeFragment(){
        if(getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).closeCurrentFragment();
        }
    }
}
