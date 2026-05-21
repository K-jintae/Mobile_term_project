package com.example.term_project;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

public class QuizPlayFragment extends Fragment {

    // 문제 출력용 뷰
    private TextView tvQuestion;
    private RadioGroup radioGroup;
    private RadioButton option1, option2, option3, option4;
    private Button btnSubmit;

    // 정답/오답 이펙트 관련 뷰
    private com.airbnb.lottie.LottieAnimationView lottieEffect;
    private View layoutResult;
    private TextView tvResultStatus;

    // 문제 로딩용 객체
    private QuizRepository repository;
    private QuizQuestion currentQuestion;

    // 현재 과목(스테이지) 번호
    private int currentSubjectId = 1;

    // 현재 선택된 난이도
    // difficulty_level 값은 Firestore / Repository 기준으로 easy, normal, hard 사용
    private String currentDifficultyLevel = "easy";

    // Repository에서 받아온 출제 문제 목록
    private List<QuizQuestion> selectedQuestions = new ArrayList<>();

    // 현재 몇 번째 문제인지
    private int currentQuestionIndex = 0;

    // 결과 집계용 변수
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

        // 뷰 연결
        tvQuestion = view.findViewById(R.id.tvQuestion);
        radioGroup = view.findViewById(R.id.radioGroupOptions);
        option1 = view.findViewById(R.id.option1);
        option2 = view.findViewById(R.id.option2);
        option3 = view.findViewById(R.id.option3);
        option4 = view.findViewById(R.id.option4);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        quizFaceImage = view.findViewById(R.id.quizFaceImage);
        quizHatImage = view.findViewById(R.id.quizHatImage);
        quizClothesImage = view.findViewById(R.id.quizClothesImage);

        repository = new QuizRepository();

        setupCharacterImages();

        // 버튼 터치 애니메이션 적용
        applyPressAnimation(btnSubmit);

