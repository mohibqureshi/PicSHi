package com.example.picshi.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.picshi.R;
import com.example.picshi.TheirProfileActivity;
import com.example.picshi.models.ModelUser;
import com.squareup.picasso.Picasso;

import java.util.List;

public class AdapterUser extends RecyclerView.Adapter<AdapterUser.MyHolder>{

    Context context;
    List<ModelUser> userList;

    // constructor
    public AdapterUser(Context context, List<ModelUser> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // inflating layout row_users.xml
        View view = LayoutInflater.from(context).inflate(R.layout.row_users, parent,false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        // get data
        final String hisUID = userList.get(position).getUid();
        String userImage = userList.get(position).getImage();
        String userName = userList.get(position).getName();
        final String userEmail = userList.get(position).getEmail();

        // set data
        holder.nameTv.setText(userName);
        holder.emailTv.setText(userEmail);
        try{
            Picasso.get().load(userImage)
                    .placeholder(R.drawable.ic_default_img_purple)
                    .into(holder.avatarIv);
        }
        catch (Exception e){

        }

        //handle item click
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // click to go to TheirProfileActivity with uid, this uid is of clicked user which
                // will be used to show user specific data posts
                Intent intent = new Intent(context, TheirProfileActivity.class);
                intent.putExtra("uid",hisUID);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // creating view holder class
    class MyHolder extends RecyclerView.ViewHolder{

        ImageView avatarIv;
        TextView nameTv,emailTv;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            // initializing views
            avatarIv = itemView.findViewById(R.id.avatar_Iv_row);
            nameTv = itemView.findViewById(R.id.name_tv_row);
            emailTv = itemView.findViewById(R.id.email_tv_row);



        }
    }
}
