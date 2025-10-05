package com.example.csia;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.csia.Firebase.FirebaseAppointmentReq;
import com.example.csia.Firebase.FirebaseDoctor;
import com.example.csia.Firebase.FirebaseGroomer;
import com.example.csia.Firebase.FirebaseOwner;
import com.example.csia.Utilities.AppointmentOption;
import com.example.csia.Utilities.AppointmentRequest;
import com.example.csia.Utilities.AppointmentSlotCalc;
import com.example.csia.Utilities.BusyTime;
import com.example.csia.Utilities.ChatMessage;
import com.example.csia.Utilities.ChatAdapter;
import com.google.firebase.auth.FirebaseAuth;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OwnerHome extends AppCompatActivity {
    private String username;
    private double weight;
    private double dailyIntake;
    private String latestShoppingDateStr;
    private String ownerKey; //used to track Firebase key
    private String status;
    private ArrayList<BusyTime> busyTimes;
    private String currentNextShoppingDate; //store current next shopping date

    //note function:
    private RecyclerView chatRecyclerView;
    private EditText chatInput;
    private ImageButton sendChatButton, reloadGroom, reloadDoc;
    private Button linkDoctorBtn, linkGroomerBtn, rescheduleShopping, boughtFood;
    private ChatAdapter chatAdapter;
    private final List<ChatMessage> chatMsgs = new ArrayList<>();
    private String linkedGroomerId, linkedDoctorId;
    private boolean isGroomerLinked = false, isDoctorLinked = false;
    private TextView nextShopTxt;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.owner_homepage);

        //initialize UI element
        nextShopTxt = findViewById(R.id.textView23);

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
        currentNextShoppingDate = calculateNextShoppingDate(weight, dailyIntake, latestShoppingDateStr);

        if (nextShopTxt != null && currentNextShoppingDate != null){
            nextShopTxt.setText(currentNextShoppingDate);
        }

        if (busyTimes == null){ busyTimes = new ArrayList<>(); }

        loadDoctors();
        loadGroomers();

        linkGroomerBtn = findViewById(R.id.linkGroomerButton);
        linkGroomerBtn.setOnClickListener(v -> showGroomerSelection());

        //check link status on startup
        checkDoctorLinkStatus();
        checkGroomerLinkStatus();

        //initalise UI to display input data
        TextView welcomeTxt = findViewById(R.id.textView2);
        welcomeTxt.setText("Welcome, " + username);

        TextView storeNameTxt = findViewById(R.id.textView16);
        storeNameTxt.setText("Food supply from " + storeName);

        reloadGroom = findViewById(R.id.imageButton8);
        reloadDoc = findViewById(R.id.imageButton9);
        rescheduleShopping = findViewById(R.id.button);
        boughtFood = findViewById(R.id.button17);

        fetchOwnerDataFromFirebase();

        rescheduleShopping.setOnClickListener(v -> rescheduleShopping());

        boughtFood.setOnClickListener(v -> onFoodBought());

        setupChatSystem();
        trackAppointmentStatus();
    }

    private void updateFoodInfo(){
        //calc food left
        double foodLeft = calculateFoodLeft(weight, dailyIntake, latestShoppingDateStr);
        TextView foodLeftTxt = findViewById(R.id.textView21);
        foodLeftTxt.setText(String.format("%.2fkg", foodLeft));

        //calc next shopping date
        String nextShopDate = calculateNextShoppingDate(weight, dailyIntake, latestShoppingDateStr);
        nextShopTxt.setText(nextShopDate);
        currentNextShoppingDate = nextShopDate;

        // Check for low food and send notification
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date nextDate = sdf.parse(currentNextShoppingDate);
            Date today = new Date();
            long daysLeft = (nextDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24);

            if (daysLeft <= 2) {
                sendFoodNotification("Low Food Warning",
                        "Food will run out in " + daysLeft + " days! ");
            }
        } catch (ParseException e) {
            sendFoodNotification("Date Error", "Error parsing date");
        }
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
            int safeDays = Math.max(0, totalDays - 10); //never negative, 10 day buffer until food runs out

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

    private void rescheduleShopping() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date currentDate = sdf.parse(currentNextShoppingDate);
            Calendar cal = Calendar.getInstance();
            cal.setTime(currentDate);
            cal.add(Calendar.DAY_OF_YEAR, -1); // Reduce by 1 day

            Date newDate = cal.getTime();
            Date today = new Date();

            // Check if new date is before today (invalid)
            if (newDate.before(today)) {
                sendFoodNotification("Food Alert", "Cannot reschedule: date is in the past.");
                return;
            }

            // Check if food will run out tomorrow
            long daysUntilRunOut = (newDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24);
            if (daysUntilRunOut <= 1) {
                rescheduleShopping.setEnabled(false);
                sendFoodNotification("Critical Food Alert", "Cannot reschedule: food will run out tomorrow!");
                return;
            }
            //update to firebase
            updateShoppingDateInFirebase(newDate);

            // Update the current next shopping date and UI
            currentNextShoppingDate = sdf.format(newDate);
            nextShopTxt.setText(currentNextShoppingDate);

            // Notify user
            sendFoodNotification("Food Rescheduled", "Rescheduled to: " + currentNextShoppingDate);

        } catch (ParseException e) {
            sendFoodNotification("Date Error", "Error parsing date");
        }
    }

    private void updateShoppingDateInFirebase(Date newDate){
        if(ownerKey != null){
            DatabaseReference ownerRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(ownerKey)
                    .child("latestShoppingDate");
            ownerRef.setValue(newDate)
                    .addOnSuccessListener(aVoid -> {
                        //update local data and UI
                        latestShoppingDateStr = newDate.toString();
                        updateFoodInfo();
                        Toast.makeText(this, "Shopping date updated!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->{
                        Toast.makeText(this, "Failed to update date: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void onFoodBought() {
        // get the original registered weight
        double newSupplyWeight = weight;

        // calculate current food left
        double currentFoodLeft = calculateFoodLeft(weight, dailyIntake, latestShoppingDateStr);

        // Update total weight: current left + new supply
        double updatedWeight = currentFoodLeft + newSupplyWeight;

        // update last shopping date to today
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String today = sdf.format(new Date());

        updateFoodWeightAndDateInFirebase(updatedWeight, today);// update Firebase

        Toast.makeText(this, "Added new food supply!", Toast.LENGTH_SHORT).show();
    }

    private void updateFoodWeightAndDateInFirebase(double newWeight, String newDate) {
        if (ownerKey != null) {
            DatabaseReference ownerRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(ownerKey);

            Map<String, Object> updates = new HashMap<>();
            updates.put("weight", newWeight);
            updates.put("latestShoppingDate", newDate);

            ownerRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        weight = newWeight;
                        latestShoppingDateStr = newDate;
                        updateFoodInfo();
                        Toast.makeText(this, "Food supply updated!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                        //only load time slots if this groomer is linekd with current owner
                        if (isDoctorLinked && doctorSnap.getKey().equals(linkedDoctorId)){
                            //need to implement this with the actual Google calendar api
                            ArrayList<String> slots = AppointmentSlotCalc.findAvailableSlot(
                                    busyTimes,
                                    doctor,
                                    doctor.openDays,
                                    doctor.durationMin
                            );
                            showSlots(doctor.name, slots, "doctor");
                        }
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
                        //only load the time slots if this groomer is linked with the current owner
                        if (isGroomerLinked && groomerSnap.getKey().equals(linkedGroomerId)){
                            ArrayList<String> slots = AppointmentSlotCalc.findAvailableSlot(
                                    busyTimes, groomer, groomer.openDays, groomer.durationMin
                            );
                            showSlots(groomer.name, slots, "groomer");
                        }

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error){
                Toast.makeText(OwnerHome.this, "Failed to load groomer's availabilities", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSlots(String providerName, ArrayList<String> slots, String type){
        int[] slotTextIds;
        int[] buttonIds;

        if (type.equals("groomer")){
            slotTextIds = new int[]{R.id.textView27, R.id.textView29, R.id.textView30};
            buttonIds = new int[]{R.id.button11, R.id.button4, R.id.button12};

            findViewById(R.id.textView26).setVisibility(View.VISIBLE);
        } else { //doctor
            slotTextIds = new int[]{R.id.textView37, R.id.textView42, R.id.textView43};
            buttonIds = new int[]{R.id.button13, R.id.button14, R.id.button15};

            findViewById(R.id.textView36).setVisibility(View.VISIBLE);
        }

        for (int i = 0; i < 3; i ++){
            TextView slotText = findViewById(slotTextIds[i]);
            Button slotButton = findViewById(buttonIds[i]);

            if(slotText != null && slotButton != null) {
                if (i < slots.size() && slots.get(i) != null && !slots.get(i).isEmpty()) {
                    slotText.setText("Option " + (i + 1) + ": " + slots.get(i));
                    slotText.setVisibility(View.VISIBLE);

                    //create final copies for use
                    final String finalProviderName = providerName;
                    final String finalSlot = slots.get(i);
                    final String finalType = type;

                    slotButton.setOnClickListener(v -> requestAppointment(finalProviderName, finalSlot, finalType));
                    slotText.setVisibility(View.VISIBLE);
                    slotButton.setVisibility(View.VISIBLE);
                    slotButton.setEnabled(true);
                } else {
                    slotText.setText("No slot available");
                    slotText.setVisibility(View.VISIBLE);
                    slotButton.setVisibility(View.GONE);//hide buttonw hen no slot
                }
            }
        }
        // Update status text
        TextView statusText = findViewById(type.equals("groomer") ? R.id.textView32 : R.id.textView17); //Java short-hand notation (“W3Schools.com”)
        if (slots != null && !slots.isEmpty()) {
            statusText.setText("Available - Select a slot");
        } else {
            statusText.setText("No available slots");
        }

    }

    private void requestAppointment(String providerName, String slotTime, String type) {
        // Create request with all necessary info
        AppointmentOption option = new AppointmentOption(slotTime);
        AppointmentRequest request = new AppointmentRequest(ownerKey, providerName, type, option);

        // Save to Firebase
        DatabaseReference requestsRef = FirebaseDatabase.getInstance()
                .getReference("appointment_requests");//create new parent node in firebase
        String requestKey = requestsRef.push().getKey();//save req into a string (reduced queries)
        requestsRef.child(requestKey).setValue(request); //create owner's appointment request and add details

        // Immediately show status UI
        TextView dateTxt = findViewById(R.id.textView44);
        TextView statusTxt = findViewById(R.id.textView17);
        if (dateTxt != null) {
            dateTxt.setText("Status for Requested Date: " + slotTime);
            dateTxt.setVisibility(View.VISIBLE);
        }
        if (statusTxt != null) {
            statusTxt.setText("pending");
            statusTxt.setVisibility(View.VISIBLE);
        }

        // Disable all buttons for this provider
        int[] buttonIds = type.equals("groomer")
                ? new int[]{R.id.button11, R.id.button4, R.id.button12} //Java short-hand notation (“W3Schools.com”)
                : new int[]{R.id.button13, R.id.button14, R.id.button15};

        for (int id : buttonIds) {
            findViewById(id).setEnabled(false);
        }

        // Update status
        TextView statusText = findViewById(type.equals("groomer")
                ? R.id.textView32 : R.id.textView17); //Java short-hand notation (“W3Schools.com”)
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
                ArrayList<String> options = new ArrayList<>();
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


    private void setupChatSystem() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatInput = findViewById(R.id.chatInput);
        sendChatButton = findViewById(R.id.sendChatButton);
        linkDoctorBtn = findViewById(R.id.linkDoctorButton);

        // Initialize RecyclerView
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatAdapter(chatMsgs, ownerKey);
        chatRecyclerView.setAdapter(chatAdapter);

        // Check if already linked with a doctor
        checkDoctorLinkStatus();

        // Set up send button
        sendChatButton.setOnClickListener(v -> {
            String msg = chatInput.getText().toString().trim();
            if (!msg.isEmpty() && linkedDoctorId != null) {
                sendMsg(msg, linkedDoctorId);
                chatInput.setText("");
            } else if (linkedDoctorId == null) {
                Toast.makeText(this, "Please link with a doctor first", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up link doctor button
        linkDoctorBtn.setOnClickListener(v -> showDoctorSelection());
    }

    private void checkDoctorLinkStatus() {
        if (ownerKey != null) {
            DatabaseReference ownerRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(ownerKey)
                    .child("linkedDoctorId");

            ownerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        linkedDoctorId = snapshot.getValue(String.class);
                        linkDoctorBtn.setVisibility(View.GONE);
                        isDoctorLinked = true;
                        updateUIVisibility(isDoctorLinked, "doctor");

                        loadDoctors();
                        loadChatMsgs(linkedDoctorId);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(OwnerHome.this, "Failed to check doctor link status", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void checkGroomerLinkStatus(){
        if (ownerKey != null) {
            DatabaseReference ownerRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(ownerKey)
                    .child("linkedGroomerId");

            ownerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        linkedGroomerId = snapshot.getValue(String.class);
                        linkGroomerBtn.setVisibility(View.GONE);
                        isGroomerLinked = true;
                        updateUIVisibility(isGroomerLinked, "groomer");

                        loadGroomers();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(OwnerHome.this, "Failed to check groomer link", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showGroomerSelection() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a Groomer");

        Query groomersQuery = FirebaseDatabase.getInstance()
                .getReference("users")
                .orderByChild("identity")
                .equalTo("groomer");

        groomersQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> groomerNames = new ArrayList<>();
                final List<String> groomerIds = new ArrayList<>();

                for (DataSnapshot groomerSnap : snapshot.getChildren()) {
                    FirebaseGroomer groomer = groomerSnap.getValue(FirebaseGroomer.class);
                    if (groomer != null) {
                        groomerNames.add(groomer.getName());
                        groomerIds.add(groomerSnap.getKey());
                    }
                }

                builder.setItems(groomerNames.toArray(new String[0]), (dialog, which) -> {
                    String selectedGroomerId = groomerIds.get(which);
                    linkWithGroomer(selectedGroomerId);
                });
                builder.show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OwnerHome.this, "Error loading groomers", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void linkWithGroomer(String groomerId) {
        DatabaseReference ownerRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(ownerKey);

        //trying to connect selected groomer with current owner and update firebase AND groomer's islinked status
        ownerRef.child("linkedGroomerId").setValue(groomerId)
                .addOnSuccessListener(aVoid -> {
                    linkedGroomerId = groomerId;
                    linkGroomerBtn.setVisibility(View.GONE);//hiding button, link successful

                    //immediately load groomers to calculate and display slots
                    isGroomerLinked = true;
                    updateUIVisibility(true, "groomer");
                    loadGroomers();

                    //also update groomer's linked owner, produce success/failure Toast messages
                    DatabaseReference groomerRef = FirebaseDatabase.getInstance().getReference("users").child(groomerId);
                    groomerRef.child("linkedOwnerId").setValue(ownerKey).addOnSuccessListener(aVoid2 ->{
                        Toast.makeText(this, "Groomer linked successfully", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to link groomer", Toast.LENGTH_SHORT).show();
                });
    }

    //Shows a dialog for selecting a doctor to link with
    private void showDoctorSelection() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a Doctor");

        // Query doctors from Firebase
        Query doctorsQuery = FirebaseDatabase.getInstance()
                .getReference("users")
                .orderByChild("identity")
                .equalTo("doctor");

        doctorsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> doctorNames = new ArrayList<>();
                final List<String> doctorIds = new ArrayList<>();

                for (DataSnapshot doctorSnap : snapshot.getChildren()) {
                    FirebaseDoctor doctor = doctorSnap.getValue(FirebaseDoctor.class);
                    if (doctor != null) {
                        doctorNames.add(doctor.getName());
                        doctorIds.add(doctorSnap.getKey());
                    }
                }

                builder.setItems(doctorNames.toArray(new String[0]), (dialog, which) -> {
                    String selectedDoctorId = doctorIds.get(which);
                    linkWithDoctor(selectedDoctorId);
                });

                builder.show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OwnerHome.this, "Error loading doctors", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void linkWithDoctor(String doctorId) {
        DatabaseReference ownerRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(ownerKey);

        ownerRef.child("linkedDoctorId").setValue(doctorId)
                .addOnSuccessListener(aVoid -> {
                    linkedDoctorId = doctorId;
                    linkDoctorBtn.setVisibility(View.GONE);

                    isDoctorLinked = true;
                    updateUIVisibility(true, "doctor");
                    loadDoctors();
                    loadChatMsgs(doctorId);

                    // Also update doctor's linked owner databse and produce success/ fail toast msgs
                    DatabaseReference doctorRef = FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(doctorId);
                    doctorRef.child("linkedOwnerId").setValue(ownerKey)
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, "Doctor linked successfully", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to link doctor", Toast.LENGTH_SHORT).show();
                });
    }

    //chatbox function
    private void loadChatMsgs(String doctorId) {
        String conversationId = ownerKey.compareTo(doctorId) < 0
                ? ownerKey + "_" + doctorId
                : doctorId + "_" + ownerKey;

        DatabaseReference chatRef = FirebaseDatabase.getInstance()
                .getReference("chats")
                .child(conversationId);

        chatRef.addValueEventListener(new ValueEventListener() {//for persistent listening
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatMsgs.clear();
                for (DataSnapshot messageSnap : snapshot.getChildren()) {
                    ChatMessage msg = messageSnap.getValue(ChatMessage.class);
                    if (msg != null) {
                        chatMsgs.add(msg);
                    }
                }
                chatAdapter.notifyDataSetChanged();
                if (chatMsgs.size() > 0) {
                    chatRecyclerView.scrollToPosition(chatMsgs.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OwnerHome.this, "Error loading messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Sends a message to the specified doctor
     //msgText =The text of the message to send
     //doctorId = The ID of the doctor to send the message to
    private void sendMsg(String msgTxt, String doctorId) {
        String conversationId = ownerKey.compareTo(doctorId) < 0
                ? ownerKey + "_" + doctorId
                : doctorId + "_" + ownerKey;
        String msgId = FirebaseDatabase.getInstance()
                .getReference("chats")
                .child(conversationId)
                .push()
                .getKey();

        if (msgId == null) {
            Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create and send the message
        ChatMessage msg = new ChatMessage(ownerKey, username, doctorId, msgTxt);

        FirebaseDatabase.getInstance()
                .getReference("chats")
                .child(conversationId)
                .child(msgId)
                .setValue(msg)
                .addOnSuccessListener(aVoid -> {
                    chatInput.setText("");
                    // SUCCESS: msg sent to Firebase
                    // The ValueEventListener in loadChatMsgs shld automatically update the UI
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendFoodNotification(String title, String msg) {
        // Save notification to Firebase to trigger FCM
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(ownerKey)
                .push();

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("title", title);
        notificationData.put("message", msg);
        notificationData.put("timestamp", System.currentTimeMillis());
        notificationData.put("type", "food_alert");

        notificationsRef.setValue(notificationData);
    }

    private void updateUIVisibility(boolean isLinked, String identity){
        if (isLinked && identity.equals("groomer")){
            findViewById(R.id.linkGroomerButton).setEnabled(false);//can't link with another

            findViewById(R.id.textView26).setVisibility(View.VISIBLE);
            findViewById(R.id.textView27).setVisibility(View.VISIBLE);
            findViewById(R.id.button11).setVisibility(View.VISIBLE);
            findViewById(R.id.textView29).setVisibility(View.VISIBLE);
            findViewById(R.id.button4).setVisibility(View.VISIBLE);
            findViewById(R.id.textView30).setVisibility(View.VISIBLE);
            findViewById(R.id.button12).setVisibility(View.VISIBLE);
            findViewById(R.id.imageButton8).setVisibility(View.VISIBLE);

            //hide status until request is chosen
            findViewById(R.id.textView31).setVisibility(View.GONE);
            findViewById(R.id.textView32).setVisibility(View.GONE);

        } else if (isLinked && identity.equals("doctor")){
            findViewById(R.id.linkDoctorButton).setEnabled(false);//can't link with another

            findViewById(R.id.textView33).setVisibility(View.VISIBLE);
            findViewById(R.id.chatRecyclerView).setVisibility(View.VISIBLE);
            findViewById(R.id.chatInput).setVisibility(View.VISIBLE);
            findViewById(R.id.sendChatButton).setVisibility(View.VISIBLE);
            findViewById(R.id.textView15).setVisibility(View.VISIBLE);
            findViewById(R.id.view5).setVisibility(View.VISIBLE);
            findViewById(R.id.textView36).setVisibility(View.VISIBLE);
            findViewById(R.id.textView37).setVisibility(View.VISIBLE);
            findViewById(R.id.button13).setVisibility(View.VISIBLE);
            findViewById(R.id.textView42).setVisibility(View.VISIBLE);
            findViewById(R.id.button14).setVisibility(View.VISIBLE);
            findViewById(R.id.textView43).setVisibility(View.VISIBLE);
            findViewById(R.id.button15).setVisibility(View.VISIBLE);
            findViewById(R.id.imageButton9).setVisibility(View.VISIBLE);

            //hide status until request is chosen
            findViewById(R.id.textView44).setVisibility(View.GONE);
            findViewById(R.id.textView17).setVisibility(View.GONE);
        }
    }

    private void trackAppointmentStatus() {
        DatabaseReference requestsRef = FirebaseDatabase.getInstance()
                .getReference("appointment_requests");//create new field

        requestsRef.orderByChild("ownerId").equalTo(ownerKey)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            AppointmentRequest request = ds.getValue(AppointmentRequest.class);
                            if (request != null) {
                                String requestedDate = request.getRequestedOption().getDateTimeRange();
                                String status = request.getStatus();
                                String type = request.getType();

                                // Update UI based on request type
                                if ("groomer".equals(type)) {
                                    TextView dateTxt = findViewById(R.id.textView31);
                                    TextView statusTxt = findViewById(R.id.textView32);

                                    if (dateTxt != null) {
                                        dateTxt.setText("Status for Requested Date: " + requestedDate);
                                        dateTxt.setVisibility(View.VISIBLE);
                                    }
                                    if (statusTxt != null) {
                                        statusTxt.setText(status);
                                        statusTxt.setVisibility(View.VISIBLE);
                                    }

                                    // Enable/disable groomer request buttons based on status
                                    boolean buttonsEnabled = !"pending".equals(status);
                                    findViewById(R.id.button11).setEnabled(buttonsEnabled);
                                    findViewById(R.id.button4).setEnabled(buttonsEnabled);
                                    findViewById(R.id.button12).setEnabled(buttonsEnabled);

                                } else if ("doctor".equals(type)) {
                                    TextView dateTxt = findViewById(R.id.textView44);
                                    TextView statusTxt = findViewById(R.id.textView17);

                                    if (dateTxt != null) {
                                        dateTxt.setText("Status for Requested Date: " + requestedDate);
                                        dateTxt.setVisibility(View.VISIBLE);
                                    }
                                    if (statusTxt != null) {
                                        statusTxt.setText(status);
                                        statusTxt.setVisibility(View.VISIBLE);
                                    }

                                    // Enable/disable doctor request buttons based on status
                                    boolean buttonsEnabled = !"pending".equals(status);
                                    findViewById(R.id.button13).setEnabled(buttonsEnabled);
                                    findViewById(R.id.button14).setEnabled(buttonsEnabled);
                                    findViewById(R.id.button15).setEnabled(buttonsEnabled);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(OwnerHome.this, "Failed to load request status", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}