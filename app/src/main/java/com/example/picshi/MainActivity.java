package com.example.picshi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
// project public facing name project-460128252678
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button registerBtn,loginBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerBtn = findViewById(R.id.signup_btn_main);
        loginBtn = findViewById(R.id.login_btn_main);

        registerBtn.setOnClickListener(this);
        loginBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){

            case R.id.signup_btn_main:
                startActivity(new Intent(MainActivity.this,RegisterActivity.class));
                break;

            case R.id.login_btn_main:
                startActivity(new Intent(MainActivity.this,LoginActivity.class));
                break;

        }
    }
}