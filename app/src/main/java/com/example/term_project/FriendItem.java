package com.example.term_project;

public class FriendItem {
    private String uid;           // 친구의 고유 UID (Firebase 식별용)
    private String name;          // 화면에 표시할 이름
    private String status;        // 관계 상태 ("pending_sent": 보냄, "pending_received": 받음, "confirmed": 서로친구, "pending_none": 추천친구)
    private String reason;        // 추천 사유 또는 실시간 접속 상태 태그
    private String level;         // 유저의 난이도 레벨 (고수/중수/하수/없음)
    private boolean alreadyFriend; // 이미 친구이거나 관계가 정립된 유저인지 판별하는 플래그

    /*
     * [생성자 1] Firebase Realtime Database에서 데이터를 객체로 파싱할 때
     * 필수적으로 요구되는 빈 생성자 (삭제 금지)
     */
    public FriendItem() {
    }

    /*
     * [생성자 2] 기존 4인자 생성자 (내 친구 목록 로드 및 기본적인 연동용)
     */
    public FriendItem(String uid, String name, String status, String level) {
        this.uid = uid;
        this.name = name;
        this.status = status;
        this.level = (level != null && !level.isEmpty()) ? level : "없음";
        this.alreadyFriend = "confirmed".equals(status); // status 상태에 맞춰 친구 여부 플래그를 자동으로 세팅
    }

    /*
     * [생성자 3] 기존 3인자 생성자 (구형 코드 환경 및 친구 요청 발송 시스템 호환용)
     */
    public FriendItem(String uid, String name, String status){
        this.uid = uid;
        this.name = name;
        this.status = status;
        this.level = "없음";
        this.alreadyFriend = "confirmed".equals(status);
    }

    /*
     * [생성자 4] 추천 알고리즘 및 필터링 시 사용하는 5인자 생성자 (두 번째 코드 매칭)
     */
    public FriendItem(String uid, String name, String level, String reason, boolean alreadyFriend) {
        this.uid = uid;
        this.name = name;
        this.level = (level != null && !level.isEmpty()) ? level : "없음";
        this.reason = reason;
        this.alreadyFriend = alreadyFriend;
        // 추천친구인 경우 상호 관계에 따라 status를 알맞게 매칭
        this.status = alreadyFriend ? "confirmed" : "pending_none";
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
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
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

    public void setAlreadyFriend(boolean alreadyFriend) {
        this.alreadyFriend = alreadyFriend;
    }
}