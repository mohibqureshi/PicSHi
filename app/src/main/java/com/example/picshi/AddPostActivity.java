package com.example.picshi;

import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

public class AddPostActivity extends AppCompatActivity {

    // FirebaseAuth
    FirebaseAuth firebaseAuth;
    DatabaseReference databaseReference;

    ActionBar actionBar;

    // constants for permissions
    private static final int CAMERA_REQUEST_CODE = 10;
    private static final int STORAGE_REQUEST_CODE = 20;

    // constants to pick images
    private static final int IMAGE_PICK_CAMERA_CODE = 30;
    private static final int IMAGE_PICK_GALLERY_CODE = 40;

    // user info
    String name,email,dp, uid;

    // arrays to store permissions
    String[] cameraPermissions;
    String[] storagePermissions;

    // getting views
    EditText postTitleEt, postDescriptionEt;
    ImageView postImageIv;
    Button postUploadBtn;



    // picked image uri
    Uri imageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        actionBar = getSupportActionBar();
        actionBar.setTitle("Add New Post");

        // enable back button to go back
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        // Initializing FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();
        checkUserStatus();
        actionBar.setSubtitle(email);

        // initializing permissions arrays
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        // initializing views
        postTitleEt = findViewById(R.id.post_title_et);
        postDescriptionEt = findViewById(R.id.post_description_et);
        postImageIv = findViewById(R.id.post_image_iv);
        postUploadBtn = findViewById(R.id.post_upload_btn);

