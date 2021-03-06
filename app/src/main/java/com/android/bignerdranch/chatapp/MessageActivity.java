package com.android.bignerdranch.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.bignerdranch.chatapp.Adapter.MessageAdapter;
import com.android.bignerdranch.chatapp.Fragments.APIService;
import com.android.bignerdranch.chatapp.Model.Chat;
import com.android.bignerdranch.chatapp.Model.User;
import com.android.bignerdranch.chatapp.Notifications.Client;
import com.android.bignerdranch.chatapp.Notifications.Data;
import com.android.bignerdranch.chatapp.Notifications.MyResponse;
import com.android.bignerdranch.chatapp.Notifications.Sender;
import com.android.bignerdranch.chatapp.Notifications.Token;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageActivity extends AppCompatActivity {

    CircleImageView mProfileImage;
    TextView mUserName;

    FirebaseUser mFirebaseUser;
    DatabaseReference mReference;
    Intent mIntent;
    Toolbar mToolbar;

    ImageButton mSendButton;
    EditText mSendText;

    MessageAdapter messageAdapter;
    List<Chat> mChat;

    RecyclerView recyclerView;

    ValueEventListener seenListener;
    String userid;

    APIService apiService;
    boolean notify = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        // thi???t l???p toolbar th??nh app bar
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MessageActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });

        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        // li??n k???t c??c bi???n v???i c??c Widget
        mProfileImage = findViewById(R.id.profile_image);
        mUserName = findViewById(R.id.username);
        mSendButton = (ImageButton) findViewById(R.id.btn_send);
        mSendText = (EditText) findViewById(R.id.text_send);

        // l???y Intent c???a Activity hi???n t???i
        mIntent = getIntent();

        // l???y n???i dung extra
        userid = mIntent.getStringExtra("userid");

        // l???y d??? li???u ng?????i d??ng hi???n t???i
        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notify = true;

                // l???y n???i dung tin nh???n
                String msg = mSendText.getText().toString();
                if(!msg.equals("")){
                    sendMessage(mFirebaseUser.getUid(), userid, msg);
                } else{
                    // b??o l???i
                    String youcantsendemptymessage = getResources().getString(R.string.youcantsendemptymessage);
                    Toast.makeText(MessageActivity.this, youcantsendemptymessage,Toast.LENGTH_SHORT).show();
                }
                // ????a v??? r???ng
                mSendText.setText("");
            }
        });

        mReference = FirebaseDatabase.getInstance().getReference("Users").child(userid);

        mReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                mUserName.setText(user.getUsername());
                if (user.getImageURL().equals("default")){
                    // set ???nh m???c ?????nh
                    mProfileImage.setImageResource(R.mipmap.ic_launcher);
                } else {
                    // t???i ???nh l??n theo url
                    Glide.with(getApplicationContext()).load(user.getImageURL()).into(mProfileImage);
                }

                readMessages(mFirebaseUser.getUid(), userid, user.getImageURL());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        seenMessage(userid);
    }

    private void seenMessage(final String userid){

        mReference = FirebaseDatabase.getInstance().getReference("Chats");

        seenListener =mReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){

                    Chat chat = snapshot.getValue(Chat.class);

                    if (chat.getReceiver().equals(mFirebaseUser.getUid()) && chat.getSender().equals(userid)){

                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("isseen", true);
                        // l???y tham chi???u ?????n v??? tr?? d??? li???u, update thu???c t??nh "isseen" c???a ?????i t?????ng Chat
                        snapshot.getRef().updateChildren(hashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    private void sendMessage(String sender, String receiver, String message){

        // l???y tham chi???u ?????n c?? s??? d??? li???u
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        // t???o bi???n l??u tr?? d??? li???u
        HashMap<String, Object> hashMap = new HashMap<>();

        // th???c hi???n l??u d??? li???u v??o hashmap
        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("message", message);
        hashMap.put("isseen", false);

        // ?????y d??? li???u v??o tr?????ng d??? li???u "Chats" trong Firebase
        reference.child("Chats").push().setValue(hashMap);

        // kh???i t???o ?????i t?????ng l???y tham chi???u ?????n v??? tr?? tr?????ng con c???a "Chatlist"
        final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(mFirebaseUser.getUid())
                .child(userid);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()){
                    // set gi?? tr??? "id" l?? id ng?????i nh???n tin nh???n
                    chatRef.child("id").setValue(userid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // kh???i t???o ?????i t?????ng l???y tham chi???u ?????n v??? tr?? tr?????ng con c???a "Chatlist"
        final DatabaseReference chatRefReceiver = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(userid)
                .child(mFirebaseUser.getUid());

        // set gi?? tr??? "id" l?? id ng?????i d??ng hi???n t???i
        chatRefReceiver.child("id").setValue(mFirebaseUser.getUid());

        final String msg = message;

        // l???y d??? li???u tham chi???u t??? tr?????ng d??? li???u "Users" theo Id ng?????i d??ng
        reference = FirebaseDatabase.getInstance().getReference("Users").child(mFirebaseUser.getUid());

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (notify) {
                    sendNotifiaction(receiver, user.getUsername(), msg);
                }
                notify = false;
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // g???i th??ng b??o t??? api Service
    private void sendNotifiaction(String receiver, final String username, final String message){

        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");

        // t???o truy v???n theo id ng?????i nh???n
        Query query = tokens.orderByKey().equalTo(receiver);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){

                    Token token = snapshot.getValue(Token.class);
                    String newmessage = getResources().getString(R.string.newmessage);

                    Data data = new Data(mFirebaseUser.getUid(), R.mipmap.ic_launcher, username+": "+message, newmessage,
                            userid);

                    Sender sender = new Sender(data, token.getToken());

                    apiService.sendNotification(sender)
                            .enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                                    if (response.code() == 200){
                                        if (response.body().success != 1){
                                            String failed = getResources().getString(R.string.failed);
                                            Toast.makeText(MessageActivity.this, failed, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<MyResponse> call, Throwable t) {

                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // ?????c tin nh???n nh???n ???????c
    private void readMessages(String myid, String userid, String imageurl) {

        // kh???i t???o m???ng ch???a c??c tin nh???n
        mChat = new ArrayList<>();

        // l???y d??? li???u tham chi???u t??? tr?????ng d??? li???u "Chats"
        mReference = FirebaseDatabase.getInstance().getReference("Chats");

        mReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mChat.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    // l???y d??? li???u t??? database, s???p x???p th??nh d??? li???u c???a l???p Chat
                    Chat chat = dataSnapshot.getValue(Chat.class);

                    if (chat.getReceiver().equals(myid) && chat.getSender().equals(userid) ||
                    chat.getReceiver().equals(userid) && chat.getSender().equals(myid)) {
                        mChat.add(chat);
                    }

                    // kh???i t???o Adater
                    messageAdapter = new MessageAdapter(MessageActivity.this, mChat, imageurl);
                    // c??i adapter l??n recyclerview
                    recyclerView.setAdapter(messageAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void currentUser(String userid){
        // t???o editor ????? s???a file
        SharedPreferences.Editor editor = getSharedPreferences("PREFS", MODE_PRIVATE).edit();
        // th??m id ng?????i d??ng
        editor.putString("currentuser", userid);
        // x??c nh???n ch???nh s???a
        editor.apply();
    }

    private void status(String status){
        mReference = FirebaseDatabase.getInstance().getReference("Users").child(mFirebaseUser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        mReference.updateChildren(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        status("online");
        currentUser(userid);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mReference.removeEventListener(seenListener);
        status("offline");
        currentUser("none");
    }
}