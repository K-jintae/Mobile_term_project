package com.example.term_project;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class QuizDifficultyFragment extends Fragment {

    private int subjectId;

    // 난이도 이름
    private static final String EASY = "easy";
    private static final String NORMAL = "normal";
    private static final String HARD = "hard";

    // 난이도별 목표 점수
    private static final int EASY_TARGET_SCORE = 50;
    private static final int NORMAL_TARGET_SCORE = 80;
    private static final int HARD_TARGET_SCORE = 120;

    public QuizDifficultyFragment() {
        // 기본 생성자
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_quiz_difficulty, container, false);

        if (getArguments() != null) {
            subjectId = getArguments().getInt("subject_id");
        }

        Button btnEasy = view.findViewById(R.id.btnEasy);
        Button btnNormal = view.findViewById(R.id.btnNormal);
        Button btnHard = view.findViewById(R.id.btnHard);

        btnEasy.setOnClickListener(v ->
                moveToQuizPlay(EASY, EASY_TARGET_SCORE)
        );

        btnNormal.setOnClickListener(v ->
                moveToQuizPlay(NORMAL, NORMAL_TARGET_SCORE)
        );

        btnHard.setOnClickListener(v ->
                moveToQuizPlay(HARD, HARD_TARGET_SCORE)
        );

        return view;
    }

    private void moveToQuizPlay(String difficulty, int targetScore) {
        Bundle bundle = new Bundle();

        bundle.putInt("subject_id", subjectId);
        bundle.putString("difficulty", difficulty);
        bundle.putInt("target_score", targetScore);

        QuizPlayFragment fragment = new QuizPlayFragment();
        fragment.setArguments(bundle);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openFragment(fragment);
        }
    }
}