 package com.example.picshi;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.picshi.adapters.AdapterPosts;
import com.example.picshi.models.ModelPost;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


 public class HomeFragment extends Fragment {

     FirebaseAuth firebaseAuth;

     RecyclerView recyclerView;
     List<ModelPost> postList;
     AdapterPosts adapterPosts;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_home, container, false);

        // Initializing FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();

        // setting up recyclerView and it's properties
        recyclerView = view.findViewById(R.id.posts_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());

        // to show newest posts first, we load from last
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);

        // set layout to recycler view
        recyclerView.setLayoutManager(layoutManager);

        // init postList
        postList = new ArrayList<>();

        loadPosts();

        return view;
    }

     private void loadPosts() {
        // path of all posts
         DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");

         // getting all data from this reference
         reference.addValueEventListener(new ValueEventListener() {
             @Override
             public void onDataChange(@NonNull DataSnapshot snapshot) {
                 postList.clear();
                 for(DataSnapshot ds : snapshot.getChildren()){
                     ModelPost modelPost = ds.getValue(ModelPost.class);
                     postList.add(modelPost);

                     // adapter
                     adapterPosts = new AdapterPosts(getActivity(), postList);

                     // set adapter to recycler view
                     recyclerView.setAdapter(adapterPosts);

                 }
             }

             @Override
             public void onCancelled(@NonNull DatabaseError error) {
                // some error occurred
//                 Toast.makeText(getContext(), ""+error.getMessage(), Toast.LENGTH_SHORT).show();
             }
         });
     }

     private void searchPosts(final String searchQuery){

         DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");

         // getting all data from this reference
         reference.addValueEventListener(new ValueEventListener() {
             @Override
             public void onDataChange(@NonNull DataSnapshot snapshot) {
                 postList.clear();
                 for(DataSnapshot ds : snapshot.getChildren()){
                     ModelPost modelPost = ds.getValue(ModelPost.class);

                     if(modelPost.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                             modelPost.getpDescription().toLowerCase().contains(searchQuery.toLowerCase())) {
                         postList.add(modelPost);
                     }

                     // adapter
                     adapterPosts = new AdapterPosts(getActivity(), postList);

                     // set adapter to recycler view
                     recyclerView.setAdapter(adapterPosts);

                 }
             }

             @Override
             public void onCancelled(@NonNull DatabaseError error) {
                 // some error occurred
                 Toast.makeText(getActivity(), ""+error.getMessage(), Toast.LENGTH_SHORT).show();
             }
         });

     }

     @Override
     public void onCreate(Bundle savedInstanceState) {
         setHasOptionsMenu(true); // to show options menu in fragment
         super.onCreate(savedInstanceState);

     }


     private void checkUserStatus(){
         // get current user
         FirebaseUser user;
         user= firebaseAuth.getCurrentUser();
         if(user!=null){
             // if user is signed in stay here and set email of logged in user
//            profileTv.setText(user.getEmail());

         }
         else{
             // if user is not signed in, navigate to main activity
             startActivity(new Intent(getActivity(),MainActivity.class));
             getActivity().finish();
         }
     }


     // inflating options menu
     @Override
     public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

         inflater.inflate(R.menu.menu_main,menu);

         // search view to search posts by post title/description
         MenuItem item = menu.findItem(R.id.action_search);
         SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
         super.onCreateOptionsMenu(menu,inflater);

         // search listener
         searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
             @Override
             public boolean onQueryTextSubmit(String query) {
                 // called when user presses search button
                 if(!TextUtils.isEmpty(query)){
                     searchPosts(query);
                 }
                 else{
                     loadPosts();
                 }
                 return false;
             }

             @Override
             public boolean onQueryTextChange(String newText) {
                 //called when user presses any letter
                 if(!TextUtils.isEmpty(newText)){
                     searchPosts(newText);
                 }
                 else{
                     loadPosts();
                 }
                 return false;
             }
         });
     }

     // handle menu item clicks

     @Override
     public boolean onOptionsItemSelected(@NonNull MenuItem item) {

         // getting item id
         switch(item.getItemId()){
             case R.id.action_logout:
                firebaseAuth.signOut();
                checkUserStatus();
                break;

             case R.id.action_add_post:
                 startActivity(new Intent(getActivity(),AddPostActivity.class));
                 break;
         }
         return super.onOptionsItemSelected(item);
     }
}