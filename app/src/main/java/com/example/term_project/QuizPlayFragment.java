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

public class QuizPlayFragment extends Fragment {

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
    private QuizQuestion currentQuestion;

    private int currentSubjectId = 1;
    private int currentQuestionId = 1;
    private String currentDifficultyLevel = "easy";

    private int totalSolvedCount = 0;
    private int correctCount = 0;
    private int earnedGold = 0;

    private CharacterViewModel characterViewModel;
    private ImageView quizFaceImage;
    private ImageView quizHatImage;
    private ImageView quizClothesImage;

    public QuizPlayFragment() {
        // 기본 생성자
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

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

        loadQuestion(currentSubjectId, currentQuestionId, currentDifficultyLevel);

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

    private void loadQuestion(int subjectId, int questionId, String difficultyLevel) {
        btnSubmit.setEnabled(false);
        tvQuestion.setText("문제를 불러오는 중입니다...");

        repository.getQuizQuestionFromFirestore(
                subjectId,
                questionId,
                difficultyLevel,
                new QuizRepository.OnQuestionFetchedListener() {
                    @Override
                    public void onSuccess(QuizQuestion question) {
                        if (!isAdded()) {
                            return;
                        }

                        currentQuestion = question;
                        bindQuestion(question);

                        radioGroup.clearCheck();
                        updateOptionBackgrounds();

                        btnSubmit.setEnabled(true);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (!isAdded()) {
                            return;
                        }

                        // 첫 문제부터 없으면 출제 가능한 문제가 없는 상태
                        if (currentQuestionId == 1) {
                            tvQuestion.setText(
                                    "출제 가능한 문제가 없습니다.\n"
                                            + "과목 번호: " + currentSubjectId + "\n"
                                            + "난이도: " + getDifficultyKoreanName(currentDifficultyLevel) + "\n\n"
                                            + e.getMessage()
                            );
                            btnSubmit.setEnabled(false);
                            return;
                        }

                        // 2번 이후 문제가 없으면 해당 난이도 문제를 다 푼 것으로 처리
                        completeStageIfClear();
                        showFinalResult();
                    }
                }
        );
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

            int goldToAdd = calculateGold();
            earnedGold += goldToAdd;

            playEffect(R.raw.success, "정답입니다!\n+" + goldToAdd + "G");
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

                    currentQuestionId++;
                    loadQuestion(currentSubjectId, currentQuestionId, currentDifficultyLevel);
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

    private int calculateGold() {
        if (currentQuestion == null) {
            return 0;
        }

        String level = currentQuestion.getDifficultyLevel();

        if ("hard".equals(level)) {
            return 30;
        }

        if ("normal".equals(level)) {
            return 20;
        }

        return 10;
    }

    private void completeStageIfClear() {
        if (totalSolvedCount == 0) {
            return;
        }

        double correctRate = ((double) correctCount / totalSolvedCount) * 100.0;

        // 70% 이상이면 클리어
        if (correctRate < 70.0) {
            return;
        }

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("quiz_progress", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();

        // 현재 과목 클리어 저장
        editor.putInt("subject_" + currentSubjectId + "_" + currentDifficultyLevel + "_clear", 1);

        // 기존 단계 해금 구조 유지: 현재 과목 클리어 시 다음 과목 해금
        editor.putInt("stage_" + currentSubjectId + "_clear", 1);
        editor.putInt("stage_" + (currentSubjectId + 1) + "_before_clear", 1);

        editor.apply();

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

    private void showFinalResult() {
        if (getContext() == null) {
            return;
        }

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).addGold(earnedGold);
        }

        double correctRate = totalSolvedCount > 0
                ? ((double) correctCount / totalSolvedCount) * 100.0
                : 0.0;

        boolean isClear = correctRate >= 70.0;

        String clearMessage;

        if (isClear) {
            clearMessage = "과목 " + currentSubjectId + "의 "
                    + getDifficultyKoreanName(currentDifficultyLevel)
                    + " 난이도 클리어 성공!\n\n";
        } else {
            clearMessage = "과목 " + currentSubjectId + "의 "
                    + getDifficultyKoreanName(currentDifficultyLevel)
                    + " 난이도 클리어 실패\n"
                    + "70% 이상 맞혀야 클리어됩니다.\n\n";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("퀴즈 결과")
                .setMessage(
                        clearMessage
                                + "총 푼 문제 수: " + totalSolvedCount + "문제\n"
                                + "맞춘 문제 수: " + correctCount + "문제\n"
                                + "틀린 문제 수: " + (totalSolvedCount - correctCount) + "문제\n"
                                + "정답률: " + String.format("%.1f", correctRate) + "%\n"
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
                option1.isChecked()
                        ? R.drawable.bg_quiz_option_selected
                        : R.drawable.bg_message_box
        );

        option2.setBackgroundResource(
                option2.isChecked()
                        ? R.drawable.bg_quiz_option_selected
                        : R.drawable.bg_message_box
        );

        option3.setBackgroundResource(
                option3.isChecked()
                        ? R.drawable.bg_quiz_option_selected
                        : R.drawable.bg_message_box
        );

        option4.setBackgroundResource(
                option4.isChecked()
                        ? R.drawable.bg_quiz_option_selected
                        : R.drawable.bg_message_box
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