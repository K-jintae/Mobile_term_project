package com.example.term_project;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import androidx.lifecycle.ViewModelProvider;
import androidx.activity.OnBackPressedCallback;
import android.app.Dialog;
import android.view.Window;
import androidx.appcompat.widget.AppCompatButton;

public class QuizPlayFragment extends Fragment {

    // 문제 출력용 뷰
    private TextView tvQuestion;
    private RadioGroup radioGroup;
    private RadioButton option1, option2, option3, option4;
    private Button btnSubmit;

    // 결과 집계용 변수
    private int totalSolvedCount = 0;
    private int correctCount = 0;
    private int earnedGold = 0;

    // 정답/오답 이펙트 관련 뷰
    private com.airbnb.lottie.LottieAnimationView lottieEffect;
    private View layoutResult;
    private TextView tvResultStatus;

    // 문제 로딩용 객체
    private QuizRepository repository;
    private QuizQuestion currentQuestion;

    // 현재 과목(스테이지) 번호
    private int currentSubjectId;

    // 현재 문제 번호
    private int currentQuestionId = 1;

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
        lottieEffect = view.findViewById(R.id.lottieEffect);
        layoutResult = view.findViewById(R.id.layoutResult);
        tvResultStatus = view.findViewById(R.id.tvResultStatus);
        quizClothesImage = view.findViewById(R.id.quizClothesImage);

        quizFaceImage = view.findViewById(R.id.quizFaceImage);
        quizHatImage = view.findViewById(R.id.quizHatImage);

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


        // Repository 생성
        repository = new QuizRepository();

        // 버튼 터치 애니메이션 적용
        applyPressAnimation(btnSubmit);

