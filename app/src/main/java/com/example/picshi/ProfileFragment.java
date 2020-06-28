package com.example.picshi;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.picshi.adapters.AdapterPosts;
import com.example.picshi.models.ModelPost;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.app.Activity.RESULT_OK;


public class ProfileFragment extends Fragment {

    // Firebase
    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    // getting views
    TextView nameTv,emailTv;
    ImageView profilePicIv,coverPicIv;
    FloatingActionButton floatingActionButton;
    RecyclerView postsRecyclerView;

    // constants for permissions
    private static final int CAMERA_REQUEST_CODE = 10;
    private static final int STORAGE_REQUEST_CODE = 20;
    private static final int IMAGE_PICK_GALLERY_CODE = 30;
    private static final int IMAGE_PICK_CAMERA_CODE = 40;

    List<ModelPost> postList;
    AdapterPosts adapterPosts;
    String uid;


    // permissions to be requested are stored in arrays
    String[] cameraPermissions;
    String[] storagePermissions;

    // uri of picked image
    Uri imageUri;

    // to check if it's a profile photo or a cover photo
    // "profile" for profile photo and "cover" for cover photo
    String profileOrCoverPhoto;

    // Firebase Storage reference
    StorageReference storageReference = FirebaseStorage.getInstance().getReference();

    // path where images of users' profile and cover will be stored
    String storagePath = "Users_Profile_Cover_Images/";

    boolean flag = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("Users");

