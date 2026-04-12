package com.example.term_project;

public class QuizItem {
    private int id;
    private String title;
    private String description;
    private int requiredGold;

    public QuizItem(int id, String title, String description, int requiredGold) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.requiredGold = requiredGold;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getRequiredGold() {
        return requiredGold;
    }
}