package com.example.term_project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    LinearLayout signupLayout;

    EditText idInput, pwInput;
    EditText signupId, signupPw, signupName;

    TextView signupError, loginError;

    Button loginBtn, goSignupBtn, signupBtn, backBtn;

    SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        pref = getSharedPreferences("user", MODE_PRIVATE);

        // 자동 로그인 체크
        boolean isLogin = pref.getBoolean("isLogin", false);
        if (isLogin) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }

        // 연결
        signupLayout = findViewById(R.id.signupLayout);

        idInput = findViewById(R.id.idInput);
        pwInput = findViewById(R.id.pwInput);

        signupId = findViewById(R.id.signupId);
        signupPw = findViewById(R.id.signupPw);
        signupName = findViewById(R.id.signupName);

        signupError = findViewById(R.id.signupError);
        loginError = findViewById(R.id.loginError);

        loginBtn = findViewById(R.id.loginBtn);
        goSignupBtn = findViewById(R.id.goSignupBtn);
        signupBtn = findViewById(R.id.signupBtn);
        backBtn = findViewById(R.id.backBtn);

        // 회원가입 화면 열기
        goSignupBtn.setOnClickListener(v -> {
            signupLayout.setVisibility(View.VISIBLE);
        });

        // 뒤로가기
        backBtn.setOnClickListener(v -> {
            signupLayout.setVisibility(View.GONE);
        });

        // 회원가입
        signupBtn.setOnClickListener(v -> {

            String id = signupId.getText().toString().trim();
            String pw = signupPw.getText().toString().trim();
            String name = signupName.getText().toString().trim();

            String savedId = pref.getString("id", "");

            signupError.setVisibility(View.GONE);

            // 빈 값 검사
            if (id.isEmpty() || pw.isEmpty() || name.isEmpty()) {
                signupError.setText("모든 항목을 입력해주세요.");
                signupError.setVisibility(View.VISIBLE);
                return;
            }

            // 아이디 길이
            if (id.length() < 4) {
                signupError.setText("아이디는 4글자 이상이어야 합니다.");
                signupError.setVisibility(View.VISIBLE);
                return;
            }

            // 비밀번호 조건 (8자 + 특수문자)
            if (pw.length() < 8 || !pw.matches(".*[!@#$%^&*()].*")) {
                signupError.setText("비밀번호는 8자 이상, 특수문자를 포함해야 합니다.");
                signupError.setVisibility(View.VISIBLE);
                return;
            }

            // 아이디 중복
            if (id.equals(savedId)) {
                signupError.setText("이미 사용 중인 아이디입니다.");
                signupError.setVisibility(View.VISIBLE);
                return;
            }

            // 저장
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("id", id);
            editor.putString("pw", pw);
            editor.putString("name", name);
            editor.apply();

            Toast.makeText(this, "회원가입 완료", Toast.LENGTH_SHORT).show();
            signupLayout.setVisibility(View.GONE);
        });

        // 로그인
        loginBtn.setOnClickListener(v -> {

            String inputId = idInput.getText().toString().trim();
            String inputPw = pwInput.getText().toString().trim();

            String savedId = pref.getString("id", "");
            String savedPw = pref.getString("pw", "");

            loginError.setVisibility(View.GONE);

            if (inputId.equals(savedId) && inputPw.equals(savedPw)) {

                SharedPreferences.Editor editor = pref.edit();
                editor.putBoolean("isLogin", true);
                editor.apply();

                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();

            } else {
                loginError.setText("아이디 또는 비밀번호가 틀렸습니다.");
                loginError.setVisibility(View.VISIBLE);
            }
        });
    }
}