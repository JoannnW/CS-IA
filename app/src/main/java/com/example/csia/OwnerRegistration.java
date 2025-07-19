package com.example.csia;

import static com.example.csia.Identities.Owner.isValidTimeRange;
import static com.example.csia.Identities.Owner.isValidWeight;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.csia.Firebase.FirebaseOwner;
import com.example.csia.Identities.Owner;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class OwnerRegistration extends AppCompatActivity {

    private String username;

    private EditText storeNameInpt, openingHoursInpt, weightInpt;
    private Button submitBtn;
    private ImageButton submitBtn2, exitBtn;
    //pdf not done

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.owner_register);

        username = getIntent().getStringExtra("username");  // get username from intent

        storeNameInpt = findViewById(R.id.editTextText2);
        openingHoursInpt = findViewById(R.id.editTextText3);
        weightInpt = findViewById(R.id.editTextText5);
        submitBtn = findViewById(R.id.button5);
        submitBtn2 = findViewById(R.id.imageButton10);
        exitBtn = findViewById(R.id.imageButton5);

        submitBtn.setOnClickListener(view -> onSubmit());
        submitBtn2.setOnClickListener(view -> onSubmit());
        exitBtn.setOnClickListener(view -> onExit());

        //pdf missing
    }

    private void onExit(){
        Toast.makeText(this, "Your information was not saved. Returning to login...", Toast.LENGTH_LONG).show();
        new android.os.Handler().postDelayed(()-> {
            Intent intent = new Intent(OwnerRegistration.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }, 2000); //toast display and screen pauses for 2 seconds before screen switches back to login page
    }

    private Owner collectOwnerInpt(){
        String storeName = storeNameInpt.getText().toString().trim();
        String openingHours = openingHoursInpt.getText().toString().trim();

        String weightStr = weightInpt.getText().toString().trim().toLowerCase().replace(" ","");//remove ANY spaces or uppercase
        double weight = Double.parseDouble(weightStr.replace("kg", ""));

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

        return new Owner(username, storeName, openingHours, weight, daysOpen);
    }

    private void onSubmit(){
        Owner owner = collectOwnerInpt();

        if (!validateOwner(owner)){
            return; //stops process
        }

        //save to Firebase - only if its validated
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        FirebaseOwner firebaseOwner = new FirebaseOwner(owner.getName(),owner.getOpeningHours(),owner.getOpeningHours(),owner.getWeight(),owner.getOpenDays());
        usersRef.push().setValue(firebaseOwner); //adds saved details into the firebase

        //proceed to intent and go to home page
        Intent intent = new Intent(this, OwnerHome.class);
        intent.putExtra("username", owner.getName());
        intent.putExtra("storeName", owner.getStoreName());
        intent.putExtra("openingHours", owner.getOpeningHours());
        intent.putExtra("weight", owner.getWeight());
        intent.putStringArrayListExtra("openDays", new ArrayList<>(owner.getOpenDays()));

        startActivity(intent);
        finish();
    }

    private boolean validateOwner(Owner owner){

        //field validations
        //check if any slots are empty/ no checkboxes are checked
        if(owner.getStoreName().isEmpty() || owner.getOpeningHours().isEmpty() || owner.getWeight() < 0.0 || owner.getOpenDays().isEmpty()){
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Owner.isValidTimeRange(owner.getOpeningHours())){
            Toast.makeText(this, "Please enter a valid time range (e.g. 09:00-17:00)", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Owner.isValidWeight(owner.getWeight())){
            Toast.makeText(this, "Please enter weight in format XX.XXkg or XXX.XXkg", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}