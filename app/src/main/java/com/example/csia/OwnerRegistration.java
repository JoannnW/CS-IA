package com.example.csia;

import static com.example.csia.Identities.Owner.isValidTimeRange;
import static com.example.csia.Identities.Owner.isValidWeight;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.csia.Firebase.FirebaseOwner;
import com.example.csia.Identities.Owner;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.internal.GoogleSignInOptionsExtensionParcelable;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.api.services.calendar.CalendarScopes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class OwnerRegistration extends AppCompatActivity {

    private String username;

    private EditText storeNameInpt, openingHoursInpt, weightInpt, dailyIntakeInpt, latestShoppingDateInpt;
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
        setContentView(R.layout.owner_register);
        btnGoogleUpdate = findViewById(R.id.textView49);

        //Initialize Google sign-in
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope("https://www.googleapis.com/auth/calendar"))
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this,gso);
        googleSignInButton = findViewById(R.id.sign_in_button);

        username = getIntent().getStringExtra("username");  // get username from intent

        storeNameInpt = findViewById(R.id.editTextText2);
        openingHoursInpt = findViewById(R.id.editTextText3);
        weightInpt = findViewById(R.id.editTextText5);
        dailyIntakeInpt = findViewById(R.id.editTextText10);
        latestShoppingDateInpt = findViewById(R.id.editTextText12);
        submitBtn = findViewById(R.id.button5);
        submitBtn2 = findViewById(R.id.imageButton10);
        exitBtn = findViewById(R.id.imageButton5);

        googleSignInButton.setOnClickListener(v -> {
            signInWithGoogle();
        });

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

        submitBtn.setOnClickListener(view -> {
            Owner owner = collectOwnerInpt();
            if (validateOwner(owner) && isGoogleConnected){ onSubmit(owner); }
            else if (!isGoogleConnected) {
                Toast.makeText(this,"Please complete Google account first", Toast.LENGTH_SHORT).show();}
            else {
                Toast.makeText(this, "Please fix validation errors", Toast.LENGTH_SHORT).show();
            } });

        submitBtn2.setOnClickListener(view -> {
            Owner owner = collectOwnerInpt();
            if (validateOwner(owner) && isGoogleConnected){ onSubmit(owner); }
            else if (!isGoogleConnected) {
                Toast.makeText(this,"Please complete Google account first", Toast.LENGTH_SHORT).show();}
            else {
                Toast.makeText(this, "Please fix validation errors", Toast.LENGTH_SHORT).show();
            } });

        exitBtn.setOnClickListener(view -> onExit());

    }

    private void onExit(){
        Toast.makeText(this, "Your information was not saved. Returning to login...", Toast.LENGTH_LONG).show();
        new android.os.Handler().postDelayed(()-> {
            Intent intent = new Intent(OwnerRegistration.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); //reset navigation
            startActivity(intent);
            finish();
        }, 2000); //toast display and screen pauses for 2 seconds before screen switches back to login page
    }

    private Owner collectOwnerInpt(){
        String storeName = storeNameInpt.getText().toString().trim();
        String openingHours = openingHoursInpt.getText().toString().trim();

        double weight = -1;  // default invalid value
        try {
            String weightStr = weightInpt.getText().toString().trim().toLowerCase().replace(" ","");
            weight = Double.parseDouble(weightStr.replace("kg", "").replace(",", "."));
        } catch (NumberFormatException e) {
            // weight remains -1
        }

        double dailyIntake = -1;
        try {
            String dailyIntakeStr = dailyIntakeInpt.getText().toString().trim().toLowerCase().replace(" ","");
            dailyIntake = Double.parseDouble(dailyIntakeStr.replace("kg", "").replace(",", "."));
        } catch (NumberFormatException e) {
            // dailyIntake remains -1
        }


        String latestShoppingDate = latestShoppingDateInpt.getText().toString().trim();

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

        return new Owner(username, storeName, openingHours, weight, dailyIntake, latestShoppingDate, daysOpen, isGoogleConnected);
    }

    private void onSubmit(Owner owner){

        //check Google sign in
        if (!isGoogleConnected){
            Toast.makeText(this, "Please connect your Google account", Toast.LENGTH_SHORT).show();
            return;
        }

        //create FirebaseOwner with all fields
        FirebaseOwner firebaseOwner = new FirebaseOwner(
                owner.getName(),
                owner.getStoreName(),           //Store name
                owner.getOpeningHours(),           //Opening Hrs
                owner.getWeight(),                 //Total food weight
                owner.getDailyIntake(),            //Daily intake
                owner.getLatestShoppingDate(),     //latest shopping date
                owner.getOpenDays());              //Open days

        //Add Google Sign-in status to Firebase data
        firebaseOwner.setGoogleConnected(true);
        firebaseOwner.setIdentity("owner");

        //save to Firebase - only if its validated and get reference
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        DatabaseReference newOwnerRef = usersRef.push();

        newOwnerRef.setValue(firebaseOwner).addOnSuccessListener(aVoid -> {
            String ownerKey = newOwnerRef.getKey();
            //proceed to home page with key for future updates
            Intent intent = new Intent(this, OwnerHome.class);
            intent.putExtra("username", owner.getName());
            intent.putExtra("storeName", owner.getStoreName());
            intent.putExtra("openingHours", owner.getOpeningHours());
            intent.putExtra("weight", owner.getWeight());
            intent.putExtra("dailyIntake", owner.getDailyIntake());
            intent.putExtra("latestShoppingDate", owner.getLatestShoppingDate());
            intent.putStringArrayListExtra("openDays", new ArrayList<>(owner.getOpenDays()));
            intent.putExtra("ownerKey", ownerKey); //needed for rescheduling

            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private boolean validateOwner(Owner owner){
        areFieldsValid = false;
        //field validations
        //check if any slots are empty/ no checkboxes are checked
        if(owner.getStoreName().isEmpty()){
            Toast.makeText(this, "Store name is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Owner.isValidTimeRange(owner.getOpeningHours())){
            Toast.makeText(this, "Please enter a valid time range (e.g. 09:00-17:00)", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (owner.getWeight() == -1 || !Owner.isValidWeight(owner.getWeight())){
            Toast.makeText(this, "Weight must be between 0.00-999.99kg",Toast.LENGTH_SHORT).show();
            return false;
        }
        if (owner.getDailyIntake() == -1 || !Owner.isValidWeight(owner.getDailyIntake())){
            Toast.makeText(this, "Weight must be between 0.00-999.99kg",Toast.LENGTH_SHORT).show();
            return false;
        }
        if (owner.getOpenDays().isEmpty()){
            Toast.makeText(this, "Please select at least one open day", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Owner.isValidDate(owner.getLatestShoppingDate())){
            Toast.makeText(this, "Please enter a valid date (e.g. 30/04/2008)", Toast.LENGTH_SHORT).show();
            return false;
        }

        areFieldsValid = true;
        return areFieldsValid;
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