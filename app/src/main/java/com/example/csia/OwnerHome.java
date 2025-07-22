package com.example.csia;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.csia.Firebase.FirebaseAppointmentReq;
import com.example.csia.Firebase.FirebaseDoctor;
import com.example.csia.Firebase.FirebaseGroomer;
import com.example.csia.Firebase.FirebaseOwner;
import com.example.csia.Utilities.AppointmentOption;
import com.example.csia.Utilities.AppointmentRequest;
import com.example.csia.Utilities.AppointmentSlotCalc;
import com.example.csia.Utilities.BusyTime;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OwnerHome extends AppCompatActivity {
    private String username;
    private double weight;
    private double dailyIntake;
    private String latestShoppingDateStr;
    private String ownerKey; //used to track Firebase key
    private String status;

    private List<BusyTime> busyTimes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.owner_homepage);

        //get intent extras
        username = getIntent().getStringExtra("username");
        String storeName = getIntent().getStringExtra("storeName");
        String openingHours = getIntent().getStringExtra("openingHours");
        weight = getIntent().getDoubleExtra("weight", 0.0);
        dailyIntake = getIntent().getDoubleExtra("dailyIntake", 0.0);
        latestShoppingDateStr = getIntent().getStringExtra("latestShoppingDate");
        ownerKey = getIntent().getStringExtra("ownerKey"); //added for firebase update
        ArrayList<String> openDays = getIntent().getStringArrayListExtra("openDays");
        busyTimes = (ArrayList<BusyTime>) getIntent().getSerializableExtra("busyTimes");
        if (busyTimes == null){ busyTimes = new ArrayList<>(); }

        //initalise UI
        TextView welcomeTxt = findViewById(R.id.textView2);
        welcomeTxt.setText("Welcome, " + username);

        TextView storeNameTxt = findViewById(R.id.textView16);
        storeNameTxt.setText("Food supply from " + storeName);

        fetchOwnerDataFromFirebase();

        //set up refresh buttons - for all 3 options
        findViewById(R.id.imageButton9).setOnClickListener(v -> loadDoctors());
        findViewById(R.id.imageButton8).setOnClickListener(v -> loadGroomers());

        //set up reschedule button - shoppingDate
        Button rescheduleBtn = findViewById(R.id.button);
        rescheduleBtn.setOnClickListener(v -> rescheduleShopping());

        loadDoctors();
        loadGroomers();
    }

    private void updateFoodInfo(){
        //calc food left
        double foodLeft = calculateFoodLeft(weight, dailyIntake, latestShoppingDateStr);
        TextView foodLeftTxt = findViewById(R.id.textView21);
        foodLeftTxt.setText(String.format("%.2fkg", foodLeft));

        //calc next shopping date
        String nextShopDate = calculateNextShoppingDate(weight, dailyIntake, latestShoppingDateStr);
        TextView nextShopTxt = findViewById(R.id.textView23);
        nextShopTxt.setText(nextShopDate);
    }

    private double calculateFoodLeft(double totalWeight, double dailyIntake, String lastDateStr){
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date lastDate = sdf.parse(lastDateStr);
            Date today = new Date();

            long diff = today.getTime() - lastDate.getTime();
            int daysPassed = (int) (diff / (1000 * 60 * 60 * 24));

            double consumed = daysPassed * dailyIntake;
            return Math.max(0, totalWeight - consumed);
        } catch (ParseException e){
            return 0.0;
        }
    }

    private String calculateNextShoppingDate(double totalWeight, double dailyIntake, String lastDateStr){
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date lastDate = sdf.parse(lastDateStr);

            int totalDays = (int) (totalWeight / dailyIntake);
            int safeDays = Math.max(0, totalDays - 3); //never negative

            //target date = last date _ safeDays
            Calendar cal = Calendar.getInstance();
            cal.setTime(lastDate);
            cal.add(Calendar.DAY_OF_YEAR, safeDays);

            //today
            Calendar today = Calendar.getInstance();

            if (cal.before(today)){
                cal = today;
            }

            return sdf.format(cal.getTime());
        } catch (ParseException e){
            return "DD/MM/YYYY";
        }
    }

    private void rescheduleShopping(){
        //get today's date
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day =c.get(Calendar.DAY_OF_MONTH);

        //show date picker dialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    //format the date
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    String newDate = sdf.format(selectedDate.getTime());

                    //update Firebase
                    updateShoppingDateInFirebase(newDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void updateShoppingDateInFirebase(String newDate){
        if(ownerKey != null){
            DatabaseReference ownerRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(ownerKey)
                    .child("latestShoppingDate");
            ownerRef.setValue(newDate)
                    .addOnSuccessListener(aVoid -> {
                        //update local data and UI
                        latestShoppingDateStr = newDate;
                        updateFoodInfo();
                        Toast.makeText(this, "Shopping date updated!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->{
                        Toast.makeText(this, "Failed to update date: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void loadDoctors(){
        Query doctorsQuery = FirebaseDatabase.getInstance().getReference("users")
                .orderByChild("identity").equalTo("doctor");

        doctorsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot doctorSnap : snapshot.getChildren()) {
                    FirebaseDoctor doctor = doctorSnap.getValue(FirebaseDoctor.class);
                    if (doctor != null) {
                        //need to implement this with the actual Google calendar api
                        List<String> slots = AppointmentSlotCalc.findAvailableSlot(
                                busyTimes,
                                doctor,
                                doctor.openDays,
                                doctor.durationMin
                        );
                        showSlots(doctor.name, slots, "doctor");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error){
                Toast.makeText(OwnerHome.this, "Failed to load doctor's availabilities", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadGroomers(){
        Query groomersQuery = FirebaseDatabase.getInstance().getReference("users")
                .orderByChild("identity").equalTo("groomer");

        groomersQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot groomerSnap : snapshot.getChildren()) {
                    FirebaseGroomer groomer = groomerSnap.getValue(FirebaseGroomer.class);
                    if (groomer != null) {
                        List<String> slots = AppointmentSlotCalc.findAvailableSlot(
                                busyTimes, groomer, groomer.openDays, groomer.durationMin
                        );
                        showSlots(groomer.name, slots, "groomer");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error){
                Toast.makeText(OwnerHome.this, "Failed to load groomer's availabilities", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSlots(String providerName, List<String> slots, String type){
        int[] slotTextIds;
        int[] buttonIds;

        if (type.equals("groomer")){
            slotTextIds = new int[]{R.id.textView27, R.id.textView29, R.id.textView30};
            buttonIds = new int[]{R.id.button11, R.id.button4, R.id.button12};
        } else { //doctor
            slotTextIds = new int[]{R.id.textView37, R.id.textView42, R.id.textView43};
            buttonIds = new int[]{R.id.button13, R.id.button14, R.id.button15};
        }

        for (int i = 0; i < 3; i ++){
            TextView slotText = findViewById(slotTextIds[i]);
            Button slotButton = findViewById(buttonIds[i]);

            if (i < slots.size()){
                slotText.setText("Option " + (i + 1) + ": " + slots.get(i));

                //create final copies for use
                final String finalProviderName = providerName;
                final String finalSlot = slots.get(i);
                final String finalType = type;

                slotButton.setOnClickListener(v -> requestAppointment(finalProviderName, finalSlot, finalType));
                slotText.setVisibility(View.VISIBLE);
                slotButton.setVisibility(View.VISIBLE);
            } else{
                slotText.setVisibility(View.GONE);
                slotButton.setVisibility(View.GONE);
            }
        }
    }

    private void requestAppointment(String providerName, String slotTime, String type) {
        // Create request with all necessary info
        AppointmentOption option = new AppointmentOption(slotTime);
        AppointmentRequest request = new AppointmentRequest(ownerKey, providerName, type, option);

        // Save to Firebase
        DatabaseReference requestsRef = FirebaseDatabase.getInstance()
                .getReference("appointment_requests");
        String requestKey = requestsRef.push().getKey();
        requestsRef.child(requestKey).setValue(request);

        // Disable all buttons for this provider
        int[] buttonIds = type.equals("groomer")
                ? new int[]{R.id.button11, R.id.button4, R.id.button12}
                : new int[]{R.id.button13, R.id.button14, R.id.button15};

        for (int id : buttonIds) {
            findViewById(id).setEnabled(false);
        }

        // Update status
        TextView statusText = findViewById(type.equals("groomer")
                ? R.id.textView32 : R.id.textView17);
        statusText.setText("Pending");

        Toast.makeText(this, "Request sent to " + providerName, Toast.LENGTH_SHORT).show();
    }

    private void loadAvailableAppointments(){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("appointments");
        Query query = FirebaseDatabase.getInstance().getReference("users")
                .orderByChild("identity").equalTo("doctor");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                status = snapshot.child("status").getValue(String.class);
                TextView statusTxt = findViewById(R.id.textView32); statusTxt.setText(status);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OwnerHome.this, "ref failed", Toast.LENGTH_SHORT).show();
            }
        });

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> options = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()){
                    FirebaseAppointmentReq appt = ds.getValue(FirebaseAppointmentReq.class);
                    if(appt != null && appt.status.equals("pending")){
                        options.add(appt.dateTime);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OwnerHome.this, "query failed", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void fetchOwnerDataFromFirebase(){
        if (ownerKey == null) return;

        DatabaseReference ownerRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(ownerKey);
        ownerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                FirebaseOwner owner = snapshot.getValue(FirebaseOwner.class);
                if (owner != null){
                    weight = owner.weight;
                    dailyIntake = owner.dailyIntake;
                    latestShoppingDateStr = owner.latestShoppingDate;

                    updateFoodInfo();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OwnerHome.this, "Failed to load owner data", Toast.LENGTH_SHORT).show();
            }
        });
    }

}