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
import com.example.csia.Identities.Doctor;
import com.example.csia.Identities.Groomer;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class GroomerRegistration extends AppCompatActivity {
    private String username;
    private String durationStr;

    private EditText businessHrsInpt, durationInpt;
    private Button submitBtn;
    private ImageButton submitBtn2, exitBtn;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.groomer_register);

        username = getIntent().getStringExtra("username");  // get username from intent

        businessHrsInpt = findViewById(R.id.editTextText7);
        durationInpt = findViewById(R.id.editTextText9);
        submitBtn = findViewById(R.id.button16);
        submitBtn2 = findViewById(R.id.imageButton12);
        exitBtn = findViewById(R.id.imageButton13);


        submitBtn.setOnClickListener(view -> onSubmit());
        submitBtn2.setOnClickListener(view -> onSubmit());
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

    private Groomer collectgroomerInpt(){
        String businessHrs = businessHrsInpt.getText().toString().trim();
        durationStr = durationInpt.getText().toString().trim().toLowerCase().replace(" ","");//remove ANY spaces or uppercase
        String str = ""; boolean flag = false; int durationMin = 0;
        int i = 0; while(i < durationStr.length() && flag == false){
            if(durationStr.charAt(i) == 'h'){        //number before h, times 60; number after h, keep

                for(int j = i; j >= 0; j--){
                    str += durationStr.charAt(j);
                }
                durationMin += Integer.parseInt(str) * 60;
                str = "";
                for (int f = i; f < durationStr.length(); f++){
                    str += durationStr.charAt(f);
                }
                durationMin += Integer.parseInt(str);
                flag = true;
            }
            i++;
        }

        List<String> daysOpen = new ArrayList<>();
        //collect checkboxes
        if (((CheckBox)findViewById(R.id.checkBox)).isChecked()) daysOpen.add("MON");
        if (((CheckBox)findViewById(R.id.checkBox2)).isChecked()) daysOpen.add("TUE");
        if (((CheckBox)findViewById(R.id.checkBox3)).isChecked()) daysOpen.add("WED");
        if (((CheckBox)findViewById(R.id.checkBox4)).isChecked()) daysOpen.add("THU");
        if (((CheckBox)findViewById(R.id.checkBox5)).isChecked()) daysOpen.add("FRI");
        if (((CheckBox)findViewById(R.id.checkBox6)).isChecked()) daysOpen.add("SAT");
        if (((CheckBox)findViewById(R.id.checkBox7)).isChecked()) daysOpen.add("SUN");
        if (((CheckBox)findViewById(R.id.checkBox8)).isChecked()) daysOpen.add("Public Holidays");

        return new Groomer(username, daysOpen, businessHrs, durationMin);
    }

    private void onSubmit(){
        Groomer groomer = collectgroomerInpt();

        if (!validategroomer(groomer)){
            return; //stops process
        }

        //save to Firebase - only if its validated
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        FirebaseGroomer firebaseGroomer = new FirebaseGroomer(groomer.getName(), groomer.getOpenDays(), groomer.getBusinessHrs(), groomer.getDurationMin());
        usersRef.push().setValue(firebaseGroomer); //adds saved details into the firebase

        //proceed to intent and go to home page
        Intent intent = new Intent(this, GroomerHome.class);
        intent.putExtra("username", groomer.getName());
        intent.putExtra("businessHours", groomer.getBusinessHrs());
        intent.putExtra("durationInMinutes",groomer.getDurationMin());
        intent.putStringArrayListExtra("openDays", new ArrayList<>(groomer.getOpenDays()));

        startActivity(intent);
        finish();
    }


    private boolean validategroomer(Groomer groomer){
        int status = Doctor.isValidHrMin(durationStr);


        //field validations
        //check if any slots are empty/ no checkboxes are checked
        if(collectgroomerInpt().getOpenDays().isEmpty()){
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (status == Groomer.VALID_TIME) {
            return true;
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
}