package com.example.term_project;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.media.MediaPlayer;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.lifecycle.ViewModelProvider;

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
    //소리 제어
    private boolean isSoundOn = true;
    // 게임 재화(골드)
    private int gold = 1200;
    private static final String PREF_USER_STATE = "user_state";
    private static final String KEY_LAST_LOGIN_TIME = "last_login_time";
    private static final String KEY_NEED_QUIZ_RECOVERY = "need_quiz_recovery";
    private static final long TWO_DAYS_MILLIS = 48L * 60L * 60L * 1000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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
            String uid = mAuth.getCurrentUser().getUid();
            db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    // 골드 로드
                    Long g = doc.getLong("gold");
                    this.gold = (g != null) ? g.intValue() : 0;
                    updateTopBar();

                    // 의상 정보 로드
                    CharacterViewModel viewModel = new androidx.lifecycle.ViewModelProvider(this).get(CharacterViewModel.class);

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

                    // 골드 업데이트가 끝나면 화면을 다시 투명하게
                    updateTopBar();
                    viewPager.setAdapter(new ViewPagerAdapter(this));
                    viewPager.setCurrentItem(1, false);
                } else {
                    // 유저 데이터가 없으면 새로 생성
                    java.util.Map<String, Object> newUser = new java.util.HashMap<>();
                    newUser.put("gold", 100);
                    newUser.put("hat", "none");
                    newUser.put("clothes", "none");
                    newUser.put("background", "none");
                    db.collection("users").document(uid).set(newUser);
                }
            });
        }

        // 저장된 유저 닉네임 불러오기
        SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
        String nickname = prefs.getString("name", "기본닉네임");

        // 상단 정보 표시
        tvPlayerName.setText(nickname);
        updateTopBar();

        // 뒤로가기 처리
        // overlay fragment가 열려 있으면 그것부터 닫고,
        // 없으면 원래 앱 뒤로가기 동작 수행
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();

                    // popBackStack 이후 컨테이너를 숨길지 확인
                    getSupportFragmentManager().executePendingTransactions();

                    if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                        fragmentContainer.setVisibility(View.GONE);
                    }
                } else {
                    finish();
                }
            }
        }

        );
    }


    public boolean isSoundOn() {
        return isSoundOn;
    }

    //음악 상태 shared preference에 저장
    private void saveSoundSetting(boolean isOn) {
        getSharedPreferences("settings", MODE_PRIVATE)
                .edit()
                .putBoolean("sound", isOn)
                .apply();
    }

    private boolean loadSoundSetting() {
        return getSharedPreferences("settings", MODE_PRIVATE)
                .getBoolean("sound", true); // 기본값 true
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

    //음악 제어
    @Override
    protected void onPause() {
        super.onPause();
        // 앱이 백그라운드로 가면 음악 일시정지
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 앱으로 다시 돌아오면 음악 재시작
        if (mediaPlayer != null && isSoundOn && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 앱 종료 시 자원 해제
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    public void updateUserGold(int amount) {
        // 1. 로컬 변수(내 지갑) 업데이트
        this.gold += amount;

        // 2. 파이어베이스 서버의 'gold' 필드 업데이트
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            db.collection("users").document(uid)
                    .update("gold", FieldValue.increment(amount)) // 💡 기존 골드에 amount만큼 더함
                    .addOnSuccessListener(aVoid -> {
                        // 상단바 코인 텍스트가 있다면 여기서 갱신해줍니다.
                        // tvGold.setText(String.valueOf(this.gold));
                    });
        }
    }
    // 상단 골드 표시 갱신
    public void updateTopBar() {
        tvGold.setText(String.valueOf(gold));
    }

    // 골드 추가 ex)  ((MainActivity) getActivity()).addGold(50);
    public void addGold(int amount) {
        gold += amount;
        updateTopBar();
        if (mAuth.getCurrentUser() != null) {
            // 🔥 에러 해결의 핵심: 여기서 uid가 누구인지 정의해 줍니다!
            String uid = mAuth.getCurrentUser().getUid();

            db.collection("users").document(uid)
                    .update("gold", this.gold)
                    .addOnSuccessListener(aVoid -> {
                        // 저장 성공 시 남기는 로그 (안 보이게 숨겨진 기록)
                        android.util.Log.d("Firebase", "골드 저장 완료: " + this.gold);
                    })
                    .addOnFailureListener(e -> {
                        // 저장 실패 시 에러 확인용
                        android.util.Log.e("Firebase", "골드 저장 실패", e);
                    });
        }
    }

    // 골드 사용  ex)  boolean success = ((MainActivity) getActivity()).spendGold(200);
    public boolean spendGold(int amount) {
        if (gold >= amount) {
            gold -= amount;
            updateTopBar();
            return true;
        }
        return false;
    }

    // 현재 골드 반환
    public int getGold() {
        return gold;
    }

    // ViewPager 위에 새로운 Fragment를 띄우는 함수
    // 예: QuizSelectFragment, QuizPlayFragment 등
    public void openFragment(Fragment fragment) {
        fragmentContainer.setVisibility(View.VISIBLE);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    // 의상정보 저장
    public void updateEquippedItem(String category, String itemName) {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();

            db.collection("users").document(uid)
                    .update(category, itemName)
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("Firebase", category + " 저장 완료: " + itemName);
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("Firebase", "저장 실패", e);
                    });
        }
    }
    // 현재 overlay fragment를 닫는 함수
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
    private void checkLongAbsenceState() {
        SharedPreferences prefs = getSharedPreferences(PREF_USER_STATE, MODE_PRIVATE);

        long now = System.currentTimeMillis();
        long lastLoginTime = prefs.getLong(KEY_LAST_LOGIN_TIME, 0L);
        boolean needRecovery = prefs.getBoolean(KEY_NEED_QUIZ_RECOVERY, false);

        CharacterViewModel viewModel =
                new ViewModelProvider(this).get(CharacterViewModel.class);

        if (lastLoginTime != 0L && now - lastLoginTime >= TWO_DAYS_MILLIS) {
            needRecovery = true;
            prefs.edit()
                    .putBoolean(KEY_NEED_QUIZ_RECOVERY, true)
                    .apply();
        }

        if (needRecovery) {
            viewModel.setFace(R.drawable.face_sad);
        }

        prefs.edit()
                .putLong(KEY_LAST_LOGIN_TIME, now)
                .apply();
    }

    public boolean isNeedQuizRecovery() {
        return getSharedPreferences(PREF_USER_STATE, MODE_PRIVATE)
                .getBoolean(KEY_NEED_QUIZ_RECOVERY, false);
    }

    public void clearLongAbsenceStateAfterQuiz() {
        getSharedPreferences(PREF_USER_STATE, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_NEED_QUIZ_RECOVERY, false)
                .putLong(KEY_LAST_LOGIN_TIME, System.currentTimeMillis())
                .apply();

        CharacterViewModel viewModel =
                new ViewModelProvider(this).get(CharacterViewModel.class);

        viewModel.setFace(R.drawable.face_default);
    }
}