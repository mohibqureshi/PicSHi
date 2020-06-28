package com.example.picshi;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener{

    //EditTexts
    EditText etEmail,etPassword;

    //Buttons
    Button registerBtn;

    //Progressbar while user signs up
    AlertDialog alertDialog;

    // Declare an instance of FirebaseAuth
    private FirebaseAuth mAuth;

    TextView haveAccountTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Create Account");

        //back button
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        //getting views
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        registerBtn = findViewById(R.id.signup_btn_register);
        haveAccountTextView = findViewById(R.id.have_account_textview);
        registerBtn.setOnClickListener(this);
        haveAccountTextView.setOnClickListener(this);


        // Initializing FirebaseAuth instance
        mAuth = FirebaseAuth.getInstance();
    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){

            case R.id.signup_btn_register:

                // getting inputs
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                // matching the string patterns for the corresponding inputs
                if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                   etEmail.setError("Email is invalid");
                   etEmail.setFocusable(true);
                }
                // checking if password is at least 8 characters long!
                else if(password.length() < 8){
                   etPassword.setError("Password must be at least 8 characters long!");
                   etPassword.setFocusable(true);
                }

                // else, register the user
                else{
                    registerUser(email,password);
                }
                break;

            case R.id.have_account_textview:
                startActivity(new Intent(RegisterActivity.this,LoginActivity.class));
                finish();
                break;
        }
    }

    private void registerUser(String email, String password) {
        // as email and password is valid. register the user


        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, get current user

                            final FirebaseUser user = mAuth.getCurrentUser();

                            // Get user email and user id from auth
                            String email = user.getEmail();
                            String uid = user.getUid();

                            // As the user is registered,store user info in firebase realtime database using HashMap
                            final HashMap<Object, String> hashMap = new HashMap<>();

                            // putting information in HashMap
                            hashMap.put("email", email);
                            hashMap.put("uid", uid);
                            hashMap.put("name","");
                            hashMap.put("phone","");
                            hashMap.put("image","");
                            hashMap.put("cover","");

                            // create an instance of Firebase database
                            FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

                            // path to store user data named "Users"
                            DatabaseReference reference = firebaseDatabase.getReference("Users");

                            // putting data within hashmap in database
                            reference.child(uid).setValue(hashMap);
                            Toast.makeText(RegisterActivity.this, "Registered \n" + user.getEmail(), Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this, DashboardActivity.class));
                            finish();
                        } else {
                            // If sign in fails, display a message to the user.
//                            progressDialog.dismiss();
                            Toast.makeText(RegisterActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();

                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //dismiss the progressDialog and get and show the error message
//                progressDialog.dismiss();
                Toast.makeText(RegisterActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });




    }
}
