package com.example.picshi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.ReceiverCallNotAllowedException;
import android.drm.DrmStore;
import android.os.Bundle;
import android.view.View;

import com.example.picshi.adapters.AdapterUser;
import com.example.picshi.models.ModelUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PostLikedByActivity extends AppCompatActivity {

    // to get the id of the concerned post
    String postId;

    // instantiating userList and adapterUser
    private List<ModelUser> userList;
    private AdapterUser adapterUser;

    // instance of FirebaseAuth
    private FirebaseAuth firebaseAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_liked_by);

        // get recycler view
        final RecyclerView recyclerView = findViewById(R.id.recyclerViewLikedUsers);

        // Action bar and it's properties
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("People who liked this post");

        firebaseAuth = FirebaseAuth.getInstance();

        // setting actionbar subtitle as the email of the signed in user
        actionBar.setSubtitle(firebaseAuth.getCurrentUser().getEmail());

        // Adding back button to navigate back to previous activity
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        // get the postId
        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");

        userList = new ArrayList<>();

        // getting the list of UIDs of users who liked the posts
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Likes");
        reference.child(postId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()){
                    String hisUid = ds.getRef().getKey();

                    // getting user info from each Id
                    getUsers(hisUid, recyclerView);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getUsers(String hisUid, final RecyclerView recyclerView) {
        // get information of each user using Uid
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("uid").equalTo(hisUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot ds : snapshot.getChildren()){
                    ModelUser modelUser = ds.getValue(ModelUser.class);
                    userList.add(modelUser);
                }

                // setting up adapter
                adapterUser = new AdapterUser(PostLikedByActivity.this,userList);

                // setting adapter to recycler view
                recyclerView.setAdapter(adapterUser);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}