package com.example.kareem.photoblog;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment {

    private RecyclerView homeListView;
    private List<Post> homeList;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private PostsRecyclerAdapter postsRecyclerAdapter;
    private DocumentSnapshot lastVisible;

    public HomeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        homeList = new ArrayList<>();

        homeListView = view.findViewById(R.id.home_list_view);
        firebaseAuth = FirebaseAuth.getInstance();

        postsRecyclerAdapter = new PostsRecyclerAdapter(homeList);
        homeListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        homeListView.setAdapter(postsRecyclerAdapter);

        if (firebaseAuth.getCurrentUser() != null){


            homeListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    boolean reached = !recyclerView.canScrollVertically(1);
                    if (reached){

                        loadMorePosts();
                        //Toast.makeText(container.getContext(), "reached", Toast.LENGTH_SHORT).show();
                    }
                }
            });


            firebaseFirestore = FirebaseFirestore.getInstance();
            Query firstQuery = firebaseFirestore.collection("posts")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(10);
            firstQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

                    // Get the last visible document
                    lastVisible = documentSnapshots.getDocuments()
                            .get(documentSnapshots.size() -1);

                    for (DocumentChange doc : documentSnapshots.getDocumentChanges()){
                        if (doc.getType() == DocumentChange.Type.ADDED){
                            Post post = doc.getDocument().toObject(Post.class);
                            homeList.add(post);
                            postsRecyclerAdapter.notifyDataSetChanged();
                        }
                    }
                }
            });

        }

        return view;
    }
    public void loadMorePosts(){

        Query nextQuery = firebaseFirestore.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(lastVisible)
                .limit(10);

        nextQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

                if (!documentSnapshots.isEmpty()) {
                    // Get the last visible document
                    lastVisible = documentSnapshots.getDocuments()
                            .get(documentSnapshots.size() - 1);

                    for (DocumentChange doc : documentSnapshots.getDocumentChanges()) {
                        if (doc.getType() == DocumentChange.Type.ADDED) {
                            Post post = doc.getDocument().toObject(Post.class);
                            homeList.add(post);
                            postsRecyclerAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }
        });
    }

}
