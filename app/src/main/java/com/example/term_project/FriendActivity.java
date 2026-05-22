package com.example.term_project;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.firestore.*;

import java.util.*;

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

    // ===================== 내 아이디 =====================
    private void setupMyId(LinearLayout layout) {

        if (auth.getCurrentUser() == null) return;

        String myUid = auth.getCurrentUser().getUid();

        db.collection("users").document(myUid)
                .get()
                .addOnSuccessListener(doc -> {

                    String myId = doc.getString("id");
                    if (myId == null) return;

                    tvMyUserId.setText("내 아이디: " + myId);
                    tvMyUserId.setPaintFlags(
                            tvMyUserId.getPaintFlags()
                                    | android.graphics.Paint.UNDERLINE_TEXT_FLAG
                    );

                    layout.setOnClickListener(v -> {

                        ClipboardManager cm =
                                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

                        ClipData clip = ClipData.newPlainText("id", myId);
                        cm.setPrimaryClip(clip);

                        Toast.makeText(this,
                                "아이디 복사 완료", Toast.LENGTH_SHORT).show();
                    });
                });
    }

    // ===================== 버튼 =====================
    private void setupButtons() {

        btnConfirm.setOnClickListener(v -> addFriendById());

        btnCancel.setOnClickListener(v -> finish());

        btnGetRecommendations.setOnClickListener(v -> {

            isMyFriendMode = !isMyFriendMode;

            friendList.clear();
            adapter.notifyDataSetChanged();

            if (isMyFriendMode) {
                btnGetRecommendations.setText("추천 친구");
                loadMyRealFriends();
            } else {
                btnGetRecommendations.setText("내 친구");
                loadFriendRecommendations();
            }
        });
    }

    // ===================== 친구 목록 =====================
    private void loadMyRealFriends() {

        String myUid = auth.getCurrentUser().getUid();
        removeListeners();

        friendList.clear();
        adapter.notifyDataSetChanged();

        db.collection("users").document(myUid)
                .get()
                .addOnSuccessListener(doc -> {

                    myLevel = doc.getString("level");

                    List<String> friends =
                            (List<String>) doc.get("friends");

                    if (friends == null) return;

                    myFriendsList = new ArrayList<>(friends);

                    for (String uid : friends) {
                        loadFriendItem(uid);
                    }
                });
    }

    private void loadFriendItem(String uid) {

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!isMyFriendMode || !doc.exists()) return;

                    String name = doc.getString("name");
                    if (name == null) name = doc.getString("id");

                    String level = doc.getString("level");

                    FriendItem item = new FriendItem(
                            uid,
                            name,
                            level,
                            "내 친구",
                            true
                    );

                    friendList.add(item);
                    int pos = friendList.size() - 1;
                    adapter.notifyItemInserted(pos);

                    attachPresence(uid, item);
                });
    }

    // ===================== 추천 친구 =====================
    private void loadFriendRecommendations() {

        friendList.clear();
        excludedUids.clear();
        myFriendsList.clear();
        adapter.notifyDataSetChanged();

        String myUid = auth.getCurrentUser().getUid();
        excludedUids.add(myUid);

        db.collection("users")
                .document(myUid)
                .get()
                .addOnSuccessListener(doc -> {

                    myLevel = doc.getString("level");

                    List<String> friends =
                            (List<String>) doc.get("friends");

                    if (friends != null) {
                        myFriendsList = friends;
                        excludedUids.addAll(friends);
                    }

                    fetchFriendsOfFriends();
                });
    }

    // 1순위: 친구의 친구
    private void fetchFriendsOfFriends() {

        if (myFriendsList == null || myFriendsList.isEmpty()) {
            fetchLevelRecommendations();
            return;
        }

        final int[] left = {myFriendsList.size()};

        for (String uid : myFriendsList) {

            db.collection("users").document(uid)
                    .get()
                    .addOnSuccessListener(doc -> {

                        List<String> fof =
                                (List<String>) doc.get("friends");

                        if (fof != null) {

                            for (String u : fof) {
                                addRecommendedUser(u, "1순위 친구의 친구");
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

    // 2순위: 같은 레벨
    private void fetchLevelRecommendations() {

        db.collection("users")
                .whereEqualTo("level", myLevel)
                .limit(40)
                .get()
                .addOnSuccessListener(q -> {

                    for (DocumentSnapshot doc : q) {
                        addRecommendedUser(doc.getId(), "2순위 레벨");
                    }

                    fetchRandomRecommendations();
                });
    }

    // 3순위: 랜덤
    private void fetchRandomRecommendations() {

        db.collection("users")
                .limit(40)
                .get()
                .addOnSuccessListener(q -> {

                    List<DocumentSnapshot> docs =
                            new ArrayList<>(q.getDocuments());

                    Collections.shuffle(docs);

                    for (DocumentSnapshot doc : docs) {
                        addRecommendedUser(doc.getId(), "추천");
                    }
                });
    }

    // ===================== 추천 추가 핵심 =====================
    private void addRecommendedUser(String uid, String reason) {

        if (isMyFriendMode) return;

        String myUid = auth.getCurrentUser().getUid();

        if (uid.equals(myUid)) return;
        if (excludedUids.contains(uid)) return;

        db.collection("users").document(myUid)
                .get()
                .addOnSuccessListener(myDoc -> {

                    List<String> friends =
                            (List<String>) myDoc.get("friends");

                    if (friends != null && friends.contains(uid)) return;

                    excludedUids.add(uid);

                    db.collection("users").document(uid)
                            .get()
                            .addOnSuccessListener(doc -> {

                                if (!doc.exists() || isMyFriendMode) return;

                                String name = doc.getString("name");
                                if (name == null) name = doc.getString("id");

                                String level = doc.getString("level");

                                FriendItem item = new FriendItem(
                                        uid,
                                        name,
                                        level,
                                        reason,
                                        false
                                );

                                friendList.add(item);
                                adapter.notifyItemInserted(friendList.size() - 1);
                            });
                });
    }

    // ===================== 친구 추가 =====================
    private void addFriendById() {

        String input = etFriendEmail.getText().toString().trim();

        if (input.isEmpty()) return;

        String myUid = auth.getCurrentUser().getUid();

        db.collection("users")
                .whereEqualTo("id", input)
                .get()
                .addOnSuccessListener(q -> {

                    if (q.isEmpty()) {
                        Toast.makeText(this,
                                "존재하지 않는 아이디", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String friendUid = q.getDocuments().get(0).getId();

                    if (friendUid.equals(myUid)) return;

                    db.collection("users")
                            .document(myUid)
                            .update("friends",
                                    FieldValue.arrayUnion(friendUid))
                            .addOnSuccessListener(a -> {

                                Toast.makeText(this,
                                        "친구 추가 완료", Toast.LENGTH_SHORT).show();

                                removeFromList(friendUid);
                                etFriendEmail.setText("");
                            });
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

    // ===================== 온라인 상태 =====================
    private void attachPresence(String uid, FriendItem item) {

        DatabaseReference ref =
                FirebaseDatabase.getInstance().getReference("/status/" + uid);

        ValueEventListener listener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!isMyFriendMode) return;

                String status = snapshot.getValue(String.class);

                item.setReason(
                        "online".equals(status)
                                ? "● 접속중"
                                : "○ 오프라인"
                );

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
            presenceRefs.get(i)
                    .removeEventListener(presenceListeners.get(i));
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