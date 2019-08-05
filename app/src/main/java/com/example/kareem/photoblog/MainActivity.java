package com.example.kareem.photoblog;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private Toolbar mainToolBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFireStore;
    private FloatingActionButton addPostBtn;
    private String currentUserId = null;

    private BottomNavigationView mainBottomNav;
    private Fragment homeFragment;
    private Fragment notificationFragment;
    private Fragment accountFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // for toolbar
        mainToolBar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mainToolBar);
        getSupportActionBar().setTitle(R.string.main_page_title);
        /////////////////////////////////
        mAuth = FirebaseAuth.getInstance();
        mFireStore = FirebaseFirestore.getInstance();


        if (mAuth.getCurrentUser() != null){

            addPostBtn = (FloatingActionButton)findViewById(R.id.add_post_btn);
            mainBottomNav = findViewById(R.id.main_bottom_nav);
            //fragments
            homeFragment = new HomeFragment();
            notificationFragment = new NotificationFragment();
            accountFragment = new AccountFragment();

            replaceFragment(homeFragment);

            mainBottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {


                    switch (menuItem.getItemId()) {
                        case R.id.bottom_action_home:
                            replaceFragment(homeFragment);
                            return true;
                        case R.id.bottom_action_notification:
                            replaceFragment(notificationFragment);
                            return true;
                        case R.id.bottom_account:
                            replaceFragment(accountFragment);
                            return true;
                        default:
                            return false;
                    }
                }
            });

            addPostBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent newPostIntent = new Intent(MainActivity.this,NewPostActivity.class);
                    startActivity(newPostIntent);

                }
            });
        }



    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser == null){
            sendToLogin();
        }else{
            currentUserId = mAuth.getCurrentUser().getUid();
            mFireStore.collection("users").document(currentUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()){
                        if(!task.getResult().exists()){
                            Intent setupIntent = new Intent(MainActivity.this,SetupActivity.class);
                            startActivity(setupIntent);
                            finish();
                        }

                    }
                }
            });

        }

    }

    private void sendToLogin() {
        Intent intent = new Intent(MainActivity.this,LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu,menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.action_logout_btn:
                logout();
                return true;
            case R.id.action_setting_btn:
                sendToSettings();
                return true;

            default:
                return false;
        }
    }

    private void logout() {
        mAuth.signOut();
        sendToLogin();
    }
    private void sendToSettings(){
        Intent settingIntent = new Intent(MainActivity.this,SetupActivity.class);
        startActivity(settingIntent);

    }
    private void replaceFragment(Fragment fragment){
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.main_container,fragment);
        fragmentTransaction.commit();
    }
}
