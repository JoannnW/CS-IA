package com.example.csia;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.csia.Firebase.FirebaseAppointmentReq;
import com.example.csia.Firebase.FirebaseDoctor;
import com.example.csia.Utilities.AppointmentOption;
import com.example.csia.Utilities.AppointmentRequest;
import com.example.csia.Utilities.AppointmentSlotCalc;
import com.example.csia.Utilities.BusyTime;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.Firebase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Queue;

public class DoctorHome extends AppCompatActivity {
    private AppointmentOption option1, option2, option3;
    private AppointmentRequest currentRequest;

    private FirebaseDoctor doctor;
    private ArrayList<BusyTime> ownerBusy;
    private String ownerId;

    private int[] textIds = {R.id.textView37, R.id.textView42, R.id.textView43};
    private int[] btnIds = {R.id.button11, R.id.button4, R.id.button12};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.doctor_homepage);

        String username = getIntent().getStringExtra("username");
        ArrayList<String> openDays = getIntent().getStringArrayListExtra("openDays");
        String businessHrs = getIntent().getStringExtra("businessHours");
        int durationMin = getIntent().getIntExtra("durationInMinutes",0);
        ownerId = getIntent().getStringExtra("ownerId");

        findViewById(R.id.button6).setVisibility(View.GONE);
        findViewById(R.id.button7).setVisibility(View.GONE);

        //build providor & busy list
        doctor = new FirebaseDoctor(username, openDays, businessHrs, durationMin);
        ownerBusy = (ArrayList<BusyTime>) getIntent().getSerializableExtra("busyTimes");
        if (ownerBusy == null) ownerBusy = new ArrayList<>();

        //compute initial slots
        ArrayList<String> slots = AppointmentSlotCalc.findAvailableSlot(
                ownerBusy, doctor, doctor.getOpenDays(), doctor.getDurationMin()
        );

        //display
        displaySlots(slots);

        //connect refresh button
        ImageButton refresh = findViewById(R.id.imageButton9);
        refresh.setOnClickListener(v -> {
            ArrayList<String> newSlots = AppointmentSlotCalc.findAvailableSlot(
                    ownerBusy, doctor, doctor.getOpenDays(), doctor.getDurationMin()
            );
            displaySlots(newSlots);
        });

        TextView textView = findViewById(R.id.textView2);
        textView.setText("Welcome, Dr. " + username);

        loadIncomingRequest();
    }

    private void displaySlots(ArrayList<String> slots){
        for (int i = 0; i < 3; i++) {
            TextView slotText = findViewById(textIds[i]);
            Button deferBtn = findViewById(btnIds[i]);

            if (i < slots.size()) {
                String slot = slots.get(i);
                slotText.setText("Option " + (i + 1) + ": " + slot);
                deferBtn.setEnabled(true);

                int finalI = i;
                deferBtn.setOnClickListener(v -> deferSlot(slot, finalI));
            } else {
                slotText.setText("No slot");
                deferBtn.setEnabled(false);
            }
        }
    }

    private void deferSlot(String originalSlot, int index) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            calendar.set(year, month, day);
            new TimePickerDialog(this, (timeView, hour, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                String newSlot = sdf.format(calendar.getTime());

                // Create the query properly
                DatabaseReference ref = FirebaseDatabase.getInstance()
                        .getReference("appointment_requests");
                Query query = ref.orderByChild("requestedOption/dateTimeRange")
                        .equalTo(originalSlot);

                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            // Use ds.getRef() to get the reference to the specific node
                            DatabaseReference requestRef = ds.getRef();
                            requestRef.child("requestedOption/dateTimeRange").setValue(newSlot);
                            requestRef.child("requestedOption/isDeferred").setValue(true);

                            // Update UI
                            TextView slotText = findViewById(textIds[index]);
                            slotText.setText("Option " + (index + 1) + ": " + newSlot);
                            Toast.makeText(DoctorHome.this, "Slot deferred", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(DoctorHome.this, "Failed to defer", Toast.LENGTH_SHORT).show();
                    }
                });
            }, 12, 0, false).show();
        },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void sendRequest(String slot){
        FirebaseAppointmentReq req = new FirebaseAppointmentReq(
                ownerId, doctor.getName(), "doctor", slot, doctor.getDurationMin()
        );
        FirebaseDatabase.getInstance()
                .getReference("appointments")
                .push()
                .setValue(req);
        Toast.makeText(this, "Requested " + slot, Toast.LENGTH_SHORT).show();
    }

    private void loadIncomingRequest(){

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("appointment_requests");
        Query query = ref.orderByChild("providerName")
                .equalTo(doctor.getName());

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    AppointmentRequest request = ds.getValue(AppointmentRequest.class);
                    if (request != null && "pending".equals(request.getStatus())) {
                        // Show request
                        TextView requestTxt = findViewById(R.id.textView44);
                        requestTxt.setText("Requested: " + request.getRequestedOption().getDateTimeRange());

                        // Hide defer buttons
                        int[] deferBtns = {R.id.button13, R.id.button14, R.id.button15};
                        for (int btn : deferBtns) findViewById(btn).setVisibility(View.GONE);

                        // Show accept/reject buttons
                        Button acceptBtn = findViewById(R.id.button6);
                        Button rejectBtn = findViewById(R.id.button7);
                        acceptBtn.setVisibility(View.VISIBLE);
                        rejectBtn.setVisibility(View.VISIBLE);

                        // Set click listeners
                        acceptBtn.setOnClickListener(v -> {
                            ds.getRef().child("status").setValue("accepted");
                            acceptBtn.setEnabled(false);
                            rejectBtn.setEnabled(false);
                        });

                        rejectBtn.setOnClickListener(v -> {
                            ds.getRef().child("status").setValue("rejected");
                            acceptBtn.setEnabled(false);
                            rejectBtn.setEnabled(false);
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DoctorHome.this, "Failed to load requests", Toast.LENGTH_SHORT).show();
            }
        });
    }
}