package com.example.term_project;

public class FriendItem {

    private String uid;       // 친구 UID
    private String name;      // 화면 표시 이름
    private String userId;    // battle-quiz 호환용 표시 ID
    private String status;    // pending_sent, pending_received, confirmed, pending_none
    private String reason;    // 추천 사유 또는 온라인 상태
    private String level;     // 유저 레벨
    private boolean alreadyFriend;
    private boolean online;

    // Firebase Realtime Database 객체 변환용 빈 생성자
    public FriendItem() {
    }

    // main 기존 구조 호환용
    public FriendItem(String uid, String name, String status, String level) {
        this.uid = uid;
        this.name = safeName(name);
        this.userId = safeName(name);
        this.status = safeStatus(status);
        this.level = normalizeLevel(level);
        this.reason = "";
        this.alreadyFriend = "confirmed".equals(this.status);
        this.online = false;
    }

    // main 기존 구조 호환용
    public FriendItem(String uid, String name, String status) {
        this(uid, name, status, "없음");
    }

    // main 추천 친구 구조 호환용
    public FriendItem(String uid, String name, String level, String reason, boolean alreadyFriend) {
        this.uid = uid;
        this.name = safeName(name);
        this.userId = safeName(name);
        this.level = normalizeLevel(level);
        this.reason = reason == null ? "" : reason;
        this.alreadyFriend = alreadyFriend;
        this.status = alreadyFriend ? "confirmed" : "pending_none";
        this.online = false;
    }

    // battle-quiz 구조 호환용
    public FriendItem(String uid, String userId, String level, String reason, boolean alreadyFriend, boolean online) {
        this.uid = uid;
        this.name = safeName(userId);
        this.userId = safeName(userId);
        this.level = normalizeLevel(level);
        this.reason = reason == null ? "" : reason;
        this.alreadyFriend = alreadyFriend;
        this.status = alreadyFriend ? "confirmed" : "pending_none";
        this.online = online;
    }

    private String normalizeLevel(String level) {
        if (level == null || level.trim().isEmpty()) {
            return "없음";
        }
        return level;
    }

    private String safeName(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "사용자";
        }
        return value;
    }

    private String safeStatus(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "pending_none";
        }
        return value;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    // main 기존 코드 호환
    public String getName() {
        if (name == null || name.trim().isEmpty()) {
            return userId == null ? "사용자" : userId;
        }
        return name;
    }

    public void setName(String name) {
        this.name = safeName(name);
        this.userId = safeName(name);
    }

    // battle-quiz 기존 코드 호환
    public String getUserId() {
        if (userId == null || userId.trim().isEmpty()) {
            return getName();
        }
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = safeName(userId);
        this.name = safeName(userId);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = safeStatus(status);
        this.alreadyFriend = "confirmed".equals(this.status);
    }

    public String getReason() {
        return reason == null ? "" : reason;
    }

    public void setReason(String reason) {
        this.reason = reason == null ? "" : reason;
    }

    public String getLevel() {
        return normalizeLevel(level);
    }

    public void setLevel(String level) {
        this.level = normalizeLevel(level);
    }

    public boolean isAlreadyFriend() {
        return alreadyFriend || "confirmed".equals(status);
    }

    public void setAlreadyFriend(boolean alreadyFriend) {
        this.alreadyFriend = alreadyFriend;
        if (alreadyFriend) {
            this.status = "confirmed";
        }
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}