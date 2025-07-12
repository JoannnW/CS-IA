package com.example.csia;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import com.example.csia.Identities.Doctor;
import com.example.csia.Identities.Groomer;
import com.example.csia.Identities.Identity;
import com.example.csia.Identities.Owner;
import com.google.firebase.Firebase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class MainActivity extends AppCompatActivity {
    Firebase firestore;

    private EditText editTextName;
    private Button btnGroomer, btnDoctor, btnOwner;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = Firebase.INSTANCE;

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login); //shows login page

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //link GUI to Java + initialise
        editTextName = findViewById(R.id.editTextText);
        btnOwner = findViewById(R.id.button3);
        btnGroomer = findViewById(R.id.button1);
        btnDoctor = findViewById(R.id.button2);

        //initialise firebase database
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        //Set listeners
        btnGroomer.setOnClickListener(view -> handleLogin("groomer"));
        btnDoctor.setOnClickListener(view -> handleLogin("doctor"));
        btnOwner.setOnClickListener(view -> handleLogin("owner"));

    }

    private void handleLogin(String identity){
        String name = editTextName.getText().toString().trim();//trim() removes whitespace from the beginning and end of the string, in case the user accidentally does that
        if (name.isEmpty()){
            Toast.makeText(this,"Please enter your name", Toast.LENGTH_SHORT).show();//first S.C. - whether user inputs name or not
            return;
        }

        usersRef.orderByChild("name").equalTo(name).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) { //runs when data is successfully fetched
                boolean userExists = false;

                for (DataSnapshot userSnap : snapshot.getChildren()){ //advanced for loop: runs through all users to check identity
                    String existingIdentity = userSnap.child("identity").getValue(String.class);
                    if (existingIdentity != null && existingIdentity.equalsIgnoreCase(identity)){ //user's already registered
                        userExists = true;
                        break;
                    }
                }

                Identity user = getIdentityObject(identity, name);
                if (user != null){
                    if (userExists){
                        user.goToHome(MainActivity.this);
                    } else{
                        //Unregistered user --> go to register page
                        usersRef.push().setValue(new User(name, identity)); //save new user to Firebase
                        user.goToRegistration(MainActivity.this);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {//runs if the request fails (e.g. no internet)
                Toast.makeText(MainActivity.this, "Failed to load data.", Toast.LENGTH_SHORT).show();//Firebase error
            }
        });
    }

    private Identity getIdentityObject(String identity, String name){
        if(identity.equalsIgnoreCase("owner")){
            return new Owner(name);
        }
        else if(identity.equalsIgnoreCase("doctor")){
            return new Doctor(name);
        }
        else if(identity.equalsIgnoreCase("groomer")){
            return new Groomer(name);
        }
        else{ return null; }
    }

    //FOR FIREBASE: User class: used for saving new users to or retrieving user data from Firebase/ passing user data between activities
    public static class User {
        public String name;
        public String identity;

        public User(){}

        public User(String name, String identity){
            this.name = name;
            this.identity = identity;
        }
    }
}