        // 라디오 버튼 선택 시 배경색 업데이트
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> updateOptionBackgrounds());

        // 전달받은 과목 번호 읽기
        if (getArguments() != null) {
            currentSubjectId = getArguments().getInt("subject_id");
            loadQuestion(currentSubjectId, currentQuestionId);
        }


        // 제출 버튼 클릭 시 정답 확인
        btnSubmit.setOnClickListener(v -> checkAnswer());

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


    /**
     * Firestore에서 문제를 불러온다.
     * 다음 문제가 없으면 퀴즈 종료로 처리한다.
     */
    private void loadQuestion(int subjectId, int questionId) {
        repository.getQuizQuestionFromFirestore(subjectId, questionId, new QuizRepository.OnQuestionFetchedListener() {

            @Override
            public void onSuccess(QuizQuestion question) {
                // Fragment가 현재 화면에 붙어있지 않으면 작업 중단
                if (!isAdded()) {
                    return;
                }

                // 현재 문제 저장 후 화면에 표시
                currentQuestion = question;
                bindQuestion(question);

                // 이전 문제에서 선택했던 답 및 배경색 초기화
                radioGroup.clearCheck();
                updateOptionBackgrounds();
            }

            @Override
            public void onFailure(Exception e) {
                // Fragment가 현재 화면에 붙어있지 않으면 작업 중단
                if (!isAdded()) {
                    return;
                }
                android.util.Log.e("QUIZ_DEBUG", "퀴즈 불러오기 실패 원인: " + e.getMessage());
                // 첫 문제부터 못 불러오면 해당 과목 문제 데이터가 없는 상태
                if (currentQuestionId == 1) {
                    tvQuestion.setText("문제를 찾을 수 없습니다.");
                    btnSubmit.setEnabled(false);
                    return;
                }

                // 첫 문제는 있었고, 다음 문제를 못 불러왔다는 것은
                // 현재 단계의 마지막 문제까지 모두 푼 상태라는 뜻
                // 따라서 단계 클리어 조건을 검사한 뒤 결과창을 띄운다.
                completeStageIfAllCorrect();
                showFinalResult();
            }
        });
    }

    /**
     * 문제와 보기를 화면에 표시한다.
     */
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

        if (selectedIndex == currentQuestion.getCorrectAnswerIndex()) {
            handleResult(true);
        } else {
            handleResult(false);
        }
    }

    /**
     * 정답/오답 결과를 처리한다.
     */
    private void handleResult(boolean isCorrect) {
        btnSubmit.setEnabled(false);
        totalSolvedCount++;

        if (isCorrect) {
            quizFaceImage.setImageResource(R.drawable.face_happy);

            correctCount++;
            int goldToAdd = calculateGold();
            earnedGold += goldToAdd;

            playEffect(R.raw.success, "정답입니다!\n+" + goldToAdd + "G", true);
        } else {
            quizFaceImage.setImageResource(R.drawable.face_sad);

            playEffect(R.raw.fail, "오답입니다!", true);
        }
    }

    /**
     * 이펙트를 재생한 뒤 다음 문제를 불러온다.
     */
    private void playEffect(int rawResId, String statusText, boolean moveNextQuestion) {
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
                    btnSubmit.setEnabled(true);

                    if (moveNextQuestion) {
                        currentQuestionId++;
                        loadQuestion(currentSubjectId, currentQuestionId);
                    }
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

    /**
     * 문제 난이도에 따라 지급할 골드를 계산한다.
     */
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

    /**
     * 정답률이 70% 이상인 경우 현재 단계를 클리어 처리하고 다음 단계를 해금한다.
     */
    private void completeStageIfAllCorrect() {
        // 문제를 하나도 안 풀었으면 종료
        if (totalSolvedCount == 0) {
            return;
        }

        // 정답률 계산 70%로 완화
        double correctRate = ((double) correctCount / totalSolvedCount) * 100;
        boolean isClear = (correctRate >= 70.0);

        if (!isClear) {
            return;
        }

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("quiz_progress", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();

        // 현재 단계 clear = 1
        editor.putInt("stage_" + currentSubjectId + "_clear", 1);
        // 다음 단계 before_clear = 현재 단계 clear 값
        editor.putInt("stage_" + (currentSubjectId + 1) + "_before_clear", 1);

        editor.apply();

        // firebase 서버에 다음 스테이지 해금 기록 저장
        com.google.firebase.auth.FirebaseAuth mAuth =
                com.google.firebase.auth.FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();

            // "unlocked_stage_X: true" 형태로 저장됨
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .update("unlocked_stage_" + (currentSubjectId + 1), true)
                    .addOnSuccessListener(aVoid -> {
                        // 서버 저장 성공 처리 (로그 등)
                    });
        }
    }

    /**
     * 퀴즈 결과 다이얼로그를 출력한다.
     */
    private void showFinalResult() {
        if (getContext() == null) {
            return;
        }

        // 획득 골드 반영
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).addGold(earnedGold);
            ((MainActivity) getActivity()).clearLongAbsenceStateAfterQuiz();
        }

        double correctRate = (totalSolvedCount > 0)
                ? ((double) correctCount / totalSolvedCount) * 100
                : 0;

        boolean isClear = (correctRate >= 70.0);

        String clearMessage;
        if (isClear) {
            clearMessage = "단계 클리어: 성공\n다음 단계가 해금되었습니다.\n\n";
        } else {
            clearMessage = "단계 클리어: 실패\n70% 이상 맞혀야 다음 단계가 해금됩니다.\n\n";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("퀴즈 결과")
                .setMessage(
                        clearMessage +
                                "총 푼 문제 수: " + totalSolvedCount + "문제\n" +
                                "틀린 문제 수: " + (totalSolvedCount - correctCount) + "문제\n" +
                                "맞춘 문제 수: " + correctCount + "문제\n" +
                                "획득 골드: " + earnedGold + "G"
                )
                .setCancelable(false)
                .setPositiveButton("확인", (dialog, which) -> closeFragment())
                .show();
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
     * 사용자가 선택한 보기에 따라 라디오 버튼의 배경을 업데이트한다.
     */
    private void updateOptionBackgrounds() {
        option1.setBackgroundResource(option1.isChecked()
                ? R.drawable.bg_quiz_option_selected
                : R.drawable.bg_message_box);

        option2.setBackgroundResource(option2.isChecked()
                ? R.drawable.bg_quiz_option_selected
                : R.drawable.bg_message_box);

        option3.setBackgroundResource(option3.isChecked()
                ? R.drawable.bg_quiz_option_selected
                : R.drawable.bg_message_box);

        option4.setBackgroundResource(option4.isChecked()
                ? R.drawable.bg_quiz_option_selected
                : R.drawable.bg_message_box);
    }

    /**
     * 버튼 터치 시 눌리는 듯한 시각적 효과(스케일 축소)를 적용한다.
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
}