package com.example.term_project;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import android.util.Log;
import com.google.firebase.firestore.DocumentSnapshot;

public class FriendActivity extends AppCompatActivity {
    private EditText etFriendEmail;
    private Button btnConfirm, btnCancel, btnGetRecommendations;
    private RecyclerView rvFriendList;
    private FriendAdapter adapter;
    private List<FriendItem> friendList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private Set<String> excludedUids = new HashSet<>();
    private List<String> myFriendsList = new ArrayList<>();
    private String myLevel = "중";

    private boolean isMyFriendMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etFriendEmail = findViewById(R.id.etFriendEmail);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnCancel = findViewById(R.id.btnCancel);
        btnGetRecommendations = findViewById(R.id.btnGetRecommendations);
        rvFriendList = findViewById(R.id.rvFriendList);

        adapter = new FriendAdapter(friendList);
        rvFriendList.setLayoutManager(new LinearLayoutManager(this));
        rvFriendList.setAdapter(adapter);

        btnConfirm.setOnClickListener(v -> addFriendById());

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }


        btnGetRecommendations.setOnClickListener(v -> {
            if (isMyFriendMode) {
                // 현재 '내 친구 목록'인 상태에서 눌렀으므로 ➔ 추천 친구 모드로 전환
                isMyFriendMode = false;
                btnGetRecommendations.setText("추천 친구"); // 버튼 글자 변경
                btnGetRecommendations.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F5EBE0"))); // 색상 하이라이트
                loadFriendRecommendations();
            } else {
                // 현재 '추천 친구 목록'인 상태에서 눌렀으므로 ➔ 내 친구 모드로 원상복구
                isMyFriendMode = true;
                btnGetRecommendations.setText("내 친구"); // 버튼 글자 변경
                btnGetRecommendations.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFFFFF"))); // 색상 기본화
                loadMyRealFriends();
            }
        });


        isMyFriendMode = true;
        btnGetRecommendations.setText("내 친구");
        btnGetRecommendations.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
        loadMyRealFriends();
    }

    private void loadMyRealFriends() {
        String myUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (myUid == null) return;

        friendList.clear();
        myFriendsList.clear();

        db.collection("users").document(myUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String lv = documentSnapshot.getString("level");
                        myLevel = (lv != null) ? lv : "하";

                        List<String> friends = (List<String>) documentSnapshot.get("friends");

                        if (friends != null && !friends.isEmpty()) {
                            myFriendsList.addAll(friends);

                            for (String friendUid : friends) {
                                db.collection("users").document(friendUid).get()
                                        .addOnSuccessListener(doc -> {
                                            if (doc.exists()) {
                                                String userId = doc.getString("userId");
                                                if (userId == null) userId = doc.getString("name");

                                                String flv = doc.getString("level");
                                                String levelStr = (flv != null) ? flv : "하";

                                                FriendItem item = new FriendItem(userId, levelStr, "내 친구");
                                                friendList.add(item);
                                                adapter.notifyDataSetChanged();
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(this, "등록된 친구가 없습니다. 추천을 받아보세요!", Toast.LENGTH_SHORT).show();
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void loadFriendRecommendations() {
        friendList.clear();
        excludedUids.clear();

        String myUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (myUid != null) {
            excludedUids.add(myUid);
        }

        if (myUid != null) {
            db.collection("users").document(myUid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            List<String> friends = (List<String>) documentSnapshot.get("friends");

                            if (friends != null && !friends.isEmpty()) {
                                myFriendsList = friends;
                                excludedUids.addAll(friends);
                                fetchFriendsOfFriends();
                            } else {
                                myFriendsList.clear();
                                fetchRandomRecommendations();
                            }
                        } else {
                            fetchRandomRecommendations();
                        }
                    })
                    .addOnFailureListener(e -> fetchRandomRecommendations());
        }
    }

    private void fetchFriendsOfFriends() {
        if (myFriendsList.isEmpty()) {
            fetchRandomRecommendations();
            return;
        }

        final int[] remainingTasks = {myFriendsList.size()};

        for (String friendUid : myFriendsList) {
            db.collection("users").document(friendUid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            List<String> FofF = (List<String>) doc.get("friends");
                            if (FofF != null) {
                                for (String uid : FofF) {
                                    if (!excludedUids.contains(uid)) {
                                        addRecommendedUserToList(uid, "1순위 (함께 아는 친구)");
                                        excludedUids.add(uid);
                                    }
                                }
                            }
                        }
                        checkAndProceed(remainingTasks);
                    })
                    .addOnFailureListener(e -> checkAndProceed(remainingTasks));
        }
    }

    private void checkAndProceed(int[] remainingTasks) {
        remainingTasks[0]--;
        if (remainingTasks[0] == 0) {
            fetchLevelRecommendations();
        }
    }

    private void fetchLevelRecommendations() {

        db.collection("users")
                .whereEqualTo("level", myLevel)
                .limit(40)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String uid = doc.getId();
                        if (!excludedUids.contains(uid)) {
                            addRecommendedUserToList(uid, "2순위 비슷한 레벨");
                            excludedUids.add(uid);
                        }
                    }
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
                        if (!excludedUids.contains(uid)) {
                            addRecommendedUserToList(uid, "추천친구");
                            excludedUids.add(uid);
                            count++;

                            if (count >= 40) {
                                break;
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("firebase", "랜덤 추천 로드 실패", e));
    }

    private void addRecommendedUserToList(String uid, String reason) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String userId = doc.getString("userId");
                        if (userId == null) {
                            userId = doc.getString("name");
                        }

                        String lv = doc.getString("level");
                        String levelStr = (lv != null) ? lv : "하";

                        FriendItem item = new FriendItem(userId, levelStr, reason);
                        friendList.add(item);
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void addFriendById() {
        String inputId = etFriendEmail.getText().toString().trim();
        if (inputId.isEmpty()) {
            Toast.makeText(this, "아이디를 입력해라", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) return;

        db.collection("users")
                .whereEqualTo("userId", inputId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String friendUid = queryDocumentSnapshots.getDocuments().get(0).getId();
                        String myUid = mAuth.getCurrentUser().getUid();

                        if (friendUid.equals(myUid)) {
                            Toast.makeText(this, "자기 자신은 추가할 수 없습니다", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        db.collection("users").document(myUid)
                                .update("friends", FieldValue.arrayUnion(friendUid))
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "친구 추가 완료!", Toast.LENGTH_SHORT).show();
                                    etFriendEmail.setText("");

                                    friendList.clear();
                                    myFriendsList.clear();
                                    excludedUids.clear();

                                    // 친구 추가 완료 후 상태를 내 친구 모드로 복귀 및 리프레시
                                    isMyFriendMode = true;
                                    btnGetRecommendations.setText("추천 친구");
                                    btnGetRecommendations.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
                                    loadMyRealFriends();
                                });
                    } else {
                        Toast.makeText(this, "존재하지 않는 유저입니다", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
