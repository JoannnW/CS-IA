package com.example.csia;

import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import com.example.csia.Firebase.FirebaseOwner;
import com.example.csia.Identities.Doctor;
import com.example.csia.Identities.Groomer;
import com.example.csia.Identities.Identity;
import com.example.csia.Identities.Owner;
import com.example.csia.Utilities.BusyTime;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.CalendarScopes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;


public class MainActivity extends AppCompatActivity {
    Firebase firestore;

    private EditText editTextName;
    private Button btnGroomer, btnDoctor, btnOwner;
    private DatabaseReference usersRef;
    private GoogleSignInClient mGoogleSignInClient;
    private String currentIdentity;
    private String firebaseUserKey;

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

        //link UI to Java + initialise
        editTextName = findViewById(R.id.editTextText);
        btnOwner = findViewById(R.id.button3);
        btnGroomer = findViewById(R.id.button1);
        btnDoctor = findViewById(R.id.button2);

        //sets up Google sign-in + calendar access permission
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(CalendarScopes.CALENDAR)) //Request access to user's Google Calendar
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

//        //transitions to Google's sign in screen
//        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
//        startActivityForResult(signInIntent, 1001);

        //initialise firebase database
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        //Set listeners
        btnGroomer.setOnClickListener(view -> handleLogin("groomer"));
        btnDoctor.setOnClickListener(view -> handleLogin("doctor"));
        btnOwner.setOnClickListener(view -> handleLogin("owner"));

    }

    private void handleLogin(String identity) {
        String name = editTextName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }
        currentIdentity = identity;

        // Query by name first, then filter by identity
        usersRef.orderByChild("name").equalTo(name)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean userExists = false;
                        boolean isGoogleConnected = false;
                        String userKey = null;
                        String existingIdentity = null;

                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            existingIdentity = userSnap.child("identity").getValue(String.class);
                            String existingName = userSnap.child("name").getValue(String.class);

                            // Check if both name AND identity match
                            if (identity.equalsIgnoreCase(existingIdentity) && name.equalsIgnoreCase(existingName)) {
                                userExists = true;
                                userKey = userSnap.getKey();
                                firebaseUserKey = userKey;

                                // Check Google connection status
                                Boolean googleStatus = userSnap.child("googleConnected").getValue(Boolean.class);
                                isGoogleConnected = (googleStatus != null) && googleStatus;
                                break;
                            }
                        }

                        if (userExists) {
                            if (isGoogleConnected) {
                                // User exists and is Google connected - proceed directly to home
                                firebaseUserKey = userKey; //save unique key
                                proceedToHome(new ArrayList<>(), new ArrayList<>());
                            } else {
                                // this case shouldn't happen.... User exists but not Google connected
                                Toast.makeText(MainActivity.this, "Error: User exists but not Google connected", Toast.LENGTH_SHORT).show();
                                proceedToHome(new ArrayList<>(), new ArrayList<>());
                            }
                        } else {
                            // New user - go to registration
                            Intent registrationIntent = new Intent(MainActivity.this,
                                    identity.equals("doctor") ? DoctorRegistration.class : //Java short-hand notation (“W3Schools.com”)
                                            identity.equals("groomer") ? GroomerRegistration.class : OwnerRegistration.class); //Java short-hand notation (“W3Schools.com”)
                            registrationIntent.putExtra("username", name);
                            startActivity(registrationIntent);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MainActivity.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void initiateGoogleSignIn(){
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, 1001);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            if (task.isSuccessful()) {
                GoogleSignInAccount account = task.getResult();
                // User is signed in and we have the account, now fetch calendar events
                fetchCalendarEvents(account);
            } else {
                Toast.makeText(this, "Sign-in failed", Toast.LENGTH_SHORT).show();
                // If sign-in fails, send them back to the login screen
                Intent loginIntent = new Intent(this, MainActivity.class);
                startActivity(loginIntent);
                finish();
            }
        }
    }

    private void fetchCalendarEvents(GoogleSignInAccount account){
        new Thread(() -> {
            try{
                //1. set up credentials
                GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                        this, Collections.singleton(CalendarScopes.CALENDAR)
                );
                credential.setSelectedAccount(account.getAccount());

                //2. Create Calendar service
                com.google.api.services.calendar.Calendar service =
                        new com.google.api.services.calendar.Calendar.Builder(
                                new NetHttpTransport(),
                                new GsonFactory(),
                                credential)
                                .setApplicationName("My Dog Bernard")
                                .build();

                //3. fetch events
                DateTime now = new DateTime(System.currentTimeMillis());
                com.google.api.services.calendar.model.Events events = service.events()
                        .list("primary")
                        .setTimeMin(now) // NOW onwards (upcoming events, avoids past events)
                        .setOrderBy("startTime")
                        .setSingleEvents(true) // De-recurse repeating events
                        .execute();

                //4. process events - two parts:
                //       a. busytimes for scheduling logic
                //       b. event strings for display
                ArrayList<BusyTime> busyTimes = new ArrayList<>();
                ArrayList<String> eventStrings = new ArrayList<>();

                for (com.google.api.services.calendar.model.Event event : events.getItems()){
                    DateTime start = event.getStart().getDateTime();
                    DateTime end = event.getEnd().getDateTime();

                    //4a: for scheduling (busytimes)
                    if(start != null && end != null){
                        busyTimes.add(new BusyTime(
                                new Date(start.getValue()),
                                new Date(end.getValue())
                        ));
                    }

                    //4b: for display (event strings)
                    String summary;
                    if (event.getSummary() != null){
                        summary = event.getSummary();
                    } else {
                        summary = "(No Title)";
                    }

                    String startTime;
                    if (start != null){
                        startTime = start.toStringRfc3339();
                    } else {
                        startTime = "N/A";
                    }

                    eventStrings.add(summary + " - " + startTime);
                }

                //5. pass both to the next activity
                runOnUiThread(() -> {
                    proceedToHome(busyTimes, eventStrings);
                });
            } catch (Exception e){
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    proceedToHome(new ArrayList<>(), new ArrayList<>());
                });
            }
        }).start();
    }

    private void proceedToHome(ArrayList<BusyTime> busyTimes, ArrayList<String> eventStrings){
        Identity user = getIdentityObject(currentIdentity, editTextName.getText().toString().trim());
        if (user instanceof Owner){
            //fetch the rest of their profile from Firebase before launching
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(firebaseUserKey) //key usage here
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            FirebaseOwner fo = snapshot.getValue(FirebaseOwner.class);
                            if (fo == null){
                                Toast.makeText(MainActivity.this, "Failed to load owner data", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            //build Intent with all the extras
                            Intent home = new Intent(MainActivity.this, OwnerHome.class);
                            home.putExtra("username", fo.getName() != null ? fo.getName() : ""); //Java short-hand notation (“W3Schools.com”)
                            home.putExtra("storeName", fo.getStoreName() != null ? fo.getStoreName() : ""); //Java short-hand notation (“W3Schools.com”)
                            home.putExtra("openingHours", fo.getOpeningHours() != null ? fo.getOpeningHours() : ""); //Java short-hand notation (“W3Schools.com”)
                            home.putExtra("weight", fo.getWeight());
                            home.putExtra("dailyIntake", fo.getDailyIntake());
                            home.putExtra("latestShoppingDate", fo.getLatestShoppingDate() != null ? fo.getLatestShoppingDate() : ""); //Java short-hand notation (“W3Schools.com”)
                            home.putStringArrayListExtra("openDays", fo.getOpenDays() != null ? new ArrayList<>(fo.getOpenDays()) : new ArrayList<>()); //Java short-hand notation (“W3Schools.com”)
                            home.putExtra("ownerKey", firebaseUserKey);
                            home.putExtra("busyTimes", new ArrayList<>(busyTimes));
                            home.putStringArrayListExtra("eventStrings", new ArrayList<>(eventStrings));
                            startActivity(home);
                            finish();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(MainActivity.this, "Error loading owner profile", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else if (user instanceof Groomer){
            Intent home = new Intent(this, GroomerHome.class);
            home.putExtra("username", user.getName());
            home.putExtra("busyTimes", new ArrayList<>(busyTimes));
            home.putStringArrayListExtra("eventStrings", new ArrayList<>(eventStrings));
            home.putExtra("groomerKey", firebaseUserKey);
            home.putExtra("ownerId", "");
            startActivity(home);
            finish();
        } else if (user instanceof Doctor) {
            Intent home = new Intent(this, DoctorHome.class);
            home.putExtra("username", user.getName());
            home.putExtra("busyTimes", new ArrayList<>(busyTimes));
            home.putStringArrayListExtra("eventStrings", new ArrayList<>(eventStrings));
            home.putExtra("doctorKey", firebaseUserKey);
            home.putExtra("ownerId", "");
            startActivity(home);
            finish();
        }
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

}