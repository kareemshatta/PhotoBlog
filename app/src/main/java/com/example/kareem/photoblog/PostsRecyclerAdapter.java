package com.example.kareem.photoblog;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;


import java.util.Date;
import java.util.List;

public class PostsRecyclerAdapter extends RecyclerView.Adapter<PostsRecyclerAdapter.ViewHolder> {


    private List<Post> homeList;
    private Context context;
    private FirebaseFirestore firebaseFirestore;

    public PostsRecyclerAdapter(List<Post> homeList){
        this.homeList = homeList;
    }

    @NonNull
    @Override
    public PostsRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

        context = viewGroup.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.posts_list_view_item,viewGroup,false);
        firebaseFirestore = FirebaseFirestore.getInstance();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final PostsRecyclerAdapter.ViewHolder viewHolder, int i) {

        String descData = homeList.get(i).getDesc();
        viewHolder.setDescText(descData);

        String imageUri =  homeList.get(i).getImage_url();
        viewHolder.setImageView(imageUri);

        String userId = homeList.get(i).getUser_id();
        firebaseFirestore.collection("users").document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){

                    String userName = task.getResult().getString("name");
                    String userImageUri = task.getResult().getString("image");

                    viewHolder.setUserName(userName);
                    viewHolder.setUserImage(userImageUri);

                }
            }
        });

        long millisecond = homeList.get(i).getTimestamp().getTime();
        String dateString = DateFormat.format("dd/MM/yyyy",new Date(millisecond)).toString();
        viewHolder.setPostDate(dateString);


    }

    @Override
    public int getItemCount() {
        return homeList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private View view;
        private TextView descView;
        private TextView userName;
        private TextView postDate;
        private ImageView postImageView;
        private ImageView userImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;
        }

        public void setDescText(String descText){
            descView = view.findViewById(R.id.post_desc);
            descView.setText(descText);

        }
        public void setImageView(String downloadUri){
            postImageView = view.findViewById(R.id.post_image);
            RequestOptions placeholder = new RequestOptions();
            placeholder.placeholder(R.drawable.ic_launcher_background);
            Glide.with(context).applyDefaultRequestOptions(placeholder).load(downloadUri).into(postImageView);
        }
        public void setPostDate(String dateString){
            postDate = view.findViewById(R.id.post_date);
            postDate.setText(dateString);
        }
        public void setUserName(String name){
            userName = view.findViewById(R.id.post_username);
            userName.setText(name);
        }
        public void setUserImage(String imageUri){
            userImage = view.findViewById(R.id.post_user_image);
            RequestOptions placeholder = new RequestOptions();
            placeholder.placeholder(R.mipmap.default_image);
            Glide.with(context).applyDefaultRequestOptions(placeholder).load(imageUri).into(userImage);
        }
    }

}
