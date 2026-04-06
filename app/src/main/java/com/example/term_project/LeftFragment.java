package com.example.term_project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class LeftFragment extends Fragment {

    public LeftFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_left, container, false);

        // 클릭 가능한 퀴즈 카드 3개
        View cardQuiz1 = view.findViewById(R.id.cardQuiz1);
        View cardQuiz2 = view.findViewById(R.id.cardQuiz2);
        View cardQuiz3 = view.findViewById(R.id.cardQuiz3);

        // 잠긴 카드 2개
        View cardLocked1 = view.findViewById(R.id.cardLocked1);
        View cardLocked2 = view.findViewById(R.id.cardLocked2);

        // 열려 있는 카드 클릭
        cardQuiz1.setOnClickListener(v ->
                Toast.makeText(getActivity(), "초급 퀴즈 페이지로 이동", Toast.LENGTH_SHORT).show()
        );

        cardQuiz2.setOnClickListener(v ->
                Toast.makeText(getActivity(), "중급 퀴즈 페이지로 이동", Toast.LENGTH_SHORT).show()
        );

        cardQuiz3.setOnClickListener(v ->
                Toast.makeText(getActivity(), "고급 퀴즈 페이지로 이동", Toast.LENGTH_SHORT).show()
        );

        // 잠긴 카드 클릭
        cardLocked1.setOnClickListener(v ->
                Toast.makeText(getActivity(), "개념 중급을 풀어야 해금됩니다.", Toast.LENGTH_SHORT).show()
        );

        cardLocked2.setOnClickListener(v ->
                Toast.makeText(getActivity(), "개념 중급을 풀어야 해금됩니다.", Toast.LENGTH_SHORT).show()
        );

        return view;
    }
}