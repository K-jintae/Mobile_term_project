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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    public interface OnBattleClickListener {
        void onBattleClick(FriendItem friendItem);
    }

    private final List<FriendItem> friendList;
    private final OnBattleClickListener battleClickListener;

    public FriendAdapter(List<FriendItem> friendList, OnBattleClickListener battleClickListener) {
        this.friendList = friendList;
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

        holder.tvItemUserId.setText(item.getUserId());
        holder.tvItemLevel.setText("레벨: " + item.getLevel());

        if (item.isAlreadyFriend()) {
            holder.tvItemReason.setText(item.isOnline() ? "내 친구 · 온라인" : "내 친구 · 오프라인");
        } else {
            holder.tvItemReason.setText(item.getReason());
        }

        if (item.isAlreadyFriend()) {
            holder.btnItemAddFriend.setVisibility(View.GONE);
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
        } else {
            holder.btnItemAddFriend.setVisibility(View.VISIBLE);
            holder.btnBattle.setVisibility(View.GONE);
        }

        holder.btnBattle.setOnClickListener(v -> {
            if (!item.isOnline()) {
                Toast.makeText(v.getContext(), "온라인 친구에게만 대전을 신청할 수 있습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (battleClickListener != null) {
                battleClickListener.onBattleClick(item);
            }
        });

        holder.btnItemAddFriend.setOnClickListener(v -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseAuth auth = FirebaseAuth.getInstance();

            if (auth.getCurrentUser() == null) {
                Toast.makeText(v.getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            String myUid = auth.getCurrentUser().getUid();
            String friendUid = item.getUid();

            if (friendUid == null || friendUid.isEmpty()) {
                Toast.makeText(v.getContext(), "친구 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (friendUid.equals(myUid)) {
                Toast.makeText(v.getContext(), "자기 자신은 추가할 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("users").document(myUid)
                    .update("friends", FieldValue.arrayUnion(friendUid))
                    .addOnSuccessListener(unused ->
                            Toast.makeText(v.getContext(), item.getUserId() + "님과 친구가 되었습니다.", Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(v.getContext(), "친구 추가 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    public static class FriendViewHolder extends RecyclerView.ViewHolder {

        TextView tvItemUserId;
        TextView tvItemLevel;
        TextView tvItemReason;
        ImageButton btnItemAddFriend;
        Button btnBattle;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);

            tvItemUserId = itemView.findViewById(R.id.tvItemUserId);
            tvItemLevel = itemView.findViewById(R.id.tvItemLevel);
            tvItemReason = itemView.findViewById(R.id.tvItemReason);
            btnItemAddFriend = itemView.findViewById(R.id.btnItemAddFriend);
            btnBattle = itemView.findViewById(R.id.btnBattle);
        }
    }
}