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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {
    private List<FriendItem> friendList;

    public FriendAdapter(List<FriendItem> list){
        this.friendList = list;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position){
        FriendItem item = friendList.get(position);
        
        holder.tvItemUserId.setText(item.getUserId());
        holder.tvItemLevel.setText("레벨:  " + item.getLevel());
        holder.tvItemReason.setText(item.getReason());

        holder.btnItemAddFriend.setOnClickListener(v -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseAuth mAuth = FirebaseAuth.getInstance();

            if(mAuth.getCurrentUser() == null){
                return;
            }

            String myUid = mAuth.getCurrentUser().getUid();

            db.collection("users")
                    .whereEqualTo("name", item.getUserId())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if(!queryDocumentSnapshots.isEmpty()) {
                            String friendUid = queryDocumentSnapshots.getDocuments().get(0).getId();

                            if (friendUid.equals(myUid)) {
                                Toast.makeText(v.getContext(), "자기자신은 추가할 수 없음", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // 변경된 부분: Friends 컬렉션에 내 UID 문서 아래에 친구 UID를 true로 저장
                            Map<String, Object> friendData = new HashMap<>();
                            friendData.put(friendUid, true);

                            db.collection("Friends").document(myUid)
                                    .set(friendData, SetOptions.merge()) // 기존 친구 목록을 유지하며 새 친구 병합
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(v.getContext(), item.getUserId() + "님과 친구가 되었어요!", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    });
        });

    }
    @Override
    public int getItemCount(){
        return friendList.size();
    }
    public static class FriendViewHolder extends RecyclerView.ViewHolder{
        TextView tvItemUserId, tvItemLevel, tvItemReason;
        ImageButton btnItemAddFriend;
        public FriendViewHolder(@NonNull View itemView){
            super(itemView);
            tvItemUserId = itemView.findViewById(R.id.tvItemUserId);
            tvItemLevel = itemView.findViewById(R.id.tvItemLevel);
            tvItemReason = itemView.findViewById(R.id.tvItemReason);
            btnItemAddFriend = itemView.findViewById(R.id.btnItemAddFriend);

        }
    }
}


