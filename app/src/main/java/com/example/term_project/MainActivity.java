package com.example.term_project;

import android.os.Bundle;
import android.widget.TextView;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TextView tvPlayerName;
    private TextView tvGold;
    private TextView tvGem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewPager);


        tvPlayerName = findViewById(R.id.tvPlayerName);
        tvGold = findViewById(R.id.tvGold);
        tvGem = findViewById(R.id.tvGem);

        viewPager.setAdapter(new ViewPagerAdapter(this));
        viewPager.setCurrentItem(1, false);   // 추가

        // 저장되어 있는 유저 닉네임 불러와서 표시
        SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
        String nickname = prefs.getString("name", "기본닉네임");

        tvPlayerName.setText(nickname);
        tvGold.setText("Gold: 1200");
        tvGem.setText("Gem: 35");

    }
}