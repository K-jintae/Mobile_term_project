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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    LinearLayout signupLayout;

    EditText idInput, pwInput;
    EditText signupId, signupPw, signupName;

    TextView loginError;
    TextView idError, pwError, nameError;

    Button loginBtn, goSignupBtn, signupBtn, backBtn;
    Button checkIdBtn;

    SharedPreferences pref;

    boolean isIdChecked = false;
    String checkedId = "";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        pref = getSharedPreferences("user", MODE_PRIVATE);

        // 자동 로그인 체크
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();

            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Boolean isTested = documentSnapshot.getBoolean("isTested");

                            if (isTested == null || !isTested) {
                                startActivity(new Intent(LoginActivity.this, LevelTestActivity.class));
                            } else {
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            }

                            finish();
                        } else {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }
                    });

            return;
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

            idError.setVisibility(View.VISIBLE);

            if (id.isEmpty()) {
                idError.setText("아이디를 입력해주세요.");
                idError.setTextColor(getColor(android.R.color.holo_red_dark));
                isIdChecked = false;
                return;
            }

            if (id.length() < 4) {
                idError.setText("아이디는 4글자 이상입니다.");
                idError.setTextColor(getColor(android.R.color.holo_red_dark));
                isIdChecked = false;
                return;
            }

            if (!id.matches("^[a-zA-Z0-9]+$")) {
                idError.setText("아이디는 영어와 숫자만 가능합니다.");
                idError.setTextColor(getColor(android.R.color.holo_red_dark));
                isIdChecked = false;
                return;
            }

            String email = id + "@termproject.com";

            mAuth.fetchSignInMethodsForEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            boolean isUsed =
                                    task.getResult() != null
                                            && task.getResult().getSignInMethods() != null
                                            && !task.getResult().getSignInMethods().isEmpty();

                            if (isUsed) {
                                idError.setText("이미 사용 중인 아이디입니다.");
                                idError.setTextColor(getColor(android.R.color.holo_red_dark));
                                isIdChecked = false;
                            } else {
                                idError.setText("✔ 사용 가능한 아이디입니다.");
                                idError.setTextColor(getColor(android.R.color.holo_green_dark));
                                isIdChecked = true;
                                checkedId = id;
                            }
                        } else {
                            idError.setText("중복 확인 실패");
                            idError.setTextColor(getColor(android.R.color.holo_red_dark));
                            isIdChecked = false;
                        }
                    });
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

            idError.setVisibility(View.GONE);
            pwError.setVisibility(View.GONE);
            nameError.setVisibility(View.GONE);

            // 아이디 검사
            if (id.isEmpty()) {
                idError.setText("아이디를 입력해주세요.");
                idError.setTextColor(getColor(android.R.color.holo_red_dark));
                idError.setVisibility(View.VISIBLE);
                return;
            }

            if (!id.matches("^[a-zA-Z0-9]+$")) {
                idError.setText("아이디는 영어와 숫자만 가능합니다.");
                idError.setTextColor(getColor(android.R.color.holo_red_dark));
                idError.setVisibility(View.VISIBLE);
                return;
            }

            if (id.length() < 4) {
                idError.setText("아이디는 4글자 이상이어야 합니다.");
                idError.setTextColor(getColor(android.R.color.holo_red_dark));
                idError.setVisibility(View.VISIBLE);
                return;
            }

            if (!isIdChecked) {
                idError.setText("아이디 중복 확인을 해주세요.");
                idError.setTextColor(getColor(android.R.color.holo_red_dark));
                idError.setVisibility(View.VISIBLE);
                return;
            }

            if (!id.equals(checkedId)) {
                idError.setText("아이디를 다시 확인해주세요.");
                idError.setTextColor(getColor(android.R.color.holo_red_dark));
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
                pwError.setText("비밀번호는 8자 이상, 특수문자를 포함해야 합니다.");
                pwError.setVisibility(View.VISIBLE);
                return;
            }

            // 닉네임 검사
            if (name.isEmpty()) {
                nameError.setText("닉네임을 입력해주세요.");
                nameError.setVisibility(View.VISIBLE);
                return;
            }

            String email = id + "@termproject.com";

            mAuth.createUserWithEmailAndPassword(email, pw)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();

                            Map<String, Object> newUser = new HashMap<>();
                            newUser.put("gold", 100);
                            newUser.put("name", name);

                            // 레벨테스트 아직 안 함
                            newUser.put("isTested", false);

                            // 기본 캐릭터/진행도 초기값
                            newUser.put("hat", "none");
                            newUser.put("clothes", "none");
                            newUser.put("interior", "background_hill");

                            newUser.put("unlocked_stage_1", true);
                            newUser.put("unlocked_stage_2", false);
                            newUser.put("unlocked_stage_3", false);
                            newUser.put("unlocked_stage_4", false);
                            newUser.put("unlocked_stage_5", false);

                            db.collection("users").document(uid).set(newUser)
                                    .addOnSuccessListener(aVoid -> {
                                        SharedPreferences.Editor editor = pref.edit();
                                        editor.putString("name", name);
                                        editor.apply();

                                        Toast.makeText(this, "회원가입 완료!", Toast.LENGTH_SHORT).show();

                                        // 회원가입 직후 레벨테스트로 이동
                                        startActivity(new Intent(LoginActivity.this, LevelTestActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "유저 정보 저장 실패", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(
                                    this,
                                    "가입 실패: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
        });

        // 로그인
        loginBtn.setOnClickListener(v -> {
            String inputId = idInput.getText().toString().trim();
            String inputPw = pwInput.getText().toString().trim();

            loginError.setVisibility(View.GONE);

            if (inputId.isEmpty() || inputPw.isEmpty()) {
                loginError.setText("아이디와 비밀번호를 입력해주세요.");
                loginError.setVisibility(View.VISIBLE);
                return;
            }

            String email = inputId + "@termproject.com";

            mAuth.signInWithEmailAndPassword(email, inputPw)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();

                            db.collection("users").document(uid).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            String name = documentSnapshot.getString("name");
                                            if (name != null && !name.isEmpty()) {
                                                SharedPreferences.Editor editor = pref.edit();
                                                editor.putString("name", name);
                                                editor.apply();
                                            }

                                            Boolean isTested = documentSnapshot.getBoolean("isTested");

                                            if (isTested == null || !isTested) {
                                                startActivity(new Intent(LoginActivity.this, LevelTestActivity.class));
                                            } else {
                                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                            }

                                            finish();
                                        } else {
                                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                            finish();
                                        }
                                    });
                        } else {
                            loginError.setText("아이디 또는 비밀번호가 틀렸습니다.");
                            loginError.setVisibility(View.VISIBLE);
                        }
                    });
        });
    }
}
