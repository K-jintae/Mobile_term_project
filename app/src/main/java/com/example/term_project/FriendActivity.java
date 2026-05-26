package com.example.term_project;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

public class FriendActivity extends AppCompatActivity implements FriendAdapter.OnFriendActionListener {

    private RecyclerView recyclerView;
    private FriendAdapter adapter;
    private List<FriendItem> friendList;

    private DatabaseReference mDatabase;
    private FirebaseFirestore db;

    private String currentUid;
    private String currentName = "";
    private String myLevel = "중";       // 2순위: 오직 레벨테스트 결과로만 매칭하기 위한 변수

    private EditText editSearchEmail;
    private Button btnSearchFriend;
    private Button btnRecommendedFriends; // 추천 친구 (내 친구 토글 버튼 겸용)
    private TextView tvMyId;

    // 상태 관리를 위한 핵심 변수 삼총사
    private boolean isMyFriendMode = true; // 현재 내 친구를 보고 있는지 여부
    private Set<String> excludedUids = new HashSet<>(); // 나 자신 및 관계 있는 유저들 중복 방지 타겟 목록
    private List<com.google.firebase.database.ValueEventListener> presenceListeners = new ArrayList<>();
    private List<DatabaseReference> presenceRefs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend);

        // 초기화
        mDatabase = FirebaseDatabase.getInstance().getReference();
        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 뷰 연결
        editSearchEmail = findViewById(R.id.edit_search_email);
        btnSearchFriend = findViewById(R.id.btn_search_friend);
        btnRecommendedFriends = findViewById(R.id.btn_recommended_friends);
        tvMyId = findViewById(R.id.tv_my_id);
        recyclerView = findViewById(R.id.recycler_friends);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        friendList = new ArrayList<>();
        adapter = new FriendAdapter(friendList, this);
        recyclerView.setAdapter(adapter);

        // 내 정보 및 레벨 스펙 로드
        fetchMyIdentity();

        // 검색/추가 버튼 클릭 이벤트
        btnSearchFriend.setOnClickListener(v -> {
            String name = editSearchEmail.getText().toString().trim();
            if (!name.isEmpty()) {
                searchAndAddFriend(name);
            } else {
                Toast.makeText(FriendActivity.this, "검색할 이름을 입력해 주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        // 토글식 시스템 장착: 버튼 하나로 '내 친구'와 '추천 친구' 모드를 스위칭합니다.
        btnRecommendedFriends.setOnClickListener(v -> {
            if (isMyFriendMode) {
                // 내 친구 모드일 때 누르면 -> 추천 친구 알고리즘 가동
                isMyFriendMode = false;
                btnRecommendedFriends.setText("내 친구 목록 보기");
                btnRecommendedFriends.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F5EBE0"))); // 색상 전환 하이라이트
                recommendFriendsAlgorithm();
            } else {
                // 추천 친구 모드일 때 누르면 -> 다시 원래 내 친구 화면으로 원상 복구
                isMyFriendMode = true;
                btnRecommendedFriends.setText("추천 친구 찾기");
                btnRecommendedFriends.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFFFFF"))); // 기본 흰색
                loadFriendsList();
            }
        });

        // 첫 진입 시 내 진짜 친구 목록과 온/오프라인 상태를 디폴트로 띄워줍니다.
        loadFriendsList();
    }

    private void fetchMyIdentity() {
        SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
        String nickname = prefs.getString("name", null);

        if (nickname != null) {
            currentName = nickname;
            tvMyId.setText("내 아이디: " + currentName);
        }

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        if (currentName.isEmpty()) {
                            String name = documentSnapshot.getString("name");
                            currentName = (name != null) ? name : "기본닉네임";
                            tvMyId.setText("내 아이디: " + currentName);
                        }
                        // [수정] 골드 데이터를 긁어오던 코드를 제거하고, 오직 레벨 필드만 확보합니다.
                        String levelStr = documentSnapshot.getString("level");
                        myLevel = (levelStr != null) ? levelStr : "중";
                    }
                });
    }

    /*
     * [1모드] 내 친구 목록을 로드할 때도 파이어스토어와 크로스 체킹하여 진짜 난이도를 실시간 연동합니다.
     */
    private void loadFriendsList() {
        removePresenceListeners();
        friendList.clear();
        adapter.notifyDataSetChanged();

        mDatabase.child("users").child(currentUid).child("friends")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isMyFriendMode) return;

                        friendList.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            FriendItem item = dataSnapshot.getValue(FriendItem.class);
                            if (item != null) {
                                // [핵심] 리얼타임 디비에서 꺼내온 친구의 UID를 기반으로 파이어스토어에서 실시간 레벨을 동기화합니다.
                                String friendUid = item.getUid();

                                db.collection("users").document(friendUid).get()
                                        .addOnSuccessListener(doc -> {
                                            if (!isMyFriendMode) return; // 모드 스위칭 방어

                                            if (doc.exists()) {
                                                String lvl = doc.getString("level");
                                                String levelStr = (lvl != null && !lvl.isEmpty()) ? lvl : "없음";
                                                item.setLevel(levelStr); // 데이터 바구니에 진짜 레벨 세팅 완료!
                                            }

                                            // 레벨 셋팅이 완료된 직후 분기 처리를 진행하여 리사이클러뷰에 인계합니다.
                                            if ("confirmed".equals(item.getStatus())) {
                                                if (item.getReason() == null) {
                                                    item.setReason("내 친구 ○ 오프라인"); // 기본값 세팅
                                                }
                                                friendList.add(item);

                                                int position = friendList.size() - 1;

                                                // 해당 친구의 실시간 온/오프라인 노드 감시 센서 부착
                                                DatabaseReference friendStatusRef = FirebaseDatabase.getInstance().getReference("/status/" + item.getUid());
                                                com.google.firebase.database.ValueEventListener listener = new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot statusSnapshot) {
                                                        if (statusSnapshot.exists()) {
                                                            String status = statusSnapshot.getValue(String.class);
                                                            if ("online".equals(status)) {
                                                                item.setReason("● 접속중");
                                                            } else {
                                                                item.setReason("○ 오프라인");
                                                            }
                                                            adapter.notifyItemChanged(position);
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {}
                                                };

                                                friendStatusRef.addValueEventListener(listener);
                                                presenceRefs.add(friendStatusRef);
                                                presenceListeners.add(listener);
                                            } else {
                                                if ("pending_sent".equals(item.getStatus())) item.setReason("내가 보낸 요청");
                                                if ("pending_received".equals(item.getStatus())) item.setReason("나에게 온 요청");
                                                friendList.add(item);
                                            }
                                            adapter.notifyDataSetChanged();
                                        });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    /**
     * [2모드] 1순위(친구의 친구) -> 2순위(나와 난이도가 비슷한 레벨) -> 3순위(랜덤) 통합 추천 알고리즘 엔진
     */
    private void recommendFriendsAlgorithm() {
        removePresenceListeners();
        friendList.clear();
        excludedUids.clear();
        adapter.notifyDataSetChanged();

        excludedUids.add(currentUid);

        mDatabase.child("users").child(currentUid).child("friends")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot myFriendsSnapshot) {
                        List<String> myRealFriends = new ArrayList<>();

                        for (DataSnapshot ds : myFriendsSnapshot.getChildren()) {
                            String uid = ds.getKey();
                            String status = ds.child("status").getValue(String.class);
                            excludedUids.add(uid);

                            if ("confirmed".equals(status)) {
                                myRealFriends.add(uid);
                            }
                        }

                        // 👥 1순위 알고리즘 가동
                        findFriendsOfFriends(myRealFriends);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // [1순위] 내 친구의 친구들을 추적
    private void findFriendsOfFriends(List<String> myRealFriends) {
        if (myRealFriends.isEmpty()) {
            findSimilarLevelUsers();
            return;
        }

        final int[] latch = { myRealFriends.size() };
        final Set<String> firstPriorityUids = new HashSet<>();

        for (String friendUid : myRealFriends) {
            mDatabase.child("users").child(friendUid).child("friends")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                String fofUid = ds.getKey();
                                String status = ds.child("status").getValue(String.class);

                                if ("confirmed".equals(status) && !excludedUids.contains(fofUid)) {
                                    firstPriorityUids.add(fofUid);
                                }
                            }
                            latch[0]--;
                            if (latch[0] == 0) {
                                for (String uid : firstPriorityUids) {
                                    addRecommendedUserToList(uid, "함께 아는 친구입니다");
                                    excludedUids.add(uid);
                                }
                                // 이어서 2순위 가동
                                findSimilarLevelUsers();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            latch[0]--;
                            if (latch[0] == 0) findSimilarLevelUsers();
                        }
                    });
        }
    }

    // [2순위] 오직 'level'이 동일한 유저들만 20명 선별 추천
    private void findSimilarLevelUsers() {
        db.collection("users")
                .whereEqualTo("level", myLevel)
                .limit(20)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String userUid = doc.getId();
                        if (!excludedUids.contains(userUid)) {
                            addRecommendedUserToList(userUid, "추천친구");
                            excludedUids.add(userUid);
                        }
                    }
                    // 최종 보루인 3순위(랜덤 추천) 가동하여 남은 빈자리 채우기
                    findRandomUsers();
                })
                .addOnFailureListener(e -> findRandomUsers());
    }

    // [3순위] 전체 풀 무작위 셔플 랜덤 추천
    private void findRandomUsers() {
        db.collection("users")
                .limit(30)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> docs = new ArrayList<>(queryDocumentSnapshots.getDocuments());
                    Collections.shuffle(docs);

                    int count = 0;
                    for (DocumentSnapshot doc : docs) {
                        String userUid = doc.getId();
                        if (!excludedUids.contains(userUid)) {
                            addRecommendedUserToList(userUid, "추천친구");
                            excludedUids.add(userUid);
                            count++;
                            if (count >= 15) break;
                        }
                    }

                    if (friendList.isEmpty()) {
                        Toast.makeText(this, "현재 더 이상 추천할 수 있는 신규 유저가 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Log.e("FriendAlgo", "3순위 랜덤 쿼리 에러", e));
    }

    private void addRecommendedUserToList(String uid, String reason) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && !isMyFriendMode) {
                        String userId = doc.getString("userId");
                        if (userId == null) {
                            userId = doc.getString("name");
                        }

                        String lvl = doc.getString("level");
                        String levelStr = (lvl != null && !lvl.isEmpty()) ? lvl : "없음";

                        FriendItem item = new FriendItem(uid, userId, "pending_none", levelStr);
                        item.setReason(reason);

                        friendList.add(item);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> Log.e("FriendAlgo", "추천 유저 상세 정보 로드 실패", e));
    }

    private void removePresenceListeners() {
        for (int i = 0; i < presenceRefs.size(); i++) {
            if (presenceRefs.get(i) != null && presenceListeners.get(i) != null) {
                presenceRefs.get(i).removeEventListener(presenceListeners.get(i));
            }
        }
        presenceRefs.clear();
        presenceListeners.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removePresenceListeners();
    }

    private void searchAndAddFriend(String inputName) {
        db.collection("users").whereEqualTo("name", inputName).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            String targetUid = doc.getId();
                            String targetName = doc.getString("name");

                            if (targetUid.equals(currentUid)) {
                                Toast.makeText(FriendActivity.this, "자기 자신은 추가할 수 없습니다.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            sendFriendRequest(targetUid, targetName);
                        }
                    } else {
                        Toast.makeText(FriendActivity.this, "존재하지 않는 사용자입니다. (이름 확인)", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(FriendActivity.this, "검색 중 서버 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendFriendRequest(String targetUid, String targetName) {
        // 친구 데이터 구조 정합성을 위해 4인자 생성자에 빈 값 대신 기본값 매칭 처리 보완
        FriendItem sent = new FriendItem(targetUid, targetName, "pending_sent", "하");
        mDatabase.child("users").child(currentUid).child("friends").child(targetUid).setValue(sent);

        FriendItem received = new FriendItem(currentUid, currentName, "pending_received", myLevel);
        mDatabase.child("users").child(targetUid).child("friends").child(currentUid).setValue(received);

        Toast.makeText(this, targetName + "님에게 친구 요청을 보냈습니다.", Toast.LENGTH_SHORT).show();

        isMyFriendMode = true;
        btnRecommendedFriends.setText("추천 친구 찾기");
        btnRecommendedFriends.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
        loadFriendsList();
    }

    @Override
    public void onAccept(FriendItem item) {
        String targetUid = item.getUid();
        mDatabase.child("users").child(currentUid).child("friends").child(targetUid).child("status").setValue("confirmed");
        mDatabase.child("users").child(targetUid).child("friends").child(currentUid).child("status").setValue("confirmed");
        Toast.makeText(this, item.getName() + "님과 친구가 되었습니다!", Toast.LENGTH_SHORT).show();
        loadFriendsList();
    }

    @Override
    public void onReject(FriendItem item) {
        String targetUid = item.getUid();
        mDatabase.child("users").child(currentUid).child("friends").child(targetUid).removeValue();
        mDatabase.child("users").child(targetUid).child("friends").child(currentUid).removeValue();
        Toast.makeText(this, "요청을 거절했습니다.", Toast.LENGTH_SHORT).show();
        loadFriendsList();
    }

    @Override
    public void onAddFriendRequested(FriendItem item) {
        String targetUid = item.getUid();
        String targetName = item.getName();
        sendFriendRequest(targetUid, targetName);
    }
}