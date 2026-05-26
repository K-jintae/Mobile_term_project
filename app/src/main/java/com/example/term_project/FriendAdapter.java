package com.example.term_project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    public interface OnFriendActionListener {
        void onAccept(FriendItem item);
        void onReject(FriendItem item);
        void onAddFriendRequested(FriendItem item);
    }

    public interface OnBattleClickListener {
        void onBattleClick(FriendItem friendItem);
    }

    private final List<FriendItem> friendList;
    private final OnFriendActionListener actionListener;
    private final OnBattleClickListener battleClickListener;

    // main 기존 호출 호환용
    public FriendAdapter(List<FriendItem> friendList) {
        this(friendList, null, null);
    }

    // main의 친구 신청/수락/거절용
    public FriendAdapter(List<FriendItem> friendList, OnFriendActionListener actionListener) {
        this(friendList, actionListener, null);
    }

    // battle-quiz 기존 호출 호환용
    public FriendAdapter(List<FriendItem> friendList, OnBattleClickListener battleClickListener) {
        this(friendList, null, battleClickListener);
    }

    // 최종 병합용 생성자
    public FriendAdapter(
            List<FriendItem> friendList,
            OnFriendActionListener actionListener,
            OnBattleClickListener battleClickListener
    ) {
        this.friendList = friendList;
        this.actionListener = actionListener;
        this.battleClickListener = battleClickListener;
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

        holder.tvItemUserId.setText(item.getName());
        holder.tvItemLevel.setText("레벨: " + item.getLevel());

        String status = item.getStatus();

        if ("confirmed".equals(status)) {
            holder.tvItemReason.setText(item.isOnline() ? "내 친구 · 온라인" : "내 친구 · 오프라인");
        } else {
            holder.tvItemReason.setText(item.getReason());
        }

        resetButtons(holder);

        if ("confirmed".equals(status)) {
            bindConfirmedFriend(holder, item);
        } else if ("pending_received".equals(status)) {
            bindReceivedRequest(holder, item);
        } else if ("pending_sent".equals(status)) {
            bindSentRequest(holder);
        } else {
            bindRecommendedFriend(holder, item);
        }
    }

    private void resetButtons(FriendViewHolder holder) {
        if (holder.btnItemAddFriend != null) {
            holder.btnItemAddFriend.setVisibility(View.GONE);
            holder.btnItemAddFriend.setEnabled(true);
            holder.btnItemAddFriend.setAlpha(1.0f);
            holder.btnItemAddFriend.setOnClickListener(null);
        }

        if (holder.btnAcceptFriend != null) {
            holder.btnAcceptFriend.setVisibility(View.GONE);
            holder.btnAcceptFriend.setOnClickListener(null);
        }

        if (holder.btnRejectFriend != null) {
            holder.btnRejectFriend.setVisibility(View.GONE);
            holder.btnRejectFriend.setOnClickListener(null);
        }

        if (holder.btnBattle != null) {
            holder.btnBattle.setVisibility(View.GONE);
            holder.btnBattle.setEnabled(true);
            holder.btnBattle.setAlpha(1.0f);
            holder.btnBattle.setOnClickListener(null);
        }
    }

    private void bindConfirmedFriend(FriendViewHolder holder, FriendItem item) {
        if (holder.btnBattle == null) {
            return;
        }

        holder.btnBattle.setVisibility(View.VISIBLE);

        if (item.isOnline()) {
            holder.btnBattle.setEnabled(true);
            holder.btnBattle.setText("대전");
            holder.btnBattle.setAlpha(1.0f);
        } else {
            holder.btnBattle.setEnabled(false);
            holder.btnBattle.setText("오프라인");
            holder.btnBattle.setAlpha(0.45f);
        }

        holder.btnBattle.setOnClickListener(v -> {
            if (!item.isOnline()) {
                Toast.makeText(
                        v.getContext(),
                        "온라인 친구에게만 대전을 신청할 수 있습니다.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (battleClickListener != null) {
                battleClickListener.onBattleClick(item);
            }
        });
    }

    private void bindReceivedRequest(FriendViewHolder holder, FriendItem item) {
        if (holder.btnAcceptFriend != null) {
            holder.btnAcceptFriend.setVisibility(View.VISIBLE);
            holder.btnAcceptFriend.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onAccept(item);
                }
            });
        }

        if (holder.btnRejectFriend != null) {
            holder.btnRejectFriend.setVisibility(View.VISIBLE);
            holder.btnRejectFriend.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onReject(item);
                }
            });
        }

        // XML에 수락/거절 버튼이 아직 없다면 기존 추가 버튼을 수락 버튼처럼 사용
        if (holder.btnAcceptFriend == null && holder.btnItemAddFriend != null) {
            holder.btnItemAddFriend.setVisibility(View.VISIBLE);
            holder.btnItemAddFriend.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onAccept(item);
                }
            });
        }
    }

    private void bindSentRequest(FriendViewHolder holder) {
        if (holder.btnItemAddFriend != null) {
            holder.btnItemAddFriend.setVisibility(View.VISIBLE);
            holder.btnItemAddFriend.setEnabled(false);
            holder.btnItemAddFriend.setAlpha(0.35f);
        }
    }

    private void bindRecommendedFriend(FriendViewHolder holder, FriendItem item) {
        if (holder.btnItemAddFriend == null) {
            return;
        }

        holder.btnItemAddFriend.setVisibility(View.VISIBLE);
        holder.btnItemAddFriend.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onAddFriendRequested(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return friendList == null ? 0 : friendList.size();
    }

    public static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemUserId;
        TextView tvItemLevel;
        TextView tvItemReason;

        ImageButton btnItemAddFriend;
        ImageButton btnAcceptFriend;
        ImageButton btnRejectFriend;
        Button btnBattle;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);

            tvItemUserId = itemView.findViewById(R.id.tvItemUserId);
            tvItemLevel = itemView.findViewById(R.id.tvItemLevel);
            tvItemReason = itemView.findViewById(R.id.tvItemReason);

            btnItemAddFriend = itemView.findViewById(R.id.btnItemAddFriend);
            btnAcceptFriend = itemView.findViewById(R.id.btnAcceptFriend);
            btnRejectFriend = itemView.findViewById(R.id.btnRejectFriend);
            btnBattle = itemView.findViewById(R.id.btnBattle);
        }
    }
}