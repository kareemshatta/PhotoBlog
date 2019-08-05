package com.example.kareem.photoblog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class SetupActivity extends AppCompatActivity {

    private CircleImageView setupImage;
    private Uri mainImageUri = null;
    private EditText setupName;
    private Button setupBtn;
    private ProgressBar setupProgBar;
    private String userId = null;
    private boolean isChanged = false;

    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;
    private FirebaseFirestore mFireStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        Toolbar setupToolbar = (Toolbar)findViewById(R.id.setup_toolbar);
        setSupportActionBar(setupToolbar);
        getSupportActionBar().setTitle(R.string.setup_page_title);


        //initialization of variables

        setupName = (EditText)findViewById(R.id.setup_name);
        setupBtn = (Button) findViewById(R.id.setup_save_btn);
        setupImage = (CircleImageView) findViewById(R.id.setup_image);
        setupProgBar = (ProgressBar) findViewById(R.id.setup_progressBar);
        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mFireStore = FirebaseFirestore.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        setupProgBar.setVisibility(View.VISIBLE);
        setupBtn.setEnabled(false);
        mFireStore.collection("users").document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    if(task.getResult().exists()){
                        Toast.makeText(SetupActivity.this, "Not exist", Toast.LENGTH_SHORT).show();

                        mainImageUri = Uri.parse(task.getResult().get("image").toString());
                        RequestOptions requestOptions = new RequestOptions();
                        requestOptions.placeholder(R.mipmap.default_image);
                        Glide.with(SetupActivity.this).setDefaultRequestOptions(requestOptions).load(mainImageUri).into(setupImage);

                        String name = task.getResult().get("name").toString();
                        setupName.setText(name);

                    }else{
                        Toast.makeText(SetupActivity.this, "Not exist", Toast.LENGTH_SHORT).show();
                    }

                }else{
                    String error = task.getException().getMessage();
                    Toast.makeText(SetupActivity.this, "FireStore Retrieving Error: "+error, Toast.LENGTH_SHORT).show();
                }
                setupProgBar.setVisibility(View.INVISIBLE);
                setupBtn.setEnabled(true);
            }
        });



        setupImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    if (ContextCompat.checkSelfPermission(SetupActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(SetupActivity.this, "permission denied", Toast.LENGTH_LONG).show();
                        ActivityCompat.requestPermissions(SetupActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

                    }else{
                        bringImagePicker();
                    }
                }else{
                    bringImagePicker();
                }
            }
        });


        setupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String userName = setupName.getText().toString().trim();
                if (!TextUtils.isEmpty(userName) && mainImageUri != null) {

                    if (isChanged) {

                        setupProgBar.setVisibility(View.VISIBLE);
                        StorageReference imagePath = mStorageRef.child("profile_images").child(userId + ".jpg");
                        imagePath.putFile(mainImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                                if (task.isSuccessful()) {

                                    storeFireStore(task, userName);

                                } else {
                                    String error = task.getException().getMessage();
                                    Toast.makeText(SetupActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                                }
                                setupProgBar.setVisibility(View.INVISIBLE);
                            }
                        });

                    } else {
                        storeFireStore(null, userName);

                    }
                }else{
                    Toast.makeText(SetupActivity.this, "Please fill the fields...", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    private void bringImagePicker(){
        // start picker to get image for cropping and then use the image in cropping activity
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1,1)
                .start(SetupActivity.this);

    }

    private void storeFireStore(@NonNull Task<UploadTask.TaskSnapshot> task,String userName){
        Uri downloadUri;
        if (task != null){
            downloadUri = task.getResult().getDownloadUrl();
        }else{
            downloadUri = mainImageUri;
        }

        Map<String,String> userMap = new HashMap<>();
        userMap.put("name",userName);
        userMap.put("image",downloadUri.toString());

        mFireStore.collection("users").document(userId).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    finish();
                    Toast.makeText(SetupActivity.this, "Settings is uploaded", Toast.LENGTH_SHORT).show();

                }else{
                    String error = task.getException().getMessage();
                    Toast.makeText(SetupActivity.this, "FireStore Error: "+error, Toast.LENGTH_SHORT).show();

                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                mainImageUri = result.getUri();
                //To get screen width and height.
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                final int height = displayMetrics.heightPixels;
                final int width = displayMetrics.widthPixels;

                setupImage.setImageURI(mainImageUri);
                setupImage.getLayoutParams().height = (width/3)-10;
                setupImage.getLayoutParams().width = (width/3)-10;
                setupImage.requestLayout();


            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Toast.makeText(SetupActivity.this, "Error: "+error, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
