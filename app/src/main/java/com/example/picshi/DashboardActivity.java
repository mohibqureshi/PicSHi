package com.example.picshi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class DashboardActivity extends AppCompatActivity {

    // getting views
    TextView profileTv;

    // firebase auth
    FirebaseAuth firebaseAuth;

    // Initializing ActionBar
    ActionBar actionBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        actionBar = getSupportActionBar();
        actionBar.setTitle("Profile");


        // initializing firebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();

        // getting bottom navigation view
        BottomNavigationView navigationView = findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(selectedListener);

        // Home fragment transaction, by default on start
        actionBar.setTitle("Home");
        HomeFragment homeFragment = new HomeFragment();
        FragmentTransaction homeFragmentTransaction = getSupportFragmentManager().beginTransaction();
        homeFragmentTransaction.replace(R.id.content,homeFragment,"");
        homeFragmentTransaction.commit();


    }

    private BottomNavigationView.OnNavigationItemSelectedListener selectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            // handling item clicks
            switch(item.getItemId()){
                case R.id.nav_home:
                    // home fragment transaction
                    actionBar.setTitle("Home");
                    HomeFragment homeFragment = new HomeFragment();
                    FragmentTransaction homeFragmentTransaction = getSupportFragmentManager().beginTransaction();
                    homeFragmentTransaction.replace(R.id.content,homeFragment,"");
                    homeFragmentTransaction.commit();
                    return true;

                case R.id.nav_profile:
                    // profile fragment transaction
                    // home fragment transaction
                    actionBar.setTitle("Profile");
                    ProfileFragment profileFragment = new ProfileFragment();
                    FragmentTransaction profileFragmentTransaction = getSupportFragmentManager().beginTransaction();
                    profileFragmentTransaction.replace(R.id.content,profileFragment,"");
                    profileFragmentTransaction.commit();
                    return true;

                case R.id.nav_all_users:
                    // all users fragment transaction
                    actionBar.setTitle("Users");
                    UsersFragment usersFragment = new UsersFragment();
                    FragmentTransaction usersFragmentTransaction = getSupportFragmentManager().beginTransaction();
                    usersFragmentTransaction.replace(R.id.content,usersFragment,"");
                    usersFragmentTransaction.commit();
                    return true;

            }

            return false;
        }
    };

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
            startActivity(new Intent(DashboardActivity.this,MainActivity.class));
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onStart() {

        super.onStart();
        checkUserStatus();
    }


}
