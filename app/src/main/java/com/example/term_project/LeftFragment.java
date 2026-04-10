package com.example.term_project;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

public class LeftFragment extends Fragment {

    public LeftFragment() {
        // 필수 생성자
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // 1. fragment_quiz_select 레이아웃을 인플레이트(화면에 띄움)합니다.
        View view = inflater.inflate(R.layout.fragment_left, container, false);

        // 2. XML에 있는 각 카드(LinearLayout)의 ID를 찾아 클릭 리스너를 연결합니다.

        // 개념 초급
        LinearLayout cardQuiz1 = view.findViewById(R.id.cardQuiz1);
        if (cardQuiz1 != null) {
            cardQuiz1.setOnClickListener(v -> moveToQuizPlay(1));
        }

        // 개념 중급
        LinearLayout cardQuiz2 = view.findViewById(R.id.cardQuiz2);
        if (cardQuiz2 != null) {
            cardQuiz2.setOnClickListener(v -> moveToQuizPlay(2));
        }

        // 개념 고급
        LinearLayout cardQuiz3 = view.findViewById(R.id.cardQuiz3);
        if (cardQuiz3 != null) {
            cardQuiz3.setOnClickListener(v -> moveToQuizPlay(3));
        }

        // 잠긴 문제 1
        LinearLayout cardLocked1 = view.findViewById(R.id.cardLocked1);
        if (cardLocked1 != null) {
            cardLocked1.setOnClickListener(v ->
                    Toast.makeText(getContext(), "아직 잠겨있는 문제입니다.", Toast.LENGTH_SHORT).show());
        }

        // 잠긴 문제 2
        LinearLayout cardLocked2 = view.findViewById(R.id.cardLocked2);
        if (cardLocked2 != null) {
            cardLocked2.setOnClickListener(v ->
                    Toast.makeText(getContext(), "아직 잠겨있는 문제입니다.", Toast.LENGTH_SHORT).show());
        }

        return view;
    }

    // 퀴즈 플레이 화면으로 이동하는 메서드
    private void moveToQuizPlay(int quizId) {
        Bundle bundle = new Bundle();
        bundle.putInt("quiz_id", quizId);

        QuizPlayFragment fragment = new QuizPlayFragment();
        fragment.setArguments(bundle);

        // MainActivity에 미리 만들어둔 openFragment 함수를 호출하여 화면 전환
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openFragment(fragment);
        }
    }
}
