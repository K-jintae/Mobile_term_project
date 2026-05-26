package com.example.term_project;

public class FriendItem {

    private String uid;
    private String name;

    /*
     * status 값
     * pending_none     : 아직 친구 아님, 추천 친구 상태
     * pending_sent     : 내가 친구 요청을 보낸 상태
     * pending_received : 내가 친구 요청을 받은 상태
     * confirmed        : 서로 친구 상태
     */
    private String status;

    private String reason;
    private String level;
    private boolean alreadyFriend;

    // Firebase Realtime Database 객체 변환용 빈 생성자
    public FriendItem() {
    }

    public FriendItem(String uid, String name, String status, String level) {
        this.uid = uid;
        this.name = name;
        this.status = status;
        this.level = normalizeLevel(level);
        this.alreadyFriend = "confirmed".equals(status);
    }

    public FriendItem(String uid, String name, String status) {
        this.uid = uid;
        this.name = name;
        this.status = status;
        this.level = "없음";
        this.alreadyFriend = "confirmed".equals(status);
    }

    public FriendItem(String uid, String name, String level, String reason, boolean alreadyFriend) {
        this.uid = uid;
        this.name = name;
        this.level = normalizeLevel(level);
        this.reason = reason;
        this.alreadyFriend = alreadyFriend;
        this.status = alreadyFriend ? "confirmed" : "pending_none";
    }

    private String normalizeLevel(String level) {
        if (level == null || level.trim().isEmpty()) {
            return "없음";
        }
        return level;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.alreadyFriend = "confirmed".equals(status);
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = normalizeLevel(level);
    }

    public boolean isAlreadyFriend() {
        return alreadyFriend;
    }

    public void setAlreadyFriend(boolean alreadyFriend) {
        this.alreadyFriend = alreadyFriend;
    }
}