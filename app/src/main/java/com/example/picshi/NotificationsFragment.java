package com.example.picshi;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.picshi.adapters.AdapterNotification;
import com.example.picshi.models.ModelNotification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


public class NotificationsFragment extends Fragment {

    // recycler view for notifications
    RecyclerView notificationsRv;

    // FirebaseAuth
    FirebaseAuth firebaseAuth;

    private ArrayList<ModelNotification> notificationsList;

    private AdapterNotification adapterNotification;

    public NotificationsFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_notifications, container, false);

        // initialize recycler view
        notificationsRv = view.findViewById(R.id.notificationsRv);

        firebaseAuth = FirebaseAuth.getInstance();

        getAllNotifications();

        return view;
    }

    private void getAllNotifications() {
        notificationsList = new ArrayList<>();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid()).child("Notifications").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notificationsList.clear();
                for (DataSnapshot ds : snapshot.getChildren()){
                    // get data
                    ModelNotification model = ds.getValue(ModelNotification.class);

                    // add to list
                    notificationsList.add(model);
                }

                // adapter
                adapterNotification = new AdapterNotification(getActivity(), notificationsList);

                // set adapter to recyclerview
                notificationsRv.setAdapter(adapterNotification);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


}

