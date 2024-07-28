package com.example.chatapp.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.ChatActivity;
import com.example.chatapp.R;
import com.example.chatapp.model.UserModel;
import com.example.chatapp.utils.AndroidUtil;
import com.example.chatapp.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class SearchUserRecyclerAdapter extends FirestoreRecyclerAdapter<UserModel, SearchUserRecyclerAdapter.UserModelViewHolder> {

    Context context;
    private String searchTerm; // Define the searchTerm variable

    public SearchUserRecyclerAdapter(@NonNull FirestoreRecyclerOptions<UserModel> options, Context context, String searchTerm) {
        super(options);
        this.context = context;
        this.searchTerm = searchTerm; // Initialize the searchTerm variable
    }

    @Override
    protected void onBindViewHolder(@NonNull UserModelViewHolder holder, int position, @NonNull UserModel model) {
        holder.usernameText.setText(model.getUsername());
        holder.phoneText.setText(model.getPhone());
        if(model.getUserId().equals(FirebaseUtil.currentUserId())){
            holder.usernameText.setText(model.getUsername()+"(Me)");
        }

        FirebaseUtil.getOtherProfilePicStorageRef(model.getUserId()).getDownloadUrl()
                .addOnCompleteListener(t -> {
                    if(t.isSuccessful()){
                        Uri uri  = t.getResult();
                        AndroidUtil.setProfilePic(context,uri,holder.profilePic);
                    }
                });


        // Client-side filtering logic
        boolean isMatch = true;
        for (char searchChar : searchTerm.toLowerCase().toCharArray()) {
            if (!holder.usernameText.getText().toString().toLowerCase().contains(String.valueOf(searchChar))) {
                isMatch = false;
                break;
            }
        }

        // Update item visibility based on the match
        holder.itemView.setVisibility(isMatch ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            Intent intent =new Intent(context, ChatActivity.class);
            AndroidUtil.passUserModelAsIntent(intent,model);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    @NonNull
    @Override
    public UserModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.search_user_recycler_row,parent,false);
        return new UserModelViewHolder(view);
    }

    class UserModelViewHolder extends RecyclerView.ViewHolder{

        TextView usernameText;
        TextView phoneText;
        ImageView profilePic;

        public UserModelViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.user_name_text);
            phoneText = itemView.findViewById(R.id.phone_text);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);

        }
    }
}
