package com.example.term_project;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.os.Bundle;
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
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FriendActivity extends AppCompatActivity {

    private EditText etFriendEmail;
    private Button btnConfirm, btnCancel, btnGetRecommendations;
    private RecyclerView rvFriendList;
    private TextView tvMyUserId;

    private FriendAdapter adapter;
    private final List<FriendItem> friendList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final Set<String> excludedUids = new HashSet<>();
    private List<String> myFriendsList = new ArrayList<>();

    private String myLevel;
    private boolean isMyFriendMode = true;

    private final List<DatabaseReference> presenceRefs = new ArrayList<>();
    private final List<ValueEventListener> presenceListeners = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etFriendEmail = findViewById(R.id.etFriendEmail);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnCancel = findViewById(R.id.btnCancel);
        btnGetRecommendations = findViewById(R.id.btnGetRecommendations);
        rvFriendList = findViewById(R.id.rvFriendList);
        tvMyUserId = findViewById(R.id.tvMyUserId);

        LinearLayout layoutMyUserId = findViewById(R.id.layoutMyUserId);

        setupMyId(layoutMyUserId);
        setupRecycler();
        setupButtons();

        loadMyRealFriends();
    }

    private void setupRecycler() {
        adapter = new FriendAdapter(friendList);
        rvFriendList.setLayoutManager(new LinearLayoutManager(this));
        rvFriendList.setAdapter(adapter);
    }

    private void setupMyId(LinearLayout layout) {
        if (auth.getCurrentUser() == null) return;
        String myUid = auth.getCurrentUser().getUid();

        db.collection("users").document(myUid).get()
                .addOnSuccessListener(doc -> {
                    String myId = doc.getString("id");
                    if (myId == null) myId = doc.getString("name");
                    if (myId == null) return;

                    tvMyUserId.setText("내 아이디: " + myId);
                    tvMyUserId.setPaintFlags(tvMyUserId.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);

                    String finalMyId = myId;
                    layout.setOnClickListener(v -> {
                        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("id", finalMyId);
                        cm.setPrimaryClip(clip);
                        Toast.makeText(this, "아이디 복사 완료", Toast.LENGTH_SHORT).show();
                    });
                });
    }

    private void setupButtons() {
        btnConfirm.setOnClickListener(v -> addFriendById());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> finish());

        btnGetRecommendations.setOnClickListener(v -> {
            isMyFriendMode = !isMyFriendMode;
            friendList.clear();
            adapter.notifyDataSetChanged();

            if (isMyFriendMode) {
                btnGetRecommendations.setText("추천 친구");
                btnGetRecommendations.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
                loadMyRealFriends();
            } else {
                btnGetRecommendations.setText("내 친구");
                btnGetRecommendations.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F5EBE0")));
                loadFriendRecommendations();
            }
        });
    }

    private void loadMyRealFriends() {
        String myUid = auth.getCurrentUser().getUid();
        removeListeners();
        friendList.clear();
        myFriendsList.clear();
        adapter.notifyDataSetChanged();

        db.collection("users").document(myUid).get().addOnSuccessListener(doc -> {
            myLevel = doc.getString("level");

            db.collection("Friends").document(myUid).get().addOnSuccessListener(friendDoc -> {
                if (friendDoc.exists() && friendDoc.getData() != null && !friendDoc.getData().isEmpty()) {
                    Set<String> friends = friendDoc.getData().keySet();
                    myFriendsList.addAll(friends);

                    for (String uid : friends) {
                        loadFriendItem(uid);
                    }
                } else {
                    Toast.makeText(this, "등록된 친구가 없습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void loadFriendItem(String uid) {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (!isMyFriendMode || !doc.exists()) return;

            String name = doc.getString("name");
            if (name == null) name = doc.getString("id");
            String level = doc.getString("level");
            String levelStr = (level != null) ? level : "레벨 없음";

            // 5파라미터 FriendItem 사용!
            FriendItem item = new FriendItem(uid, name, levelStr, "내 친구 ○ 오프라인", true);
            friendList.add(item);
            adapter.notifyItemInserted(friendList.size() - 1);

            attachPresence(uid, item);
        });
    }

    private void loadFriendRecommendations() {
        friendList.clear();
        excludedUids.clear();
        myFriendsList.clear();
        adapter.notifyDataSetChanged();

        String myUid = auth.getCurrentUser().getUid();
        excludedUids.add(myUid);

        db.collection("users").document(myUid).get().addOnSuccessListener(doc -> {
            myLevel = doc.getString("level");

            db.collection("Friends").document(myUid).get().addOnSuccessListener(friendDoc -> {
                if (friendDoc.exists() && friendDoc.getData() != null) {
                    myFriendsList.addAll(friendDoc.getData().keySet());
                    excludedUids.addAll(myFriendsList);
                }
                fetchFriendsOfFriends();
            });
        });
    }

    private void fetchFriendsOfFriends() {
        if (myFriendsList.isEmpty()) {
            fetchLevelRecommendations();
            return;
        }

        final int[] left = {myFriendsList.size()};
        for (String uid : myFriendsList) {
            db.collection("Friends").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && doc.getData() != null) {
                            for (String fofUid : doc.getData().keySet()) {
                                addRecommendedUser(fofUid, "1순위 (함께 아는 친구)");
                            }
                        }
                        checkDone(left);
                    })
                    .addOnFailureListener(e -> checkDone(left));
        }
    }

    private void checkDone(int[] left) {
        left[0]--;
        if (left[0] == 0) {
            fetchLevelRecommendations();
        }
    }

    private void fetchLevelRecommendations() {
        String levelQuery = (myLevel != null) ? myLevel : "하";
        db.collection("users").whereEqualTo("level", levelQuery).limit(40).get()
                .addOnSuccessListener(q -> {
                    for (DocumentSnapshot doc : q) {
                        addRecommendedUser(doc.getId(), "2순위 비슷한 레벨");
                    }
                    fetchRandomRecommendations();
                });
    }

    private void fetchRandomRecommendations() {
        db.collection("users").limit(40).get()
                .addOnSuccessListener(q -> {
                    List<DocumentSnapshot> docs = new ArrayList<>(q.getDocuments());
                    Collections.shuffle(docs);
                    for (DocumentSnapshot doc : docs) {
                        addRecommendedUser(doc.getId(), "추천 친구");
                    }
                });
    }

    private void addRecommendedUser(String uid, String reason) {
        if (isMyFriendMode) return;
        String myUid = auth.getCurrentUser().getUid();

        // 자기자신이거나 이미 확인된 유저, 혹은 내 친구라면 스킵
        if (uid.equals(myUid) || excludedUids.contains(uid) || myFriendsList.contains(uid)) return;

        excludedUids.add(uid);

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (!doc.exists() || isMyFriendMode) return;
            String name = doc.getString("name");
            if (name == null) name = doc.getString("id");
            String level = doc.getString("level");
            String levelStr = (level != null) ? level : "레벨 없음";

            FriendItem item = new FriendItem(uid, name, levelStr, reason, false);
            friendList.add(item);
            adapter.notifyItemInserted(friendList.size() - 1);
        });
    }

    // 아이디 검색 및 Friends 독립 컬렉션 추가 로직
    private void addFriendById() {
        String input = etFriendEmail.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "아이디를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1차 검색: id 필드
        db.collection("users").whereEqualTo("id", input).get().addOnSuccessListener(q -> {
            if (!q.isEmpty()) {
                handleFriendAdd(q.getDocuments().get(0).getId());
            } else {
                // 2차 검색: name 필드
                db.collection("users").whereEqualTo("name", input).get().addOnSuccessListener(nameQ -> {
                    if (!nameQ.isEmpty()) {
                        handleFriendAdd(nameQ.getDocuments().get(0).getId());
                    } else {
                        Toast.makeText(this, "존재하지 않는 유저입니다", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void handleFriendAdd(String friendUid) {
        String myUid = auth.getCurrentUser().getUid();

        if (friendUid.equals(myUid) || myFriendsList.contains(friendUid)) {
            Toast.makeText(this, "자기 자신이나 이미 등록된 친구는 추가할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> friendData = new HashMap<>();
        friendData.put(friendUid, true);

        // 우리의 강력한 Friends 저장 방식 유지!
        db.collection("Friends").document(myUid).set(friendData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "친구 추가 완료!", Toast.LENGTH_SHORT).show();
                    etFriendEmail.setText("");
                    removeFromList(friendUid);
                });
    }

    private void removeFromList(String uid) {
        for (int i = 0; i < friendList.size(); i++) {
            if (friendList.get(i).getUid().equals(uid)) {
                friendList.remove(i);
                adapter.notifyItemRemoved(i);
                return;
            }
        }
    }

    // 실시간 상태 확인 리스너
    private void attachPresence(String uid, FriendItem item) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("/status/" + uid);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isMyFriendMode) return;
                String status = snapshot.getValue(String.class);

                item.setReason("online".equals(status) ? "내 친구 ● 접속중" : "내 친구 ○ 오프라인");
                int pos = friendList.indexOf(item);
                if (pos != -1) adapter.notifyItemChanged(pos);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        ref.addValueEventListener(listener);
        presenceRefs.add(ref);
        presenceListeners.add(listener);
    }

    private void removeListeners() {
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
        removeListeners();
    }
}