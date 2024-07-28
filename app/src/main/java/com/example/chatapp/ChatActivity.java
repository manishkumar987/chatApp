package com.example.chatapp;

import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.adapter.ChatRecyclerAdapter;
import com.example.chatapp.model.ChatMessageModel;
import com.example.chatapp.model.ChatroomModel;
import com.example.chatapp.model.UserModel;
import com.example.chatapp.utils.AndroidUtil;
import com.example.chatapp.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    UserModel otherUser;
    String chatroomId;

    ChatroomModel chatroomModel;

    EditText messageInput;
    ImageButton sendMessageBtn;
    ImageButton backBtn;
    TextView otherUsername;
    ChatRecyclerAdapter adapter;

    RecyclerView recyclerView;
    ImageView imageView;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        otherUser = AndroidUtil.getUserModelFromIntent(getIntent());
        chatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(), otherUser.getUserId());


        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        backBtn = findViewById(R.id.back_btn);
        otherUsername = findViewById(R.id.other_username);

        recyclerView = findViewById(R.id.chat_recycler_view);
        imageView = findViewById(R.id.profile_pic_image_view);

        FirebaseUtil.getOtherProfilePicStorageRef(otherUser.getUserId()).getDownloadUrl()
                .addOnCompleteListener(t -> {
                    if(t.isSuccessful()){
                        Uri uri  = t.getResult();
                        AndroidUtil.setProfilePic(this,uri,imageView);
                    }
                });

        backBtn.setOnClickListener(v -> {
            // Check if the Intent has the extra for the previous activity
            String previousActivityName = getIntent().getStringExtra("previousActivity");
            if (previousActivityName != null) {
                try {
                    // Dynamically create an Intent to the previous activity
                    Class<?> previousActivityClass = Class.forName(previousActivityName);
                    Intent intent = new Intent(ChatActivity.this, previousActivityClass);
                    startActivity(intent);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    // Fallback to SearchUserActivity if the class name is not found
                    Intent intent = new Intent(ChatActivity.this, SearchUserActivity.class);
                    startActivity(intent);
                } 
            } else {
                // Fallback to SearchUserActivity if no previous activity is specified
                Intent intent = new Intent(ChatActivity.this, SearchUserActivity.class);
                startActivity(intent);
            }
            finish(); // Close the current activity
        });

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Check if the Intent has the extra for the previous activity
                String previousActivityName = getIntent().getStringExtra("previousActivity");
                if (previousActivityName != null) {
                    try {
                        // Dynamically create an Intent to the previous activity
                        Class<?> previousActivityClass = Class.forName(previousActivityName);
                        Intent intent = new Intent(ChatActivity.this, previousActivityClass);
                        startActivity(intent);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        // Fallback to SearchUserActivity if the class name is not found
                        Intent intent = new Intent(ChatActivity.this, SearchUserActivity.class);
                        startActivity(intent);
                    }
                } else {
                    // Fallback to SearchUserActivity if no previous activity is specified
                    Intent intent = new Intent(ChatActivity.this, SearchUserActivity.class);
                    startActivity(intent);
                }
                finish(); // Close the current activity
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        // Correctly set the text of otherUsername TextView
        otherUsername.setText(otherUser.getUsername());

         sendMessageBtn.setOnClickListener(v -> {
             String message = messageInput.getText().toString().trim();
             if(message.isEmpty())
                 return;
             sendMessageToUser(message);
         });

        getOrCreateChatRoomModel();
        setupChatRecyclerView();

        final View activityRootView = findViewById(android.R.id.content);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                activityRootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = activityRootView.getRootView().getHeight();
                int keyboardHeight = screenHeight - r.bottom;

                if (keyboardHeight > screenHeight * 0.20) { // 0.15 ratio is used as a threshold
                    // Keyboard is visible, adjust your layout here
                    adjustLayoutForKeyboard(true);
                } else {
                    // Keyboard is hidden, reset your layout here
                    adjustLayoutForKeyboard(false);
                }
            }
        });
    }

    void setupChatRecyclerView(){
        Query query = FirebaseUtil.getChatroomMessageReference(chatroomId)
            .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(query,ChatMessageModel.class).build();

        adapter = new ChatRecyclerAdapter(options,getApplicationContext());
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        adapter.startListening();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerView.smoothScrollToPosition(2500);
            }
        });

    }

    void sendMessageToUser(String message)
    {
         chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
        chatroomModel.setLastMessage(message);
        FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);

        ChatMessageModel chatMessageModel = new ChatMessageModel(message,FirebaseUtil.currentUserId(),Timestamp.now());

        FirebaseUtil.getChatroomMessageReference(chatroomId).add(chatMessageModel)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                   if (task.isSuccessful()){
                        messageInput.setText("");
                       sendNotification(message);

                   }
                    }
                });
    }
    void getOrCreateChatRoomModel(){
        FirebaseUtil.getChatroomReference(chatroomId).get().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                chatroomModel = task.getResult().toObject(ChatroomModel.class);
                if(chatroomModel==null){
                    //first time chat
                    chatroomModel = new ChatroomModel(
                            chatroomId,
                            Arrays.asList(FirebaseUtil.currentUserId(),otherUser.getUserId()),
                            Timestamp.now(),
                            ""
                    );
                    FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);
                }
            }
        });
    }

    private void adjustLayoutForKeyboard(boolean isKeyboardVisible) {
        // Adjust the bottom margin of the EditText
        ViewGroup.MarginLayoutParams messageInputParams = (ViewGroup.MarginLayoutParams) messageInput.getLayoutParams();
        if (isKeyboardVisible) {
            // Adjust the bottom margin to move the EditText up
            messageInputParams.bottomMargin = 520;
        } else {
            // Reset the bottom margin
            messageInputParams.bottomMargin = 10;
        }
        messageInput.setLayoutParams(messageInputParams);

        // Adjust the bottom margin of the send button
        ViewGroup.MarginLayoutParams sendButtonParams = (ViewGroup.MarginLayoutParams) sendMessageBtn.getLayoutParams();
        if (isKeyboardVisible) {
            // Adjust the bottom margin to move the send button up
            sendButtonParams.bottomMargin = 520;
        } else {
            // Reset the bottom margin
            sendButtonParams.bottomMargin = 10;
        }
        sendMessageBtn.setLayoutParams(sendButtonParams);
    }
    void sendNotification(String message){

        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                UserModel currentUser = task.getResult().toObject(UserModel.class);
                try{
                    JSONObject jsonObject  = new JSONObject();

                    JSONObject notificationObj = new JSONObject();
                    notificationObj.put("title",currentUser.getUsername());
                    notificationObj.put("body",message);

                    JSONObject dataObj = new JSONObject();
                    dataObj.put("userId",currentUser.getUserId());

                    jsonObject.put("notification",notificationObj);
                    jsonObject.put("data",dataObj);
                    jsonObject.put("to",otherUser.getFcmToken());

                    callApi(jsonObject);


                }catch (Exception e){

                }

            }
        });


    }
    void callApi(JSONObject jsonObject){
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        String url = "https://fcm.googleapis.com/fcm/send";
        RequestBody body = RequestBody.create(jsonObject.toString(),JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization","Bearer AAAAfisjk9k:APA91bErYpOmbrkwJIyZOSzTyzAEX5axhpoPhl2iwHy8t2Ux9aEUCt5_QVI1TT5EiN49a3OwK13oKHs3qczUhd_l_5VDuTnooUF97O87T6zGjUikjSYcLt5N2bFP0d4Ppwq6gnTXzWji")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

            }
        });


}}

