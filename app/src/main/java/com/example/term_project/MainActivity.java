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

    // 게임 재화(골드)
    private int gold = 1200;

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

        //배경음악 재생
        mediaPlayer = MediaPlayer.create(this, R.raw.chestnut_cookie);
        if(mediaPlayer != null){
            mediaPlayer.setLooping(true);//반복
            mediaPlayer.start();//시작
        }

        // 로그인 유저 설정 불러오기
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Long g = doc.getLong("gold");
                    this.gold = (g != null) ? g.intValue() : 0;

                    // 골드 업데이트가 끝나면 화면을 다시 투명하게
                    updateTopBar();
                    viewPager.setAdapter(new ViewPagerAdapter(this));
                    viewPager.setCurrentItem(1, false);
                } else {
                    // 유저 데이터가 없으면 새로 생성
                    java.util.Map<String, Object> newUser = new java.util.HashMap<>();
                    newUser.put("gold", 0);
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
        });
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
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
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
}