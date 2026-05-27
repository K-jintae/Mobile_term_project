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
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class QuizPlayFragment extends Fragment {

    private static final String PREF_CONTINUE_QUIZ = "continue_quiz";

    private TextView tvQuestion;
    private RadioGroup radioGroup;
    private RadioButton option1, option2, option3, option4;
    private Button btnSubmit;

    private QuizRepository repository;
    private QuizQuestion currentQuestion;

    private int currentSubjectId = 1;
    private String currentDifficultyLevel = "easy";

    private List<QuizQuestion> selectedQuestions = new ArrayList<>();
    private int currentQuestionIndex = 0;

    private int totalSolvedCount = 0;
    private int correctCount = 0;
    private int earnedGold = 0;

    private boolean isAnswerSubmitted = false;
    private boolean isContinueMode = false;

    private CharacterViewModel characterViewModel;
    private ImageView quizFaceImage;
    private ImageView quizHatImage;
    private ImageView quizClothesImage;

    private String myUserLevel = "하수";

    public QuizPlayFragment() {
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

        quizFaceImage = view.findViewById(R.id.quizFaceImage);
        quizHatImage = view.findViewById(R.id.quizHatImage);
        quizClothesImage = view.findViewById(R.id.quizClothesImage);

        repository = new QuizRepository();

        setupCharacterImages();
        applyPressAnimation(btnSubmit);

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (!isAnswerSubmitted) {
                updateOptionBackgrounds();
            }
        });

        if (getArguments() != null) {
            currentSubjectId = getArguments().getInt("subject_id", 1);
            currentDifficultyLevel = getArguments().getString("difficulty_level", "easy");

            if (currentDifficultyLevel == null || currentDifficultyLevel.trim().isEmpty()) {
                currentDifficultyLevel = getArguments().getString("difficulty", "easy");
            }

            isContinueMode = getArguments().getBoolean("continue_mode", false);
        }

        currentDifficultyLevel = normalizeDifficultyLevel(currentDifficultyLevel);

        if (isContinueMode) {
            loadContinueQuizState();
        }

        //문제를 바로 로드하지 않고, 내 진짜 파이어스토어 티어를 선행 로드한 뒤 연쇄적으로 문제를 뽑아냅니다.
        fetchMyActualTierAndLoadQuiz();

        btnSubmit.setOnClickListener(v -> {
            if (!isAnswerSubmitted) {
                checkAnswer();
            } else {
                moveToNextQuestion();
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

    // [신규 메서드] 파이어스토어에서 로그인한 유저의 "level" 정보를 안전하게 긁어옵니다.
    private void fetchMyActualTierAndLoadQuiz(){
        btnSubmit.setEnabled(false);
        tvQuestion.setText("유저 정보 분석 중입니다 ...");

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if(auth.getCurrentUser() == null){
            myUserLevel = "하수";
            if (!validateDifficultyWithTier()) {
                Toast.makeText(getContext(), "접근 권한이 없습니다.", Toast.LENGTH_SHORT).show();
                closeFragment();
                return;
            }
            loadQuestionSet(currentSubjectId, currentDifficultyLevel);
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.exists()){
                        String lvl = documentSnapshot.getString("level");
                        if(lvl != null && !lvl.isEmpty()){
                            myUserLevel = lvl.trim(); // 실시간 등급 동기화
                        }
                    }

                    // [최종 방어선] 유저의 진짜 티어와 선택한 난이도를 비교 검증
                    if (!validateDifficultyWithTier()) {
                        Toast.makeText(getContext(), "현재 등급(" + myUserLevel + ")으로는 진입할 수 없는 난이도입니다.", Toast.LENGTH_SHORT).show();
                        closeFragment(); // 권한 미달 시 즉시 이전 화면으로 탈출
                        return;
                    }

                    // 검증 통과 시 문제 로드
                    loadQuestionSet(currentSubjectId, currentDifficultyLevel);
                })
                .addOnFailureListener(e -> {
                    if (!validateDifficultyWithTier()) {
                        Toast.makeText(getContext(), "권한 검증 오류로 진입할 수 없습니다.", Toast.LENGTH_SHORT).show();
                        closeFragment();
                        return;
                    }
                    loadQuestionSet(currentSubjectId, currentDifficultyLevel);
                });
    }

    private boolean validateDifficultyWithTier() {
        // 기존에 구현된 안전한 난이도 치환 활용 ("easy", "normal", "hard")
        String normalizedDiff = normalizeDifficultyLevel(currentDifficultyLevel);

        if (myUserLevel.contains("하수")) {
            // 하수는 오직 하(easy)만 허용
            return "easy".equals(normalizedDiff);

        } else if (myUserLevel.contains("중수")) {
            // 중수는 하(easy), 중(normal)까지 허용
            return "easy".equals(normalizedDiff) || "normal".equals(normalizedDiff);

        } else if (myUserLevel.contains("고수")) {
            // 고수는 프리패스
            return true;
        }

        return "easy".equals(normalizedDiff);
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

    // [QuizPlayFragment.java 내부의 loadQuestionSet 메서드 정석 복구 구역]
    private void loadQuestionSet(int subjectId, String difficultyLevel) {
        btnSubmit.setEnabled(false);
        tvQuestion.setText("문제를 불러오는 중입니다...");

        int dbSubjectId = getFirestoreSubjectId(subjectId);

        // 앞단 화면에서 자물쇠로 버튼을 원천 봉쇄하므로, 여기서는 사용자가 클릭하고 들어온
        // 정당한 난이도(difficultyLevel)를 100% 신뢰하고 정석대로 서버에 퀴즈를 요청합니다.
        repository.getQuizQuestionsFromFirestore(
                dbSubjectId,
                difficultyLevel, // 원본 난이도 그대로 전달
                10,
                new QuizRepository.OnQuestionsFetchedListener() {
                    @Override
                    public void onSuccess(List<QuizQuestion> questions) {
                        if (!isAdded()) return;

                        if (questions == null || questions.isEmpty()) {
                            tvQuestion.setText("출제 가능한 문제가 없습니다.");
                            btnSubmit.setEnabled(false);
                            return;
                        }

                        // 순수 정석 포맷으로 돌아온 QuizSelector 백트래킹 엔진 가동
                        try {
                            selectedQuestions = QuizSelector.selectQuestions(questions, difficultyLevel, myUserLevel);
                        } catch(Exception e) {
                            selectedQuestions = questions;
                        }

                        if (!isContinueMode) {
                            currentQuestionIndex = 0;
                            totalSolvedCount = 0;
                            correctCount = 0;
                            earnedGold = 0;
                        }

                        if (currentQuestionIndex < 0) currentQuestionIndex = 0;
                        if (currentQuestionIndex >= selectedQuestions.size()) {
                            currentQuestionIndex = selectedQuestions.size() - 1;
                        }

                        showQuestionByIndex();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (!isAdded()) return;
                        tvQuestion.setText("출제 가능한 문제가 없습니다...\n" + e.getMessage());
                        btnSubmit.setEnabled(false);
                    }
                }
        );
    }

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
        resetOptionBackgrounds();

        isAnswerSubmitted = false;
        btnSubmit.setText("제출");
        btnSubmit.setEnabled(true);
        enableAllOptions(true);

        if (quizFaceImage != null) {
            Integer currentFaceId = characterViewModel.getFace().getValue();
            if (currentFaceId != null && currentFaceId != 0) {
                quizFaceImage.setImageResource(currentFaceId);
            } else {
                quizFaceImage.setImageResource(R.drawable.face_default);
            }
        }

        saveContinueQuiz(currentQuestionIndex);
    }

    private void bindQuestion(QuizQuestion question) {
        if (question == null) {
            return;
        }

        int questionNumber = currentQuestionIndex + 1;
        int totalQuestionCount = selectedQuestions.size();

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

        isAnswerSubmitted = true;
        btnSubmit.setText("다음");
        enableAllOptions(false);

        showAnswerFeedback(selectedIndex, isCorrect);
        handleResult(isCorrect);

        saveContinueQuiz(currentQuestionIndex + 1);
    }

    private void showAnswerFeedback(int selectedIndex, boolean isCorrect) {
        RadioButton[] options = {option1, option2, option3, option4};
        int correctIndex = currentQuestion.getCorrectAnswerIndex();

        if (correctIndex >= 0 && correctIndex < options.length) {
            options[correctIndex].setBackgroundResource(R.drawable.bg_option_correct);
        }

        if (!isCorrect && selectedIndex >= 0 && selectedIndex < options.length) {
            options[selectedIndex].setBackgroundResource(R.drawable.bg_option_wrong);
        }
    }

    private void handleResult(boolean isCorrect) {
        totalSolvedCount++;

        if (isCorrect) {
            if (quizFaceImage != null) {
                quizFaceImage.setImageResource(R.drawable.face_happy);
            }

            correctCount++;

            int goldToAdd = calculateGold(currentQuestion);
            earnedGold += goldToAdd;
        } else {
            if (quizFaceImage != null) {
                quizFaceImage.setImageResource(R.drawable.face_sad);
            }
        }
    }

    private void moveToNextQuestion() {
        currentQuestionIndex++;
        showQuestionByIndex();
    }

    private void saveContinueQuiz(int nextIndex) {
        if (getContext() == null) {
            return;
        }

        if (nextIndex >= selectedQuestions.size()) {
            return;
        }

        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREF_CONTINUE_QUIZ, Context.MODE_PRIVATE);

        prefs.edit()
                .putBoolean("has_continue", true)
                .putInt("subject_id", currentSubjectId)
                .putString("difficulty_level", currentDifficultyLevel)
                .putInt("question_index", nextIndex)
                .putInt("total_solved_count", totalSolvedCount)
                .putInt("correct_count", correctCount)
                .putInt("earned_gold", earnedGold)
                .apply();
    }

    private void loadContinueQuizState() {
        if (getContext() == null) {
            return;
        }

        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREF_CONTINUE_QUIZ, Context.MODE_PRIVATE);

        boolean hasContinue = prefs.getBoolean("has_continue", false);

        if (!hasContinue) {
            return;
        }

        currentSubjectId = prefs.getInt("subject_id", currentSubjectId);
        currentDifficultyLevel = prefs.getString("difficulty_level", currentDifficultyLevel);
        currentDifficultyLevel = normalizeDifficultyLevel(currentDifficultyLevel);

        currentQuestionIndex = prefs.getInt("question_index", 0);
        totalSolvedCount = prefs.getInt("total_solved_count", 0);
        correctCount = prefs.getInt("correct_count", 0);
        earnedGold = prefs.getInt("earned_gold", 0);
    }

    private void clearContinueQuiz() {
        if (getContext() == null) {
            return;
        }

        requireContext()
                .getSharedPreferences(PREF_CONTINUE_QUIZ, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    private void resetOptionBackgrounds() {
        option1.setBackgroundResource(R.drawable.bg_option_default);
        option2.setBackgroundResource(R.drawable.bg_option_default);
        option3.setBackgroundResource(R.drawable.bg_option_default);
        option4.setBackgroundResource(R.drawable.bg_option_default);
    }

    private void enableAllOptions(boolean enabled) {
        option1.setEnabled(enabled);
        option2.setEnabled(enabled);
        option3.setEnabled(enabled);
        option4.setEnabled(enabled);
    }

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

    private boolean isCurrentDifficultyClear() {
        int targetScore = getTargetScore();

        if (targetScore <= 0) {
            return false;
        }

        return earnedGold >= targetScore * 0.8;
    }

    private boolean canUnlockNextStage() {
        return "hard".equals(normalizeDifficultyLevel(currentDifficultyLevel))
                && isCurrentDifficultyClear();
    }

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

        editor.putInt(
                "subject_" + currentSubjectId + "_" + currentDifficultyLevel + "_clear",
                1
        );

        if (canUnlockNextStage()) {
            editor.putInt("stage_" + currentSubjectId + "_clear", 1);
            editor.putInt("stage_" + (currentSubjectId + 1) + "_before_clear", 1);
        }

        editor.apply();

        // 상 난이도의 문제를 목표 점수 이상으로 완벽하게 풀어냈을 때 작동합니다.
        if (canUnlockNextStage()) {
            saveNextStageUnlockToFirebase();

            // [신규 추가] 상 난이도 클리어 과목 개수를 판정하여 등급업을 심사합니다.
            checkAndUpgradeUserLevel();
        }
    }

    /*
     * 상 난이도 클리어 과목 개수(3개 이상 중수 / 5개 이상 고수)를 판정하여 레벨업을 진행하는 메서드
     */
    private void checkAndUpgradeUserLevel() {
        com.google.firebase.auth.FirebaseAuth mAuth =
                com.google.firebase.auth.FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        com.google.firebase.firestore.DocumentReference userRef =
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid);

        // 현재 과목의 상 난이도 클리어 도장을 파이어스토어 장부에 기록합니다.
        userRef.update("subject_" + currentSubjectId + "_hard_clear", true)
                .addOnSuccessListener(aVoid -> {

                    // 장부 기록 성공 직후, 유저 문서를 다시 한 번 읽어와 전체 과목 상태를 카운트합니다.
                    userRef.get().addOnSuccessListener(documentSnapshot -> {
                        if (!documentSnapshot.exists()) return;

                        int hardClearCount = 0;

                        // 현재 등록되어 있는 1번부터 8번 과목까지 순회하며 상 난이도가 풀렸는지 체크합니다.
                        for (int i = 1; i <= 8; i++) {
                            Boolean isHardClear = documentSnapshot.getBoolean("subject_" + i + "_hard_clear");
                            if (isHardClear != null && isHardClear) {
                                hardClearCount++;
                            }
                        }

                        android.util.Log.d("UserLevelUpgrade", "현재 상 난이도 클리어 과목 총 개수: " + hardClearCount);

                        String currentLevel = documentSnapshot.getString("level");
                        if (currentLevel == null) currentLevel = "하수";
                        currentLevel = currentLevel.trim();

                        String nextLevel = currentLevel;

                        // 기획하신 레벨업 조건 실시간 대입 알고리즘 구역
                        if (hardClearCount >= 5) {
                            // 상 난이도 5개 이상 클리어 시 최고 존엄 '고수'로 등급 상승
                            if (!"고수".equals(currentLevel)) {
                                nextLevel = "고수";
                            }
                        } else if (hardClearCount >= 3) {
                            // 상 난이도 3개 이상 클리어 시 '하수'에서 '중수'로 등급 상승
                            if ("하수".equals(currentLevel)) {
                                nextLevel = "중수";
                            }
                        }

                        // 조건 조율 결과 실제 등급 변화가 일어났을 때만 파이어스토어 최종 장부를 업데이트합니다.
                        if (!nextLevel.equals(currentLevel)) {
                            final String finalNextLevel = nextLevel;
                            userRef.update("level", nextLevel)
                                    .addOnSuccessListener(aVoid2 -> {
                                        myUserLevel = finalNextLevel; // 내 로컬 변수도 즉시 업데이트하여 싱크를 맞춥니다.
                                        if (getContext() != null) {
                                            Toast.makeText(getContext(), "🎉 등급 상승! 이제부터 [" + finalNextLevel + "] 등급입니다! 🎉", Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    });
                });
    }

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

    private void showFinalResult() {
        if (getContext() == null) {
            return;
        }

        clearContinueQuiz();

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

        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

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

    private void closeFragment() {
        // [신규 추가] 뒤에서 대기 중인 LeftFragment에게 화면을 갱신하라는 무전 신호를 발송합니다.
        getParentFragmentManager().setFragmentResult("quiz_refresh_signal", new Bundle());

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

    private int getFirestoreSubjectId(int stageId) {
        switch (stageId) {
            case 1: return 1; // C언어 (기존 1번)
            case 2: return 4; // 객체지향프로그래밍 (기존 4번)
            case 3: return 7; // 자료구조 (기존 7번)
            case 4: return 5; // 운영체제 (기존 5번)
            case 5: return 3; // 알고리즘 (기존 3번)
            case 6: return 8; // 컴퓨터네트워크 (기존 8번)
            case 7: return 6; // 인공지능개론 (기존 6번)
            case 8: return 2; // 데이터과학 (기존 2번)
            default: return stageId;
        }
    }
}
