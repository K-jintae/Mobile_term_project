package com.example.term_project;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

// 퀴즈 선택 화면 Fragment
public class QuizSelectFragment extends Fragment {

    public QuizSelectFragment() {
        // 기본 생성자
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // fragment_quiz_select.xml 화면 연결
        View view = inflater.inflate(R.layout.fragment_left, container, false);

        // 1. 개념 초급 카드 클릭 이벤트
        LinearLayout cardQuiz1 = view.findViewById(R.id.cardQuiz1);
        if (cardQuiz1 != null) {
            cardQuiz1.setOnClickListener(v -> moveToQuizPlay(1));
        }

        // 2. 개념 중급 카드 클릭 이벤트
        LinearLayout cardQuiz2 = view.findViewById(R.id.cardQuiz2);
        if (cardQuiz2 != null) {
            cardQuiz2.setOnClickListener(v -> moveToQuizPlay(2));
        }

        // 3. 개념 고급 카드 클릭 이벤트
        LinearLayout cardQuiz3 = view.findViewById(R.id.cardQuiz3);
        if (cardQuiz3 != null) {
            cardQuiz3.setOnClickListener(v -> moveToQuizPlay(3));
        }

        // 4. 잠긴 문제 클릭 이벤트 (실전 문제 1)
        LinearLayout cardLocked1 = view.findViewById(R.id.cardLocked1);
        if (cardLocked1 != null) {
            cardLocked1.setOnClickListener(v ->
                    Toast.makeText(getContext(), "아직 잠겨있는 문제입니다.", Toast.LENGTH_SHORT).show()
            );
        }

        // 5. 잠긴 문제 클릭 이벤트 (실전 문제 2)
        LinearLayout cardLocked2 = view.findViewById(R.id.cardLocked2);
        if (cardLocked2 != null) {
            cardLocked2.setOnClickListener(v ->
                    Toast.makeText(getContext(), "아직 잠겨있는 문제입니다.", Toast.LENGTH_SHORT).show()
            );
        }

        return view;
    }

    // 문제 풀이 화면으로 이동하는 공통 메서드
    private void moveToQuizPlay(int quizId) {
        Bundle bundle = new Bundle();
        bundle.putInt("quiz_id", quizId);

        QuizPlayFragment fragment = new QuizPlayFragment();
        fragment.setArguments(bundle);

        // MainActivity의 openFragment 호출하여 화면 전환
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openFragment(fragment);
        }
    }
}
