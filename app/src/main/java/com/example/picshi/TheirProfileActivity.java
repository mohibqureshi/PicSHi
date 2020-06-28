package com.example.picshi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.picshi.adapters.AdapterPosts;
import com.example.picshi.models.ModelPost;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class TheirProfileActivity extends AppCompatActivity {

    // getting views
    TextView nameTv,emailTv;
    ImageView profilePicIv,coverPicIv;

    RecyclerView postsRecyclerView;

    List<ModelPost> postList;
    AdapterPosts adapterPosts;
    String uid;

    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_their_profile);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Profile");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        postsRecyclerView = findViewById(R.id.recyclerview_posts);

        firebaseAuth = FirebaseAuth.getInstance();

        profilePicIv = findViewById(R.id.profile_pic_iv);
        nameTv = findViewById(R.id.name_tv);
        emailTv = findViewById(R.id.email_tv);
        coverPicIv = findViewById(R.id.cover_photo_iv);

        // get uid of clicked user to retrieve his posts
        Intent intent = getIntent();
        uid = intent.getStringExtra("uid");


        Query query = FirebaseDatabase.getInstance().getReference("Users").orderByChild("uid").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // check until we get the required values
                for(DataSnapshot dataSnapshot : snapshot.getChildren()){
                    // getting data
                    String name = "" + dataSnapshot.child("name").getValue();
                    String email = "" + dataSnapshot.child("email").getValue();
                    String phone = "" + dataSnapshot.child("phone").getValue();
                    String image = "" + dataSnapshot.child("image").getValue();
                    String cover = "" + dataSnapshot.child("cover").getValue();

                    //set data
                    nameTv.setText(name);
                    emailTv.setText(email);

                        try {
                            Picasso.get().load(image).resize(120,120).centerCrop().into(profilePicIv);
                        }
                        catch(Exception e){
                            Picasso.get().load(R.drawable.ic_default_img).into(profilePicIv);
                        }


                    try {
                        Picasso.get().load(cover).into(coverPicIv);
                    }
                    catch(Exception e){
                        Picasso.get().load(R.drawable.ic_default_img).into(coverPicIv);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }


        });

        postList = new ArrayList<>();
        checkUserStatus();
        loadHisPosts();

    }

    private void loadHisPosts() {
        // linear layout for recycler view
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        // show newest posts first, so load from last
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);

        // set this layout to recycler view
        postsRecyclerView.setLayoutManager(layoutManager);

        // initializing posts lists
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");

        // query to load posts
        Query query = reference.orderByChild("uid").equalTo(uid);

        // getting all data from this reference
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot ds : snapshot.getChildren()){
                    ModelPost myPosts = ds.getValue(ModelPost.class);
                    // add to list
                    postList.add(myPosts);

                    // adapter
                    adapterPosts = new AdapterPosts(TheirProfileActivity.this,postList);

                    // setting this adapter to recycler view
                    postsRecyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TheirProfileActivity.this, ""+error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void searchHisPosts(final String searchQuery){

        // linear layout for recycler view
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        // show newest posts first, so load from last
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);

        // set this layout to recycler view
        postsRecyclerView.setLayoutManager(layoutManager);

        // initializing posts lists
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");

        // query to load posts
        Query query = reference.orderByChild("uid").equalTo(uid);

        // getting all data from this reference
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot ds : snapshot.getChildren()){
                    ModelPost myPosts = ds.getValue(ModelPost.class);

                    if (myPosts.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                            myPosts.getpDescription().toLowerCase().contains(searchQuery.toLowerCase())){

                        // add to list
                        postList.add(myPosts);
                    }

                    // adapter
                    adapterPosts = new AdapterPosts(TheirProfileActivity.this,postList);

                    // setting this adapter to recycler view
                    postsRecyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TheirProfileActivity.this, ""+error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });


    }

    private void checkUserStatus(){
        // get current user
        FirebaseUser user;
        user= firebaseAuth.getCurrentUser();
        if(user!=null){

        }
        else{
            // if user is not signed in, navigate to main activity
            startActivity(new Intent(this,MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        // hide add post from this activity
        menu.findItem(R.id.action_add_post).setVisible(false);

        MenuItem item = menu.findItem(R.id.action_search);

        // search view to search user specific posts
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // called when user presses search button
                if(!TextUtils.isEmpty(query)){
                    searchHisPosts(query);
                }
                else{
                    loadHisPosts();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // called whenever user presses any letter
                if(!TextUtils.isEmpty(newText)){
                    searchHisPosts(newText);
                }
                else{
                    loadHisPosts();
                }
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){
        case R.id.action_logout:
            firebaseAuth.signOut();
            checkUserStatus();
            break;

        case R.id.action_add_post:
            startActivity(new Intent(TheirProfileActivity.this, AddPostActivity.class));
            break;
        }
        return super.onOptionsItemSelected(item);
    }
}