package com.example.picshi.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.picshi.PostDetailsActivity;
import com.example.picshi.PostLikedByActivity;
import com.example.picshi.R;
import com.example.picshi.TheirProfileActivity;
import com.example.picshi.models.ModelPost;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AdapterPosts extends RecyclerView.Adapter<AdapterPosts.MyHolder>{


    Context context;
    List<ModelPost> postList;
    String myUid;

    // Database Reference for Likes database node
    private DatabaseReference likesRef;

    // Database Reference of Posts
    private DatabaseReference postsRef;

    boolean processLike =false;



    public AdapterPosts(Context context, List<ModelPost> postList) {
        this.context = context;
        this.postList = postList;
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        postsRef = FirebaseDatabase.getInstance().getReference().child("Posts");
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // inflating layout row_posts.xml
        View view = LayoutInflater.from(context).inflate(R.layout.row_posts,parent,false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyHolder holder, final int position) {

        // getting data
        final String uid = postList.get(position).getUid();
        String uEmail = postList.get(position).getuEmail();
        String uName = postList.get(position).getuName();
        String uDp = postList.get(position).getuDp();
        final String pId = postList.get(position).getpId();
        String pTitle = postList.get(position).getpTitle();
        final String pImage = postList.get(position).getpImage();
        String pDescription = postList.get(position).getpDescription();
        String pTimeStamp = postList.get(position).getpTime();
        String pLikes = postList.get(position).getpLikes();
        String pComments = postList.get(position).getpComments();

        // converting timestamp to dd/mm/yy hh:mm am/pm
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(pTimeStamp));
        String pTime = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

        // set data
        holder.userNameTv.setText(uName);
        holder.postTimeTv.setText(pTime);
        holder.postTitleTv.setText(pTitle);
        holder.postDescriptionTv.setText(pDescription);
        holder.pCommentsTv.setText(pComments);
        if(Integer.parseInt(pLikes) == 1){
            holder.postLikesTv.setText(pLikes + " Like");
        }
        else {
            holder.postLikesTv.setText(pLikes + " Likes");
        }

        if(Integer.parseInt(pComments) == 1){
            holder.pCommentsTv.setText(pComments + " Comment");
        }
        else {
            holder.pCommentsTv.setText(pComments + " Comments");
        }

        // set likes for each post
        setLikes(holder, pId);

        // set userDp
        try{
            Picasso.get().load(uDp).placeholder(R.drawable.ic_default_img_purple).into(holder.userPictureIv);
        }
        catch(Exception e){


        }

        // if there is no image, then we hide ImageView
        if(pImage.equals("noImage")){
            holder.postImageIv.setVisibility(View.GONE);
        }
        else{
            // show ImageView
            holder.postImageIv.setVisibility(View.VISIBLE);

            // set post image
            try{
                Picasso.get().load(pImage).placeholder(R.drawable.ic_default_img_purple).into(holder.postImageIv);
            }
            catch(Exception e){

            }
        }



        // handling button clicks
        holder.moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMoreOptions(holder.moreBtn, uid, myUid, pId, pImage);
            }
        });
        holder.likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get total number of likes for the post to show up
                // if user presses the like button, check whether he liked the post earlier,
                // if he liked it earlier, decrease the likes by 1, else increase by 1
                final int postLikes = Integer.parseInt(postList.get(position).getpLikes());
                processLike = true;

                // get id of the post of which like button clicked
                final String postId = postList.get(position).getpId();
                likesRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                          if(processLike){
                            if(snapshot.child(postId).hasChild(myUid)){
                                // control flow here indicates the person already liked the post earlier
                                // so remove the like now
                                postsRef.child(postId).child("pLikes").setValue("" + (postLikes - 1));
                                likesRef.child(postId).child(myUid).removeValue();
                                processLike = false;

                            }
                            else{
                                // not liked earlier, so like it now
                                postsRef.child(postId).child("pLikes").setValue("" + (postLikes + 1));
                                likesRef.child(postId).child(myUid).setValue("Liked");
                                processLike = false;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });
        holder.commentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // start PostDetailActivity
                Intent intent = new Intent(context, PostDetailsActivity.class);
                intent.putExtra("postId",pId);
                context.startActivity(intent);
            }
        });

        holder.profileLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // click to go to TheirProfileActivity with uid, this uid is of clicked user which
                // will be used to show user specific data posts
                Intent intent = new Intent(context, TheirProfileActivity.class);
                intent.putExtra("uid",uid);
                context.startActivity(intent);

            }
        });

        // click like count to start PostLikedByActivity and pass postId
        holder.postLikesTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PostLikedByActivity.class);
                intent.putExtra("postId",pId);
                context.startActivity(intent);
            }
        });
    }

    private void setLikes(final MyHolder holder, final String keyPost) {
        likesRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.child(keyPost).hasChild(myUid)){
                    // user has liked this post
                    holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_purple, 0,0,0);
                    holder.likeBtn.setText("Liked");
                }
                else{
                    holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black, 0,0,0);
                    holder.likeBtn.setText("Like");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void showMoreOptions(ImageButton moreBtn, String uid, String myUid, final String pId, final String pImage) {
        // creating pop up menu currently having option delete... shall add more options later
        PopupMenu popupMenu = new PopupMenu(context, moreBtn, Gravity.END);

        // show delete in posts of only currently signed in user
        if(uid.equals(myUid)){
            // adding items in menu
            popupMenu.getMenu().add(Menu.NONE,0,0,"Delete");

        }
        popupMenu.getMenu().add(Menu.NONE,1,0,"View Details");


        // item click handler
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case 0:
                        // delete
                        beginDelete(pId,pImage);
                        break;

                    case 1:
                        // view details
                        Intent intent = new Intent(context,PostDetailsActivity.class);
                        intent.putExtra("postId",pId);
                        context.startActivity(intent);
                }
                return false;
            }
        });

        // show menu
        popupMenu.show();
    }

    private void beginDelete(String pId, String pImage) {
        if(pImage.equals("noImage")){
            // a post without image
            deleteWithoutImage(pId);
        }
        else{
            // a post with image
            deleteWithImage(pId,pImage);
        }
    }

    private void deleteWithImage(final String pId, String pImage) {
        Toast.makeText(context, "Deleting ...", Toast.LENGTH_SHORT).show();
        StorageReference imgRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        imgRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // image deleted, now delete it from database
                Query fQuery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(pId);
                fQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()){
                            ds.getRef().removeValue(); // remove values from firebase wherever pId matches
                        }
                        // deleted
                        Toast.makeText(context, "Deleted successfully ...", Toast.LENGTH_LONG).show();
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
                Toast.makeText(context, "Failed ..." + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteWithoutImage(String pId) {
        Toast.makeText(context, "Deleting ...", Toast.LENGTH_SHORT).show();

        Query fQuery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(pId);
        fQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()){
                    ds.getRef().removeValue(); // remove values from firebase wherever pId matches
                }
                // deleted
                Toast.makeText(context, "Deleted successfully ...", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    // View Holder class
    class MyHolder extends RecyclerView.ViewHolder{

        // views from row_post xml file
        ImageView userPictureIv, postImageIv;
        TextView userNameTv, postTimeTv, postTitleTv, postDescriptionTv, postLikesTv, pCommentsTv;
        ImageButton moreBtn;
        Button likeBtn, commentBtn;
        LinearLayout profileLayout;

        public MyHolder(@NonNull View itemView){

            super(itemView);

            // initializing views
            moreBtn = itemView.findViewById(R.id.more_btn);
            userPictureIv = itemView.findViewById(R.id.user_pic_iv);
            postImageIv = itemView.findViewById(R.id.user_post_image_iv);
            userNameTv = itemView.findViewById(R.id.username_tv);
            postTimeTv = itemView.findViewById(R.id.post_time_tv);
            postTitleTv = itemView.findViewById(R.id.post_title_tv);
            postDescriptionTv = itemView.findViewById(R.id.post_description_tv);
            postLikesTv = itemView.findViewById(R.id.post_likes_tv);
            pCommentsTv = itemView.findViewById(R.id.pCommentsTv);
            likeBtn = itemView.findViewById(R.id.like_btn);
            commentBtn = itemView.findViewById(R.id.comment_btn);
            profileLayout = itemView.findViewById(R.id.profileLayout);

        }
    }


}
