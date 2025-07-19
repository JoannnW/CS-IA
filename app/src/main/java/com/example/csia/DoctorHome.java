package com.example.csia;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class DoctorHome extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.doctor_homepage);
        String username = getIntent().getStringExtra("username");
        ArrayList<String> openDays = getIntent().getStringArrayListExtra("openDays");
        String businessHrs = getIntent().getStringExtra("businessHours");
        int durationMin = getIntent().getIntExtra("durationInMinutes",0);



        TextView textView = findViewById(R.id.textView2);
        textView.setText("Welcome, Dr. " + username);
    }
}