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

import com.example.picshi.adapters.AdapterUser;
import com.example.picshi.models.ModelUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


public class UsersFragment extends Fragment {

    RecyclerView recyclerView;
    AdapterUser adapterUser;
    List<ModelUser> userList;
    FirebaseAuth firebaseAuth;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_users, container, false);

        // initializing RecyclerView
        recyclerView = view.findViewById(R.id.users_recycler_view);

        // setting the properties of recyclerView
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // initializing userList
        userList = new ArrayList<>();

        // initializing FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();

        // getAllUsers
        getAllUsers();


        return view;
    }

    private void getAllUsers() {

        // get current user
        final FirebaseUser fUSer = FirebaseAuth.getInstance().getCurrentUser();

        // get path of database named "Users" containing users info
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        // get the data from path
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for(DataSnapshot ds: snapshot.getChildren()){
                    ModelUser modelUser = ds.getValue(ModelUser.class);

                    // get all users except currently signed in user
                    if(!modelUser.getUid().equals(fUSer.getUid())){
                        userList.add(modelUser);
                    }

                    // adapter
                    adapterUser = new AdapterUser(getActivity(),userList);

                    // setting adapter to recycler view
                    recyclerView.setAdapter(adapterUser);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void searchUsers(final String newText) {
        // get current user
        final FirebaseUser fUSer = FirebaseAuth.getInstance().getCurrentUser();

        // get path of database named "Users" containing users info
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        // get the data from path
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for(DataSnapshot ds: snapshot.getChildren()){
                    ModelUser modelUser = ds.getValue(ModelUser.class);

                    // get all searched users except currently signed in user
                    if(!modelUser.getUid().equals(fUSer.getUid())){

                        if(modelUser.getName().toLowerCase().contains(newText.toLowerCase()) ||
                            modelUser.getEmail().toLowerCase().contains(newText.toLowerCase())){
                            userList.add(modelUser);
                        }
                    }

                    // adapter
                    adapterUser = new AdapterUser(getActivity(),userList);

                    // refresh adapter
                    adapterUser.notifyDataSetChanged();

                    // setting adapter to recycler view
                    recyclerView.setAdapter(adapterUser);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

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

        // add post icon must be hidden from here
        menu.findItem(R.id.action_add_post).setVisible(false);

        // SearchView
        MenuItem menuItem = menu.findItem(R.id.action_search);
        SearchView searchView =(SearchView) MenuItemCompat.getActionView(menuItem);


        // search listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // this function is called when user presses search button from keyboard
                // if search query is not empty, then search
                if (!TextUtils.isEmpty(query.trim())){
                    // search whatever is contained in the search text
                    searchUsers(query);
                }
                else{
                    // search text is empty, get all users
                    getAllUsers();
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // this function is called whenever user presses any single letter
                // if search query is not empty, then search
                    if (!TextUtils.isEmpty(newText.trim())){
                        // search whatever is contained in the search text
                        searchUsers(newText);
                    }
                    else{
                        // search text is empty, get all users
                        getAllUsers();
                    }
                return false;
            }
        });

        super.onCreateOptionsMenu(menu,inflater);

    }

    // handle menu item clicks
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        // getting item id
        if (item.getItemId() == R.id.action_logout) {
            firebaseAuth.signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }
}





