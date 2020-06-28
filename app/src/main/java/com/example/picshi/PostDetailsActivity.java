package com.example.picshi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.renderscript.Sampler;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.example.picshi.adapters.AdapterComment;
import com.example.picshi.models.ModelComment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class PostDetailsActivity extends AppCompatActivity {

    // to get detail of user and post
    String myUID, myEmail, myName, myDp, postId, pLikes, hisDp, hisName, hisUid, pImage;

    boolean processLike = false;
    boolean processComment = false;



    // views
    ImageView uPictureIv, pImageIv;
    TextView nameTv, pTimeTv, pTitleTv, pDescriptionTv, pLikesTv, pCommentsTv;
    ImageButton moreBtn;
    Button likeBtn;
    LinearLayout profileLayout;
    RecyclerView recyclerView;

    List<ModelComment> commentList;
    AdapterComment adapterComment;

    // comment views
    EditText commentEt;
    ImageButton sendBtn;
    ImageView commentAvatarIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_details);

        // action bar and its properties

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Post Detail");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        // getting post Id using intent
        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");


        // initializing views
        uPictureIv = findViewById(R.id.user_pic_iv);
        pImageIv = findViewById(R.id.user_post_image_iv);
        nameTv = findViewById(R.id.username_tv);
        pTimeTv = findViewById(R.id.post_time_tv);
        pTitleTv = findViewById(R.id.post_title_tv);
        pDescriptionTv = findViewById(R.id.post_description_tv);
        pLikesTv = findViewById(R.id.post_likes_tv);
        pCommentsTv = findViewById(R.id.pCommentsTv);
        moreBtn = findViewById(R.id.more_btn);
        likeBtn = findViewById(R.id.like_btn);
        profileLayout = findViewById(R.id.profileLayout);
        recyclerView = findViewById(R.id.recyclerView);
        commentEt = findViewById(R.id.comment_et);
        sendBtn = findViewById(R.id.sendBtn);
        commentAvatarIv = findViewById(R.id.comment_avatar_iv);


        setLikes();

        loadPostInfo();

        checkUserStatus();

        loadUserInfo();

        // setting actionbar subtitle
        actionBar.setSubtitle("signed in as: " + myEmail);

        loadComments();

        // send comment button click listener
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(PostDetailsActivity.this, "posting comment ... please wait", Toast.LENGTH_LONG).show();
                postComment();
            }
        });

        // like button click handler
        likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                likePost();
            }
        });

        moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMoreOptions();
            }
        });

        // click like count to start PostLikedByActivity and pass postId
        pLikesTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PostDetailsActivity.this, PostLikedByActivity.class);
                intent.putExtra("postId",postId);
                startActivity(intent);
            }
        });


    }

    private void addToHisNotifications (String hisUid, String pId, String notification){
        String timestamp = "" + System.currentTimeMillis();

        HashMap<Object, String> hashMap = new HashMap<>();
        hashMap.put("pId", pId);
        hashMap.put("timestamp",timestamp);
        hashMap.put("pUid", hisUid);
        hashMap.put("notification", notification);
        hashMap.put("sUid", myUID);

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(hisUid).child("Notifications").child(timestamp).setValue(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // added successfully
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // failed
            }
        });

    }

    private void loadComments() {
        // linear layout for recycler View
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());

        // set layout to recycler view
        recyclerView.setLayoutManager(layoutManager);

        // initializing comment list
        commentList = new ArrayList<>();

        // path of the post to get it's comments
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                commentList.clear();
                for (DataSnapshot ds : snapshot.getChildren()){
                    ModelComment modelComment = ds.getValue(ModelComment.class);
                    commentList.add(modelComment);

                    // initializing adapter and passing myUID and postId to CommentAdapter constructor
                    adapterComment = new AdapterComment(getApplicationContext(), commentList, myUID, postId);

                    // set adapter
                    recyclerView.setAdapter(adapterComment);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void showMoreOptions() {


        PopupMenu popupMenu = new PopupMenu(this, moreBtn, Gravity.END);

        // show delete in posts of only currently signed in user
        if(hisUid.equals(myUID)){
            // adding items in menu
            popupMenu.getMenu().add(Menu.NONE,0,0,"Delete");

        }


        // item click handler
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == 0) {// delete
                    beginDelete();
                }
                return false;
            }
        });

        // show menu
        popupMenu.show();



    }

    private void beginDelete() {
        if(pImage.equals("noImage")){
            // a post without image
            deleteWithoutImage();
        }
        else{
            // a post with image
            deleteWithImage();
        }
    }

    private void deleteWithImage() {
        Toast.makeText(this, "Deleting ...", Toast.LENGTH_SHORT).show();
        StorageReference imgRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        imgRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // image deleted, now delete it from database
                Query fQuery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
                fQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()){
                            ds.getRef().removeValue(); // remove values from firebase wherever pId matches
                        }
                        // deleted
                        Toast.makeText(PostDetailsActivity.this, "Deleted successfully ...", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // image deletion failed
                Toast.makeText(PostDetailsActivity.this, "Failed ..." + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteWithoutImage() {
        Toast.makeText(this, "Deleting ...", Toast.LENGTH_SHORT).show();

        Query fQuery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
        fQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()){
                    ds.getRef().removeValue(); // remove values from firebase wherever pId matches
                }
                // deleted
                Toast.makeText(PostDetailsActivity.this, "Deleted successfully ...", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void setLikes() {
        final DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        likesRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.child(postId).hasChild(myUID)){
                    // user has liked this post
                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_purple, 0,0,0);
                    likeBtn.setText("Liked");

                }
                else{
                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black, 0,0,0);
                    likeBtn.setText("Like");

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void likePost() {
        // get total number of likes for the post to show up
        // if user presses the like button, check whether he liked the post earlier,
        // if he liked it earlier, decrease the likes by 1, else increase by 1
        processLike = true;

        // get id of the post of which like button clicked
        final DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        final DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference().child("Posts");

        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(processLike){
                    if(snapshot.child(postId).hasChild(myUID)){
                        // control flow here indicates the person already liked the post earlier
                        // so remove the like now
                        postsRef.child(postId).child("pLikes").setValue("" + (Integer.parseInt(pLikes) - 1));
                        likesRef.child(postId).child(myUID).removeValue();
                        processLike = false;



                    }
                    else{
                        // not liked earlier, so like it now
                        postsRef.child(postId).child("pLikes").setValue("" + ((Integer.parseInt(pLikes) + 1)));
                        likesRef.child(postId).child(myUID).setValue("Liked");
                        processLike = false;

                        addToHisNotifications(""+hisUid,""+postId,"Liked your post");

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void postComment() {
        // getting data from comment editText
        String comment = commentEt.getText().toString().trim();

        // validate
        if(TextUtils.isEmpty(comment)){
            // no value is entered 
            Toast.makeText(this, "Comment is empty ...", Toast.LENGTH_SHORT).show();
            return;
        }
        String timeStamp = String.valueOf(System.currentTimeMillis());


        // each post will have a child "Comments" containing comments of that post
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");

        HashMap<String, Object> hashMap = new HashMap<>();
        // putting info in hashmap
        hashMap.put("cId", timeStamp);
        hashMap.put("comment",comment);
        hashMap.put("timestamp",timeStamp);
        hashMap.put("uid",myUID);
        hashMap.put("uDp",myDp);
        hashMap.put("uName",myName);

        // put this data in database
        ref.child(timeStamp).setValue(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // added successfully
                Toast.makeText(PostDetailsActivity.this, "Comment added...", Toast.LENGTH_SHORT).show();
                commentEt.setText("");
                updateCommentCount();
                addToHisNotifications(""+ hisUid,""+postId,"Commented on your post");

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(PostDetailsActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        });






    }

    private void updateCommentCount() {
        // whenever user adds a comment, increment the comment count by one
        processComment = true;
        final DatabaseReference ref =FirebaseDatabase.getInstance().getReference("Posts").child(postId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(processComment){
                    String comments = "" + snapshot.child("pComments").getValue();
                    int newCommentVal = Integer.parseInt(comments) + 1;
                    ref.child("pComments").setValue("" + newCommentVal);
                    processComment = false;

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadUserInfo() {
        // getting user info
        Query myRef = FirebaseDatabase.getInstance().getReference("Users");
        myRef.orderByChild("uid").equalTo(myUID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot ds : snapshot.getChildren()){
                    myName = "" + ds.child("name").getValue();
                    myDp = "" + ds.child("image").getValue();

                    // set data
                    try {
                        // set the image received
                        Picasso.get().load(myDp).placeholder(R.drawable.ic_default_img_purple).into(commentAvatarIv);
                    }
                    catch (Exception e) {
                        Picasso.get().load(R.drawable.ic_default_img_purple).into(commentAvatarIv);

                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadPostInfo() {
        // getting post using postId
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = ref.orderByChild("pId").equalTo(postId);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // run the check until the required post is found
                for(DataSnapshot ds : snapshot.getChildren()){
                    // get data
                    String pTitle =  "" + ds.child("pTitle").getValue();
                    String pDescription = "" + ds.child("pDescription").getValue();
                    pLikes = "" + ds.child("pLikes").getValue();
                    String pTimeStamp = "" + ds.child("pTime").getValue();
                    pImage = "" + ds.child("pImage").getValue();
                    hisDp = ""+ds.child("uDp").getValue();
                    hisUid = "" + ds.child("uid").getValue();
                    String uEmail = ""+ds.child("uEmail").getValue();
                    hisName = ""+ds.child("uName").getValue();
                    String commentCount = ""+ds.child("pComments").getValue();

                    // converting timestamp to dd/mm/yyyy hh:mm am/pm
                    Calendar calendar = Calendar.getInstance(Locale.getDefault());
                    calendar.setTimeInMillis(Long.parseLong(pTimeStamp));
                    String pTime = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

                    // set data
                    pTitleTv.setText(pTitle);
                    pDescriptionTv.setText(pDescription);
                    if(Integer.parseInt(pLikes) == 1){
                        pLikesTv.setText(pLikes + " Like");
                    }
                    else{
                        pLikesTv.setText(pLikes + " Likes");
                    }

                    if(Integer.parseInt(commentCount) == 1){
                        pCommentsTv.setText(commentCount + " Comment");
                    }
                    else{
                        pCommentsTv.setText(commentCount + " Comments");
                    }

                    pTimeTv.setText(pTime);
                    nameTv.setText(hisName);

                    // set image of the user who posted
                    // if there is no image, then we hide ImageView
                    if(pImage.equals("noImage")){
                        pImageIv.setVisibility(View.GONE);
                    }
                    else{
                        // show ImageView
                        pImageIv.setVisibility(View.VISIBLE);

                        // set post image
                        try{
                            Picasso.get().load(pImage).placeholder(R.drawable.ic_default_img_purple).into(pImageIv);
                        }
                        catch(Exception e){
                            Picasso.get().load(R.drawable.ic_default_img_purple).into(pImageIv);

                        }
                    }

                    // set user image in the comment section
                    try {
                        Picasso.get().load(hisDp).placeholder(R.drawable.ic_default_img_purple).into(uPictureIv);
                    }
                    catch (Exception e){
                        Picasso.get().load(R.drawable.ic_default_img_purple).into(uPictureIv);
                    }



                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void checkUserStatus(){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user == null){
            // user not signed in, go to main activity
            startActivity(new Intent(this,MainActivity.class));
            finish();
        }
        else{
            // user in signed in
            myEmail = user.getEmail();
            myUID = user.getUid();

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId() == R.id.action_logout){
            FirebaseAuth.getInstance().signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}