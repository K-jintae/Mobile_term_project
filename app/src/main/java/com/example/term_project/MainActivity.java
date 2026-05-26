package com.example.term_project;

import android.content.SharedPreferences;
import android.content.Intent;
import android.widget.Button;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.media.MediaPlayer;
import android.widget.Toast; // 추가: 피드백 메시지용

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull; // 추가: Firebase 리스너용
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener; // 추가: 실시간 DB 쿼리용
import com.google.firebase.database.DataSnapshot; // 추가
import com.google.firebase.database.DatabaseError; // 추가

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TextView tvPlayerName;
    private TextView tvGold;

    // activity_main.xml에 추가한 overlay용 컨테이너
    private View fragmentContainer;

    // 음악
    private MediaPlayer mediaPlayer;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // 추가: Realtime Database 인스턴스 전역 관리
    private FirebaseDatabase mRealtimeDatabase;

    //소리 제어
    private boolean isSoundOn = true;
    // 게임 재화(골드)
    private int gold = 1200;
    private static final String PREF_USER_STATE_PREFIX = "user_state_";
    private static final String KEY_LAST_LOGIN_TIME = "last_login_time";
    private static final String KEY_NEED_QUIZ_RECOVERY = "need_quiz_recovery";
    private static final long TWO_DAYS_MILLIS = 48L * 60L * 60L * 1000L;

    private String currentUid; // 전역 분리: 내 UID를 편리하게 재사용하기 위함

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        mRealtimeDatabase = FirebaseDatabase.getInstance(); // 실시간 DB 초기화

        // View 연결
        viewPager = findViewById(R.id.viewPager);
        tvPlayerName = findViewById(R.id.tvPlayerName);
        tvGold = findViewById(R.id.tvGold);
        fragmentContainer = findViewById(R.id.fragment_container);

        // ViewPager 설정
        viewPager.setAdapter(new ViewPagerAdapter(this));
        viewPager.setCurrentItem(1, false);

        checkLongAbsenceState();

        //배경음악 재생
        isSoundOn = loadSoundSetting();

        mediaPlayer = MediaPlayer.create(this, R.raw.chestnut_cookie);

        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            if (isSoundOn) {
                mediaPlayer.start();
            }
        }

        // 로그인 유저 설정 불러오기
        if (mAuth.getCurrentUser() != null) {
            currentUid = mAuth.getCurrentUser().getUid(); // 전역 변수에 UID 할당

            DatabaseReference myStatusRef = mRealtimeDatabase.getReference("/status/" + currentUid);
            DatabaseReference connectedRef = mRealtimeDatabase.getReference(".info/connected");

            connectedRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot){
                    boolean connected = snapshot.getValue(Boolean.class) != null && snapshot.getValue(Boolean.class);
                    if(connected){
                        myStatusRef.onDisconnect().setValue("offline");
                        myStatusRef.setValue("online");
                    }
                }

                @Override
                public void onCancelled(DatabaseError error){}
            });

            db.collection("users").document(currentUid).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    // 골드 로드
                    Long g = doc.getLong("gold");
                    this.gold = (g != null) ? g.intValue() : 0;
                    updateTopBar();

                    // 의상 정보 로드
                    CharacterViewModel viewModel = new ViewModelProvider(this).get(CharacterViewModel.class);

                    // 1. 모자 불러오기
                    String hatName = doc.getString("hat");
                    if (hatName != null && !hatName.isEmpty()) {
                        int hatResId = getResources().getIdentifier(hatName, "drawable", getPackageName());
                        if (hatResId != 0) viewModel.setHat(hatResId);
                    }

                    // 2. 옷 불러오기
                    String clothesName = doc.getString("clothes");
                    if (clothesName != null && !clothesName.isEmpty()) {
                        int clothesResId = getResources().getIdentifier(clothesName, "drawable", getPackageName());
                        if (clothesResId != 0) viewModel.setClothes(clothesResId);
                    }

                    // 3. 배경 불러오기
                    String interiorName = doc.getString("interior");
                    if (interiorName != null && !interiorName.isEmpty()) {
                        int interiorResId = getResources().getIdentifier(interiorName, "drawable", getPackageName());
                        if (interiorResId != 0) viewModel.setInterior(interiorResId);
                    }

                    updateTopBar();

                } else {
                    // 유저 데이터가 없으면 새로 생성
                    java.util.Map<String, Object> newUser = new java.util.HashMap<>();
                    newUser.put("gold", 100);
                    newUser.put("hat", "none");
                    newUser.put("clothes", "none");
                    newUser.put("background", "none");
                    newUser.put("friends", new java.util.ArrayList<String>());
                    db.collection("users").document(currentUid).set(newUser);
                }
            });

            DatabaseReference myRef = mRealtimeDatabase.getReference("test_message");
            myRef.setValue("realtime database 연결 성공!");
        }

        // 저장된 유저 닉네임 불러오기
        SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
        String nickname = prefs.getString("name", "기본닉네임");

        // 상단 정보 표시
        tvPlayerName.setText(nickname);
        updateTopBar();

        // 뒤로가기 처리
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                    getSupportFragmentManager().executePendingTransactions();

                    if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                        fragmentContainer.setVisibility(View.GONE);
                    }
                } else {
                    finish();
                }
            }
        });
    }

    public boolean isSoundOn() {
        return isSoundOn;
    }

    private void saveSoundSetting(boolean isOn) {
        getSharedPreferences("settings", MODE_PRIVATE)
                .edit()
                .putBoolean("sound", isOn)
                .apply();
    }

    private boolean loadSoundSetting() {
        return getSharedPreferences("settings", MODE_PRIVATE)
                .getBoolean("sound", true);
    }

    public void setSound(boolean isOn) {
        isSoundOn = isOn;
        saveSoundSetting(isOn);

        if (mediaPlayer == null) return;

        if (isOn) {
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        } else {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null && isSoundOn && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void updateUserGold(int amount) {
        this.gold += amount;
        if (currentUid != null) {
            db.collection("users").document(currentUid)
                    .update("gold", FieldValue.increment(amount));
        }
    }

    public void updateTopBar() {
        tvGold.setText(String.valueOf(gold));
    }

    public void addGold(int amount) {
        gold += amount;
        updateTopBar();
        if (currentUid != null) {
            db.collection("users").document(currentUid)
                    .update("gold", this.gold)
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("Firebase", "골드 저장 완료: " + this.gold);
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("Firebase", "골드 저장 실패", e);
                    });
        }
    }

    public boolean spendGold(int amount) {
        if (gold >= amount) {
            gold -= amount;
            updateTopBar();
            return true;
        }
        return false;
    }

    public int getGold() {
        return gold;
    }

    public void openFragment(Fragment fragment) {
        fragmentContainer.setVisibility(View.VISIBLE);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void updateEquippedItem(String category, String itemName) {
        if (currentUid != null) {
            db.collection("users").document(currentUid)
                    .update(category, itemName)
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("Firebase", category + " 저장 완료: " + itemName);
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("Firebase", "저장 실패", e);
                    });
        }
    }

    public void closeCurrentFragment() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            getSupportFragmentManager().executePendingTransactions();

            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                fragmentContainer.setVisibility(View.GONE);
            }
        } else {
            fragmentContainer.setVisibility(View.GONE);
        }
    }

    private SharedPreferences getCurrentUserStatePrefs() {
        String uid = (currentUid != null) ? currentUid : "guest";
        return getSharedPreferences(PREF_USER_STATE_PREFIX + uid, MODE_PRIVATE);
    }

    private void checkLongAbsenceState() {
        SharedPreferences prefs = getCurrentUserStatePrefs();

        long now = System.currentTimeMillis();
        long lastLoginTime = prefs.getLong(KEY_LAST_LOGIN_TIME, -1L);
        boolean needRecovery = prefs.getBoolean(KEY_NEED_QUIZ_RECOVERY, false);

        CharacterViewModel viewModel = new ViewModelProvider(this).get(CharacterViewModel.class);

        if (lastLoginTime == -1L) {
            prefs.edit()
                    .putLong(KEY_LAST_LOGIN_TIME, now)
                    .putBoolean(KEY_NEED_QUIZ_RECOVERY, false)
                    .apply();

            viewModel.setFace(R.drawable.face_default);
            return;
        }

        if (now - lastLoginTime >= TWO_DAYS_MILLIS) {
            needRecovery = true;
            prefs.edit().putBoolean(KEY_NEED_QUIZ_RECOVERY, true).apply();
        }

        if (needRecovery) {
            viewModel.setFace(R.drawable.face_sad);
        } else {
            viewModel.setFace(R.drawable.face_default);
        }

        prefs.edit().putLong(KEY_LAST_LOGIN_TIME, now).apply();
    }

    public boolean isNeedQuizRecovery() {
        return getCurrentUserStatePrefs().getBoolean(KEY_NEED_QUIZ_RECOVERY, false);
    }

    public void clearLongAbsenceStateAfterQuiz() {
        getCurrentUserStatePrefs()
                .edit()
                .putBoolean(KEY_NEED_QUIZ_RECOVERY, false)
                .putLong(KEY_LAST_LOGIN_TIME, System.currentTimeMillis())
                .apply();

        CharacterViewModel viewModel = new ViewModelProvider(this).get(CharacterViewModel.class);
        viewModel.setFace(R.drawable.face_default);
    }

    /*
     *  [추가된 기능] 쌍방 친구 상태 실시간 검증 로직
     * 특정 유저(targetUid)와 멀티 플레이, 공유 메모장 조회 등 핵심 기능을 수행하기 전 호출하는 방어 코드입니다.
     */
    public void checkFriendshipAndProceed(String targetUid) {
        if (currentUid == null) {
            Toast.makeText(this, "로그인이 필요한 서비스입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // FriendActivity에서 구현한 대칭형 패스 구조에 맞춰 Realtime Database 노드를 조회합니다.
        mRealtimeDatabase.getReference()
                .child("users").child(currentUid).child("friends").child(targetUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // FriendItem 모델 내부에 구현한 'status' 문자열값을 파싱합니다.
                            String status = snapshot.child("status").getValue(String.class);

                            if ("confirmed".equals(status)) {
                                // 쌍방 수락 상태가 확실히 확인 완료되었을 때만 핵심 연동 기능 진입!
                                openSharedFeature();
                            } else if ("pending_sent".equals(status)) {
                                Toast.makeText(MainActivity.this, "상대방이 아직 친구 요청을 수락하지 않았습니다.", Toast.LENGTH_SHORT).show();
                            } else if ("pending_received".equals(status)) {
                                Toast.makeText(MainActivity.this, "받은 친구 요청 수락을 먼저 진행해 주세요.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "친구 관계가 아닙니다.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MainActivity.this, "네트워크 오류가 발생했습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /*
     * [추가된 기능] 검증 통과 시 실행될 타겟 기능 진입점
     */
    private void openSharedFeature() {
        Toast.makeText(this, "쌍방 친구 인증 성공! 공유 기능을 시작합니다.", Toast.LENGTH_SHORT).show();
    }
}