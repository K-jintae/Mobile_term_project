package com.example.term_project;

import java.util.ArrayList;
import java.util.List;

public class QuizRepository {

    public List<QuizItem> getQuizList() {
        List<QuizItem> quizList = new ArrayList<>();

        quizList.add(new QuizItem(1, "개념 초급", "기초 개념 문제를 풀어볼 수 있어요", true));
        quizList.add(new QuizItem(2, "개념 중급", "조금 더 어려운 문제를 풀어볼 수 있어요", true));
        quizList.add(new QuizItem(3, "개념 고급", "심화 문제를 풀어볼 수 있어요", true));
        quizList.add(new QuizItem(4, "실전 문제 1", "개념 중급 클리어 후 해금", false));
        quizList.add(new QuizItem(5, "실전 문제 2", "개념 중급 클리어 후 해금", false));

        return quizList;
    }

    public QuizQuestion getQuestionByQuizId(int quizId) {
        if (quizId == 1) {
            return new QuizQuestion(
                    1,
                    "다음 중 운영체제의 역할로 가장 적절한 것은?",
                    new String[]{"하드웨어 제어", "그림 그리기", "영상 편집", "프린터 제조"},
                    0
            );
        } else if (quizId == 2) {
            return new QuizQuestion(
                    2,
                    "자료구조 중 FIFO 방식은 무엇인가?",
                    new String[]{"스택", "큐", "트리", "그래프"},
                    1
            );
        } else if (quizId == 3) {
            return new QuizQuestion(
                    3,
                    "컴파일러의 역할은 무엇인가?",
                    new String[]{"소스코드를 기계어로 번역", "인터넷 연결", "이미지 확대", "문서 출력"},
                    0
            );
        } else if (quizId == 4) {
            return new QuizQuestion(
                    4,
                    "다음 중 네트워크 계층에 해당하는 것은?",
                    new String[]{"Transport", "Network", "Session", "Presentation"},
                    1
            );
        } else {
            return new QuizQuestion(
                    5,
                    "다음 중 DBMS의 기능이 아닌 것은?",
                    new String[]{"데이터 저장", "데이터 관리", "질의 처리", "모니터 생산"},
                    3
            );
        }
    }
}