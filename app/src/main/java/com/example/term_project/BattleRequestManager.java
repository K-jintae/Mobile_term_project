package com.example.term_project;

import android.app.Activity;
import android.content.Intent;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class BattleRequestManager {

    private final Activity activity;
    private AlertDialog currentDialog;
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final DatabaseReference realtimeDb;

    private static final HashSet<String> shownRequestIds = new HashSet<>();
    private static final HashSet<String> startedBattleIds = new HashSet<>();

    private static final int DEFAULT_BET_GOLD = 100;
    private static final int DEFAULT_SUBJECT_ID = 1;
    private static final String DEFAULT_DIFFICULTY = "상";

    private static final int QUESTION_COUNT = 10;
    private static final long ROUND_DURATION_MS = 10_000L;
    private static final long REVEAL_DURATION_MS = 2_000L;

    public BattleRequestManager(Activity activity) {
        this.activity = activity;
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        this.realtimeDb = FirebaseDatabase.getInstance().getReference();
    }

    private String getMyUid() {
        if (auth.getCurrentUser() == null) {
            return null;
        }

        return auth.getCurrentUser().getUid();
    }

    private String getMyDisplayName() {
        if (auth.getCurrentUser() != null && auth.getCurrentUser().getEmail() != null) {
            return auth.getCurrentUser().getEmail();
        }

        return "상대";
    }

    public void showBattleRequestDialog(FriendItem friendItem) {
        String myUid = getMyUid();

        if (myUid == null) {
            Toast.makeText(activity, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (friendItem == null) {
            Toast.makeText(activity, "친구 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!friendItem.isOnline()) {
            Toast.makeText(activity, "온라인 친구에게만 대전을 신청할 수 있습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        dismissCurrentDialog();

        View dialogView = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_battle_request, null, false);

        TextView tvTitle = dialogView.findViewById(R.id.tvBattleDialogTitle);
        TextView tvGold = dialogView.findViewById(R.id.tvBattleGold);

        TextView btnCancel = dialogView.findViewById(R.id.btnBattleCancel);
        TextView btnApply = dialogView.findViewById(R.id.btnBattleApply);

        String targetName = friendItem.getUserId();

        if (targetName == null || targetName.trim().isEmpty()) {
            targetName = friendItem.getName();
        }

        if (targetName == null || targetName.trim().isEmpty()) {
            targetName = "상대";
        }

        tvTitle.setText(targetName + "님에게 대전 신청");
        tvGold.setText("베팅 골드 기본값: " + DEFAULT_BET_GOLD);


        currentDialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dismissCurrentDialog());

        btnApply.setOnClickListener(v -> {
            BattleRequestInput input = new BattleRequestInput(
                    DEFAULT_BET_GOLD,
                    DEFAULT_SUBJECT_ID,
                    DEFAULT_DIFFICULTY
            );

            dismissCurrentDialog();
            createBattleRequest(friendItem, input);
        });

        currentDialog.setOnShowListener(dialog -> {
            if (currentDialog.getWindow() != null) {
                currentDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });

        currentDialog.show();
    }

    public void sendBattleRequest(FriendItem friendItem, int betGold, int subjectId, String difficulty) {
        String myUid = getMyUid();

        if (myUid == null) {
            Toast.makeText(activity, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (friendItem == null || friendItem.getUid() == null || friendItem.getUid().trim().isEmpty()) {
            Toast.makeText(activity, "친구 정보가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        BattleRequestInput input = new BattleRequestInput(
                betGold,
                subjectId,
                difficulty
        );

        createBattleRequest(friendItem, input);
    }

    private void createBattleRequest(FriendItem friendItem, BattleRequestInput input) {
        String myUid = getMyUid();

        if (myUid == null) {
            return;
        }

        realtimeDb.child("status").child(friendItem.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    String status = snapshot.getValue(String.class);

                    if (!"online".equals(status)) {
                        Toast.makeText(activity, "상대가 현재 오프라인입니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    checkMyGoldAndCreateRequest(myUid, friendItem, input);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(activity, "상대 접속 상태 확인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkMyGoldAndCreateRequest(
            String myUid,
            FriendItem friendItem,
            BattleRequestInput input
    ) {
        firestore.collection("users").document(myUid).get()
                .addOnSuccessListener(myDoc -> {
                    Long goldLong = myDoc.getLong("gold");
                    long myGold = goldLong != null ? goldLong : 0L;

                    if (myGold < input.betGold) {
                        Toast.makeText(activity, "보유 골드가 부족합니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveBattleRequest(myUid, friendItem, input);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(activity, "골드 확인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveBattleRequest(
            String myUid,
            FriendItem friendItem,
            BattleRequestInput input
    ) {
        String requestId = realtimeDb.child("battle_requests").push().getKey();

        if (requestId == null) {
            Toast.makeText(activity, "대전 신청 생성 실패", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("fromUid", myUid);
        request.put("toUid", friendItem.getUid());
        request.put("fromName", getMyDisplayName());
        request.put("toName", friendItem.getUserId());
        request.put("betGold", input.betGold);
        request.put("subjectId", input.subjectId);
        request.put("difficulty", input.difficulty);
        request.put("questionCount", QUESTION_COUNT);
        request.put("roundDurationMs", ROUND_DURATION_MS);
        request.put("revealDurationMs", REVEAL_DURATION_MS);
        request.put("status", "pending");
        request.put("createdAt", System.currentTimeMillis());

        realtimeDb.child("battle_requests").child(requestId)
                .setValue(request)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(activity, "대전 신청을 보냈습니다.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(activity, "대전 신청 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public void listenIncomingBattleRequests() {
        String myUid = getMyUid();

        if (myUid == null) {
            return;
        }

        realtimeDb.child("battle_requests")
                .orderByChild("toUid")
                .equalTo(myUid)
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                        for (DataSnapshot requestSnap : snapshot.getChildren()) {
                            String requestId = requestSnap.getKey();
                            String status = requestSnap.child("status").getValue(String.class);

                            if (requestId == null) {
                                continue;
                            }

                            if (!"pending".equals(status)) {
                                continue;
                            }

                            if (shownRequestIds.contains(requestId)) {
                                continue;
                            }

                            shownRequestIds.add(requestId);
                            showIncomingBattleDialog(requestId, requestSnap);
                        }
                    }

                    @Override
                    public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                        Toast.makeText(activity, "대전 신청 감지 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showIncomingBattleDialog(String requestId, DataSnapshot requestSnap) {
        String fromName = requestSnap.child("fromName").getValue(String.class);
        int betGold = getIntValue(requestSnap, "betGold", DEFAULT_BET_GOLD);
        int subjectId = getIntValue(requestSnap, "subjectId", DEFAULT_SUBJECT_ID);
        String difficulty = getStringValue(requestSnap, "difficulty", DEFAULT_DIFFICULTY);

        String message =
                "신청자: " + safeText(fromName, "상대") + "\n" +
                        "베팅 골드: " + betGold + "\n" +
                        "과목 번호: " + subjectId + "\n" +
                        "난이도: " + difficulty + "\n" +
                        "문제 수: 10개\n" +
                        "문제당 제한 시간: 10초";

        dismissCurrentDialog();

        currentDialog = new AlertDialog.Builder(activity)
                .setTitle("실시간 퀴즈 대전 신청")
                .setMessage(message)
                .setNegativeButton("거절", (dialog, which) -> rejectBattleRequest(requestId))
                .setPositiveButton("수락", (dialog, which) -> acceptBattleRequest(requestId, requestSnap))
                .create();

        currentDialog.show();
    }
    public void dismissCurrentDialog() {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }

        currentDialog = null;
    }
    private void rejectBattleRequest(String requestId) {
        realtimeDb.child("battle_requests")
                .child(requestId)
                .child("status")
                .setValue("rejected")
                .addOnSuccessListener(unused -> {
                    Toast.makeText(activity, "대전 신청을 거절했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    private void acceptBattleRequest(String requestId, DataSnapshot requestSnap) {
        String myUid = getMyUid();

        if (myUid == null) {
            return;
        }

        String fromUid = requestSnap.child("fromUid").getValue(String.class);
        String toUid = requestSnap.child("toUid").getValue(String.class);

        if (fromUid == null || toUid == null) {
            Toast.makeText(activity, "대전 신청 정보가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!myUid.equals(toUid)) {
            Toast.makeText(activity, "내게 온 신청이 아닙니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        BattleRoomInput roomInput = BattleRoomInput.fromSnapshot(requestSnap);

        checkRequesterOnlineAndLockGold(requestId, fromUid, toUid, roomInput);
    }

    private void checkRequesterOnlineAndLockGold(
            String requestId,
            String fromUid,
            String toUid,
            BattleRoomInput roomInput
    ) {
        realtimeDb.child("status").child(fromUid).get()
                .addOnSuccessListener(statusSnapshot -> {
                    String status = statusSnapshot.getValue(String.class);

                    if (!"online".equals(status)) {
                        Toast.makeText(activity, "신청자가 현재 오프라인입니다.", Toast.LENGTH_SHORT).show();

                        realtimeDb.child("battle_requests")
                                .child(requestId)
                                .child("status")
                                .setValue("cancelled");

                        return;
                    }

                    GoldManager.lockBetGold(fromUid, toUid, roomInput.betGold, new GoldManager.GoldCallback() {
                        @Override
                        public void onSuccess() {
                            createBattleRoom(requestId, fromUid, toUid, roomInput);
                        }

                        @Override
                        public void onFailure(String message) {
                            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

    private void createBattleRoom(
            String requestId,
            String playerA,
            String playerB,
            BattleRoomInput input
    ) {
        String battleId = realtimeDb.child("battle_rooms").push().getKey();

        if (battleId == null) {
            Toast.makeText(activity, "대전 방 생성 실패", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> room = new HashMap<>();
        room.put("battleId", battleId);
        room.put("requestId", requestId);

        room.put("playerA", playerA);
        room.put("playerB", playerB);
        room.put("hostUid", playerA);

        room.put("betGold", input.betGold);
        room.put("subjectId", input.subjectId);
        room.put("difficulty", input.difficulty);

        room.put("questionCount", input.questionCount);
        room.put("roundDurationMs", input.roundDurationMs);
        room.put("revealDurationMs", input.revealDurationMs);

        room.put("status", "loading");
        room.put("roundState", "loading");
        room.put("roundIndex", 0);

        room.put("createdAt", System.currentTimeMillis());
        room.put("goldSettled", false);

        Map<String, Object> scores = new HashMap<>();
        scores.put(playerA, 0);
        scores.put(playerB, 0);
        room.put("scores", scores);

        realtimeDb.child("battle_rooms")
                .child(battleId)
                .setValue(room)
                .addOnSuccessListener(unused -> {
                    markRequestAccepted(requestId, battleId);
                    startBattleQuizActivity(battleId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(activity, "대전 방 생성 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void markRequestAccepted(String requestId, String battleId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "accepted");
        updates.put("battleId", battleId);

        // 수락자는 createBattleRoom 성공 후 바로 BattleQuizActivity로 이동하므로 true
        updates.put("toEntered", true);

        // 신청자는 listenAcceptedOutgoingBattleRequests()에서 감지 후 들어가야 하므로 false
        updates.put("fromEntered", false);

        updates.put("acceptedAt", System.currentTimeMillis());

        realtimeDb.child("battle_requests")
                .child(requestId)
                .updateChildren(updates);
    }

    public void listenAcceptedOutgoingBattleRequests() {
        String myUid = getMyUid();
        if (myUid == null) {
            return;
        }

        realtimeDb.child("battle_requests")
                .orderByChild("fromUid")
                .equalTo(myUid)
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                        for (DataSnapshot requestSnap : snapshot.getChildren()) {
                            String requestId = requestSnap.getKey();
                            String status = requestSnap.child("status").getValue(String.class);
                            String battleId = requestSnap.child("battleId").getValue(String.class);
                            Boolean fromEntered = requestSnap.child("fromEntered").getValue(Boolean.class);

                            if (requestId == null) {
                                continue;
                            }

                            if (!"accepted".equals(status)) {
                                continue;
                            }

                            if (battleId == null || battleId.trim().isEmpty()) {
                                continue;
                            }

                            // 이미 신청자 쪽에서 한 번 퀴즈방에 들어간 요청이면 다시 실행하지 않음
                            if (Boolean.TRUE.equals(fromEntered)) {
                                continue;
                            }

                            if (startedBattleIds.contains(battleId)) {
                                continue;
                            }

                            startedBattleIds.add(battleId);

                            // 먼저 fromEntered를 true로 찍고 이동해야
                            // 앱 재진입 시 같은 accepted 요청으로 퀴즈가 다시 열리지 않음
                            realtimeDb.child("battle_requests")
                                    .child(requestId)
                                    .child("fromEntered")
                                    .setValue(true)
                                    .addOnSuccessListener(unused -> {
                                        startBattleQuizActivity(battleId);
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                        Toast.makeText(activity, "대전 수락 감지 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startBattleQuizActivity(String battleId) {
        Intent intent = new Intent(activity, BattleQuizActivity.class);
        intent.putExtra("battleId", battleId);
        activity.startActivity(intent);
    }

    private int getIntValue(DataSnapshot snapshot, String key, int defaultValue) {
        Long value = snapshot.child(key).getValue(Long.class);

        if (value == null) {
            return defaultValue;
        }

        return value.intValue();
    }

    private long getLongValue(DataSnapshot snapshot, String key, long defaultValue) {
        Long value = snapshot.child(key).getValue(Long.class);

        if (value == null) {
            return defaultValue;
        }

        return value;
    }

    private String getStringValue(DataSnapshot snapshot, String key, String defaultValue) {
        String value = snapshot.child(key).getValue(String.class);

        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value;
    }

    private String safeText(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value;
    }

    private static class BattleRequestInput {
        int betGold;
        int subjectId;
        String difficulty;

        BattleRequestInput(int betGold, int subjectId, String difficulty) {
            this.betGold = betGold;
            this.subjectId = subjectId;
            this.difficulty = difficulty;
        }
    }

    private static class BattleRoomInput {
        int betGold;
        int subjectId;
        String difficulty;
        int questionCount;
        long roundDurationMs;
        long revealDurationMs;

        static BattleRoomInput fromSnapshot(DataSnapshot snapshot) {
            BattleRoomInput input = new BattleRoomInput();

            Long betGoldLong = snapshot.child("betGold").getValue(Long.class);
            Long subjectIdLong = snapshot.child("subjectId").getValue(Long.class);
            Long questionCountLong = snapshot.child("questionCount").getValue(Long.class);
            Long roundDurationLong = snapshot.child("roundDurationMs").getValue(Long.class);
            Long revealDurationLong = snapshot.child("revealDurationMs").getValue(Long.class);
            String difficultyText = snapshot.child("difficulty").getValue(String.class);

            input.betGold = betGoldLong != null ? betGoldLong.intValue() : DEFAULT_BET_GOLD;
            input.subjectId = subjectIdLong != null ? subjectIdLong.intValue() : DEFAULT_SUBJECT_ID;
            input.difficulty = difficultyText != null ? difficultyText : DEFAULT_DIFFICULTY;
            input.questionCount = questionCountLong != null ? questionCountLong.intValue() : QUESTION_COUNT;
            input.roundDurationMs = roundDurationLong != null ? roundDurationLong : ROUND_DURATION_MS;
            input.revealDurationMs = revealDurationLong != null ? revealDurationLong : REVEAL_DURATION_MS;

            return input;
        }
    }
}