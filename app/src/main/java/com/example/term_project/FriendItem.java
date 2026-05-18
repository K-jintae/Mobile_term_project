package com.example.term_project;

public class FriendItem {
    private String userId;
    private String level;
    private String reason;

    public FriendItem(String userId, String level, String reason){
        this.userId = userId;
        this.level = level;
        this.reason = reason;
    }

    public String getUserId(){
        return userId;
    }
    public String getLevel(){
        return level;
    }
    public String getReason(){
        return reason;
    }
}

