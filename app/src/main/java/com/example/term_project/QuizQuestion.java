package com.example.term_project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class QuizQuestion {

    private int quizId;
    private String question;
    private String[] options;
    private int correctAnswerIndex;
    private String difficultyLevel;

    public QuizQuestion() {
        // Firebase 역직렬화용 기본 생성자
    }

    public QuizQuestion(int quizId, String question, String[] options, int correctAnswerIndex, String difficultyLevel) {
        this.quizId = quizId;
        this.question = question;
        this.options = options;
        this.correctAnswerIndex = correctAnswerIndex;
        this.difficultyLevel = difficultyLevel;
    }

    public int getQuizId() {
        return quizId;
    }

    public void setQuizId(int quizId) {
        this.quizId = quizId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String[] getOptions() {
        return options;
    }

    public void setOptions(String[] options) {
        this.options = options;
    }

    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }

    public void setCorrectAnswerIndex(int correctAnswerIndex) {
        this.correctAnswerIndex = correctAnswerIndex;
    }

    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    // Realtime Database 저장용
    // String[]를 그대로 저장하면 터지므로, 여기서만 ArrayList로 변환
    public Map<String, Object> toBattleMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("quizId", quizId);
        map.put("question", question);

        ArrayList<String> optionList = new ArrayList<>();
        if (options != null) {
            for (String option : options) {
                optionList.add(option);
            }
        }

        map.put("options", optionList);
        map.put("correctAnswerIndex", correctAnswerIndex);
        map.put("difficultyLevel", difficultyLevel);

        return map;
    }
}