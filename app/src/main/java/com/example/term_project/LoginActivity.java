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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    LinearLayout signupLayout;
    EditText idInput, pwInput;
    EditText signupId, signupPw, signupName;
    TextView loginError;
    Button loginBtn, goSignupBtn, signupBtn, backBtn;
    SharedPreferences pref;
    TextView idError, pwError, nameError;
    Button checkIdBtn;
    boolean isIdChecked = false;
    String checkedId = "";

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001; // 로그인 요청 코드
    private TextView signupError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        signupError = findViewById(R.id.signupError);

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

        loginError = findViewById(R.id.loginError);
        idError = findViewById(R.id.idError);
        pwError = findViewById(R.id.pwError);
        nameError = findViewById(R.id.nameError);
        checkIdBtn = findViewById(R.id.checkIdBtn);

        //signupBtn을 부르는 함수만 추가
        signupBtn = findViewById(R.id.signupBtn);
        loginBtn = findViewById(R.id.loginBtn);
        goSignupBtn = findViewById(R.id.goSignupBtn);
        backBtn = findViewById(R.id.backBtn);

        // 회원가입 화면 열기
        goSignupBtn.setOnClickListener(v -> {
            signupLayout.setVisibility(View.VISIBLE);
        });

        // 뒤로가기
        backBtn.setOnClickListener(v -> {
            signupLayout.setVisibility(View.GONE);
        });

        // 아이디 중복 확인
        checkIdBtn.setOnClickListener(v -> {
            String id = signupId.getText().toString().trim();
            String savedId = pref.getString("id", "");

            idError.setVisibility(View.VISIBLE);

            if (id.isEmpty()) {
                idError.setText("아이디를 입력해주세요.");
                isIdChecked = false;
                return;
            }

            if (id.length() < 4) {
                idError.setText("아이디는 4글자 이상입니다.");
                isIdChecked = false;
                return;
            }

            if (!id.matches("^[a-zA-Z0-9]+$")) {
                idError.setText("아이디는 영어와 숫자만 가능합니다.");
                idError.setVisibility(View.VISIBLE);
                return;
            }

            if (id.equals(savedId)) {
                idError.setText("이미 사용 중인 아이디입니다.");
                isIdChecked = false;
                idError.setTextColor(getColor(android.R.color.holo_red_dark));
            } else {
                idError.setText("✔ 사용 가능한 아이디입니다.");
                isIdChecked = true;
                checkedId = id;
                idError.setTextColor(getColor(android.R.color.holo_green_dark));
            }
        });

        // 아이디 중복 확인 후 아이디 수정하면 다시 확인하도록
        signupId.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isIdChecked = false;
                idError.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        // 회원가입
        signupBtn.setOnClickListener(v -> {

            String id = signupId.getText().toString().trim();
            String pw = signupPw.getText().toString().trim();
            String name = signupName.getText().toString().trim();

            String savedId = pref.getString("id", "");


            // 빈 값 검사
            idError.setVisibility(View.GONE);
            pwError.setVisibility(View.GONE);
            nameError.setVisibility(View.GONE);

            // 아이디 검사
            if (id.isEmpty()) {
                idError.setText("아이디를 입력해주세요.");
                idError.setVisibility(View.VISIBLE);
                return;
            }

            if (!id.matches("^[a-zA-Z0-9]+$")) {
                idError.setText("아이디는 영어와 숫자만 가능합니다.");
                idError.setVisibility(View.VISIBLE);
                return;
            }

            if (id.length() < 4) {
                idError.setText("아이디는 4글자 이상이어야 합니다.");
                idError.setVisibility(View.VISIBLE);
                return;
            }

            if (!isIdChecked) {
                idError.setText("아이디 중복 확인을 해주세요.");
                idError.setVisibility(View.VISIBLE);
                return;
            }

            if (!id.equals(checkedId)) {
                idError.setText("아이디를 다시 확인해주세요.");
                idError.setVisibility(View.VISIBLE);
                isIdChecked = false;
                return;
            }

            // 비밀번호 검사
            if (pw.isEmpty()) {
                pwError.setText("비밀번호를 입력해주세요.");
                pwError.setVisibility(View.VISIBLE);
                return;
            }

            if (pw.length() < 8 || !pw.matches(".*[!@#$%^&*()].*")) {
                pwError.setText("비밀번호는 8자 이상, 특수문자 포함를 포함해야 합니다.");
                pwError.setVisibility(View.VISIBLE);
                return;
            }

            // 닉네임 검사
            if (name.isEmpty()) {
                nameError.setText("닉네임을 입력해주세요.");
                nameError.setVisibility(View.VISIBLE);
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
            isIdChecked = false;
            checkedId = "";
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
