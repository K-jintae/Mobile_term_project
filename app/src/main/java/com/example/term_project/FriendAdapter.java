package com.example.term_project;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton; //추가 확인
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    private List<FriendItem> friendList;
    private OnFriendActionListener actionListener;

    public interface OnFriendActionListener {
        void onAccept(FriendItem item);
        void onReject(FriendItem item);
        void onAddFriendRequested(FriendItem item);
    }

    public FriendAdapter(List<FriendItem> friendList, OnFriendActionListener actionListener) {
        this.friendList = friendList;
        this.actionListener = actionListener;
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
        holder.textName.setText(item.getName());

        String levelPrefix = (item.getLevel() != null && !item.getLevel().isEmpty()) ? "[난이도: " + item.getLevel() + "] " : "";

        holder.layoutButtons.setVisibility(View.GONE);
        holder.btnAddFriend.setVisibility(View.GONE);

        String status = item.getStatus() != null ? item.getStatus() : "";

        switch (status) {
            case "pending_received":
                holder.textStatus.setText(levelPrefix + "친구 요청을 보냈습니다.");
                holder.layoutButtons.setVisibility(View.VISIBLE);
                holder.textStatus.setTextColor(Color.parseColor("#4CAF50"));
                break;

            case "pending_sent":
                holder.textStatus.setText(levelPrefix + "수락 대기 중...");
                holder.textStatus.setTextColor(Color.parseColor("#2196F3"));
                break;

            case "confirmed":
                String reasonStr = (item.getReason() != null) ? " " + item.getReason() : "서로 친구 상태입니다 ✓";
                holder.textStatus.setText(levelPrefix + reasonStr);

                if (reasonStr.contains("● 접속중")) {
                    holder.textStatus.setTextColor(Color.parseColor("#4CAF50"));
                } else {
                    holder.textStatus.setTextColor(Color.parseColor("#888888"));
                }
                break;

            case "pending_none":
            case "":
            default:
                String recReason = (item.getReason() != null) ? item.getReason() : "추천친구";
                holder.textStatus.setText(levelPrefix + recReason);
                holder.textStatus.setTextColor(Color.parseColor("#FF8A00"));

                holder.btnAddFriend.setVisibility(View.VISIBLE); // ImageButton 정상 작동
                break;
        }

        holder.btnAccept.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onAccept(item);
        });

        holder.btnReject.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onReject(item);
        });

        holder.btnAddFriend.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onAddFriendRequested(item);
        });
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    public static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView textName, textStatus;
        LinearLayout layoutButtons;
        Button btnAccept, btnReject;
        ImageButton btnAddFriend; //Button에서 ImageButton으로 형변환 수정 완료!

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_friend_name);
            textStatus = itemView.findViewById(R.id.text_friend_status);
            layoutButtons = itemView.findViewById(R.id.layout_action_buttons);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnReject = itemView.findViewById(R.id.btn_reject);
            btnAddFriend = itemView.findViewById(R.id.btn_add_friend);
        }
    }
}