        // Upload Image click handler
        postImageIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // show image pick dialog
                showImagePickDialog();
            }
        });

        // get some info of current user
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        Query query = databaseReference.orderByChild("email").equalTo(email);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot ds : snapshot.getChildren()){
                    name = ""+ds.child("name").getValue();
                    email = ""+ds.child("email").getValue();
                    dp = ""+ds.child("image").getValue();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        // upload button click handler
        postUploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // getting title and description
                String title = postTitleEt.getText().toString().trim();
                String description = postDescriptionEt.getText().toString().trim();
                if(TextUtils.isEmpty(title)){
                    postTitleEt.setError("Post title cannot be empty");
                    Toast.makeText(AddPostActivity.this, "Please enter title ...", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(description)){
                    postDescriptionEt.setError("Post Description cannot be empty");
                    Toast.makeText(AddPostActivity.this, "Please enter description ...", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(imageUri != null){
                    Toast.makeText(AddPostActivity.this, "Publishing post... please wait!", Toast.LENGTH_SHORT).show();
                    uploadData(title,description,String.valueOf(imageUri));
                }
                else{
                    // post without image
                    Toast.makeText(AddPostActivity.this, "Publishing post... please wait!", Toast.LENGTH_SHORT).show();
                    uploadData(title,description,"noImage");
                }

            }
        });
    }

    private void uploadData(final String title, final String description, String valueOfUri) {
        // for post-image name, post-id and post-publish time
        final String timeStamp = String.valueOf(System.currentTimeMillis());

        String filePathAndName = "Posts/" + "post_" + timeStamp;
        if(!valueOfUri.equals("noImage")){
            // post with image
            StorageReference reference = FirebaseStorage.getInstance().getReference().child(filePathAndName);
            reference.putFile(Uri.parse(valueOfUri)).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // image is uploaded to firebase storage successfully, so get it's uri
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                    while(!uriTask.isSuccessful());
                    String downloadUri = uriTask.getResult().toString();
                    if(uriTask.isSuccessful()){
                        // url is received, so upload post to firebase database
                        HashMap<Object, String> hashMap = new HashMap<>();

                        // put post info
                        hashMap.put("uid",uid);
                        hashMap.put("uName",name);
                        hashMap.put("uEmail",email);
                        hashMap.put("uDp",dp);
                        hashMap.put("pId",timeStamp);
                        hashMap.put("pTitle",title);
                        hashMap.put("pDescription",description);
                        hashMap.put("pImage",downloadUri);
                        hashMap.put("pTime",timeStamp);
                        hashMap.put("pLikes","0");
                        hashMap.put("pComments","0");

                        // path to store post data
                        DatabaseReference ref =  FirebaseDatabase.getInstance().getReference("Posts");

                        // put data in this reference
                        ref.child(timeStamp).setValue(hashMap)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // post published
                                        Toast.makeText(AddPostActivity.this, "Post published ...", Toast.LENGTH_LONG).show();

                                        // reset views
                                        postTitleEt.setText("");
                                        postDescriptionEt.setText("");
                                        postImageIv.setImageDrawable(getDrawable(R.drawable.ic_add_image_post));
                                        imageUri = null;
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // some error occurred, get and show error message
                                        Toast.makeText(AddPostActivity.this, ""+ e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });

                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // there was some error uploading the image, so get and show the error message
                    Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        else{
            // post without image

            HashMap<Object, String> hashMap = new HashMap<>();

            // put post info
            hashMap.put("uid",uid);
            hashMap.put("uName",name);
            hashMap.put("uEmail",email);
            hashMap.put("uDp",dp);
            hashMap.put("pId",timeStamp);
            hashMap.put("pTitle",title);
            hashMap.put("pDescription",description);
            hashMap.put("pImage","noImage");
            hashMap.put("pTime",timeStamp);
            hashMap.put("pLikes","0");
            hashMap.put("pComments","0");

            // path to store post data
            DatabaseReference ref =  FirebaseDatabase.getInstance().getReference("Posts");

            // put data in this reference
            ref.child(timeStamp).setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // post published
                            Toast.makeText(AddPostActivity.this, "Post published ...", Toast.LENGTH_LONG).show();
                            // reset views
                            postTitleEt.setText("");
                            postDescriptionEt.setText("");
                            postImageIv.setImageDrawable(getDrawable(R.drawable.ic_add_image_post));
                            imageUri = null;
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // some error occurred, get and show error message
                            Toast.makeText(AddPostActivity.this, ""+ e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void showImagePickDialog() {

        String[] options= {"Camera","Gallery"};

        // build Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Image from:");

        // setting options to Dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // item click handler
                switch (which){
                    case 0:
                        if(!checkCameraPermission()){
                            requestCameraPermission();
                        }
                        else {
                            pickFromCamera();
                        }
                        break;

                    case 1:
                        if(!checkStoragePermission()){
                            requestStoragePermission();
                        }
                        else{
                            pickFromGallery();
                        }
                        break;
                }
            }
        });

        // create and show dialog
        builder.create().show();
    }

    private void pickFromCamera() {

        // intent to pick image from Camera
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE,"Temp Pick");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION,"Temp Description");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);

    }

    private void pickFromGallery() {

        // intent to pick image from gallery
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // after user has successfully picked image from camera or gallery, this function is called
        if(resultCode == RESULT_OK){
            switch(requestCode){
                case IMAGE_PICK_GALLERY_CODE:
                    imageUri = data.getData();
                    postImageIv.setImageURI(imageUri);
                    break;

                case IMAGE_PICK_CAMERA_CODE:

                    postImageIv.setImageURI(imageUri);
                    break;
            }

        }

        super.onActivityResult(requestCode, resultCode, data);
    }































    // function to handle permissions results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // When user either allows or denies, this method is called, so we shall handle the two cases here

        switch(requestCode){
            case CAMERA_REQUEST_CODE:{
                if(grantResults.length>0){
                    boolean cameraGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageGranted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if(cameraGranted && storageGranted){
                        pickFromCamera();
                    }
                    else{
                        Toast.makeText(this, "Camera & storage permissions denied ...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE:{
                if(grantResults.length > 0){
                    boolean storageGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if(storageGranted){
                        pickFromGallery();
                    }
                    else{
                        Toast.makeText(this, "Storage permission denied ...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;

        }


        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onStart(){
        super.onStart();
        checkUserStatus();
    }

    @Override
    protected void onResume(){
        super.onResume();
        checkUserStatus();
    }

    // function to check if storage permission is granted
    private boolean checkStoragePermission(){

        boolean result = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);

        return result;
    }

    // function to request storage permission while the app is running
    private void requestStoragePermission(){
        ActivityCompat.requestPermissions(this,storagePermissions,STORAGE_REQUEST_CODE);
    }


    // function to check if camera permission is granted
    private boolean checkCameraPermission(){

        boolean result = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);

        boolean result1 = ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);
        boolean finalResult = result && result1;
        return finalResult;
    }

    // function to request Camera permission while the app is running
    private void requestCameraPermission(){
        ActivityCompat.requestPermissions(this,cameraPermissions,CAMERA_REQUEST_CODE);
    }

    private void checkUserStatus(){
        // get current user
        FirebaseUser user;
        user= firebaseAuth.getCurrentUser();
        if(user==null){
            // if user is not signed in, navigate to main activity
            startActivity(new Intent(this,MainActivity.class));
            finish();
        }
        else{
            email = user.getEmail();
            uid = user.getUid();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        // go to previous Activity
        onBackPressed();

        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main,menu);

        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);
        return super.onCreateOptionsMenu(menu);
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