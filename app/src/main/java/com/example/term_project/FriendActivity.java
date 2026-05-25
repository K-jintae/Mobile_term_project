package com.example.term_project;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class FriendActivity extends AppCompatActivity {

    private EditText etFriendEmail;
    private Button btnConfirm;
    private Button btnCancel;
    private Button btnGetRecommendations;
    private RecyclerView rvFriendList;

    private FriendAdapter adapter;
    private final List<FriendItem> friendList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private DatabaseReference realtimeDb;

    private BattleRequestManager battleRequestManager;

    private boolean isMyFriendMode = true;

    private final HashSet<String> excludedUids = new HashSet<>();
    private final List<String> myFriendsList = new ArrayList<>();

    private String myLevel = "중";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        realtimeDb = FirebaseDatabase.getInstance().getReference();

        battleRequestManager = new BattleRequestManager(this);

        bindViews();
        setupRecyclerView();
        setupClickEvents();

        loadMyRealFriends();

        battleRequestManager.listenIncomingBattleRequests();
        battleRequestManager.listenAcceptedOutgoingBattleRequests();
    }

    private void bindViews() {
        etFriendEmail = findViewById(R.id.etFriendEmail);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnCancel = findViewById(R.id.btnCancel);
        btnGetRecommendations = findViewById(R.id.btnGetRecommendations);
        rvFriendList = findViewById(R.id.rvFriendList);
    }

    private void setupRecyclerView() {
        adapter = new FriendAdapter(friendList, friendItem -> {
            battleRequestManager.showBattleRequestDialog(friendItem);
        });

        rvFriendList.setLayoutManager(new LinearLayoutManager(this));
        rvFriendList.setAdapter(adapter);
    }

    private void setupClickEvents() {
        btnConfirm.setOnClickListener(v -> addFriendById());

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }

        btnGetRecommendations.setOnClickListener(v -> toggleFriendMode());

        setMyFriendModeButton();
    }

    private String getMyUid() {
        if (auth.getCurrentUser() == null) {
            return null;
        }

        return auth.getCurrentUser().getUid();
    }

    private void toggleFriendMode() {
        if (isMyFriendMode) {
            isMyFriendMode = false;
            setRecommendationModeButton();
            loadFriendRecommendations();
        } else {
            isMyFriendMode = true;
            setMyFriendModeButton();
            loadMyRealFriends();
        }
    }

    private void setMyFriendModeButton() {
        btnGetRecommendations.setText("내 친구");
        btnGetRecommendations.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
        );
    }

    private void setRecommendationModeButton() {
        btnGetRecommendations.setText("추천 친구");
        btnGetRecommendations.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#F5EBE0"))
        );
    }

    private void clearFriendList() {
        friendList.clear();
        adapter.notifyDataSetChanged();
    }

    private void loadMyRealFriends() {
        String myUid = getMyUid();

        if (myUid == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        clearFriendList();
        myFriendsList.clear();

        db.collection("users").document(myUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "유저 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String level = documentSnapshot.getString("level");
                    myLevel = level != null ? level : "하";

                    List<String> friends = (List<String>) documentSnapshot.get("friends");

                    if (friends == null || friends.isEmpty()) {
                        Toast.makeText(this, "등록된 친구가 없습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    myFriendsList.addAll(friends);

                    for (String friendUid : friends) {
                        loadFriendItem(friendUid, true, "내 친구");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "친구 목록 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadFriendItem(String friendUid, boolean alreadyFriend, String reason) {
        db.collection("users").document(friendUid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        return;
                    }

                    String userId = doc.getString("userId");

                    if (userId == null || userId.trim().isEmpty()) {
                        userId = doc.getString("name");
                    }

                    if (userId == null || userId.trim().isEmpty()) {
                        userId = "unknown";
                    }

                    String level = doc.getString("level");

                    if (level == null || level.trim().isEmpty()) {
                        level = "하";
                    }

                    FriendItem item = new FriendItem(
                            friendUid,
                            userId,
                            level,
                            reason,
                            alreadyFriend,
                            false
                    );

                    friendList.add(item);
                    adapter.notifyDataSetChanged();

                    if (alreadyFriend) {
                        listenFriendOnlineStatus(friendUid, item);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FriendActivity", "친구 정보 로드 실패", e);
                });
    }

    private void listenFriendOnlineStatus(String friendUid, FriendItem item) {
        realtimeDb.child("status").child(friendUid)
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                        String status = snapshot.getValue(String.class);

                        boolean online = "online".equals(status);

                        item.setOnline(online);
                        item.setReason(online ? "내 친구 · 온라인" : "내 친구 · 오프라인");

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                        Log.e("FriendActivity", "온라인 상태 감지 실패", error.toException());
                    }
                });
    }

    private void addFriendById() {
        String inputId = etFriendEmail.getText().toString().trim();
        String myUid = getMyUid();

        if (myUid == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (inputId.isEmpty()) {
            Toast.makeText(this, "아이디를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .whereEqualTo("userId", inputId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "존재하지 않는 유저입니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String friendUid = queryDocumentSnapshots.getDocuments().get(0).getId();

                    if (friendUid.equals(myUid)) {
                        Toast.makeText(this, "자기 자신은 추가할 수 없습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("users").document(myUid)
                            .update("friends", FieldValue.arrayUnion(friendUid))
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "친구 추가 완료", Toast.LENGTH_SHORT).show();

                                etFriendEmail.setText("");

                                isMyFriendMode = true;
                                setMyFriendModeButton();
                                loadMyRealFriends();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "친구 추가 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "유저 검색 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadFriendRecommendations() {
        String myUid = getMyUid();

        if (myUid == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        clearFriendList();
        excludedUids.clear();
        excludedUids.add(myUid);

        db.collection("users").document(myUid).get()
                .addOnSuccessListener(myDoc -> {
                    if (!myDoc.exists()) {
                        fetchRandomRecommendations();
                        return;
                    }

                    String level = myDoc.getString("level");
                    myLevel = level != null ? level : "하";

                    List<String> friends = (List<String>) myDoc.get("friends");

                    if (friends != null) {
                        excludedUids.addAll(friends);
                    }

                    fetchLevelRecommendations();
                })
                .addOnFailureListener(e -> {
                    fetchRandomRecommendations();
                });
    }

    private void fetchLevelRecommendations() {
        db.collection("users")
                .whereEqualTo("level", myLevel)
                .limit(30)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String uid = doc.getId();

                        if (excludedUids.contains(uid)) {
                            continue;
                        }

                        excludedUids.add(uid);
                        loadFriendItem(uid, false, "비슷한 레벨의 추천 친구");
                    }

                    if (friendList.isEmpty()) {
                        fetchRandomRecommendations();
                    }
                })
                .addOnFailureListener(e -> {
                    fetchRandomRecommendations();
                });
    }

    private void fetchRandomRecommendations() {
        db.collection("users")
                .limit(40)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> docs = new ArrayList<>(queryDocumentSnapshots.getDocuments());
                    Collections.shuffle(docs);

                    int count = 0;

                    for (DocumentSnapshot doc : docs) {
                        String uid = doc.getId();

                        if (excludedUids.contains(uid)) {
                            continue;
                        }

                        excludedUids.add(uid);
                        loadFriendItem(uid, false, "추천 친구");

                        count++;

                        if (count >= 20) {
                            break;
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "추천 친구 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    @Override
    protected void onDestroy() {
        if (battleRequestManager != null) {
            battleRequestManager.dismissCurrentDialog();
        }

        super.onDestroy();
    }
}