        // 라디오 버튼 선택 시 배경색 업데이트
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (!isAnswerSubmitted) {
                updateOptionBackgrounds();
            }
        });

        // 전달받은 과목 번호와 난이도 읽기
        if (getArguments() != null) {
            currentSubjectId = getArguments().getInt("subject_id", 1);

            // difficulty-test 계열 코드에서 넘기는 키가 difficulty_level일 가능성이 높음
            currentDifficultyLevel = getArguments().getString("difficulty_level", "easy");

            // 혹시 다른 파일에서 "difficulty"라는 키로 넘기고 있다면 fallback 처리
            if (currentDifficultyLevel == null || currentDifficultyLevel.trim().isEmpty()) {
                currentDifficultyLevel = getArguments().getString("difficulty", "easy");
            }
        }

        currentDifficultyLevel = normalizeDifficultyLevel(currentDifficultyLevel);

        // 기존 방식:
        // loadQuestion(currentSubjectId, currentQuestionId);
        //
        // 수정 방식:
        // 과목 + 난이도 기준으로 문제 목록을 한 번에 받아온 뒤,
        // currentQuestionIndex만 증가시키면서 화면에 표시한다.
        loadQuestionSet(currentSubjectId, currentDifficultyLevel);

        // 제출 버튼 클릭 시 정답 확인
        // 제출/다음 버튼 클릭 처리
        btnSubmit.setOnClickListener(v -> {
            if (!isAnswerSubmitted) {
                checkAnswer(); // 제출 상태
            } else {
                moveToNextQuestion(); // 다음 문제로 이동
            }
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        showExitQuizDialog();
                    }
                }
        );

        return view;
    }

    private void setupCharacterImages() {
        characterViewModel = new ViewModelProvider(requireActivity()).get(CharacterViewModel.class);

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

    /**
     * Firestore에서 현재 과목 + 난이도 기준으로 문제 목록을 가져온다.
     * 현재 QuizRepository의 새 구조에 맞춘 방식이다.
     */
    private void loadQuestionSet(int subjectId, String difficultyLevel) {
        btnSubmit.setEnabled(false);
        tvQuestion.setText("문제를 불러오는 중입니다...");

        repository.getQuizQuestionsFromFirestore(
                subjectId,
                difficultyLevel,
                10,
                new QuizRepository.OnQuestionsFetchedListener() {

                    @Override
                    public void onSuccess(List<QuizQuestion> questions) {
                        if (!isAdded()) {
                            return;
                        }

                        if (questions == null || questions.isEmpty()) {
                            tvQuestion.setText("출제 가능한 문제가 없습니다.");
                            btnSubmit.setEnabled(false);
                            return;
                        }

                        selectedQuestions = questions;
                        currentQuestionIndex = 0;

                        totalSolvedCount = 0;
                        correctCount = 0;
                        earnedGold = 0;

                        showQuestionByIndex();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (!isAdded()) {
                            return;
                        }

                        tvQuestion.setText(
                                "출제 가능한 문제가 없습니다.\n\n" +
                                        "과목 번호: " + currentSubjectId + "\n" +
                                        "난이도: " + getDifficultyKoreanName(currentDifficultyLevel) + "\n\n" +
                                        e.getMessage()
                        );
                        btnSubmit.setEnabled(false);
                    }
                }
        );
    }

    /**
     * selectedQuestions에서 현재 index의 문제를 화면에 출력한다.
     */
    private void showQuestionByIndex() {
        if (selectedQuestions == null || selectedQuestions.isEmpty()) {
            tvQuestion.setText("출제 가능한 문제가 없습니다.");
            btnSubmit.setEnabled(false);
            return;
        }

        if (currentQuestionIndex >= selectedQuestions.size()) {
            completeStageIfClear();
            showFinalResult();
            return;
        }

        currentQuestion = selectedQuestions.get(currentQuestionIndex);
        bindQuestion(currentQuestion);

        radioGroup.clearCheck();
        resetOptionBackgrounds(); // 모든 배경 초기화
        isAnswerSubmitted = false;
        btnSubmit.setText("제출");
        btnSubmit.setEnabled(true);
        enableAllOptions(true);

        // 얼굴 기본 상태로 복원
        if (quizFaceImage != null) {
            Integer currentFaceId = characterViewModel.getFace().getValue();
            if (currentFaceId != null && currentFaceId != 0) {
                quizFaceImage.setImageResource(currentFaceId);
            } else {
                quizFaceImage.setImageResource(R.drawable.face_default);
            }
        }
    }

    /**
     * 문제와 보기를 화면에 표시한다.
     */
    private void bindQuestion(QuizQuestion question) {
        if (question == null) {
            return;
        }

        int questionNumber = currentQuestionIndex + 1;
        int totalQuestionCount = selectedQuestions.size();

        // 화면에 "과목 번호: 1 / 난이도: 중" 같은 디버그성 문구를 크게 띄우지 않기 위해
        // 문제 번호와 문제 내용만 표시한다.
        tvQuestion.setText(
                "[" + questionNumber + " / " + totalQuestionCount + "] " +
                        question.getQuestion()
        );

        String[] options = question.getOptions();

        option1.setVisibility(View.GONE);
        option2.setVisibility(View.GONE);
        option3.setVisibility(View.GONE);
        option4.setVisibility(View.GONE);

        option1.setChecked(false);
        option2.setChecked(false);
        option3.setChecked(false);
        option4.setChecked(false);

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

    /**
     * 사용자가 선택한 답이 정답인지 검사한다.
     */
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
        // 정답/오답 처리
        isAnswerSubmitted = true;
        btnSubmit.setText("다음");
        enableAllOptions(false);

        showAnswerFeedback(selectedIndex, isCorrect);
        handleResult(isCorrect);
    }

    /**
     * 정답/오답을 테두리 색상으로 표시
     */
    private void showAnswerFeedback(int selectedIndex, boolean isCorrect) {
        RadioButton[] options = {option1, option2, option3, option4};
        int correctIndex = currentQuestion.getCorrectAnswerIndex();

        // 정답 표시 (초록색)
        if (correctIndex >= 0 && correctIndex < options.length) {
            options[correctIndex].setBackgroundResource(R.drawable.bg_option_correct);
        }

        // 오답 표시 (빨간색) - 선택한 답이 정답이 아닐 경우에만
        if (!isCorrect && selectedIndex >= 0 && selectedIndex < options.length) {
            options[selectedIndex].setBackgroundResource(R.drawable.bg_option_wrong);
        }
    }

    /**
     * 정답/오답 결과를 처리한다.
     */
    private void handleResult(boolean isCorrect) {
        //btnSubmit.setEnabled(false);
        totalSolvedCount++;

        if (isCorrect) {
            if (quizFaceImage != null) {
                quizFaceImage.setImageResource(R.drawable.face_happy);
            }

            correctCount++;

            int goldToAdd = calculateGold(currentQuestion);
            earnedGold += goldToAdd;

            // playEffect(R.raw.success, "정답입니다!\n+" + goldToAdd + "G");
        } else {
            if (quizFaceImage != null) {
                quizFaceImage.setImageResource(R.drawable.face_sad);
            }

            // playEffect(R.raw.fail, "오답입니다!");
        }
    }

    /**
    /**
     * 이펙트를 재생한 뒤 다음 문제로 이동한다.

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
                    showQuestionByIndex();
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
     */

    /**
     * 다음 문제로 이동
     */
    private void moveToNextQuestion() {
        currentQuestionIndex++;
        showQuestionByIndex();
    }

    /**
     * 모든 옵션 배경 초기화
     */
    private void resetOptionBackgrounds() {
        option1.setBackgroundResource(R.drawable.bg_option_default);
        option2.setBackgroundResource(R.drawable.bg_option_default);
        option3.setBackgroundResource(R.drawable.bg_option_default);
        option4.setBackgroundResource(R.drawable.bg_option_default);
    }

    /**
     * 모든 옵션 활성화/비활성화
     */
    private void enableAllOptions(boolean enabled) {
        option1.setEnabled(enabled);
        option2.setEnabled(enabled);
        option3.setEnabled(enabled);
        option4.setEnabled(enabled);
    }

    /**
     * 문제 난이도에 따라 점수/골드를 계산한다.
     * easy = 15, normal = 30, hard = 45
     */
    private int calculateGold(QuizQuestion question) {
        if (question == null) {
            return 0;
        }

        return getScoreByDifficulty(question.getDifficultyLevel());
    }

    private int getScoreByDifficulty(String difficultyLevel) {
        String level = normalizeDifficultyLevel(difficultyLevel);

        if ("hard".equals(level)) {
            return 45;
        }

        if ("normal".equals(level)) {
            return 30;
        }

        return 15;
    }

    /**
     * 현재 출제된 문제들의 총 목표 점수.
     */
    private int getTargetScore() {
        int targetScore = 0;

        if (selectedQuestions == null) {
            return 0;
        }

        for (QuizQuestion question : selectedQuestions) {
            targetScore += getScoreByDifficulty(question.getDifficultyLevel());
        }

        return targetScore;
    }

    /**
     * 사용자가 선택한 보기에 따라 라디오 버튼의 배경을 업데이트한다.
     */
    private void updateOptionBackgrounds() {
        option1.setBackgroundResource(option1.isChecked()
                ? R.drawable.bg_quiz_option_selected
                : R.drawable.bg_option_default);

        option2.setBackgroundResource(option2.isChecked()
                ? R.drawable.bg_quiz_option_selected
                : R.drawable.bg_option_default);

        option3.setBackgroundResource(option3.isChecked()
                ? R.drawable.bg_quiz_option_selected
                : R.drawable.bg_option_default);

        option4.setBackgroundResource(option4.isChecked()
                ? R.drawable.bg_quiz_option_selected
                : R.drawable.bg_option_default);
    }

    /**
     * 현재 난이도 클리어 여부.
     * 기준: 획득 점수가 목표 점수의 80% 이상.
     */
    private boolean isCurrentDifficultyClear() {
        int targetScore = getTargetScore();

        if (targetScore <= 0) {
            return false;
        }

        return earnedGold >= targetScore * 0.8;
    }

    /**
     * 다음 스테이지 해금 여부.
     * 기준: hard(상) 난이도에서 목표 점수의 80% 이상.
     */
    private boolean canUnlockNextStage() {
        return "hard".equals(normalizeDifficultyLevel(currentDifficultyLevel))
                && isCurrentDifficultyClear();
    }
    // 추가: 정답 제출 후 상태 관리
    private boolean isAnswerSubmitted = false;
    /**
     * 난이도 클리어 및 다음 단계 해금 처리.
     */
    private void completeStageIfClear() {
        if (totalSolvedCount == 0) {
            return;
        }

        if (!isCurrentDifficultyClear()) {
            return;
        }

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("quiz_progress", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();

        // 현재 과목 + 현재 난이도 클리어 저장
        // 예: subject_1_easy_clear, subject_1_normal_clear, subject_1_hard_clear
        editor.putInt(
                "subject_" + currentSubjectId + "_" + currentDifficultyLevel + "_clear",
                1
        );

        // hard 난이도를 80% 이상 달성한 경우에만 다음 스테이지 해금
        if (canUnlockNextStage()) {
            editor.putInt("stage_" + currentSubjectId + "_clear", 1);
            editor.putInt("stage_" + (currentSubjectId + 1) + "_before_clear", 1);
        }

        editor.apply();

        if (canUnlockNextStage()) {
            saveNextStageUnlockToFirebase();
        }
    }

    /**
     * Firebase에 다음 스테이지 해금 상태 저장.
     */
    private void saveNextStageUnlockToFirebase() {
        com.google.firebase.auth.FirebaseAuth mAuth =
                com.google.firebase.auth.FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("unlocked_stage_" + (currentSubjectId + 1), true);
    }

    /**
     * 퀴즈 결과 다이얼로그를 출력한다.
     */
    //퀴즈 결과 출력 함수 수정 및 추가
    private void showFinalResult() {
        if (getContext() == null) {
            return;
        }

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).addGold(earnedGold);
            ((MainActivity) getActivity()).clearLongAbsenceStateAfterQuiz();
        }

        Dialog dialog = new Dialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_quiz_result, null);
        dialog.setContentView(dialogView);
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvResultMessage = dialogView.findViewById(R.id.tvResultMessage);

        TextView tvStatEarnedGold = dialogView.findViewById(R.id.tvStatEarnedGold);
        TextView tvStatAccuracy = dialogView.findViewById(R.id.tvStatAccuracy);
        TextView tvStatSubject = dialogView.findViewById(R.id.tvStatSubject);
        TextView tvStatDifficulty = dialogView.findViewById(R.id.tvStatDifficulty);
        TextView tvStatCorrectCount = dialogView.findViewById(R.id.tvStatCorrectCount);
        TextView tvStatWrongCount = dialogView.findViewById(R.id.tvStatWrongCount);
        TextView tvStatTargetScore = dialogView.findViewById(R.id.tvStatTargetScore);
        TextView tvStatClearScore = dialogView.findViewById(R.id.tvStatClearScore);

        android.widget.Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        int targetScore = getTargetScore();
        int clearScore = (int) Math.ceil(targetScore * 0.8);
        double correctRate = (totalSolvedCount > 0) ? ((double) correctCount / totalSolvedCount) * 100.0 : 0.0;
        int wrongCount = totalSolvedCount - correctCount;

        boolean difficultyClear = isCurrentDifficultyClear();
        boolean nextStageUnlocked = canUnlockNextStage();

        if (nextStageUnlocked) {
            tvResultMessage.setText("상 난이도 목표 점수의 80% 이상을 달성했습니다.\n다음 단계가 해금되었습니다.");
        } else if (difficultyClear) {
            tvResultMessage.setText(getDifficultyKoreanName(currentDifficultyLevel) + " 난이도를 클리어했습니다.\n다음 단계 해금은 상 난이도 80% 이상 달성 시 가능합니다.");
        } else {
            tvResultMessage.setText("클리어 실패\n목표 점수의 80% 이상을 달성해야 합니다.");
        }

        // 8개 항목 대시보드 구조에 할당
        tvStatEarnedGold.setText(earnedGold + " 점");
        tvStatAccuracy.setText(String.format("%.1f", correctRate) + "%");
        tvStatSubject.setText(String.valueOf(currentSubjectId));
        tvStatDifficulty.setText(getDifficultyKoreanName(currentDifficultyLevel));
        tvStatCorrectCount.setText(correctCount + "문제");
        tvStatWrongCount.setText(wrongCount + "문제");
        tvStatTargetScore.setText(targetScore + "점");
        tvStatClearScore.setText(clearScore + "점");

        applyPressAnimation(btnConfirm);
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            closeFragment();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.86),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    /**
     * 현재 프래그먼트를 닫는다.
     */
    private void closeFragment() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).closeCurrentFragment();
        }
    }

    private void showExitQuizDialog() {
        Dialog dialog = new Dialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_quiz_exit, null);

        dialog.setContentView(dialogView);
        dialog.setCancelable(true);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setDimAmount(0.001f);
        }

        AppCompatButton btnStay = dialogView.findViewById(R.id.btnStay);
        AppCompatButton btnExit = dialogView.findViewById(R.id.btnExit);

        btnStay.setOnClickListener(v -> dialog.dismiss());

        btnExit.setOnClickListener(v -> {
            dialog.dismiss();
            closeFragment();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.82),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    /**
     * 버튼 터치 시 눌리는 듯한 시각적 효과를 적용한다.
     */
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

    /**
     * 난이도 값 정규화.
     * 내부 로직은 easy, normal, hard만 사용한다.
     */
    private String normalizeDifficultyLevel(String difficultyLevel) {
        if (difficultyLevel == null) {
            return "easy";
        }

        String level = difficultyLevel.trim().toLowerCase();

        if ("hard".equals(level) || "상".equals(level)) {
            return "hard";
        }

        if ("normal".equals(level) || "middle".equals(level) || "중".equals(level)) {
            return "normal";
        }

        return "easy";
    }

    /**
     * 난이도 한글 표시.
     */
    private String getDifficultyKoreanName(String difficultyLevel) {
        String level = normalizeDifficultyLevel(difficultyLevel);

        if ("hard".equals(level)) {
            return "상";
        }

        if ("normal".equals(level)) {
            return "중";
        }

        return "하";
    }
}
