package com.example.csia;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class OwnerHome extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.owner_homepage);

        String userName = getIntent().getStringExtra("userName");
        TextView textView = findViewById(R.id.textView2); // Example ID
        textView.setText("Welcome, " + userName);
    }
}