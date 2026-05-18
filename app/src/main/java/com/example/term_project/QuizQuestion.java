package com.example.term_project;

// 퀴즈 문제 1개를 저장하는 클래스
public class QuizQuestion {

    // 문제 고유 번호
    private int quizId;

    // 문제 내용
    private String question;

    // 보기 목록
    // 예: {"하드웨어 제어", "그림 그리기", "영상 편집", "프린터 제조"}
    private String[] options;

    // 정답 보기의 인덱스 번호
    // 예: 첫 번째 보기가 정답이면 0
    private int correctAnswerIndex;

    // 문제 난이도
    private String difficultyLevel;

    // QuizQuestion 객체를 만들 때 필요한 값들을 받음
    public QuizQuestion(int quizId, String question, String[] options, int correctAnswerIndex, String difficultyLevel) {
        this.quizId = quizId;
        this.question = question;
        this.options = options;
        this.correctAnswerIndex = correctAnswerIndex;
        this.difficultyLevel = difficultyLevel;
    }

    // 문제 번호 반환
    public int getQuizId() {
        return quizId;
    }

    // 문제 문장 반환
    public String getQuestion() {
        return question;
    }

    // 보기 배열 반환
    public String[] getOptions() {
        return options;
    }

    // 정답 인덱스 반환
    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }

    // 문제 난이도 반환
    public String getDifficultyLevel() { return difficultyLevel; }
}