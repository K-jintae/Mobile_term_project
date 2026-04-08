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

// 퀴즈 선택 화면 Fragment
public class QuizSelectFragment extends Fragment {

    // 퀴즈 카드들을 담아둘 레이아웃
    private LinearLayout quizContainer;

    // 퀴즈 데이터를 가져오는 객체
    // 지금은 가짜 데이터를 반환하지만, 나중에 DB 연결 시 여기만 수정하면 됨
    private QuizRepository repository;

    public QuizSelectFragment() {
        // 기본 생성자
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // fragment_quiz_select.xml 화면 연결
        View view = inflater.inflate(R.layout.fragment_quiz_select, container, false);

        // XML에 있는 quizContainer 연결
        quizContainer = view.findViewById(R.id.quizContainer);

        // Repository 객체 생성
        repository = new QuizRepository();

        // 퀴즈 목록 화면에 추가
        loadQuizList();

        return view;
    }

    // 퀴즈 목록을 불러와 카드 형태로 화면에 붙이는 함수
    private void loadQuizList() {

        // Repository에서 퀴즈 목록 가져오기
        List<QuizItem> quizList = repository.getQuizList();

        // 퀴즈 목록을 하나씩 꺼내서 카드 생성
        for (QuizItem item : quizList) {

            // item_quiz_card.xml을 카드 1개로 inflate
            View card = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_quiz_card, quizContainer, false);

            // 카드 안에 있는 제목, 설명, 잠금 표시 TextView 연결
            TextView tvTitle = card.findViewById(R.id.tvQuizTitle);
            TextView tvDesc = card.findViewById(R.id.tvQuizDesc);
            TextView tvLock = card.findViewById(R.id.tvLock);

            // 카드에 퀴즈 제목/설명 표시
            tvTitle.setText(item.getTitle());
            tvDesc.setText(item.getDescription());

            // 퀴즈가 열려 있는 상태인지 확인
            if (item.isUnlocked()) {

                // 열린 문제면 잠금 표시 숨김
                tvLock.setVisibility(View.GONE);

                // 카드 클릭 시 문제 풀이 화면으로 이동
                card.setOnClickListener(v -> {

                    // 다음 Fragment로 넘길 데이터 준비
                    Bundle bundle = new Bundle();
                    bundle.putInt("quiz_id", item.getId());

                    // 문제 풀이 Fragment 생성
                    QuizPlayFragment fragment = new QuizPlayFragment();

                    // quiz_id 전달
                    fragment.setArguments(bundle);

                    // 현재 화면을 QuizPlayFragment로 교체
                    ((MainActivity) requireActivity()).openFragment(fragment);
                });

            } else {
                // 잠긴 문제면 잠금 표시 보이기
                tvLock.setVisibility(View.VISIBLE);

                // 클릭해도 문제로 이동하지 않고 안내 메시지만 출력
                card.setOnClickListener(v ->
                        Toast.makeText(getContext(), "아직 잠겨있는 문제입니다.", Toast.LENGTH_SHORT).show()
                );
            }

            // 만든 카드를 quizContainer에 추가
            quizContainer.addView(card);
        }
    }
}