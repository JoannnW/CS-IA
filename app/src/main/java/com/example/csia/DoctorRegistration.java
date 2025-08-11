package com.example.csia;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.csia.Firebase.FirebaseDoctor;
import com.example.csia.Identities.Doctor;
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

public class DoctorRegistration extends AppCompatActivity {
    private String username;

    private EditText businessHrsInpt, durationInpt;
    private Button submitBtn;
    private ImageButton submitBtn2, exitBtn;
    private String durationStr;

    //Google Sign-in
    private GoogleSignInClient mGoogleSignInClient;
    private boolean areFieldsValid = false;
    private boolean isGoogleConnected = false;
    private SignInButton googleSignInButton; private TextView btnGoogleUpdate;
    private ActivityResultLauncher<Intent> googleSignInLauncher;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.doctor_register);

        //Initialize Google sign-in
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
                        if (task.isSuccessful()) {
                            GoogleSignInAccount account = task.getResult();
                            isGoogleConnected = true;
                            btnGoogleUpdate = findViewById(R.id.textView20);
                            btnGoogleUpdate.setText("Google connected!");
                            Toast.makeText(this, "Successful Google Sign-in", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Google Sign-In canceled or failed.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        username = getIntent().getStringExtra("username");  // get username from intent

        businessHrsInpt = findViewById(R.id.editTextText6);
        durationInpt = findViewById(R.id.editTextText8);
        submitBtn = findViewById(R.id.button5);
        submitBtn2 = findViewById(R.id.imageButton10);
        exitBtn = findViewById(R.id.imageButton5);


        googleSignInButton.setOnClickListener(v -> {
            Doctor doctor = collectDoctorInpt();
            if (validateDoctor(doctor)){signInWithGoogle(); }
            else { Toast.makeText(this,"Please fill all fields correctly first",Toast.LENGTH_SHORT).show();}});

        submitBtn.setOnClickListener(view -> {
            Doctor doctor = collectDoctorInpt();
            if (validateDoctor(doctor) && isGoogleConnected){ onSubmit(doctor); }
            else if (!isGoogleConnected) {Toast.makeText(this,"Please complete all steps", Toast.LENGTH_SHORT).show();} });

        submitBtn2.setOnClickListener(view -> {
            Doctor doctor = collectDoctorInpt();
            if (validateDoctor(doctor) && isGoogleConnected){ onSubmit(doctor); }
            else if (!isGoogleConnected) {Toast.makeText(this,"Please complete all steps", Toast.LENGTH_SHORT).show();} });

        exitBtn.setOnClickListener(view -> onExit());
    }

    private void onExit(){
        Toast.makeText(this, "Your information was not saved. Returning to login...", Toast.LENGTH_LONG).show();
        new android.os.Handler().postDelayed(()-> {
            Intent intent = new Intent(DoctorRegistration.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }, 2000); //toast display and screen pauses for 2 seconds before screen switches back to login page
    }

    private Doctor collectDoctorInpt(){

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
        if (((CheckBox)findViewById(R.id.checkBox)).isChecked()) daysOpen.add("MON");
        if (((CheckBox)findViewById(R.id.checkBox2)).isChecked()) daysOpen.add("TUE");
        if (((CheckBox)findViewById(R.id.checkBox3)).isChecked()) daysOpen.add("WED");
        if (((CheckBox)findViewById(R.id.checkBox4)).isChecked()) daysOpen.add("THU");
        if (((CheckBox)findViewById(R.id.checkBox5)).isChecked()) daysOpen.add("FRI");
        if (((CheckBox)findViewById(R.id.checkBox6)).isChecked()) daysOpen.add("SAT");
        if (((CheckBox)findViewById(R.id.checkBox7)).isChecked()) daysOpen.add("SUN");
        if (((CheckBox)findViewById(R.id.checkBox8)).isChecked()) daysOpen.add("Public Holidays");

        return new Doctor(username, daysOpen, businessHrs, durationMin, isGoogleConnected);
    }

    private void onSubmit(Doctor doctor){
        //check Google sign in
        if (!isGoogleConnected){
            Toast.makeText(this, "Please connect your Google account", Toast.LENGTH_SHORT).show();
            return;
        }

        //create FirebaseDoctor with all fields
        FirebaseDoctor firebaseDoctor = new FirebaseDoctor(
                doctor.getName(),
                doctor.getOpenDays(),
                doctor.getBusinessHrs(),
                doctor.getDurationMin());

        //Add Google Sign-in status to Firebase data
        firebaseDoctor.setGoogleConnected(true);
        firebaseDoctor.setIdentity("doctor");


        //save to Firebase - only if its validated
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        DatabaseReference newDoctorRef = usersRef.push();
        newDoctorRef.setValue(firebaseDoctor);
        String doctorKey = newDoctorRef.getKey();

        //proceed to intent and go to home page
        Intent intent = new Intent(this, DoctorHome.class);
        intent.putExtra("username", doctor.getName());
        intent.putExtra("businessHours", doctor.getBusinessHrs());
        intent.putExtra("durationInMinutes",doctor.getDurationMin());
        intent.putStringArrayListExtra("openDays", new ArrayList<>(doctor.getOpenDays()));
        intent.putExtra("doctorKey", doctorKey); //needed for rescheduling

        startActivity(intent);
        finish();
    }


    private boolean validateDoctor(Doctor doctor){
        areFieldsValid = false;
        int status = Doctor.isValidHrMin(durationStr);

        //field validations
        //check if any slots are empty/ no checkboxes are checked
        if(doctor.getOpenDays().isEmpty()){
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Doctor.isValidTimeRange(doctor.getBusinessHrs())){
            Toast.makeText(this, "Please enter a valid time range (e.g. 09:00-17:00)", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (status == Doctor.VALID_TIME) {
            areFieldsValid = true;
            return areFieldsValid;
        } else {
            String errorMessage;

            if (status == Doctor.EMPTY_TIME) {
                errorMessage = "Duration cannot be empty";
            }
            else if (status == Doctor.MISSING_H) {
                errorMessage = "Missing 'h' in duration (e.g., 2h30m)";
            }
            else if (status == Doctor.INVALID_FORMAT) {
                errorMessage = "Invalid format - use like 2h30m";
            }
            else if (status == Doctor.HOURS_NOT_NUMBER) {
                errorMessage = "Hours must be a number";
            }
            else if (status == Doctor.MINUTES_NOT_NUMBER) {
                errorMessage = "Minutes must be a number";
            }
            else if (status == Doctor.HOURS_NEGATIVE) {
                errorMessage = "Hours cannot be negative";
            }
            else if (status == Doctor.MINUTES_INVALID) {
                errorMessage = "Minutes must be between 00-59";
            }
            else if (status == Doctor.MISSING_M) {
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
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            // Now launch the sign-in intent AFTER sign out
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }
}