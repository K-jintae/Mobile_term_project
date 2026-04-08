# Mobile_term_project
## test
### test2
### 로그인 기능

회원가입 후 로그인 가능. 메인 화면 설정창에서 로그아웃 가능</br>
현재는 회원가입 정보 SharedPreferences에 저장됨. DB 연결 후 수정 필요</br>
회원가입 시 닉네임 한국어 가능한지 확인 필요->문제 확인 필요</br>
로그인 입력 창 키보드에 가려지지 않도록 개선(실물 폰으로 테스트 필요)</br>
(2026.04.07 14:19)


### 옷장구현

현재
윗 부분
캐릭터, 초기화 버튼(기본값 - character_base + face_default)

아랫 부분
옷장 카테고리 별 항목들, 각 항목들 다 합쳐진 전체
칸이 부족할 시 스크롤

상호 작용
각 항목들에 있는 것들을 드래그해서 윗 부분으로 올릴 시 캐릭터에 적용
- 처음 적용시킬 시 그냥 옷이나 모자가 씌워짐
- 그 위에 새로 적용시 원래 것을 지우고 새로 씌워짐

앞으로 할 것
초기화 버튼 top_status_bar 밑으로 내리기
전체에 왜 있는지 모르는 공백 지우기
보유한 옷, 보유하지 않은 옷 구분
보유 x -> 반투명, 드래그 불가, 클릭 시 구매버튼 팝업


전체적으로 db연동

### 캐릭터 상태변화에 맞게 메인도 반영되는 식으로 변경 게임창은 임시구현


(2026.04.08-15:45)
##캐릭터 메세지창 구현

[XML 코드]
 <!-- 메세지창 -->
    <FrameLayout
        android:id="@+id/msg_container"
        android:layout_width="match_parent"
        android:layout_height="120dp"
        android:layout_alignParentBottom="true"
        android:layout_margin="20dp"
        android:background="#CCFFFFFF"
        android:padding="15dp">

        <TextView
            android:id="@+id/tv_message"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:text="안녕! 잘 지냈어?!"
            android:textColor="#000000"
            android:textSize="16sp"
            android:gravity="center"/>
        </FrameLayout>

  [java 코드]
   tvMessage = findViewById(R.id.tv_message); //메세지창을 아이디를 통해 데려옴 --> onCreate() 함수 밖에 위치
   updateMessage(CharacterState.NORMAL); //메세지를 실행함 --> onCreate()함수 안에 있어야함

// 상태에 따라 출력되는 메세지가 다름 (메인화면일 때, 배고픈 상황일 때, 옷을 새로 구매할 때 --> 배고플 때, 새 옷 구매 시의 반응은 구현해야함)

private void updateMessage(CharacterState state){  //onCreate() 함수 밖에 위치
        String message = "";
        Random random = new Random();

        switch(state){
            case NORMAL:
                String[] normalMessage = {
                        "안녕! 반가워!",
                        "안녕~! 오늘 하루 잘 보냈어?",
                        "안녕!! 보고싶었어!",
                        "오늘도 좋은 하루!"
                };
                message = normalMessage[random.nextInt(normalMessage.length)];
                break;

            case HUNGRY:
                message = "나 배고파...";
                break;
            case NEW_CLOTHES:
                String[] clothMessage = {
                        "우와! 이거 멋있다!!",
                        "나 이거 맘에 들어!"
                };
                message = clothMessage[random.nextInt(clothMessage.length)];
                break;
        }
        tvMessage.setText(message);
    }

##캐릭터 코인창 구현
[XML 파일]
<!-- coin -->
 <LinearLayout
            android:id="@+id/layout_coin"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_margin="16dp"
            android:gravity="center_vertical"
            android:orientation="horizontal"
            android:background="@drawable/bg_coin_window"
            android:paddingEnd="16dp"
            android:paddingStart="12dp"
            android:paddingTop="6dp"
            android:paddingBottom="6dp">

            <ImageView
                android:layout_width="28dp"
                android:layout_height="28dp"
                android:src="@drawable/ic_coins_stack"
                android:layout_marginEnd="8dp" />

        <TextView
                android:id="@+id/tv_coin"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="100"
                android:textColor="#333333"
                android:textStyle="bold"
                android:textSize="18sp"/>
        </LinearLayout>

[java 코드]
tvCoin = findViewById(R.id.tv_coin); // coin을 아이디로 가져옴.
tvCoin.setText("100"); //코인의 텍스트를 우선 100으로 고정함

--> 퀴즈를 풀거나 옷을 살 때 코인의 수가 바뀌는 것을 구현해야함

