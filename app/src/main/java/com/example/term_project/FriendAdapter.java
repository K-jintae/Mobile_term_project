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
        
        holder.tvItemUserId.setText(item.getName());
        holder.tvItemLevel.setText("레벨:  " + item.getLevel());
        holder.tvItemReason.setText(item.getReason());

        if(item.isAlreadyFriend()){

            holder.btnItemAddFriend.setVisibility(View.GONE);

        } else{

            holder.btnItemAddFriend.setVisibility(View.VISIBLE);
        }

        holder.btnItemAddFriend.setOnClickListener(v -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseAuth mAuth = FirebaseAuth.getInstance();

            if(mAuth.getCurrentUser() == null){
                return;
            }

            String myUid = mAuth.getCurrentUser().getUid();

            db.collection("users")
                    .whereEqualTo("name", item.getName())
                    .get()
                    .addOnSuccessListener(aVoid -> {

                        Toast.makeText(
                                v.getContext(),
                                item.getName() + "님과 친구가 되었어요!",
                                Toast.LENGTH_SHORT
                        ).show();

                        int currentPosition = holder.getAdapterPosition();

                        if(currentPosition != RecyclerView.NO_POSITION){

                            friendList.remove(currentPosition);

                            notifyItemRemoved(currentPosition);

                            notifyItemRangeChanged(
                                    currentPosition,
                                    friendList.size()
                            );
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


