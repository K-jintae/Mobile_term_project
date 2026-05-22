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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import android.util.Log;
import com.google.firebase.firestore.DocumentSnapshot;

import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

    private List<com.google.firebase.database.ValueEventListener> presenceListeners = new ArrayList<>();
    private List<DatabaseReference> presenceRefs = new ArrayList<>();

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

        removePresenceListeners();
        friendList.clear();
        myFriendsList.clear();

        db.collection("users").document(myUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String lv = documentSnapshot.getString("level");
                        myLevel = (lv != null) ? lv : "하";

                        // 새로운 Friends 컬렉션에서 내 친구 목록 가져오기
                        db.collection("Friends").document(myUid).get()
                                .addOnSuccessListener(friendDoc -> {
                                    if (friendDoc.exists() && friendDoc.getData() != null && !friendDoc.getData().isEmpty()) {
                                        Set<String> friends = friendDoc.getData().keySet();
                                        myFriendsList.addAll(friends);

                                        for (String friendUid : friends) {
                                            db.collection("users").document(friendUid).get()
                                                    .addOnSuccessListener(doc -> {
                                                        if (doc.exists()) {
                                                            String userId = doc.getString("userId");
                                                            if (userId == null) userId = doc.getString("name");
                                                            String flv = doc.getString("level");
                                                            String levelStr = (flv != null) ? flv : "하";

                                                            FriendItem item = new FriendItem(userId, levelStr, "내 친구 ○ 오프라인");
                                                            friendList.add(item);
                                                            int position = friendList.size() - 1;
                                                            adapter.notifyDataSetChanged();

                                                            // 실시간 상태 확인 (기존 코드와 동일)
                                                            DatabaseReference friendStatusRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                                                                    .getReference("/status/" + friendUid);

                                                            com.google.firebase.database.ValueEventListener listener = new com.google.firebase.database.ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                                                                    if (snapshot.exists()) {
                                                                        String status = snapshot.getValue(String.class);
                                                                        if ("online".equals(status)) {
                                                                            item.setReason("내 친구 ● 접속중");
                                                                        } else {
                                                                            item.setReason("내 친구 ○ 오프라인");
                                                                        }
                                                                        adapter.notifyItemChanged(position);
                                                                    }
                                                                }

                                                                @Override
                                                                public void onCancelled(com.google.firebase.database.DatabaseError error) {}
                                                            };
                                                            friendStatusRef.addValueEventListener(listener);
                                                            presenceRefs.add(friendStatusRef);
                                                            presenceListeners.add(listener);
                                                        }
                                                    });
                                        }
                                    } else {
                                        Toast.makeText(this, "등록된 친구가 없습니다. 추천을 받아보세요!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                });
    }

    private void removePresenceListeners(){
        for(int i = 0; i < presenceRefs.size(); i++){
            if (presenceRefs.get(i) != null && presenceListeners.get(i) !=null) {
                presenceRefs.get(i).removeEventListener(presenceListeners.get(i));
            }
        }
        presenceRefs.clear();
        presenceListeners.clear();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        removePresenceListeners();
    }
    private void loadFriendRecommendations() {
        friendList.clear();
        excludedUids.clear();
        myFriendsList.clear();
        adapter.notifyDataSetChanged();

        String myUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (myUid != null) {
            excludedUids.add(myUid);
        } else {
            return;
        }

        db.collection("users").document(myUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String lv = documentSnapshot.getString("level");
                        myLevel = (lv != null) ? lv : "하";

                        // 추천 친구를 위해 Friends 컬렉션 참조
                        db.collection("Friends").document(myUid).get()
                                .addOnSuccessListener(friendDoc -> {
                                    if (friendDoc.exists() && friendDoc.getData() != null && !friendDoc.getData().isEmpty()) {
                                        Set<String> friends = friendDoc.getData().keySet();
                                        myFriendsList.addAll(friends);
                                        excludedUids.addAll(friends);
                                        fetchFriendsOfFriends();
                                    } else {
                                        fetchRandomRecommendations();
                                    }
                                })
                                .addOnFailureListener(e -> fetchRandomRecommendations());
                    } else {
                        fetchRandomRecommendations();
                    }
                })
                .addOnFailureListener(e -> fetchRandomRecommendations());
    }

    private void fetchFriendsOfFriends() {
        if (myFriendsList.isEmpty()) {
            fetchRandomRecommendations();
            return;
        }

        final int[] remainingTasks = {myFriendsList.size()};

        for (String friendUid : myFriendsList) {
            db.collection("Friends").document(friendUid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && doc.getData() != null) {
                            Set<String> FofF = doc.getData().keySet();
                            for (String uid : FofF) {
                                if (!excludedUids.contains(uid)) {
                                    addRecommendedUserToList(uid, "1순위 (함께 아는 친구)");
                                    excludedUids.add(uid);
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

                        // Friends 컬렉션에 저장하도록 변경
                        Map<String, Object> friendData = new HashMap<>();
                        friendData.put(friendUid, true);

                        db.collection("Friends").document(myUid)
                                .set(friendData, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "친구 추가 완료!", Toast.LENGTH_SHORT).show();
                                    etFriendEmail.setText("");

                                    friendList.clear();
                                    myFriendsList.clear();
                                    excludedUids.clear();

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
