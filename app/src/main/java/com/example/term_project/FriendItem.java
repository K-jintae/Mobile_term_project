package com.example.term_project;

public class FriendItem {

    private String uid;
    private String userId;
    private String level;
    private String reason;
    private boolean alreadyFriend;
    private boolean online;

    public FriendItem(String uid, String userId, String level, String reason, boolean alreadyFriend, boolean online) {
        this.uid = uid;
        this.userId = userId;
        this.level = level;
        this.reason = reason;
        this.alreadyFriend = alreadyFriend;
        this.online = online;
    }

    public String getUid() {
        return uid;
    }

    public String getUserId() {
        return userId;
    }

    public String getLevel() {
        return level;
    }

    public String getReason() {
        return reason;
    }

    public boolean isAlreadyFriend() {
        return alreadyFriend;
    }

    public boolean isOnline() {
        return online;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}