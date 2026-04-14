package com.example.term_project;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public class LeftFragment extends Fragment {

    // 파이어베이스에서 데이터를 가져와줄 창고
    private QuizRepository repository;

    public LeftFragment() {
        // 필수 생성자
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // UI
        View view = inflater.inflate(R.layout.fragment_left, container, false);

        // 내 골드 가져오기
        int tmpGold = 0;
        if (getActivity() instanceof MainActivity) {
            tmpGold = ((MainActivity) getActivity()).getGold();
        }
        final int myGold = tmpGold;

        // 화면에 있는 빈 카드와 글자칸(TextView)들을 미리 찾아둡니다.
        LinearLayout cardQuiz1 = view.findViewById(R.id.cardQuiz1);
        if (cardQuiz1 != null) cardQuiz1.setOnClickListener(v -> moveToQuizPlay(1));

        LinearLayout cardQuiz2 = view.findViewById(R.id.cardQuiz2);
        if (cardQuiz2 != null) cardQuiz2.setOnClickListener(v -> moveToQuizPlay(2));

        LinearLayout cardQuiz3 = view.findViewById(R.id.cardQuiz3);
        if (cardQuiz3 != null) cardQuiz3.setOnClickListener(v -> moveToQuizPlay(3));

        LinearLayout cardLocked1 = view.findViewById(R.id.cardLocked1);
        if (cardLocked1 != null) {
            if (myGold >= 30) {
                cardLocked1.setAlpha(1.0f);
                cardLocked1.setOnClickListener(v -> moveToQuizPlay(4));
            } else {
                cardLocked1.setAlpha(0.5f);
                cardLocked1.setOnClickListener(v -> Toast.makeText(getContext(), "30 골드가 필요합니다. (현재: " + myGold + ")", Toast.LENGTH_SHORT).show());
            }
        }

        LinearLayout cardLocked2 = view.findViewById(R.id.cardLocked2);
        if (cardLocked2 != null) {
            if (cardLocked2 != null) {
                if (myGold >= 100) {
                    cardLocked2.setAlpha(1.0f);
                    cardLocked2.setOnClickListener(v -> moveToQuizPlay(5));
                } else {
                    cardLocked2.setAlpha(0.5f);
                    cardLocked2.setOnClickListener(v -> Toast.makeText(getContext(), "100 골드가 필요합니다. (현재: " + myGold + ")", Toast.LENGTH_SHORT).show());
                }
            }
        }

        return view;
    }


    // 화면 이동 함수 클릭한 과목 번호를 들고 퀴즈 푸는 화면으로 넘어갑니다.
    private void moveToQuizPlay(int subjectId) {
        Bundle bundle = new Bundle();
        bundle.putInt("subject_id", subjectId);

        QuizPlayFragment fragment = new QuizPlayFragment();
        fragment.setArguments(bundle);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openFragment(fragment);
        }
    }
}