package com.example.csia;

import static com.example.csia.Identities.Owner.isValidTimeRange;
import static com.example.csia.Identities.Owner.isValidWeight;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.csia.Firebase.FirebaseOwner;
import com.example.csia.Identities.Owner;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.internal.GoogleSignInOptionsExtensionParcelable;
import com.google.android.gms.common.api.ApiException;
import com.google.api.services.calendar.CalendarScopes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class OwnerRegistration extends AppCompatActivity {

    private String username;

    private EditText storeNameInpt, openingHoursInpt, weightInpt, dailyIntakeInpt, latestShoppingDateInpt;
    private Button submitBtn;
    private ImageButton submitBtn2, exitBtn;

    //Google Sign-in
    private GoogleSignInClient mGoogleSignInClient;
    private boolean areFieldsValid = false;
    private boolean isGoogleConnected = false;
    private Button googleSignInButton; private TextView btnGoogleUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.owner_register);

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
            Owner owner = collectOwnerInpt();
            if (validateOwner(owner)){signInWithGoogle(); }
            else { Toast.makeText(this,"Please fill all fields correctly first",Toast.LENGTH_SHORT).show();}});

        submitBtn.setOnClickListener(view -> {
            Owner owner = collectOwnerInpt();
            if (validateOwner(owner) && isGoogleConnected){ onSubmit(owner); }
            else if (!isGoogleConnected) {Toast.makeText(this,"Please complete all steps", Toast.LENGTH_SHORT).show();} });

        submitBtn2.setOnClickListener(view -> {
            Owner owner = collectOwnerInpt();
            if (validateOwner(owner) && isGoogleConnected){ onSubmit(owner); }
            else if (!isGoogleConnected) {Toast.makeText(this,"Please complete all steps", Toast.LENGTH_SHORT).show();} });

        exitBtn.setOnClickListener(view -> onExit());

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

        String dailyIntakeStr = dailyIntakeInpt.getText().toString().trim().toLowerCase().replace(" ","");
        double dailyIntake = Double.parseDouble(dailyIntakeStr.replace("kg",""));

        String latestShoppingDate = latestShoppingDateInpt.getText().toString().trim();

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
                owner.getWeight(),                 //Total food wiehgt
                owner.getDailyIntake(),            //Daily intake
                owner.getLatestShoppingDate(),     //latest shopping date
                owner.getOpenDays());              //Open days

        //Add Google Sign-in status to Firebase data
        firebaseOwner.setGoogleConnected(true);

        //save to Firebase - only if its validated and get reference
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        DatabaseReference newOwnerRef = usersRef.push();
        newOwnerRef.setValue(firebaseOwner);
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
        if (!Owner.isValidWeight(owner.getWeight())){
            Toast.makeText(this, "Weight must be between 0.00-999.99kg",Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Owner.isValidWeight(owner.getDailyIntake())){
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

    private void signInWithGoogle(){
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent,1001);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == 1001){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            if (task.isSuccessful()){
                GoogleSignInAccount account = task.getResult();
                isGoogleConnected = true;
                btnGoogleUpdate = findViewById(R.id.textView49);
                btnGoogleUpdate.setText("Google connected!");
                Toast.makeText(this,"Successful Google Sign-in", Toast.LENGTH_SHORT).show();

                submitBtn.setVisibility(View.VISIBLE);
                submitBtn2.setVisibility(View.VISIBLE);

            } else {
                try{
                    Exception exception = task.getException();
                    if (exception instanceof ApiException){
                        ApiException apiException = (ApiException) exception;
                        int statuscode = apiException.getStatusCode();
                        Toast.makeText(this,"Google Sign-In failed: " + statuscode, Toast.LENGTH_SHORT).show();
                    } else{
                        //handle other exceptions
                        Toast.makeText(this,"Google Sign-In failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e){
                    Toast.makeText(this, "Unexpected error during sign-in ", Toast.LENGTH_SHORT).show();

                }
            }
        }
    }
}