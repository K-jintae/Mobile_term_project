package com.example.term_project;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FriendActivity extends AppCompatActivity
        implements FriendAdapter.OnFriendActionListener,
        BattleRequestDialogFragment.OnBattleRequestConfirmListener {

    private EditText editSearchEmail;
    private Button btnSearchFriend;
    private Button btnRecommendedFriends;
    private RecyclerView recyclerFriends;
    private TextView tvMyId;
    private LinearLayout layoutMyUserId;
    private ImageButton btnCopyId;

    private FriendAdapter adapter;
    private final List<FriendItem> friendList = new ArrayList<>();

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private DatabaseReference realtimeDb;
    private BattleRequestManager battleRequestManager;

    private String currentUid;
    private String currentName = "";
    private String myLevel = "레벨 없음";

    private boolean isMyFriendMode = true;

    private final Set<String> excludedUids = new HashSet<>();
    private final List<DatabaseReference> presenceRefs = new ArrayList<>();
    private final List<ValueEventListener> presenceListeners = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        realtimeDb = FirebaseDatabase.getInstance().getReference();

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUid = auth.getCurrentUser().getUid();
        battleRequestManager = new BattleRequestManager(this);

        bindViews();
        setupRecycler();
        setupButtons();
        fetchMyIdentity();
        loadFriendsList();

        battleRequestManager.listenIncomingBattleRequests();
        battleRequestManager.listenAcceptedOutgoingBattleRequests();
    }

    @Override
    public void onBattleRequestConfirm(FriendItem targetFriend, int betGold, int subjectNo, String difficulty) {
        if (targetFriend == null || targetFriend.getUid() == null || targetFriend.getUid().isEmpty()) {
            Toast.makeText(this, "대전 상대 정보가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (battleRequestManager == null) {
            battleRequestManager = new BattleRequestManager(this);
        }

        battleRequestManager.sendBattleRequest(
                targetFriend,
                betGold,
                subjectNo,
                difficulty
        );
    }

    private void bindViews() {
        editSearchEmail = findViewById(R.id.edit_search_email);
        btnSearchFriend = findViewById(R.id.btn_search_friend);
        btnRecommendedFriends = findViewById(R.id.btn_recommended_friends);
        recyclerFriends = findViewById(R.id.recycler_friends);
        tvMyId = findViewById(R.id.tv_my_id);
        layoutMyUserId = findViewById(R.id.layout_my_user_id);
        btnCopyId = findViewById(R.id.btn_copy_id);
    }

    private void setupRecycler() {
        adapter = new FriendAdapter(
                friendList,
                this,
                this::showBattleRequestDialog
        );

        recyclerFriends.setLayoutManager(new LinearLayoutManager(this));
        recyclerFriends.setAdapter(adapter);
    }

    private void showBattleRequestDialog(FriendItem friendItem) {
        BattleRequestDialogFragment dialog =
                BattleRequestDialogFragment.newInstance(friendItem);

        dialog.show(getSupportFragmentManager(), "BattleRequestDialog");
    }

    private void setupButtons() {
        btnSearchFriend.setOnClickListener(v -> {
            String input = editSearchEmail.getText().toString().trim();

            if (input.isEmpty()) {
                Toast.makeText(this, "검색할 닉네임을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            searchFriendAndShowResult(input);
        });

        btnRecommendedFriends.setOnClickListener(v -> {
            if (isMyFriendMode) {
                isMyFriendMode = false;
                btnRecommendedFriends.setText("내 친구 목록 보기");
                btnRecommendedFriends.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor("#F5EBE0"))
                );
                recommendFriendsAlgorithm();
            } else {
                isMyFriendMode = true;
                btnRecommendedFriends.setText("추천 친구 찾기");
                btnRecommendedFriends.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
                );
                loadFriendsList();
            }
        });
    }

    private void fetchMyIdentity() {
        firestore.collection("users")
                .document(currentUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        currentName = "사용자";
                        tvMyId.setText("내 아이디: " + currentName);
                        setupCopyMyId(currentName);
                        return;
                    }

                    String id = doc.getString("id");
                    String userId = doc.getString("userId");
                    String name = doc.getString("name");
                    String level = doc.getString("level");

                    if (id != null && !id.isEmpty()) {
                        currentName = id;
                    } else if (userId != null && !userId.isEmpty()) {
                        currentName = userId;
                    } else if (name != null && !name.isEmpty()) {
                        currentName = name;
                    } else {
                        currentName = "사용자";
                    }

                    if (level != null && !level.isEmpty()) {
                        myLevel = level;
                    }

                    tvMyId.setText("내 아이디: " + currentName);
                    setupCopyMyId(currentName);
                })
                .addOnFailureListener(e -> {
                    currentName = "사용자";
                    tvMyId.setText("내 아이디: " + currentName);
                    setupCopyMyId(currentName);
                });
    }

    private void setupCopyMyId(String myId) {
        if (layoutMyUserId == null || tvMyId == null || btnCopyId == null) {
            return;
        }

        tvMyId.setPaintFlags(tvMyId.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);

        View.OnClickListener copyListener = v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("my_id", myId);
            cm.setPrimaryClip(clip);

            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S_V2) {
                Toast.makeText(
                        FriendActivity.this,
                        "내 아이디가 복사되었습니다.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        };

        layoutMyUserId.setOnClickListener(copyListener);
        tvMyId.setOnClickListener(copyListener);
        btnCopyId.setOnClickListener(copyListener);
    }

    private void loadFriendsList() {
        removePresenceListeners();

        friendList.clear();
        adapter.notifyDataSetChanged();

        realtimeDb.child("users")
                .child(currentUid)
                .child("friends")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isMyFriendMode) {
                            return;
                        }

                        friendList.clear();

                        if (!snapshot.exists()) {
                            adapter.notifyDataSetChanged();
                            Toast.makeText(FriendActivity.this, "등록된 친구가 없습니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            FriendItem item = dataSnapshot.getValue(FriendItem.class);

                            if (item == null) {
                                continue;
                            }

                            if (item.getUid() == null || item.getUid().isEmpty()) {
                                item.setUid(dataSnapshot.getKey());
                            }

                            if (item.getUid() == null || item.getUid().isEmpty()) {
                                continue;
                            }

                            syncUserProfileAndAdd(item);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(FriendActivity.this, "친구 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void syncUserProfileAndAdd(FriendItem item) {
        firestore.collection("users")
                .document(item.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isMyFriendMode) {
                        return;
                    }

                    if (doc.exists()) {
                        String level = doc.getString("level");
                        String displayName = doc.getString("id");

                        if (displayName == null || displayName.isEmpty()) {
                            displayName = doc.getString("userId");
                        }

                        if (displayName == null || displayName.isEmpty()) {
                            displayName = doc.getString("name");
                        }

                        if (displayName != null && !displayName.isEmpty()) {
                            item.setName(displayName);
                        }

                        if (level != null && !level.isEmpty()) {
                            item.setLevel(level);
                        }
                    }

                    String status = item.getStatus();

                    if ("confirmed".equals(status)) {
                        item.setAlreadyFriend(true);

                        if (item.getReason() == null || item.getReason().isEmpty()) {
                            item.setReason("○ 오프라인");
                        }

                        friendList.add(item);
                        adapter.notifyItemInserted(friendList.size() - 1);
                        attachPresenceListener(item);

                    } else if ("pending_sent".equals(status)) {
                        item.setReason("내가 보낸 요청");
                        friendList.add(item);
                        adapter.notifyItemInserted(friendList.size() - 1);

                    } else if ("pending_received".equals(status)) {
                        item.setReason("나에게 온 요청");
                        friendList.add(item);
                        adapter.notifyItemInserted(friendList.size() - 1);
                    }
                });
    }

    private void attachPresenceListener(FriendItem item) {
        DatabaseReference statusRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("status")
                .child(item.getUid());

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isMyFriendMode) {
                    return;
                }

                String status = snapshot.getValue(String.class);
                boolean online = "online".equals(status);

                item.setOnline(online);
                item.setReason(online ? "● 접속중" : "○ 오프라인");

                int position = friendList.indexOf(item);
                if (position != -1) {
                    adapter.notifyItemChanged(position);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        statusRef.addValueEventListener(listener);
        presenceRefs.add(statusRef);
        presenceListeners.add(listener);
    }

    private void searchFriendAndShowResult(String input) {
        removePresenceListeners();

        isMyFriendMode = false;
        btnRecommendedFriends.setText("내 친구 목록 보기");
        btnRecommendedFriends.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#F5EBE0"))
        );

        friendList.clear();
        adapter.notifyDataSetChanged();

        String keyword = input.toLowerCase();

        firestore.collection("users")
                .limit(100)
                .get()
                .addOnSuccessListener(query -> {
                    friendList.clear();

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String targetUid = doc.getId();

                        if (targetUid.equals(currentUid)) {
                            continue;
                        }

                        String id = doc.getString("id");
                        String userId = doc.getString("userId");
                        String name = doc.getString("name");

                        boolean matched =
                                containsIgnoreCase(id, keyword)
                                        || containsIgnoreCase(userId, keyword)
                                        || containsIgnoreCase(name, keyword);

                        if (!matched) {
                            continue;
                        }

                        String displayName = firstNonEmpty(id, userId, name, "사용자");

                        String level = doc.getString("level");
                        if (level == null || level.isEmpty()) {
                            level = "없음";
                        }

                        FriendItem item = new FriendItem(targetUid, displayName, "pending_none", level);
                        item.setReason("검색 결과");

                        checkFriendStatusAndAddToSearchResult(item);
                    }

                    if (friendList.isEmpty()) {
                        Toast.makeText(this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "검색 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                );
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return "사용자";
    }

    private void checkFriendStatusAndAddToSearchResult(FriendItem item) {
        realtimeDb.child("users")
                .child(currentUid)
                .child("friends")
                .child(item.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isMyFriendMode) {
                            return;
                        }

                        if (snapshot.exists()) {
                            FriendItem savedItem = snapshot.getValue(FriendItem.class);

                            if (savedItem != null && savedItem.getStatus() != null) {
                                item.setStatus(savedItem.getStatus());

                                if ("pending_sent".equals(savedItem.getStatus())) {
                                    item.setReason("친구 요청 보냄");
                                } else if ("confirmed".equals(savedItem.getStatus())) {
                                    item.setReason("내 친구");
                                } else if ("pending_received".equals(savedItem.getStatus())) {
                                    item.setReason("나에게 온 요청");
                                }
                            }
                        }

                        friendList.add(item);
                        adapter.notifyItemInserted(friendList.size() - 1);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        friendList.add(item);
                        adapter.notifyItemInserted(friendList.size() - 1);
                    }
                });
    }

    private void handleFriendRequest(String targetUid, String targetName) {
        if (targetUid == null || targetUid.isEmpty()) {
            Toast.makeText(this, "친구 정보가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (targetUid.equals(currentUid)) {
            Toast.makeText(this, "자기 자신은 추가할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        realtimeDb.child("users")
                .child(currentUid)
                .child("friends")
                .child(targetUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            sendFriendRequest(targetUid, targetName);
                            return;
                        }

                        FriendItem item = snapshot.getValue(FriendItem.class);

                        if (item == null) {
                            sendFriendRequest(targetUid, targetName);
                            return;
                        }

                        String status = item.getStatus();

                        if ("confirmed".equals(status)) {
                            Toast.makeText(FriendActivity.this, "이미 친구입니다.", Toast.LENGTH_SHORT).show();
                        } else if ("pending_sent".equals(status)) {
                            Toast.makeText(FriendActivity.this, "이미 친구 요청을 보냈습니다.", Toast.LENGTH_SHORT).show();
                        } else if ("pending_received".equals(status)) {
                            Toast.makeText(FriendActivity.this, "이미 받은 요청입니다. 목록에서 수락할 수 있습니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            sendFriendRequest(targetUid, targetName);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(FriendActivity.this, "친구 요청 확인 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendFriendRequest(String targetUid, String targetName) {
        FriendItem sentItem = new FriendItem(targetUid, targetName, "pending_sent", "없음");
        sentItem.setReason("내가 보낸 요청");

        FriendItem receivedItem = new FriendItem(currentUid, currentName, "pending_received", myLevel);
        receivedItem.setReason("나에게 온 요청");

        realtimeDb.child("users")
                .child(currentUid)
                .child("friends")
                .child(targetUid)
                .setValue(sentItem);

        realtimeDb.child("users")
                .child(targetUid)
                .child("friends")
                .child(currentUid)
                .setValue(receivedItem)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            FriendActivity.this,
                            targetName + "님에게 친구 요청을 보냈습니다.",
                            Toast.LENGTH_SHORT
                    ).show();

                    editSearchEmail.setText("");

                    if (isMyFriendMode) {
                        loadFriendsList();
                    } else {
                        removeItemFromListByUid(targetUid);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "친구 요청 전송에 실패했습니다.", Toast.LENGTH_SHORT).show()
                );
    }

    private void recommendFriendsAlgorithm() {
        removePresenceListeners();

        friendList.clear();
        excludedUids.clear();
        adapter.notifyDataSetChanged();

        excludedUids.add(currentUid);

        realtimeDb.child("users")
                .child(currentUid)
                .child("friends")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> confirmedFriends = new ArrayList<>();

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String uid = ds.getKey();
                            String status = ds.child("status").getValue(String.class);

                            if (uid != null) {
                                excludedUids.add(uid);
                            }

                            if (uid != null && "confirmed".equals(status)) {
                                confirmedFriends.add(uid);
                            }
                        }

                        findFriendsOfFriends(confirmedFriends);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        findSimilarLevelUsers();
                    }
                });
    }

    private void findFriendsOfFriends(List<String> confirmedFriends) {
        if (confirmedFriends.isEmpty()) {
            findSimilarLevelUsers();
            return;
        }

        final int[] remain = {confirmedFriends.size()};
        final Set<String> firstPriorityUids = new HashSet<>();

        for (String friendUid : confirmedFriends) {
            realtimeDb.child("users")
                    .child(friendUid)
                    .child("friends")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                String fofUid = ds.getKey();
                                String status = ds.child("status").getValue(String.class);

                                if (fofUid != null
                                        && "confirmed".equals(status)
                                        && !excludedUids.contains(fofUid)) {
                                    firstPriorityUids.add(fofUid);
                                }
                            }

                            remain[0]--;

                            if (remain[0] == 0) {
                                for (String uid : firstPriorityUids) {
                                    excludedUids.add(uid);
                                    addRecommendedUserToList(uid, "함께 아는 친구");
                                }

                                findSimilarLevelUsers();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            remain[0]--;

                            if (remain[0] == 0) {
                                findSimilarLevelUsers();
                            }
                        }
                    });
        }
    }

    private void findSimilarLevelUsers() {
        firestore.collection("users")
                .whereEqualTo("level", myLevel)
                .limit(20)
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        String uid = doc.getId();

                        if (!excludedUids.contains(uid)) {
                            excludedUids.add(uid);
                            addRecommendedUserToList(uid, "추천 친구");
                        }
                    }

                    findRandomUsers();
                })
                .addOnFailureListener(e -> findRandomUsers());
    }

    private void findRandomUsers() {
        firestore.collection("users")
                .limit(40)
                .get()
                .addOnSuccessListener(query -> {
                    List<DocumentSnapshot> docs = new ArrayList<>(query.getDocuments());
                    Collections.shuffle(docs);

                    int count = 0;

                    for (DocumentSnapshot doc : docs) {
                        String uid = doc.getId();

                        if (!excludedUids.contains(uid)) {
                            excludedUids.add(uid);
                            addRecommendedUserToList(uid, "추천 친구");
                            count++;

                            if (count >= 15) {
                                break;
                            }
                        }
                    }

                    if (friendList.isEmpty()) {
                        Toast.makeText(this, "현재 추천할 수 있는 친구가 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addRecommendedUserToList(String uid, String reason) {
        if (isMyFriendMode) {
            return;
        }

        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists() || isMyFriendMode) {
                        return;
                    }

                    String name = doc.getString("id");

                    if (name == null || name.isEmpty()) {
                        name = doc.getString("userId");
                    }

                    if (name == null || name.isEmpty()) {
                        name = doc.getString("name");
                    }

                    if (name == null || name.isEmpty()) {
                        name = "사용자";
                    }

                    String level = doc.getString("level");

                    if (level == null || level.isEmpty()) {
                        level = "없음";
                    }

                    String finalName = name;
                    String finalLevel = level;

                    realtimeDb.child("users")
                            .child(currentUid)
                            .child("friends")
                            .child(uid)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    String status = "pending_none";

                                    if (snapshot.exists()) {
                                        FriendItem savedItem = snapshot.getValue(FriendItem.class);

                                        if (savedItem != null && savedItem.getStatus() != null) {
                                            status = savedItem.getStatus();
                                        }
                                    }

                                    FriendItem item = new FriendItem(uid, finalName, status, finalLevel);

                                    if ("pending_sent".equals(status)) {
                                        item.setReason("친구 요청 보냄");
                                    } else if ("pending_received".equals(status)) {
                                        item.setReason("나에게 온 요청");
                                    } else if ("confirmed".equals(status)) {
                                        item.setReason("내 친구");
                                    } else {
                                        item.setReason(reason);
                                    }

                                    friendList.add(item);
                                    adapter.notifyItemInserted(friendList.size() - 1);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    FriendItem item =
                                            new FriendItem(uid, finalName, "pending_none", finalLevel);

                                    item.setReason(reason);

                                    friendList.add(item);
                                    adapter.notifyItemInserted(friendList.size() - 1);
                                }
                            });
                });
    }

    @Override
    public void onAccept(FriendItem item) {
        String targetUid = item.getUid();

        realtimeDb.child("users")
                .child(currentUid)
                .child("friends")
                .child(targetUid)
                .child("status")
                .setValue("confirmed");

        realtimeDb.child("users")
                .child(targetUid)
                .child("friends")
                .child(currentUid)
                .child("status")
                .setValue("confirmed")
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            FriendActivity.this,
                            item.getName() + "님과 친구가 되었습니다.",
                            Toast.LENGTH_SHORT
                    ).show();
                    loadFriendsList();
                });
    }

    @Override
    public void onReject(FriendItem item) {
        String targetUid = item.getUid();
        String status = item.getStatus();

        realtimeDb.child("users")
                .child(currentUid)
                .child("friends")
                .child(targetUid)
                .removeValue();

        realtimeDb.child("users")
                .child(targetUid)
                .child("friends")
                .child(currentUid)
                .removeValue()
                .addOnSuccessListener(unused -> {
                    if ("pending_sent".equals(status)) {
                        Toast.makeText(
                                FriendActivity.this,
                                "친구 요청을 취소했습니다.",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else if ("pending_received".equals(status)) {
                        Toast.makeText(
                                FriendActivity.this,
                                "친구 요청을 거절했습니다.",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else {
                        Toast.makeText(
                                FriendActivity.this,
                                "친구가 삭제되었습니다.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    removeItemFromList(item);
                });
    }

    @Override
    public void onAddFriendRequested(FriendItem item) {
        handleFriendRequest(item.getUid(), item.getName());
    }

    private void removePresenceListeners() {
        for (int i = 0; i < presenceRefs.size(); i++) {
            DatabaseReference ref = presenceRefs.get(i);
            ValueEventListener listener = presenceListeners.get(i);

            if (ref != null && listener != null) {
                ref.removeEventListener(listener);
            }
        }

        presenceRefs.clear();
        presenceListeners.clear();
    }

    @Override
    protected void onDestroy() {
        if (battleRequestManager != null) {
            battleRequestManager.dismissCurrentDialog();
        }

        removePresenceListeners();
        super.onDestroy();
    }

    private void removeItemFromList(FriendItem item) {
        int position = friendList.indexOf(item);

        if (position != -1) {
            friendList.remove(position);
            adapter.notifyItemRemoved(position);
        }
    }

    private void removeItemFromListByUid(String uid) {
        if (uid == null) {
            return;
        }

        for (int i = 0; i < friendList.size(); i++) {
            if (uid.equals(friendList.get(i).getUid())) {
                friendList.remove(i);
                adapter.notifyItemRemoved(i);
                break;
            }
        }
    }
}