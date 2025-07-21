package com.example.csia;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.csia.Firebase.FirebaseAppointmentReq;
import com.example.csia.Firebase.FirebaseDoctor;
import com.example.csia.Firebase.FirebaseGroomer;
import com.example.csia.Utilities.AppointmentSlotCalc;
import com.example.csia.Utilities.BusyTime;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GroomerHome extends AppCompatActivity {
    private FirebaseGroomer groomer;
    private List<BusyTime> ownerBusy;
    private String ownerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.groomer_homepage);
        String username = getIntent().getStringExtra("username");
        ArrayList<String> openDays = getIntent().getStringArrayListExtra("openDays");
        String businessHrs = getIntent().getStringExtra("businessHours");
        int durationMin = getIntent().getIntExtra("durationInMinutes",0);
        ownerId = getIntent().getStringExtra("ownerId");

        //build providor & busy list
        groomer = new FirebaseGroomer(username, openDays, businessHrs, durationMin);
        ownerBusy = (List<BusyTime>) getIntent().getSerializableExtra("busyTimes");
        if (ownerBusy == null) ownerBusy = new ArrayList<>();

        //compute initial slots
        List<String> slots = AppointmentSlotCalc.findAvailableSlot(
                ownerBusy, groomer, groomer.getOpenDays(), groomer.getDurationMin()
        );

        //display
        displaySlots(slots);

        //connect refresh button
        ImageButton refresh = findViewById(R.id.imageButton8);
        refresh.setOnClickListener(v -> {
            List<String> newSlots = AppointmentSlotCalc.findAvailableSlot(
                    ownerBusy, groomer, groomer.getOpenDays(), groomer.getDurationMin()
            );
            displaySlots(newSlots);
        });

        TextView textView = findViewById(R.id.textView2);
        textView.setText("Welcome, " + username);
        loadIncomingRequest();

    }
    private void displaySlots(List<String> slots){
        int[] textIds = {R.id.textView27, R.id.textView29, R.id.textView30};
        int[] btnIds = {R.id.button11, R.id.button4, R.id.button12};

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
                ownerId, groomer.getName(), "groomer", slot, groomer.getDurationMin()
        );
        FirebaseDatabase.getInstance()
                .getReference("appointments")
                .push()
                .setValue(req);
        Toast.makeText(this, "Requested " + slot, Toast.LENGTH_SHORT).show();
    }

    private void loadIncomingRequest() {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("appointments");
        Query query = ref.orderByChild("serviceProviderId")
                .equalTo(groomer.getName());

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                TextView displayReq = findViewById(R.id.textView31);
                for (DataSnapshot r : snapshot.getChildren()) {
                    FirebaseAppointmentReq appt = r.getValue(FirebaseAppointmentReq.class);
                    if (appt != null && "groomer".equals(appt.status) && "pending".equals(appt.status)) {
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