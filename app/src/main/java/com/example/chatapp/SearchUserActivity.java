package com.example.chatapp;

import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.adapter.SearchUserRecyclerAdapter;
import com.example.chatapp.model.UserModel;
import com.example.chatapp.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;

import java.util.concurrent.Executor;

public class SearchUserActivity extends AppCompatActivity {

    EditText searchInput;
    ImageButton searchButton;
    ImageButton backButton;
    RecyclerView recyclerView;

    SearchUserRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search_user);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        searchInput = findViewById(R.id.seach_username_input);
        searchButton = findViewById(R.id.search_user_btn);
        backButton = findViewById(R.id.back_btn);
        recyclerView = findViewById(R.id.search_user_recycler_view);

        searchInput.requestFocus();

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Navigate back to MainActivity
                Intent intent = new Intent(SearchUserActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Close the current activity
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        backButton.setOnClickListener(v -> {
            // Trigger the back press event
            callback.handleOnBackPressed();
        });

        searchButton.setOnClickListener(v -> {
            String searchTerm = searchInput.getText().toString();
            if (searchTerm.isEmpty() || searchTerm.length() < 3) {
                searchInput.setError("Invalid Username");
                return;
            }
            setupSearchInRecyclerView(searchTerm);
        });

}
    void setupSearchInRecyclerView(String searchTerm) {

        Query query = FirebaseUtil.allUserCollectionReference()
                .whereGreaterThanOrEqualTo("username", searchTerm  )
                .whereLessThanOrEqualTo("username", searchTerm + "\uf8ff")
                .orderBy("username");

        FirestoreRecyclerOptions<UserModel> options = new FirestoreRecyclerOptions.Builder<UserModel>()
                .setQuery(query, UserModel.class).build();

        SearchUserRecyclerAdapter adapter = new SearchUserRecyclerAdapter(options, getApplicationContext(), searchTerm);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }


    @Override
    protected void onStart() {
        super.onStart();
        if(adapter!=null)
            adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(adapter!=null)
            adapter.stopListening();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(adapter!=null)
            adapter.startListening();
    }
}











