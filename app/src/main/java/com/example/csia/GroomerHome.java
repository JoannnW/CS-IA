package com.example.csia;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.csia.Firebase.FirebaseAppointmentReq;
import com.example.csia.Firebase.FirebaseGroomer;
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

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class GroomerHome extends AppCompatActivity {
    private AppointmentOption option1, option2, option3;
    private AppointmentRequest currentRequest;

    private FirebaseGroomer groomer;
    private ArrayList<BusyTime> ownerBusy;
    private String ownerId, groomerKey;
    private boolean isChecked;
    private boolean isGroomerLinked = false;
    private TextView noOwnerMsg;

    private final int[] textIds = {R.id.textView27, R.id.textView29, R.id.textView30};
    private final int[] btnIds = {R.id.button11, R.id.button4, R.id.button12};
    private Switch availabilitySwitch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.groomer_homepage);
        String username = getIntent().getStringExtra("username");
        ArrayList<String> openDays = getIntent().getStringArrayListExtra("openDays");
        String businessHrs = getIntent().getStringExtra("businessHours");
        int durationMin = getIntent().getIntExtra("durationInMinutes",0);
        groomerKey = getIntent().getStringExtra("groomerKey");
        ownerId = getIntent().getStringExtra("ownerId");

        findViewById(R.id.button6).setVisibility(View.GONE); //hides accept button
        findViewById(R.id.button7).setVisibility(View.GONE); //hides reject button


        //check link status on startup
        checkGroomerLinkStatus();
        setupLinkStatusListener();//listen for real time changes to doctor/ owner linking

        //build providor & busy list
        groomer = new FirebaseGroomer(username, openDays, businessHrs, durationMin);
        ownerBusy = (ArrayList<BusyTime>) getIntent().getSerializableExtra("busyTimes");
        if (ownerBusy == null) ownerBusy = new ArrayList<>();


        //check availability settings
        availabilitySwitch = findViewById(R.id.switch1);
        isChecked = findViewById(R.id.switch1).isEnabled();
        //check current availability status:
        checkAvailabilityStatus();
        availabilitySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateAvailabilityStatus(isChecked, groomer);
                updateUIVisibility(availabilitySwitch.isChecked());

                // Change switch color
                if (isChecked) {
                    availabilitySwitch.getThumbDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
                } else {
                    availabilitySwitch.getThumbDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                }
            }
        });
        noOwnerMsg = findViewById(R.id.noOwnerMsg);


        //connect refresh button
        ImageButton refresh = findViewById(R.id.imageButton8);
        refresh.setOnClickListener(v -> refreshSlotsWithCurrentData());

        TextView textView = findViewById(R.id.textView2);
        textView.setText("Welcome, " + username);
        setupLinkStatusListener();
        loadIncomingRequest();
    }


    private void displaySlots(ArrayList<String> slots) {

        for (int i = 0; i < 3; i++) {
            TextView slotText = findViewById(textIds[i]);
            Button deferBtn = findViewById(btnIds[i]);

            if (slotText != null && deferBtn != null){
                if (i < slots.size() && slots.get(i) != null && !slots.get(i).isEmpty()){
                    String slot = slots.get(i);
                    slotText.setText("Option " + (i + 1) + ": " + slot);
                    slotText.setTextColor(Color.BLACK);
                    slotText.setVisibility(View.VISIBLE);

                    deferBtn.setEnabled(true);
                    deferBtn.setVisibility(View.VISIBLE);
                    int finalI = i;
                    deferBtn.setOnClickListener(v -> deferSlot(slot, finalI));
                } else{
                    slotText.setText("No slot available");
                    slotText.setTextColor(Color.GRAY);
                    slotText.setVisibility(View.VISIBLE);

                    deferBtn.setEnabled(false);
                    deferBtn.setVisibility(View.GONE);
                }
            }

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
                            Toast.makeText(GroomerHome.this, "Slot deferred", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(GroomerHome.this, "Failed to defer", Toast.LENGTH_SHORT).show();
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
                .getReference("appointment_requests");
        Query query = ref.orderByChild("providerName")
                .equalTo(groomer.getName());

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    AppointmentRequest request = ds.getValue(AppointmentRequest.class);
                    if (request != null && "pending".equals(request.getStatus())) { //Owner made a request
                        // Show request
                        TextView requestTxt = findViewById(R.id.textView31);
                        if (requestTxt != null) {
                            String requestedDate = request.getRequestedOption().getDateTimeRange();
                            requestTxt.setText("Requested Date: " + requestedDate);
                            requestTxt.setVisibility(View.VISIBLE);
                        }
                        // Hide defer buttons
                        for (int btn : btnIds) {
                            Button button = findViewById(btn);
                            if (button != null) {
                                button.setVisibility(View.GONE);
                            }
                        }

                        // Show accept/reject buttons
                        Button acceptBtn = findViewById(R.id.button6);
                        Button rejectBtn = findViewById(R.id.button7);
                        if (acceptBtn != null && rejectBtn != null) {
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GroomerHome.this, "Failed to load requests", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAvailabilityStatus() {
        if (groomerKey != null) {
            DatabaseReference groomerRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(groomerKey)
                    .child("available");

            groomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        boolean isAvailable = snapshot.getValue(Boolean.class);
                        availabilitySwitch.setChecked(isAvailable);
                    } else {
                        // Default to available if not set
                        availabilitySwitch.setChecked(true);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(GroomerHome.this, "Failed to check availability", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateAvailabilityStatus(boolean isChecked, FirebaseGroomer groomer) {
        if (groomerKey != null) {
            DatabaseReference groomerRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(groomerKey)
                    .child("available");

            groomerRef.setValue(isChecked)
                    .addOnSuccessListener(aVoid -> {
                        String status = isChecked ? "available" : "unavailable"; //Java short-hand notation (“W3Schools.com”)
                        Toast.makeText(GroomerHome.this, "You're now " + status, Toast.LENGTH_SHORT).show();
                        groomer.setLinked(isChecked);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(GroomerHome.this, "Failed to update availability", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateUIVisibility(boolean isAvailable){
        if (isAvailable){
            //available mode: hide unavailable message
            findViewById(R.id.unavailableMsg).setVisibility(View.GONE);//remove unavailable message

            if (isGroomerLinked){
                //linked with an owner - need to show all appointment functions, hide noOwner message
                noOwnerMsg.setVisibility(View.GONE);
                findViewById(R.id.textView26).setVisibility(View.VISIBLE);
                findViewById(R.id.button11).setVisibility(View.VISIBLE);
                findViewById(R.id.textView27).setVisibility(View.VISIBLE);
                findViewById(R.id.button4).setVisibility(View.VISIBLE);
                findViewById(R.id.textView29).setVisibility(View.VISIBLE);
                findViewById(R.id.button12).setVisibility(View.VISIBLE);
                findViewById(R.id.textView30).setVisibility(View.VISIBLE);
                findViewById(R.id.imageButton8).setVisibility(View.VISIBLE);

                //hide request related ui until a request has been made by owner
                findViewById(R.id.textView31).setVisibility(View.GONE);
                findViewById(R.id.button6).setVisibility(View.GONE);
                findViewById(R.id.button7).setVisibility(View.GONE);

                refreshSlotsWithCurrentData();
            } else{
                // Available but not linked: Show noOwner message, hide appointment functions
                noOwnerMsg.setVisibility(View.VISIBLE);
                findViewById(R.id.textView26).setVisibility(View.GONE);
                findViewById(R.id.button11).setVisibility(View.GONE);
                findViewById(R.id.textView27).setVisibility(View.GONE);
                findViewById(R.id.button4).setVisibility(View.GONE);
                findViewById(R.id.textView29).setVisibility(View.GONE);
                findViewById(R.id.button12).setVisibility(View.GONE);
                findViewById(R.id.textView30).setVisibility(View.GONE);
                findViewById(R.id.imageButton8).setVisibility(View.GONE);
                findViewById(R.id.textView31).setVisibility(View.GONE);
                findViewById(R.id.button6).setVisibility(View.GONE);
                findViewById(R.id.button7).setVisibility(View.GONE);
            }
        } else{
            //Unavailable mode: Show unavailable message, hide appointment functions
            findViewById(R.id.unavailableMsg).setVisibility(View.VISIBLE);
            noOwnerMsg.setVisibility(View.GONE);

            findViewById(R.id.textView26).setVisibility(View.GONE);
            findViewById(R.id.button11).setVisibility(View.GONE);
            findViewById(R.id.textView27).setVisibility(View.GONE);
            findViewById(R.id.button4).setVisibility(View.GONE);
            findViewById(R.id.textView29).setVisibility(View.GONE);
            findViewById(R.id.button12).setVisibility(View.GONE);
            findViewById(R.id.textView30).setVisibility(View.GONE);
            findViewById(R.id.imageButton8).setVisibility(View.GONE);
            findViewById(R.id.textView31).setVisibility(View.GONE);
            findViewById(R.id.button6).setVisibility(View.GONE);
            findViewById(R.id.button7).setVisibility(View.GONE);
        }
    }



    private void updateGroomerLinkStatus(boolean isLinked){
        if (groomerKey != null){
            DatabaseReference groomerRef = FirebaseDatabase.getInstance().getReference("users").child(groomerKey).child("isLinked");
            groomerRef.setValue(isLinked);
        }
    }

    private void checkGroomerLinkStatus(){
        if (groomerKey != null){
            DatabaseReference groomerRef = FirebaseDatabase.getInstance().getReference("users").child(groomerKey).child("linkedOwnerId");
            groomerRef.addValueEventListener(new ValueEventListener() {//for persistent listening
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()&& snapshot.getValue() != null) {
                        ownerId = snapshot.getValue(String.class);
                        isGroomerLinked = (ownerId != null && !ownerId.isEmpty());
                        Log.d("GroomerHome", "Link status: " + isGroomerLinked + ", Owner ID: " + ownerId);//("Understand logging", "Logging Suggestions")
                        updateUIVisibility(availabilitySwitch.isChecked());
                        if (isGroomerLinked){
                            loadOwnerBusyTimesAndRefresh();
                        } else{
                            isGroomerLinked = false;
                            ownerId = null;
                            updateUIVisibility(availabilitySwitch.isChecked());
                            refreshSlotsWithCurrentData();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(GroomerHome.this, "Failed to check link status", Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

    public void loadOwnerBusyTimesAndRefresh(){
        if (ownerId != null) {
            DatabaseReference ownerRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(ownerId);

            ownerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // Get owner's busy times from Firebase
                    ownerBusy = (ArrayList<BusyTime>) snapshot.child("busyTimes").getValue();
                    if (ownerBusy == null) ownerBusy = new ArrayList<>();

                    // Now refresh the slots with the actual owner's busy times
                    refreshSlotsWithCurrentData();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(GroomerHome.this, "Failed to load owner schedule", Toast.LENGTH_SHORT).show();
                    // Still try to refresh with empty busy times
                    refreshSlotsWithCurrentData();
                }
            });
        } else {
            refreshSlotsWithCurrentData();
        }
    }

    private void refreshSlotsWithCurrentData() {
        // This uses the same logic as your onCreate and refresh button
        ArrayList<String> slots = AppointmentSlotCalc.findAvailableSlot(
                ownerBusy, groomer, groomer.getOpenDays(), groomer.getDurationMin()
        );
        displaySlots(slots);
    }

    //adding real time updates on link status
    private void setupLinkStatusListener() {
        if (groomerKey != null) {
            DatabaseReference ownersRef = FirebaseDatabase.getInstance()
                    .getReference("users");

            Query linkedGroomerQuery = ownersRef.orderByChild("linkedGroomerId").equalTo(groomerKey);

            linkedGroomerQuery.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    isGroomerLinked = snapshot.exists() && snapshot.getChildrenCount() > 0;
                    updateUIVisibility(availabilitySwitch.isChecked());

                    if (isGroomerLinked) {
                        loadOwnerBusyTimesAndRefresh();//link status changes, reload owner busy times
                        Toast.makeText(GroomerHome.this, "An owner has linked with you!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(GroomerHome.this, "Failed to check link status", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}