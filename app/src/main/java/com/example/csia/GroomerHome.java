package com.example.csia;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class GroomerHome extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.groomer_homepage);
        String username = getIntent().getStringExtra("username");
        ArrayList<String> openDays = getIntent().getStringArrayListExtra("openDays");

        TextView textView = findViewById(R.id.textView2);
        textView.setText("Welcome, " + username);
    }
}