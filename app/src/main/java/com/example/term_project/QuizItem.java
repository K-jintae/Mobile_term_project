package com.example.term_project;

public class QuizItem {
    private int id;
    private String title;
    private String description;
    private boolean unlocked;

    public QuizItem(int id, String title, String description, boolean unlocked) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.unlocked = unlocked;
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

    public boolean isUnlocked() {
        return unlocked;
    }
}