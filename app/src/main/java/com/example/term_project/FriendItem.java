package com.example.term_project;

public class FriendItem {

    private String uid;
    private String name;
    private String level;
    private String reason;
    private boolean alreadyFriend;

    public FriendItem(String uid,
                      String name,
                      String level,
                      String reason,
                      boolean alreadyFriend) {

        this.uid = uid;
        this.name = name;
        this.level = level;
        this.reason = reason;
        this.alreadyFriend = alreadyFriend;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getLevel() {
        return level;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isAlreadyFriend() {
        return alreadyFriend;
    }
}