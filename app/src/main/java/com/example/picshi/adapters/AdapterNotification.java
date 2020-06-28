package com.example.picshi.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.picshi.PostDetailsActivity;
import com.example.picshi.R;
import com.example.picshi.models.ModelNotification;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class AdapterNotification extends RecyclerView.Adapter<AdapterNotification.HolderNotification>{

    private Context context;
    private FirebaseAuth firebaseAuth;
    private ArrayList<ModelNotification> notificationsList;

    public AdapterNotification(Context context, ArrayList<ModelNotification> notificationsList) {
        this.context = context;
        this.notificationsList = notificationsList;
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public HolderNotification onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // inflate view row_notifications.xml
        View view = LayoutInflater.from(context).inflate(R.layout.row_notifications, parent, false);
        return new HolderNotification(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final HolderNotification holder, int position) {
        // get data
        final ModelNotification model = notificationsList.get(position);
        String name = model.getsName();
        final String notification = model.getNotification();
        String image = model.getsImage();
        final String timestamp = model.getTimestamp();
        String senderUid = model.getsUid();
        final String pId = model.getpId();

        // converting timestamp to dd/mm/yy hh:mm am/pm
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(timestamp));
        final String pTime = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

        // we'll get name, email, image of user of notification from his uid
        DatabaseReference  reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.orderByChild("uid").equalTo(senderUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()){
                    String name = ""+ds.child("name").getValue();
                    String image = ""+ds.child("image").getValue();
                    String email = ""+ds.child("email").getValue();


                    // adding the acquired data to model
                    model.setsName(name);
                    model.setsEmail(email);
                    model.setsImage(image);
                    // set data to views
                    holder.nameTv.setText(name);

                    try{
                        Picasso.get().load(image).placeholder(R.drawable.ic_default_img_purple).into(holder.avatarIv);
                    }
                    catch (Exception e){
                        holder.avatarIv.setImageResource(R.drawable.ic_default_img_purple);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        holder.notificationTv.setText(notification);
        holder.timeTv.setText(pTime);

        // click notification to open post
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PostDetailsActivity.class);
                intent.putExtra("postId",pId);
                context.startActivity(intent);
            }
        });

    }

    @Override
    public int getItemCount() {
        return notificationsList.size();
    }

    // holder class to hold row_notifications.xml views
    class HolderNotification extends RecyclerView.ViewHolder{

        // declaring views
        ImageView avatarIv;
        TextView nameTv, notificationTv, timeTv;

        public HolderNotification(@NonNull View itemView) {

            super(itemView);

            // initializing views
            avatarIv = itemView.findViewById(R.id.avatarIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            notificationTv = itemView.findViewById(R.id.notificationTv);
            timeTv = itemView.findViewById(R.id.timeTv);



        }
    }
}
