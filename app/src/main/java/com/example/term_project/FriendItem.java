package com.example.term_project;

public class FriendItem {
    private String uid;           // 친구의 고유 UID (Firebase 식별용)
    private String name;          // 화면에 표시할 이름
    private String status;        // 관계 상태 ("pending_sent": 보냄, "pending_received": 받음, "confirmed": 서로친구, "pending_none": 추천친구)
    private String reason;        // 추천 사유 또는 실시간 접속 상태 태그
    private String level;         // 유저의 난이도 레벨 (고수/중수/하수/없음)
    private boolean alreadyFriend; // 이미 친구이거나 관계가 정립된 유저인지 판별하는 플래그

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

    // 레벨 값이 비어있거나 null일 때 "없음"으로 안전하게 처리하는 메서드
    private String normalizeLevel(String level) {
        if (level == null || level.trim().isEmpty()) {
            return "없음";
        }
        return level;
    }

    // ==================== Getter & Setter 영역 ====================

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
        this.alreadyFriend = "confirmed".equals(status); // 상태가 바뀔 때 플래그도 동기화
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
        this.level = normalizeLevel(level); // 값을 세팅할 때도 안전화 로직 적용
    }

    public boolean isAlreadyFriend() {
        return alreadyFriend;
    }

    public void setAlreadyFriend(boolean alreadyFriend) {
        this.alreadyFriend = alreadyFriend;
    }
}