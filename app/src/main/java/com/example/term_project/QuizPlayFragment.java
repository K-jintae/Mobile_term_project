package com.example.term_project;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class QuizPlayFragment extends Fragment {

    private static final int MAX_QUESTION_COUNT = 10;
    private static final double CLEAR_RATE_BY_TARGET_SCORE = 0.8;

    private TextView tvQuestion;
    private RadioGroup radioGroup;
    private RadioButton option1;
    private RadioButton option2;
    private RadioButton option3;
    private RadioButton option4;
    private Button btnSubmit;

    private com.airbnb.lottie.LottieAnimationView lottieEffect;
    private View layoutResult;
    private TextView tvResultStatus;

    private QuizRepository repository;

    private List<QuizQuestion> questionList = new ArrayList<>();
    private QuizQuestion currentQuestion;

    private int currentSubjectId = 1;
    private int currentQuestionIndex = 0;
    private String currentDifficultyLevel = "easy";

    private int totalSolvedCount = 0;
    private int correctCount = 0;

    // 실제 정답으로 얻은 점수
    private int earnedScore = 0;

    // 출제된 문제 전체를 다 맞혔을 때의 총점
    private int targetScore = 0;

    // 골드는 기존처럼 정답 점수와 동일하게 지급
    private int earnedGold = 0;

    private CharacterViewModel characterViewModel;
    private ImageView quizFaceImage;
    private ImageView quizHatImage;
    private ImageView quizClothesImage;

    public QuizPlayFragment() {
        // 기본 생성자
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

        quizClothesImage = view.findViewById(R.id.quizClothesImage);
        quizFaceImage = view.findViewById(R.id.quizFaceImage);
        quizHatImage = view.findViewById(R.id.quizHatImage);

        repository = new QuizRepository();

        setupCharacterImages();
        applyPressAnimation(btnSubmit);

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> updateOptionBackgrounds());

        if (getArguments() != null) {
            currentSubjectId = getArguments().getInt("subject_id", 1);
            currentDifficultyLevel = getArguments().getString("difficulty_level", "easy");
        }

        loadQuestionList();

        btnSubmit.setOnClickListener(v -> checkAnswer());

        return view;
    }

    private void setupCharacterImages() {
        characterViewModel = new ViewModelProvider(requireActivity())
                .get(CharacterViewModel.class);

        characterViewModel.getFace().observe(getViewLifecycleOwner(), resId -> {
            if (resId != null && resId != 0) {
                quizFaceImage.setImageResource(resId);
            } else {
                quizFaceImage.setImageResource(R.drawable.face_default);
            }
        });

        characterViewModel.getHat().observe(getViewLifecycleOwner(), resId -> {
            if (resId != null && resId != 0) {
                quizHatImage.setImageResource(resId);
            } else {
                quizHatImage.setImageDrawable(null);
            }
        });

        characterViewModel.getClothes().observe(getViewLifecycleOwner(), resId -> {
            if (resId != null && resId != 0) {
                quizClothesImage.setImageResource(resId);
            } else {
                quizClothesImage.setImageDrawable(null);
            }
        });
    }

    /*
     * 기존 방식:
     * quiz_id를 1,2,3... 순서대로 하나씩 가져옴.
     *
     * 변경 방식:
     * Firebase에서 해당 과목 문제 목록을 한 번에 가져온 뒤
     * 최대 10문제를 출제한다.
     */
    private void loadQuestionList() {
        btnSubmit.setEnabled(false);
        tvQuestion.setText("문제를 불러오는 중입니다...");

        repository.getQuizQuestionsFromFirestore(
                currentSubjectId,
                currentDifficultyLevel,
                MAX_QUESTION_COUNT,
                new QuizRepository.OnQuestionsFetchedListener() {
                    @Override
                    public void onSuccess(List<QuizQuestion> questions) {
                        if (!isAdded()) {
                            return;
                        }

                        questionList.clear();
                        questionList.addAll(questions);

                        currentQuestionIndex = 0;
                        totalSolvedCount = 0;
                        correctCount = 0;
                        earnedScore = 0;
                        earnedGold = 0;

                        targetScore = calculateTargetScore(questionList);

                        if (questionList.isEmpty()) {
                            showNoQuestionMessage("출제 가능한 문제가 없습니다.");
                            return;
                        }

                        bindCurrentQuestion();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (!isAdded()) {
                            return;
                        }

                        showNoQuestionMessage(e.getMessage());
                    }
                }
        );
    }

    private void showNoQuestionMessage(String message) {
        tvQuestion.setText(
                "출제 가능한 문제가 없습니다.\n"
                        + "과목 번호: " + currentSubjectId + "\n"
                        + "난이도: " + getDifficultyKoreanName(currentDifficultyLevel) + "\n\n"
                        + message
        );
        btnSubmit.setEnabled(false);
    }

    private int calculateTargetScore(List<QuizQuestion> questions) {
        int sum = 0;

        for (QuizQuestion question : questions) {
            sum += getQuestionScore(question);
        }

        return sum;
    }

    private void bindCurrentQuestion() {
        if (currentQuestionIndex >= questionList.size()) {
            completeStageIfClear();
            showFinalResult();
            return;
        }

        currentQuestion = questionList.get(currentQuestionIndex);

        bindQuestion(currentQuestion);

        radioGroup.clearCheck();
        updateOptionBackgrounds();

        btnSubmit.setEnabled(true);
    }

    private void bindQuestion(QuizQuestion question) {
        if (question == null) {
            return;
        }

        String progressText = "문제 "
                + (currentQuestionIndex + 1)
                + " / "
                + questionList.size()
                + "\n\n";

        tvQuestion.setText(progressText + question.getQuestion());

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

    private void checkAnswer() {
        if (currentQuestion == null) {
            return;
        }

        int checkedId = radioGroup.getCheckedRadioButtonId();

        if (checkedId == -1) {
            Toast.makeText(getContext(), "정답을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

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

        boolean isCorrect = selectedIndex == currentQuestion.getCorrectAnswerIndex();

        handleResult(isCorrect);
    }

    private void handleResult(boolean isCorrect) {
        btnSubmit.setEnabled(false);
        totalSolvedCount++;

        if (isCorrect) {
            correctCount++;

            if (quizFaceImage != null) {
                quizFaceImage.setImageResource(R.drawable.face_happy);
            }

            int scoreToAdd = getQuestionScore(currentQuestion);

            earnedScore += scoreToAdd;
            earnedGold += scoreToAdd;

            playEffect(R.raw.success, "정답입니다!\n+" + scoreToAdd + "점");
        } else {
            if (quizFaceImage != null) {
                quizFaceImage.setImageResource(R.drawable.face_sad);
            }

            playEffect(R.raw.fail, "오답입니다!");
        }
    }

    private void playEffect(int rawResId, String statusText) {
        layoutResult.setVisibility(View.VISIBLE);
        tvResultStatus.setText(statusText);

        lottieEffect.setAnimation(rawResId);
        lottieEffect.playAnimation();
        lottieEffect.removeAllAnimatorListeners();

        lottieEffect.addAnimatorListener(new android.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {
            }

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                lottieEffect.removeAllAnimatorListeners();

                layoutResult.postDelayed(() -> {
                    layoutResult.setVisibility(View.GONE);
                    currentQuestionIndex++;
                    bindCurrentQuestion();
                }, 500);
            }

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
            }

            @Override
            public void onAnimationRepeat(android.animation.Animator animation) {
            }
        });
    }

    /*
     * 점수 기준:
     * 하: 10점
     * 중: 20점
     * 상: 30점
     *
     * Firebase에 difficulty_level이 없을 경우에는
     * 사용자가 선택한 난이도를 기준으로 점수를 계산한다.
     */
    private int getQuestionScore(QuizQuestion question) {
        if (question == null) {
            return 0;
        }

        String level = question.getDifficultyLevel();

        if (level == null || level.trim().isEmpty()) {
            level = currentDifficultyLevel;
        }

        if ("hard".equals(level)) {
            return 30;
        }

        if ("normal".equals(level)) {
            return 20;
        }

        return 10;
    }

    private int getClearScore() {
        return (int) Math.ceil(targetScore * CLEAR_RATE_BY_TARGET_SCORE);
    }

    /*
     * 변경된 클리어 기준:
     * 문제 개수 기준 X
     * 정답률 기준 X
     * 타겟 점수의 80% 이상이면 클리어
     *
     * 다음 단계 해금은 상 난이도 클리어 시에만 수행.
     */
    private void completeStageIfClear() {
        if (targetScore <= 0) {
            return;
        }

        boolean isClear = earnedScore >= getClearScore();

        if (!isClear) {
            return;
        }

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("quiz_progress", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();

        // 현재 과목의 현재 난이도 클리어 기록
        editor.putInt("subject_" + currentSubjectId + "_" + currentDifficultyLevel + "_clear", 1);

        /*
         * 상 난이도 클리어 시에만 다음 단계 해금.
         * 예:
         * 알고리즘 상 문제 클리어 -> 실전문제 1 해금
         */
        if ("hard".equals(currentDifficultyLevel)) {
            editor.putInt("stage_" + currentSubjectId + "_clear", 1);
            editor.putInt("stage_" + (currentSubjectId + 1) + "_before_clear", 1);
        }

        editor.apply();

        /*
         * Firebase에도 다음 단계 해금 상태 저장.
         * 로그인하지 않은 상태면 SharedPreferences만 사용.
         */
        if ("hard".equals(currentDifficultyLevel)) {
            com.google.firebase.auth.FirebaseAuth mAuth =
                    com.google.firebase.auth.FirebaseAuth.getInstance();

            if (mAuth.getCurrentUser() != null) {
                String uid = mAuth.getCurrentUser().getUid();

                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .update("unlocked_stage_" + (currentSubjectId + 1), true);
            }
        }
    }

    private void showFinalResult() {
        if (getContext() == null) {
            return;
        }

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).addGold(earnedGold);
        }

        boolean isClear = targetScore > 0 && earnedScore >= getClearScore();

        String clearMessage;

        if (isClear) {
            clearMessage = "과목 "
                    + currentSubjectId
                    + "의 "
                    + getDifficultyKoreanName(currentDifficultyLevel)
                    + " 난이도 클리어 성공!\n\n";

            if ("hard".equals(currentDifficultyLevel)) {
                clearMessage += "다음 단계가 해금되었습니다.\n\n";
            }
        } else {
            clearMessage = "과목 "
                    + currentSubjectId
                    + "의 "
                    + getDifficultyKoreanName(currentDifficultyLevel)
                    + " 난이도 클리어 실패\n"
                    + "타겟 점수의 80% 이상을 획득해야 클리어됩니다.\n\n";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("퀴즈 결과")
                .setMessage(
                        clearMessage
                                + "총 푼 문제 수: " + totalSolvedCount + "문제\n"
                                + "맞춘 문제 수: " + correctCount + "문제\n"
                                + "틀린 문제 수: " + (totalSolvedCount - correctCount) + "문제\n\n"
                                + "획득 점수: " + earnedScore + "점\n"
                                + "타겟 점수: " + targetScore + "점\n"
                                + "클리어 기준 점수: " + getClearScore() + "점\n"
                                + "획득 골드: " + earnedGold + "G"
                )
                .setCancelable(false)
                .setPositiveButton("확인", (dialog, which) -> closeFragment())
                .show();
    }

    private void closeFragment() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).closeCurrentFragment();
        }
    }

    private String getDifficultyKoreanName(String difficultyLevel) {
        if ("hard".equals(difficultyLevel)) {
            return "상";
        }

        if ("normal".equals(difficultyLevel)) {
            return "중";
        }

        return "하";
    }

    private void updateOptionBackgrounds() {
        option1.setBackgroundResource(
                option1.isChecked() ? R.drawable.bg_quiz_option_selected : R.drawable.bg_message_box
        );

        option2.setBackgroundResource(
                option2.isChecked() ? R.drawable.bg_quiz_option_selected : R.drawable.bg_message_box
        );

        option3.setBackgroundResource(
                option3.isChecked() ? R.drawable.bg_quiz_option_selected : R.drawable.bg_message_box
        );

        option4.setBackgroundResource(
                option4.isChecked() ? R.drawable.bg_quiz_option_selected : R.drawable.bg_message_box
        );
    }

    private void applyPressAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate()
                            .scaleX(0.96f)
                            .scaleY(0.96f)
                            .setDuration(80)
                            .start();
                    break;

                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(80)
                            .start();
                    break;
            }

            return false;
        });
    }
}