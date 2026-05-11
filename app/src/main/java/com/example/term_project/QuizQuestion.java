package com.example.term_project;

public class QuizQuestion {
    private int quizId;
    private String question;
    private String[] options;
    private int correctAnswerIndex;
    private String difficultyLevel;

    public QuizQuestion(
            int quizId,
            String question,
            String[] options,
            int correctAnswerIndex,
            String difficultyLevel
    ) {
        this.quizId = quizId;
        this.question = question;
        this.options = options;
        this.correctAnswerIndex = correctAnswerIndex;
        this.difficultyLevel = difficultyLevel;
    }

    public int getQuizId() {
        return quizId;
    }

    public String getQuestion() {
        return question;
    }

    public String[] getOptions() {
        return options;
    }

    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }

    public String getDifficultyLevel() {
        return difficultyLevel;
    }
}