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

import com.example.csia.Firebase.FirebaseDoctor;
import com.example.csia.Identities.Doctor;
import com.example.csia.Identities.Owner;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class DoctorRegistration extends AppCompatActivity {
    private String username;

    private EditText businessHrsInpt, durationInpt;
    private Button submitBtn;
    private ImageButton submitBtn2, exitBtn;
    private String durationStr;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.doctor_register);

        username = getIntent().getStringExtra("username");  // get username from intent

        businessHrsInpt = findViewById(R.id.editTextText6);
        durationInpt = findViewById(R.id.editTextText8);
        submitBtn = findViewById(R.id.button5);
        submitBtn2 = findViewById(R.id.imageButton10);
        exitBtn = findViewById(R.id.imageButton5);


        submitBtn.setOnClickListener(view -> onSubmit());
        submitBtn2.setOnClickListener(view -> onSubmit());
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

        return new Doctor(username, daysOpen, businessHrs, durationMin);
    }

    private void onSubmit(){
        Doctor doctor = collectDoctorInpt();

        if (!validateDoctor(doctor)){
            return; //stops process
        }

        //save to Firebase - only if its validated
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        FirebaseDoctor firebaseDoctor = new FirebaseDoctor(doctor.getName(), doctor.getOpenDays(),doctor.getBusinessHrs(), doctor.getDurationMin());
        usersRef.push().setValue(firebaseDoctor); //adds saved details into the firebase

        //proceed to intent and go to home page
        Intent intent = new Intent(this, DoctorHome.class);
        intent.putExtra("username", doctor.getName());
        intent.putExtra("businessHours", doctor.getBusinessHrs());
        intent.putExtra("durationInMinutes",doctor.getDurationMin());
        intent.putStringArrayListExtra("openDays", new ArrayList<>(doctor.getOpenDays()));

        startActivity(intent);
        finish();
    }


    private boolean validateDoctor(Doctor doctor){
        int status = Doctor.isValidHrMin(durationStr);

        //field validations
        //check if any slots are empty/ no checkboxes are checked
        if(collectDoctorInpt().getOpenDays().isEmpty()){
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Doctor.isValidTimeRange(doctor.getBusinessHrs())){
            Toast.makeText(this, "Please enter a valid time range (e.g. 09:00-17:00)", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (status == Doctor.VALID_TIME) {
            return true;
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
}