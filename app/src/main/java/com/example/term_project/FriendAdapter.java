package com.example.term_project;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    public interface OnFriendActionListener {
        void onAccept(FriendItem item);
        void onReject(FriendItem item);
        void onAddFriendRequested(FriendItem item);
    }

    private final List<FriendItem> friendList;
    private final OnFriendActionListener listener;

    public FriendAdapter(List<FriendItem> friendList, OnFriendActionListener listener) {
        this.friendList = friendList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        FriendItem item = friendList.get(position);

        // 이름 바인딩
        holder.textFriendName.setText(item.getName() != null ? item.getName() : "이름 없음");

        String levelText = item.getLevel() != null ? item.getLevel() : "없음";
        String reasonText = item.getReason() != null ? item.getReason() : "";

        // 기본 상태 텍스트 레이아웃 색상 빌드 적용
        applyLevelColor(holder.textFriendStatus, levelText, reasonText);

        // 리사이클러뷰 아이템 재사용 시 뷰가 꼬이지 않도록 가시성 초기화
        holder.btnAccept.setVisibility(View.GONE);
        holder.btnReject.setVisibility(View.GONE);
        holder.btnAddFriend.setVisibility(View.GONE);

        // 💡 에러 해결: getter 제거 후 변수 직접 참조로 수정 (gridAddFriend 삭제)
        holder.btnAddFriend.setText("친구 추가");

        // 리스너 초기화를 통한 고스트 클릭 차단
        holder.btnAddFriend.setOnClickListener(null);
        holder.btnAccept.setOnClickListener(null);
        holder.btnReject.setOnClickListener(null);

        String status = item.getStatus();

        if ("pending_received".equals(status)) {
            // 나에게 온 요청: 수락 / 거절 버튼 활성화
            holder.btnAccept.setVisibility(View.VISIBLE);
            holder.btnReject.setVisibility(View.VISIBLE);

            holder.btnAccept.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAccept(item);
                }
            });

            holder.btnReject.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReject(item);
                }
            });

        } else if ("pending_sent".equals(status)) {
            // 내가 보낸 요청: 요청 취소 버튼 활성화 및 컬러 서식 적용
            applyLevelColor(holder.textFriendStatus, levelText, "친구 요청 보냄");

            holder.btnAddFriend.setVisibility(View.VISIBLE);
            holder.btnAddFriend.setText("요청 취소");

            holder.btnAddFriend.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReject(item);
                }
            });

        } else if ("confirmed".equals(status)) {
            // 이미 친구인 상태: 실시간 접속 상태 표시 유지
            if (reasonText.isEmpty()) {
                applyLevelColor(holder.textFriendStatus, levelText, "내 친구");
            } else {
                applyLevelColor(holder.textFriendStatus, levelText, reasonText);
            }

        } else {
            // pending_none (추천 친구 / 검색 결과) 상태: 친구 추가 버튼 활성화
            applyLevelColor(holder.textFriendStatus, levelText, reasonText);

            holder.btnAddFriend.setVisibility(View.VISIBLE);
            holder.btnAddFriend.setText("친구 추가");

            holder.btnAddFriend.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddFriendRequested(item);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return friendList != null ? friendList.size() : 0;
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView textFriendName;
        TextView textFriendStatus;
        Button btnAccept;
        Button btnReject;
        Button btnAddFriend;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            textFriendName = itemView.findViewById(R.id.text_friend_name);
            textFriendStatus = itemView.findViewById(R.id.text_friend_status);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnReject = itemView.findViewById(R.id.btn_reject);
            btnAddFriend = itemView.findViewById(R.id.btn_add_friend);
        }
    }

    // 티어 등급 텍스트 컬러 서식 적용 시스템 메서드
    private void applyLevelColor(TextView textView, String level, String reason) {
        String text;

        if (reason == null || reason.isEmpty()) {
            text = "레벨: " + level;
        } else {
            text = "레벨: " + level + "   |   " + reason;
        }

        SpannableString spannable = new SpannableString(text);

        int start = text.indexOf(level);
        if (start == -1) {
            textView.setText(text);
            return;
        }
        int end = start + level.length();

        int color = Color.GRAY;

        switch (level) {
            case "하수":
                color = Color.parseColor("#9CCC65"); // 연두
                break;
            case "중수":
                color = Color.parseColor("#64B5F6"); // 하늘
                break;
            case "고수":
                color = Color.parseColor("#FFB74D"); // 주황
                break;
        }

        spannable.setSpan(
                new ForegroundColorSpan(color),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        textView.setText(spannable);
    }
}