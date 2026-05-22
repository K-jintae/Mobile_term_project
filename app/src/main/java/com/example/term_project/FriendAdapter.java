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
    private List<FriendItem> friendList;

    public FriendAdapter(List<FriendItem> list) {
        this.friendList = list;
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

        holder.tvItemUserId.setText(item.getUserId());
        holder.tvItemLevel.setText("레벨:  " + item.getLevel());
        holder.tvItemReason.setText(item.getReason());

        holder.btnItemAddFriend.setOnClickListener(v -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseAuth mAuth = FirebaseAuth.getInstance();

            if (mAuth.getCurrentUser() == null) return;

            String myUid = mAuth.getCurrentUser().getUid();

            // 유저의 이름(userId)을 바탕으로 상대방의 UID 조회
            db.collection("users")
                    .whereEqualTo("userId", item.getUserId())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            String friendUid = queryDocumentSnapshots.getDocuments().get(0).getId();


                            if (friendUid.equals(myUid)) {
                                Toast.makeText(v.getContext(), "자기 자신은 추가할 수 없습니다.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            Map<String, Object> friendData = new HashMap<>();
                            friendData.put(friendUid, true);

                            // 가장 효율적인 최상위 독립 컬렉션 방식 처리
                            db.collection("Friends").document(myUid)
                                    .set(friendData, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(v.getContext(), item.getUserId() + "님과 친구가 되었어요!", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            // 혹시 userId 필드 대신 name 필드로 저장되어 있을 경우를 위한 예외 처리 예시
                            db.collection("users")
                                    .whereEqualTo("name", item.getUserId())
                                    .get()
                                    .addOnSuccessListener(snapshots -> {
                                        if(!snapshots.isEmpty()) {
                                            String friendUid = snapshots.getDocuments().get(0).getId();
                                            Map<String, Object> friendData = new HashMap<>();
                                            friendData.put(friendUid, true);

                                            db.collection("Friends").document(myUid)
                                                    .set(friendData, SetOptions.merge())
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(v.getContext(), item.getUserId() + "님과 친구가 되었어요!", Toast.LENGTH_SHORT).show();
                                                    });
                                        }
                                    });
                        }
                    });
        });
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    public static class FriendViewHolder extends RecyclerView.ViewHolder {
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