        // initializing arrays of permissions
        cameraPermissions = new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};


        // Initializing views

        profilePicIv = view.findViewById(R.id.profile_pic_iv);
        nameTv = view.findViewById(R.id.name_tv);
        emailTv = view.findViewById(R.id.email_tv);
        coverPicIv = view.findViewById(R.id.cover_photo_iv);
        floatingActionButton = view.findViewById(R.id.floating_action_btn);
        postsRecyclerView = view.findViewById(R.id.recyclerview_posts);

        // getting information of currently signed in user using user's email,(can do it using uid too but for now we stick to email)
        // Using orderByChild query, we can show detail from a node whose key named email has the value equal to currently signed in
        // user's email.
        Query query = databaseReference.orderByChild("email").equalTo(firebaseUser.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // check until we get the required values
                for(DataSnapshot dataSnapshot : snapshot.getChildren()){
                    // getting data
                    String name = "" + dataSnapshot.child("name").getValue();
                    String email = "" + dataSnapshot.child("email").getValue();
                    String phone = "" + dataSnapshot.child("phone").getValue();
                    String image = "" + dataSnapshot.child("image").getValue();
                    String cover = "" + dataSnapshot.child("cover").getValue();

                    //set data
                    nameTv.setText(name);
                    emailTv.setText(email);
                    if(flag){
                        try {

                            Picasso.get().load(image).resize(120,120).rotate(90).into(profilePicIv);
                        }
                        catch(Exception e){
                            Picasso.get().load(R.drawable.ic_default_img).into(profilePicIv);
                        }
                    }
                    else{
                        try {
                            Picasso.get().load(image).resize(120,120).into(profilePicIv);
                        }
                        catch(Exception e){
                            Picasso.get().load(R.drawable.ic_default_img).into(profilePicIv);
                        }
                    }

                    try {
                        Picasso.get().load(cover).into(coverPicIv);
                    }
                    catch(Exception e){
                        Picasso.get().load(R.drawable.ic_default_img).into(coverPicIv);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditProfileDialog();
            }
        });

        postList = new ArrayList<>();
        checkUserStatus();
        loadMyPosts();


        return view;
    }

    private void loadMyPosts() {
        // linear layout for recycler view
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());

        // show newest posts first, so load from last
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);

        // set this layout to recycler view
        postsRecyclerView.setLayoutManager(layoutManager);

        // initializing posts lists
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");

        // query to load posts
        Query query = reference.orderByChild("uid").equalTo(uid);

        // getting all data from this reference
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot ds : snapshot.getChildren()){
                    ModelPost myPosts = ds.getValue(ModelPost.class);
                    // add to list
                    postList.add(myPosts);

                    // adapter
                    adapterPosts = new AdapterPosts(getActivity(),postList);

                    // setting this adapter to recycler view
                    postsRecyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(getActivity(), ""+error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void searchMyPosts(final String searchQuery) {
        // linear layout for recycler view
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());

        // show newest posts first, so load from last
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);

        // set this layout to recycler view
        postsRecyclerView.setLayoutManager(layoutManager);

        // initializing posts lists
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");

        // query to load posts
        Query query = reference.orderByChild("uid").equalTo(uid);

        // getting all data from this reference
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot ds : snapshot.getChildren()){
                    ModelPost myPosts = ds.getValue(ModelPost.class);

                    if (myPosts.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                    myPosts.getpDescription().toLowerCase().contains(searchQuery.toLowerCase())){

                        // add to list
                        postList.add(myPosts);
                    }

                    // adapter
                    adapterPosts = new AdapterPosts(getActivity(),postList);

                    // setting this adapter to recycler view
                    postsRecyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), ""+error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // a function to check if the storage permission is enabled
    private boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(getActivity(),Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    // a function to request storage permission while app runs
    private void requestStoragePermission(){
        requestPermissions(storagePermissions,STORAGE_REQUEST_CODE);
    }

    // a function to check if the Camera permission is enabled
    private boolean checkCameraPermission(){
        boolean result = ContextCompat.checkSelfPermission(getActivity(),Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);

        boolean result1 = ContextCompat.checkSelfPermission(getActivity(),Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        boolean final_result = result && result1;
        return final_result;
    }

    // a function to request Camera permission while app runs
    private void requestCameraPermission(){
        requestPermissions(cameraPermissions,CAMERA_REQUEST_CODE);
    }

    private void showEditProfileDialog() {

        // options to show in Dialog
        String options[] ={"Edit Profile Picture","Edit Cover Photo","Edit Name","Edit Phone"};

        // alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // set title
        builder.setTitle("Choose an action to edit");
        // setting items to Dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // handling dialog item click
                profileOrCoverPhoto = "image";
                switch(which){

                    case 0:
                        // Edit Profile Picture
                        profileOrCoverPhoto = "image";
                        showImagePicDialog();
                        break;
                    case 1:
                        // Edit Cover Photo
                        profileOrCoverPhoto = "cover";
                        showImagePicDialog();
                        break;
                    case 2:
                        // Edit Name
                        // pass the value as name to update it's value in the database for the user
                        showNamePhoneUpdateDialog("name");
                        break;
                    case 3:
                        // Edit Phone
                        // pass the value as phone to update it's value in the database for the user
                        showNamePhoneUpdateDialog("phone");
                        break;
                }
            }
        });

        // create and show dialog
        builder.create().show();
    }

    private void showNamePhoneUpdateDialog(final String key) {
        // 'key' parameter to this function can take value either from phone or name that
        // define the key in the user's database

        // custom Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Update" + key);

        // setting layout of dialog
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(10,10,10,10);
        final EditText editText = new EditText(getActivity());
        editText.setHint("Enter "+key);
        linearLayout.addView(editText);
        builder.setView(linearLayout);

        // Update button in Dialog
        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // getting the text entered in editText
                final String value = editText.getText().toString().trim();

                // checking if user actually entered in EditText
                if(!TextUtils.isEmpty(value)){
                    HashMap<String,Object> result = new HashMap<>();
                    result.put(key,value);

                    databaseReference.child(firebaseUser.getUid()).updateChildren(result)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // update successful
                            Toast.makeText(getActivity(), "Update successful", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // some error encountered
                            Toast.makeText(getActivity(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                    // if user updates his name, also update his name at his posts
                    if(key.equals("name")) {
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
                        Query query = reference.orderByChild("uid").equalTo(uid);
                        query.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot ds : snapshot.getChildren()){
                                    String child = ds.getKey();
                                    snapshot.getRef().child(child).child("uName").setValue(value);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                        // update Name in comments as well
                        reference.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot ds : snapshot.getChildren()){
                                    String child = ds.getKey();
                                    if(snapshot.child(child).hasChild("Comments")){
                                        String child1 = "" + snapshot.child(child).getKey();
                                        Query child2 = FirebaseDatabase.getInstance().getReference("Posts")
                                                .child(child1).child("Comments").orderByChild("uid").equalTo(uid);
                                        child2.addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                for(DataSnapshot ds : snapshot.getChildren()){
                                                    String child = ds.getKey();
                                                    snapshot.getRef().child(child).child("uName").setValue(value);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                    }


                }
                else{
                    Toast.makeText(getActivity(), "Please enter "+key, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Cancel button in Dialog
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        // creating and showing dialog
        builder.create().show();
    }

    private void showImagePicDialog() {

        // options to show in Dialog
        String options[] ={"Camera","Gallery"};

        // alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // set title
        builder.setTitle("Pick Image From");
        // setting items to Dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // handling dialog item click
                switch(which){
                    case 0:
                        // Camera
                        if (!checkCameraPermission()){
                            requestCameraPermission();
                        }
                        else{
                            pickFromCamera();
                        }
                        break;

                    case 1:
                        // Gallery
                        if (!checkStoragePermission()){
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch(requestCode){
            case CAMERA_REQUEST_CODE:{
                // checking if camera and storage permissions are granted
                if(grantResults.length>0){
                    boolean cameraGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageGranted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (cameraGranted && writeStorageGranted){
                        // permissions granted to pick from Camera
                        pickFromCamera();
                    }

                    else{
                        // permissions denied
                        Toast.makeText(getActivity(), "Camera & Storage Permissions denied", Toast.LENGTH_LONG).show();

                    }

                }
            }
            break;
            case STORAGE_REQUEST_CODE:{

                // checking if storage permissions are granted
                if(grantResults.length>0){
                    boolean writeStorageGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (writeStorageGranted){
                        // permissions granted to pick from Gallery
                        pickFromGallery();
                    }

                    else{
                        // permissions denied
                        Toast.makeText(getActivity(), "Storage Permission denied", Toast.LENGTH_LONG).show();

                    }
                }
            }
            break;
        }
    }

    // This method is called when user has picked the image either from Camera or Gallery
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {


        if(resultCode == RESULT_OK){
            Toast.makeText(getActivity(), "uploading image ... please wait!", Toast.LENGTH_LONG).show();
            if(requestCode == IMAGE_PICK_GALLERY_CODE){
                // image is picked from Gallery, get it's uri
                imageUri = data.getData();

                uploadProfileCoverPhoto(imageUri);
            }

            if(requestCode == IMAGE_PICK_CAMERA_CODE){
                // image is picked from Gallery, get it's uri

                uploadProfileCoverPhoto(imageUri);


            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploadProfileCoverPhoto(final Uri image_Uri) {

        // Using uid of current user as name of the image so only one image is there each for profile picture and cover photo

        // setting path and name for image to be stored in firebase storage
        String filePathAndName = storagePath + "" + profileOrCoverPhoto + "_" + firebaseUser.getUid();
        StorageReference storageReference1 = storageReference.child(filePathAndName);
        storageReference1.putFile(image_Uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                // image is uploaded to storage, get the image url to store in user's database
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while(!uriTask.isSuccessful());
                final Uri downloadUri = uriTask.getResult();

                //checking if image is uploaded and url is received
                if(uriTask.isSuccessful()){

                    // image is uploaded
                    // update url in user's database
                    HashMap<String, Object> results = new HashMap<>();

                    // first parameter specifies if the photo is profile or a cover photo while the
                    // second parameter specifies the string url of the photo stored in firebase storage
                    results.put(profileOrCoverPhoto, downloadUri.toString());

                    databaseReference.child(firebaseUser.getUid()).updateChildren(results)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // control flow here indicates image url is added successfully to the database
                                    Toast.makeText(getActivity(), "Image updated successfully", Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // error adding url, get and show error message
                            Toast.makeText(getActivity(), "" + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });

                    // if user updates his name, also update his name at his posts
                    if(profileOrCoverPhoto.equals("image")) {
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
                        Query query = reference.orderByChild("uid").equalTo(uid);
                        query.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot ds : snapshot.getChildren()){
                                    String child = ds.getKey();
                                    snapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                        // update user image in user's comments on posts
                        // update Name in comments as well
                        reference.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot ds : snapshot.getChildren()){
                                    String child = ds.getKey();
                                    if(snapshot.child(child).hasChild("Comments")){
                                        String child1 = "" + snapshot.child(child).getKey();
                                        Query child2 = FirebaseDatabase.getInstance().getReference("Posts")
                                                .child(child1).child("Comments").orderByChild("uid").equalTo(uid);
                                        child2.addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                for(DataSnapshot ds : snapshot.getChildren()){
                                                    String child = ds.getKey();
                                                    snapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                    }



                }
                else{
                    // there is some error
                    Toast.makeText(getActivity(), "error uploading image", Toast.LENGTH_SHORT).show();
                }

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getActivity(), ""+e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

    }

    private void pickFromCamera() {

        // picking image from device camera
        flag = true;
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION,"Temp Description");

        // put image uri
        imageUri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);

        // Start Camera Intent
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent,IMAGE_PICK_CAMERA_CODE);

    }

    private void pickFromGallery() {
        // picking image from Gallery
        flag = false;
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent,IMAGE_PICK_GALLERY_CODE);

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
            uid = user.getUid();

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

        MenuItem item = menu.findItem(R.id.action_search);

        // search view to search user specific posts
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // called when user presses search button
                if(!TextUtils.isEmpty(query)){
                    searchMyPosts(query);
                }
                else{
                    loadMyPosts();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // called whenever user presses any letter
                if(!TextUtils.isEmpty(newText)){
                    searchMyPosts(newText);
                }
                else{
                    loadMyPosts();
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