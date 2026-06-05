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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // 두 번째 코드의 실시간 유저 티어 변수 안정적 결합
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

        // 문제를 바로 로드하지 않고, 서버 티어를 먼저 검증한 후 문제를 분석해 연쇄 추출합니다.
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

    // 로그인 유저의 UID를 안전하게 가져오는 메서드
    private String getPrefUid() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        return (auth.getCurrentUser() != null) ? auth.getCurrentUser().getUid() : "guest";
    }

    // 파이어스토어에서 로그인한 유저의 진짜 티어 레벨을 동기화 조회합니다.
    private void fetchMyActualTierAndLoadQuiz() {
        btnSubmit.setEnabled(false);
        tvQuestion.setText("유저 정보 분석 중입니다 ...");

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
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
                    if (documentSnapshot.exists()) {
                        String lvl = documentSnapshot.getString("level");
                        if (lvl != null && !lvl.isEmpty()) {
                            myUserLevel = lvl.trim();
                        }
                    }

                    if (!validateDifficultyWithTier()) {
                        Toast.makeText(getContext(), "현재 등급(" + myUserLevel + ")으로는 진입할 수 없는 난이도입니다.", Toast.LENGTH_SHORT).show();
                        closeFragment();
                        return;
                    }

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

    // 글로벌 레벨 및 현재 과목의 단계별 클리어 여부 복합 검사
    private boolean validateDifficultyWithTier() {
        if (getContext() == null) {
            return false;
        }

        String normalizedDiff = normalizeDifficultyLevel(currentDifficultyLevel);

        SharedPreferences progressPrefs = getContext().getSharedPreferences("quiz_progress_" + getPrefUid(), Context.MODE_PRIVATE);
        boolean isEasyClear = progressPrefs.getInt("subject_" + currentSubjectId + "_easy_clear", 0) == 1;
        boolean isNormalClear = progressPrefs.getInt("subject_" + currentSubjectId + "_normal_clear", 0) == 1;

        if (myUserLevel.contains("하수")) {
            if ("normal".equals(normalizedDiff)) {
                return isEasyClear;
            }
            if ("hard".equals(normalizedDiff)) {
                return isNormalClear;
            }
            return "easy".equals(normalizedDiff);

        } else if (myUserLevel.contains("중수")) {
            if ("hard".equals(normalizedDiff)) {
                return isNormalClear;
            }
            return "easy".equals(normalizedDiff) || "normal".equals(normalizedDiff);

        } else if (myUserLevel.contains("고수")) {
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

    // 상/중/하 전 난이도를 서버에서 병합 수집하여 QuizSelector 분배 스케줄러로 혼합 출제하는 핵심 구역 보존
    private void loadQuestionSet(int subjectId, String difficultyLevel) {
        btnSubmit.setEnabled(false);
        tvQuestion.setText("모든 난이도의 퀴즈 데이터를 분석 중입니다...");

        int dbSubjectId = getFirestoreSubjectId(subjectId);
        Map<Integer, QuizQuestion> uniqueQuestions = new HashMap<>();

        // 1단계: "easy" 분석 획득
        repository.getQuizQuestionsFromFirestore(
                dbSubjectId,
                "easy",
                50,
                new QuizRepository.OnQuestionsFetchedListener() {
                    @Override
                    public void onSuccess(List<QuizQuestion> easyQuestions) {
                        if (easyQuestions != null) {
                            for (QuizQuestion q : easyQuestions) {
                                uniqueQuestions.put(q.getQuizId(), q);
                            }
                        }

                        // 2단계: "normal" 분석 연쇄 획득
                        repository.getQuizQuestionsFromFirestore(
                                dbSubjectId,
                                "normal",
                                50,
                                new QuizRepository.OnQuestionsFetchedListener() {
                                    @Override
                                    public void onSuccess(List<QuizQuestion> normalQuestions) {
                                        if (normalQuestions != null) {
                                            for (QuizQuestion q : normalQuestions) {
                                                uniqueQuestions.put(q.getQuizId(), q);
                                            }
                                        }

                                        // 3단계: "hard" 분석 최종 취합
                                        repository.getQuizQuestionsFromFirestore(
                                                dbSubjectId,
                                                "hard",
                                                50,
                                                new QuizRepository.OnQuestionsFetchedListener() {
                                                    @Override
                                                    public void onSuccess(List<QuizQuestion> hardQuestions) {
                                                        if (hardQuestions != null) {
                                                            for (QuizQuestion q : hardQuestions) {
                                                                uniqueQuestions.put(q.getQuizId(), q);
                                                            }
                                                        }

                                                        if (!isAdded()) return;

                                                        List<QuizQuestion> mergedQuestions = new ArrayList<>(uniqueQuestions.values());

                                                        if (mergedQuestions.isEmpty()) {
                                                            tvQuestion.setText("출제 가능한 문제가 없습니다.");
                                                            btnSubmit.setEnabled(false);
                                                            return;
                                                        }

                                                        try {
                                                            selectedQuestions = QuizSelector.selectQuestions(mergedQuestions, difficultyLevel, myUserLevel);
                                                        } catch (Exception e) {
                                                            selectedQuestions = mergedQuestions;
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
                                                        handleLoadFailure(e);
                                                    }
                                                }
                                        );
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        handleLoadFailure(e);
                                    }
                                }
                        );
                    }

                    @Override
                    public void onFailure(Exception e) {
                        handleLoadFailure(e);
                    }
                }
        );
    }

    private void handleLoadFailure(Exception e) {
        if (!isAdded()) return;
        tvQuestion.setText("출제 가능한 문제가 없습니다...\n" + e.getMessage());
        btnSubmit.setEnabled(false);
    }

    // 캐릭터 표정 강제 오버라이드 초기화 버그 완벽 수정 해결
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
        if (getContext() == null || nextIndex >= selectedQuestions.size()) {
            return;
        }

        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREF_CONTINUE_QUIZ + "_" + getPrefUid(), Context.MODE_PRIVATE);

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
                .getSharedPreferences(PREF_CONTINUE_QUIZ + "_" + getPrefUid(), Context.MODE_PRIVATE);

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
                .getSharedPreferences(PREF_CONTINUE_QUIZ + "_" + getPrefUid(), Context.MODE_PRIVATE)
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

    // 로컬 진척도 수립 및 연쇄 데이터 승급 관리 프로세스 매칭
    private void completeStageIfClear() {
        if (totalSolvedCount == 0 || !isCurrentDifficultyClear()) {
            return;
        }

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("quiz_progress_" + getPrefUid(), Context.MODE_PRIVATE);

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

        if (canUnlockNextStage()) {
            saveNextStageUnlockToFirebase();
            checkAndUpgradeUserLevel(); // 상 난이도 데이터 및 실시간 티어 승급 심사를 동시 진행합니다.
        }
    }

    // [두 코드 통합 기획] 첫 번째 코드의 'cleared_hard_stage_N'과 두 번째 코드의 승급 카운트 필드를 하나의 맵으로 묶어 서버에 반영
    private void checkAndUpgradeUserLevel() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        com.google.firebase.firestore.DocumentReference userRef =
                FirebaseFirestore.getInstance().collection("users").document(uid);

        Map<String, Object> hardClearUpdates = new HashMap<>();
        hardClearUpdates.put("subject_" + currentSubjectId + "_hard_clear", true);
        hardClearUpdates.put("cleared_hard_stage_" + currentSubjectId, true);

        userRef.update(hardClearUpdates)
                .addOnSuccessListener(aVoid -> {
                    userRef.get().addOnSuccessListener(documentSnapshot -> {
                        if (!documentSnapshot.exists()) return;

                        int hardClearCount = 0;
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

                        if (hardClearCount >= 5) {
                            if (!"고수".equals(currentLevel)) {
                                nextLevel = "고수";
                            }
                        } else if (hardClearCount >= 3) {
                            if ("하수".equals(currentLevel)) {
                                nextLevel = "중수";
                            }
                        }

                        if (!nextLevel.equals(currentLevel)) {
                            final String finalNextLevel = nextLevel;
                            userRef.update("level", nextLevel)
                                    .addOnSuccessListener(aVoid2 -> {
                                        myUserLevel = finalNextLevel;
                                        if (getContext() != null) {
                                            Toast.makeText(getContext(), "등급 상승! 이제부터 [" + finalNextLevel + "] 등급입니다! ", Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    });
                });
    }

    private void saveNextStageUnlockToFirebase() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
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
        TextView tvResultGold = dialogView.findViewById(R.id.tvResultGold);


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

        tvResultGold.setText("+" + earnedGold + "G 획득!");
        tvStatEarnedGold.setText(earnedGold + " 점");
        tvStatEarnedGold.setText(earnedGold + " 점");
        tvStatAccuracy.setText(String.format("%.1f", correctRate) + "%");
        tvStatSubject.setText(getSubjectName(currentSubjectId));
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

    // [두 코드 신호 체계 100% 보존 통합] 양쪽 LeftFragment 수신 신호 모두 동시 발송
    private void closeFragment() {
        Bundle resultBundle = new Bundle();
        resultBundle.putBoolean("refresh", true);
        getParentFragmentManager().setFragmentResult("quiz_result", resultBundle);

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
            case 1: return 1; // C언어
            case 2: return 4; // 객체지향프로그래밍
            case 3: return 7; // 자료구조
            case 4: return 5; // 운영체제
            case 5: return 3; // 알고리즘
            case 6: return 8; // 컴퓨터네트워크
            case 7: return 6; // 인공지능개론
            case 8: return 2; // 데이터과학
            default: return stageId;
        }
    }

    private String getSubjectName(int subjectId) {
        switch (subjectId) {
            case 1:
                return "C언어";
            case 2:
                return "객체지향";
            case 3:
                return "자료구조";
            case 4:
                return "운영체제";
            case 5:
                return "인공지능";
            default:
                return "과목 " + subjectId;
        }
    }
}