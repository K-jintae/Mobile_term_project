package com.example.term_project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    private final List<FriendItem> friendList;

    public FriendAdapter(List<FriendItem> list) {
        this.friendList = list;
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

        // 이름, 레벨 표시 로직
        holder.tvItemUserId.setText(item.getName());
        holder.tvItemLevel.setText("레벨: " + item.getLevel());
        holder.tvItemReason.setText(item.getReason());

        // 이미 친구면 추가 버튼 숨기기
        holder.btnItemAddFriend.setVisibility(
                item.isAlreadyFriend() ? View.GONE : View.VISIBLE
        );

        holder.btnItemAddFriend.setOnClickListener(v -> {

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseAuth auth = FirebaseAuth.getInstance();

            if (auth.getCurrentUser() == null) return;
            String myUid = auth.getCurrentUser().getUid();


            String friendUid = item.getUid();

            if (friendUid.equals(myUid)) {
                Toast.makeText(v.getContext(), "자기 자신은 추가할 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> friendData = new HashMap<>();
            friendData.put(friendUid, true);

            db.collection("Friends").document(myUid)
                    .set(friendData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(v.getContext(), item.getName() + "님과 친구가 되었어요!", Toast.LENGTH_SHORT).show();

                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            friendList.remove(pos);
                            notifyItemRemoved(pos);
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(v.getContext(), "친구 추가 실패", Toast.LENGTH_SHORT).show()
                    );
        });
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemUserId, tvItemLevel, tvItemReason;
        ImageButton btnItemAddFriend;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemUserId = itemView.findViewById(R.id.tvItemUserId);
            tvItemLevel = itemView.findViewById(R.id.tvItemLevel);
            tvItemReason = itemView.findViewById(R.id.tvItemReason);
            btnItemAddFriend = itemView.findViewById(R.id.btnItemAddFriend);
        }
    }
}