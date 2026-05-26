package com.example.term_project;

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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        FriendItem item = friendList.get(position);

        holder.textFriendName.setText(item.getName() != null ? item.getName() : "이름 없음");

        String levelText = item.getLevel() != null ? item.getLevel() : "없음";
        String reasonText = item.getReason() != null ? item.getReason() : "";
        holder.textFriendStatus.setText("레벨: " + levelText + "  " + reasonText);

        holder.btnAccept.setVisibility(View.GONE);
        holder.btnReject.setVisibility(View.GONE);
        holder.btnAddFriend.setVisibility(View.GONE);

        String status = item.getStatus();

        if ("pending_received".equals(status)) {
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
            holder.textFriendStatus.setText("레벨: " + levelText + "  친구 요청 보냄");

        } else if ("confirmed".equals(status)) {
            if (reasonText.isEmpty()) {
                holder.textFriendStatus.setText("레벨: " + levelText + "  내 친구");
            }

        } else {
            holder.btnAddFriend.setVisibility(View.VISIBLE);

            holder.btnAddFriend.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddFriendRequested(item);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return friendList.size();
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
}