package com.example.csia;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.csia.Firebase.FirebaseAppointmentReq;
import com.example.csia.Firebase.FirebaseDoctor;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class DoctorHome extends AppCompatActivity {
    private FirebaseDoctor doctor;
    private List<BusyTime> ownerBusy;
    private String ownerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.doctor_homepage);

        String username = getIntent().getStringExtra("username");
        ArrayList<String> openDays = getIntent().getStringArrayListExtra("openDays");
        String businessHrs = getIntent().getStringExtra("businessHours");
        int durationMin = getIntent().getIntExtra("durationInMinutes",0);
        ownerId = getIntent().getStringExtra("ownerId");


        //build providor & busy list
        doctor = new FirebaseDoctor(username, openDays, businessHrs, durationMin);
        ownerBusy = (List<BusyTime>) getIntent().getSerializableExtra("busyTimes");
        if (ownerBusy == null) ownerBusy = new ArrayList<>();

        //compute initial slots
        List<String> slots = AppointmentSlotCalc.findAvailableSlot(
                ownerBusy, doctor, doctor.getOpenDays(), doctor.getDurationMin()
        );

        //display
        displaySlots(slots);

        //connect refresh button
        ImageButton refresh = findViewById(R.id.imageButton9);
        refresh.setOnClickListener(v -> {
            List<String> newSlots = AppointmentSlotCalc.findAvailableSlot(
                    ownerBusy, doctor, doctor.getOpenDays(), doctor.getDurationMin()
            );
            displaySlots(newSlots);
        });

        TextView textView = findViewById(R.id.textView2);
        textView.setText("Welcome, Dr. " + username);

        loadIncomingRequest();
    }

    private void displaySlots(List<String> slots){
        int[] textIds = {R.id.textView37, R.id.textView42, R.id.textView43};
        int[] btnIds = {R.id.button13, R.id.button14, R.id.button15};

        for (int i = 0; i < 3; i++){
            TextView slotsTxt = findViewById(textIds[i]);
            Button deferBtn = findViewById(btnIds[i]);
            if (i < slots.size()){
                String str = slots.get(i);
                slotsTxt.setText("Option " + (i + 1) + ": " + str);
                deferBtn.setEnabled(true);
                deferBtn.setOnClickListener(v -> sendRequest(str));
            } else {
                slotsTxt.setText("No slot");
                deferBtn.setEnabled(false);
            }
        }
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
                .getReference("appointments");
        Query query = ref.orderByChild("serviceProviderId")
                .equalTo(doctor.getName());

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                TextView displayReq = findViewById(R.id.textView44);
                for (DataSnapshot r : snapshot.getChildren()){
                    FirebaseAppointmentReq appt = r.getValue(FirebaseAppointmentReq.class);
                    if (appt != null && "doctor".equals(appt.status) && "pending".equals(appt.status)){
                        displayReq.setText("Requested Date:\\n" + appt.dateTime);
                        Button acceptBtn = findViewById(R.id.button6);
                        Button rejectBtn = findViewById(R.id.button7);

                        acceptBtn.setOnClickListener(v -> {
                            r.getRef().child("status").setValue("accepted");
                            acceptBtn.setEnabled(false);
                            rejectBtn.setEnabled(false);
                        });

                        rejectBtn.setOnClickListener(v -> {
                            r.getRef().child("status").setValue("rejected");
                            acceptBtn.setEnabled(false);
                            rejectBtn.setEnabled(false);
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}