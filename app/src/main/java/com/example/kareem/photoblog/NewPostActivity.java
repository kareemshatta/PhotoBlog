package com.example.kareem.photoblog;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import id.zelory.compressor.Compressor;

public class NewPostActivity extends AppCompatActivity {

    private Toolbar newPostToolbar;
    private ImageView newPostImage;
    private TextView newPostDesc;
    private Button newPostBtn;
    private Uri newPostImgUri = null;
    private ProgressBar newPostProgress;
    private StorageReference storageReference;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    private String userId = null;
    private Bitmap compressedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);

        //init
        newPostToolbar = (Toolbar)findViewById(R.id.new_post_toolbar);
        setSupportActionBar(newPostToolbar);
        getSupportActionBar().setTitle("Add New Post");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        newPostImage = findViewById(R.id.new_post_image);
        newPostDesc = findViewById(R.id.new_post_desc);
        newPostBtn = findViewById(R.id.new_post_btn);
        newPostProgress = findViewById(R.id.new_post_progress);

        firebaseFirestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();
        firebaseAuth = FirebaseAuth.getInstance();
        userId = firebaseAuth.getCurrentUser().getUid();


        newPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // start picker to get image for cropping and then use the image in cropping activity
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setMinCropResultSize(512,512)
                        .setAspectRatio(1,1)
                        .start(NewPostActivity.this);
            }
        });

        newPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String desc = newPostDesc.getText().toString();

                if (!TextUtils.isEmpty(desc) && newPostImgUri != null){

                    newPostProgress.setVisibility(View.VISIBLE);
                    final String randomImgName = UUID.randomUUID().toString();
                    StorageReference filePath = storageReference.child("post_images").child(randomImgName+".jpg");
                    filePath.putFile(newPostImgUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            final String downloadImgUrl = task.getResult().getDownloadUrl().toString();
                            if (task.isSuccessful()){

                                File newImgFile = new File(newPostImgUri.getPath());
                                try {

                                    compressedImage = new Compressor(NewPostActivity.this)
                                            .setMaxWidth(100)
                                            .setMaxHeight(100)
                                            .setQuality(2)
                                            .setCompressFormat(Bitmap.CompressFormat.WEBP)
                                            .compressToBitmap(newImgFile);
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    compressedImage.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                    byte[] thumbData = baos.toByteArray();
                                    UploadTask thumbUploadTask = storageReference.child("post_images/thumbs").child(randomImgName+".jpg")
                                            .putBytes(thumbData);
                                    thumbUploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                            String downloadThumbUrl = taskSnapshot.getDownloadUrl().toString();

                                            Map<String,Object> postMap = new HashMap<>();
                                            postMap.put("image_url",downloadImgUrl);
                                            postMap.put("thumb_url",downloadThumbUrl);
                                            postMap.put("desc",desc);
                                            postMap.put("user_id",userId);
                                            postMap.put("timestamp",FieldValue.serverTimestamp());
                                            firebaseFirestore.collection("posts").add(postMap).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                                @Override
                                                public void onComplete(@NonNull Task<DocumentReference> task) {

                                                    if(task.isSuccessful()){
                                                        Toast.makeText(NewPostActivity.this, "Post was added", Toast.LENGTH_LONG).show();
                                                        startActivity(new Intent(getBaseContext(),MainActivity.class));
                                                        finish();

                                                    }else{
                                                        Toast.makeText(NewPostActivity.this, "Post can't added", Toast.LENGTH_LONG).show();

                                                    }
                                                    newPostProgress.setVisibility(View.INVISIBLE);
                                                }
                                            });



                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            //error handling
                                        }
                                    });

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }else{
                                newPostProgress.setVisibility(View.INVISIBLE);
                            }
                        }
                    });

                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                newPostImgUri = result.getUri();
                newPostImage.setImageURI(newPostImgUri);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Toast.makeText(NewPostActivity.this, "Error: "+error, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
