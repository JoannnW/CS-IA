package com.example.csia;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.csia.Firebase.FirebaseGroomer;
import com.example.csia.Identities.Groomer;
import com.example.csia.Utilities.BusyTime;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.util.ArrayList;

public class GroomerRegistration extends AppCompatActivity {
    private String username;

    private String durationStr;
    private EditText businessHrsInpt, durationInpt;
    private Button submitBtn;
    private ImageButton submitBtn2, exitBtn;

    //Google Sign-in
    private GoogleSignInClient mGoogleSignInClient;
    private boolean areFieldsValid = false;
    private boolean isGoogleConnected = false;
    private SignInButton googleSignInButton; private TextView btnGoogleUpdate;
    private ActivityResultLauncher<Intent> googleSignInLauncher;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.groomer_register);

        //Initialize Google sign-in
        btnGoogleUpdate = findViewById(R.id.textView20);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope("https://www.googleapis.com/auth/calendar"))
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this,gso);
        googleSignInButton = findViewById(R.id.sign_in_button);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            isGoogleConnected = true;

                            if (btnGoogleUpdate != null) {
                                btnGoogleUpdate.setText("Google connected!");
                            }

                            Toast.makeText(this, "Google Sign-in successful!", Toast.LENGTH_SHORT).show();

                        } catch (ApiException e) {
                            Toast.makeText(this, "Google Sign-In failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
                            isGoogleConnected = false;
                        }
                    } else {
                        Toast.makeText(this, "Google Sign-In canceled or failed.", Toast.LENGTH_SHORT).show();
                        isGoogleConnected = false;
                    }
                }
        );

        username = getIntent().getStringExtra("username");  // get username from intent

        businessHrsInpt = findViewById(R.id.editTextText7);
        durationInpt = findViewById(R.id.editTextText9);
        submitBtn = findViewById(R.id.button16);
        submitBtn2 = findViewById(R.id.imageButton12);
        exitBtn = findViewById(R.id.imageButton13);

        googleSignInButton.setOnClickListener(v -> {
            Groomer groomer = collectGroomerInpt();
            if (validateGroomer(groomer)){signInWithGoogle(); }
            else { Toast.makeText(this,"Please fill all fields correctly first",Toast.LENGTH_SHORT).show();}});

        submitBtn.setOnClickListener(view -> {
            Groomer groomer = collectGroomerInpt();
            if (validateGroomer(groomer) && isGoogleConnected){ onSubmit(groomer); }
            else if (!isGoogleConnected) {Toast.makeText(this,"Please complete all steps", Toast.LENGTH_SHORT).show();} });

        submitBtn2.setOnClickListener(view -> {
            Groomer groomer = collectGroomerInpt();
            if (validateGroomer(groomer) && isGoogleConnected){ onSubmit(groomer); }
            else if (!isGoogleConnected) {Toast.makeText(this,"Please complete all steps", Toast.LENGTH_SHORT).show();} });

        exitBtn.setOnClickListener(view -> onExit());
    }

    private void onExit(){
        Toast.makeText(this, "Your information was not saved. Returning to login...", Toast.LENGTH_LONG).show();
        new android.os.Handler().postDelayed(()-> {
            Intent intent = new Intent(GroomerRegistration.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }, 2000); //toast display and screen pauses for 2 seconds before screen switches back to login page
    }

    private Groomer collectGroomerInpt(){
        String businessHrs = businessHrsInpt.getText().toString().trim();
        durationStr = durationInpt.getText().toString().trim().toLowerCase().replace(" ", "");
        int durationMin = 0;

        try {
            int hIndex = durationStr.indexOf("h");
            int mIndex = durationStr.indexOf("m");

            if (hIndex != -1) {
                String hoursStr = durationStr.substring(0, hIndex);
                durationMin += Integer.parseInt(hoursStr) * 60;
            }

            if (mIndex != -1 && mIndex > hIndex) {
                String minutesStr = durationStr.substring(hIndex + 1, mIndex);
                durationMin += Integer.parseInt(minutesStr);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid duration format", Toast.LENGTH_SHORT).show();
        }

        ArrayList<String> daysOpen = new ArrayList<>();
        //collect checkboxes
        if (((CheckBox)findViewById(R.id.checkBox11)).isChecked()) daysOpen.add("MON");
        if (((CheckBox)findViewById(R.id.checkBox12)).isChecked()) daysOpen.add("TUE");
        if (((CheckBox)findViewById(R.id.checkBox9)).isChecked()) daysOpen.add("WED");
        if (((CheckBox)findViewById(R.id.checkBox16)).isChecked()) daysOpen.add("THU");
        if (((CheckBox)findViewById(R.id.checkBox13)).isChecked()) daysOpen.add("FRI");
        if (((CheckBox)findViewById(R.id.checkBox15)).isChecked()) daysOpen.add("SAT");
        if (((CheckBox)findViewById(R.id.checkBox14)).isChecked()) daysOpen.add("SUN");
        if (((CheckBox)findViewById(R.id.checkBox10)).isChecked()) daysOpen.add("Public Holidays");

        return new Groomer(username, daysOpen, businessHrs, durationMin, isGoogleConnected);
    }

    private void onSubmit(Groomer groomer){

        //check Google sign in
        if (!isGoogleConnected){
            Toast.makeText(this, "Please connect your Google account", Toast.LENGTH_SHORT).show();
            return;
        }

        //create FirebaseGroomer with all fields
        FirebaseGroomer firebaseGroomer = new FirebaseGroomer(
                groomer.getName(),
                groomer.getOpenDays(),
                groomer.getBusinessHrs(),
                groomer.getDurationMin());

        //Add Google Sign-in status to Firebase data
        firebaseGroomer.setGoogleConnected(true);
        firebaseGroomer.setIdentity("groomer");

        // Save to Firebase
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        DatabaseReference newGroomerRef = usersRef.push();
        newGroomerRef.setValue(firebaseGroomer)
                .addOnSuccessListener(aVoid -> {
                    String groomerKey = newGroomerRef.getKey();

                    // Proceed to home page with key for future updates
                    Intent intent = new Intent(this, GroomerHome.class);
                    intent.putExtra("username", groomer.getName());
                    intent.putExtra("businessHours", groomer.getBusinessHrs());
                    intent.putExtra("durationInMinutes", groomer.getDurationMin());
                    intent.putStringArrayListExtra("openDays", new ArrayList<>(groomer.getOpenDays()));
                    intent.putExtra("groomerKey", groomerKey);
                    intent.putExtra("busyTimes", new ArrayList<BusyTime>());
                    intent.putStringArrayListExtra("eventStrings", new ArrayList<String>());
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private boolean validateGroomer(Groomer groomer){
        areFieldsValid = false;
        int status = Groomer.isValidHrMin(durationStr);

        //field validations
        //check if any slots are empty/ no checkboxes are checked
        if(groomer.getOpenDays().isEmpty()){
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Groomer.isValidTimeRange(groomer.getBusinessHrs())){
            Toast.makeText(this, "Please enter a valid time range (e.g. 09:00-17:00)", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (status == Groomer.VALID_TIME) {
            areFieldsValid = true;
            return areFieldsValid;
        } else {
            String errorMessage;

            if (status == Groomer.EMPTY_TIME) {
                errorMessage = "Duration cannot be empty";
            }
            else if (status == Groomer.MISSING_H) {
                errorMessage = "Missing 'h' in duration (e.g., 2h30m)";
            }
            else if (status == Groomer.INVALID_FORMAT) {
                errorMessage = "Invalid format - use like 2h30m";
            }
            else if (status == Groomer.HOURS_NOT_NUMBER) {
                errorMessage = "Hours must be a number";
            }
            else if (status == Groomer.MINUTES_NOT_NUMBER) {
                errorMessage = "Minutes must be a number";
            }
            else if (status == Groomer.HOURS_NEGATIVE) {
                errorMessage = "Hours cannot be negative";
            }
            else if (status == Groomer.MINUTES_INVALID) {
                errorMessage = "Minutes must be between 00-59";
            }
            else if (status == Groomer.MISSING_M) {
                errorMessage = "Missing 'm' in duration";
            }
            else {
                errorMessage = "Invalid duration format";
            }

            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            return false;
        }
    }


    private void signInWithGoogle() {
        // sign out to ensure the account chooser always appears
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // after sign-out is complete, start the sign-in intent
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }
}