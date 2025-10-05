package com.example.csia;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.csia.Firebase.FirebaseAppointmentReq;
import com.example.csia.Firebase.FirebaseDoctor;
import com.example.csia.Utilities.AppointmentOption;
import com.example.csia.Utilities.AppointmentRequest;
import com.example.csia.Utilities.AppointmentSlotCalc;
import com.example.csia.Utilities.BusyTime;
import com.example.csia.Utilities.ChatAdapter;
import com.example.csia.Utilities.ChatMessage;
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
import java.util.List;
import java.util.Locale;
import java.util.Queue;

public class DoctorHome extends AppCompatActivity {
    private AppointmentOption option1, option2, option3;
    private AppointmentRequest currentRequest;

    private FirebaseDoctor doctor;
    private String doctorKey;
    private ArrayList<BusyTime> ownerBusy;
    private String ownerId;
    private boolean isChecked;
    private boolean isDoctorLinked = false;
    private TextView noOwnerMsg;

    private final int[] textIds = {R.id.textView37, R.id.textView42, R.id.textView43};
    private final int[] btnIds = {R.id.button13, R.id.button14, R.id.button15};

    private RecyclerView chatRecyclerView;
    private EditText chatInput;
    private ImageButton sendChatButton;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMsgs = new ArrayList<>();
    private String currentChatPartnerId;
    private Switch availabilitySwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.doctor_homepage);

        initializeUIElements();
        String username = getIntent().getStringExtra("username");
        ArrayList<String> openDays = getIntent().getStringArrayListExtra("openDays");
        String businessHrs = getIntent().getStringExtra("businessHours");
        int durationMin = getIntent().getIntExtra("durationInMinutes",0);
        doctorKey = getIntent().getStringExtra("doctorKey");
        ownerId = getIntent().getStringExtra("ownerId");

        findViewById(R.id.button6).setVisibility(View.GONE);
        findViewById(R.id.button7).setVisibility(View.GONE);

        //check link status on startup
        checkDoctorLinkStatus();
        setupLinkStatusListener();//listen for real time changes to doctor/ owner linking

        //build providor & busy list
        doctor = new FirebaseDoctor(username, openDays, businessHrs, durationMin);
        ownerBusy = (ArrayList<BusyTime>) getIntent().getSerializableExtra("busyTimes");
        if (ownerBusy == null) ownerBusy = new ArrayList<>();

        //availability settings
        availabilitySwitch = findViewById(R.id.switch2);

        checkAvailabilityStatus();
        updateUIVisibility(availabilitySwitch.isChecked());
        availabilitySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateAvailabilityStatus(isChecked, doctor);
                updateUIVisibility(isChecked);

                //change switch colour
                if (isChecked){
                    availabilitySwitch.getThumbDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
                } else {
                    availabilitySwitch.getThumbDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                }
            }
        });

        //connect refresh button
        ImageButton refresh = findViewById(R.id.imageButton9);
        refresh.setOnClickListener(v -> refreshSlotsWithCurrentData());

        TextView textView = findViewById(R.id.textView2);
        if (textView != null){
            textView.setText("Welcome, Dr. " + username);
        }

        noOwnerMsg = findViewById(R.id.noOwnerMsg);

        setupChatSystem();

        loadIncomingRequest();

    }

    private void initializeUIElements(){
        // Initialize all UI elements that will be used
        availabilitySwitch = findViewById(R.id.switch2);
        noOwnerMsg = findViewById(R.id.noOwnerMsg);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatInput = findViewById(R.id.chatInput);
        sendChatButton = findViewById(R.id.sendChatButton);

        // Initialize slot text views and buttons
        for (int i = 0; i < textIds.length; i++) {
            TextView textView = findViewById(textIds[i]);
            Button button = findViewById(btnIds[i]);
            if (textView != null && button != null) {
                textView.setVisibility(View.GONE);
                button.setVisibility(View.GONE);
            }
        }
    }

    private void displaySlots(ArrayList<String> slots){
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
                } else {
                    slotText.setText("No slot available");
                    slotText.setTextColor(Color.GRAY);
                    slotText.setVisibility(View.VISIBLE);

                    deferBtn.setEnabled(false);
                    deferBtn.setVisibility(View.GONE);
                }
            } else {
                Log.e("DoctorHome", "UI elements not found for slot" + 1);//("Understand logging", "Logging Suggestions")
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
                            if (slotText != null){
                                slotText.setText("Option " + (index + 1) + ": " + newSlot);
                            }
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

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("appointment_requests");
        Query query = ref.orderByChild("providerName").equalTo(doctor.getName());

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    AppointmentRequest request = ds.getValue(AppointmentRequest.class);
                    if (request != null && "pending".equals(request.getStatus())) {
                        // Show request
                        TextView requestTxt = findViewById(R.id.textView44);
                        if (requestTxt != null){
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
                Toast.makeText(DoctorHome.this, "Failed to load requests", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupChatSystem() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatInput = findViewById(R.id.chatInput);
        sendChatButton = findViewById(R.id.sendChatButton);

        // Initialize chatMessages if not already done
        if (chatMsgs == null) {
            chatMsgs = new ArrayList<>();
        }

        // Setup RecyclerView
        if (chatRecyclerView != null) {
            chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            chatAdapter = new ChatAdapter(chatMsgs, doctorKey); // Use doctor's ID
            chatRecyclerView.setAdapter(chatAdapter);
        }

        // Set up send button
        if (sendChatButton != null) {
            sendChatButton.setOnClickListener(v -> {
                if (chatInput != null) {
                    String msg = chatInput.getText().toString().trim();
                    if (!msg.isEmpty() && currentChatPartnerId != null) {
                        sendMsg(msg, currentChatPartnerId);
                    } else if (currentChatPartnerId == null) {
                        Toast.makeText(DoctorHome.this, "No client linked", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void loadChatMsgs(String ownerId) {
        String conversationId = ownerId.compareTo(doctorKey) < 0
                ? ownerId + "_" + doctorKey
                : doctorKey + "_" + ownerId;

        DatabaseReference chatRef = FirebaseDatabase.getInstance()
                .getReference("chats")
                .child(conversationId);

        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatMsgs.clear();
                for (DataSnapshot messageSnap : snapshot.getChildren()) {
                    ChatMessage msg = messageSnap.getValue(ChatMessage.class);
                    if (msg != null) {
                        chatMsgs.add(msg);
                    }
                }

                if (chatAdapter != null) {
                    chatAdapter.notifyDataSetChanged();
                    if (chatRecyclerView != null && chatMsgs.size() > 0) {
                        chatRecyclerView.scrollToPosition(chatMsgs.size() - 1);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DoctorHome.this, "Error loading messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMsg(String msgText, String ownerId) {
        String conversationId = ownerId.compareTo(doctorKey) < 0
                ? ownerId + "_" + doctorKey
                : doctorKey + "_" + ownerId;
        String msgId = FirebaseDatabase.getInstance()
                .getReference("chats")
                .child(conversationId)
                .push()
                .getKey();

        String doctorName = doctor.getName();

        ChatMessage msg = new ChatMessage(doctorKey, doctorName, ownerId, msgText);

        FirebaseDatabase.getInstance()
                .getReference("chats")
                .child(conversationId)
                .child(msgId)
                .setValue(msg)
                .addOnSuccessListener(aVoid -> {
                    if (chatInput != null) {
                        chatInput.setText("");
                    }
                    // Add to local list and update UI
                    chatMsgs.add(msg);
                    if (chatAdapter != null) {
                        chatAdapter.notifyItemInserted(chatMsgs.size() - 1);
                        if (chatRecyclerView != null) {
                            chatRecyclerView.scrollToPosition(chatMsgs.size() - 1);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkAvailabilityStatus() {
        if (doctorKey != null) {
            DatabaseReference doctorRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(doctorKey)
                    .child("available");

            doctorRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        boolean isAvailable = snapshot.getValue(Boolean.class);
                        if (availabilitySwitch != null){
                            availabilitySwitch.setChecked(isAvailable);
                            updateUIVisibility(isAvailable);
                            // Set switch color
                            if (isAvailable) {
                                availabilitySwitch.getThumbDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
                            } else {
                                availabilitySwitch.getThumbDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
                            }
                        }
                    } else {
                        // Default to available if not set
                        if (availabilitySwitch != null) {
                            availabilitySwitch.setChecked(true);
                            updateUIVisibility(true);
                            availabilitySwitch.getThumbDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(DoctorHome.this, "Failed to check availability", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateAvailabilityStatus(boolean isAvailable, FirebaseDoctor doctor) {
        if (doctorKey != null) {
            DatabaseReference doctorRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(doctorKey)
                    .child("available");

            doctorRef.setValue(isAvailable)
                    .addOnSuccessListener(aVoid -> {
                        String status = isAvailable ? "available" : "unavailable";  //Java short-hand notation (“W3Schools.com”)
                        Toast.makeText(DoctorHome.this, "You're now " + status, Toast.LENGTH_SHORT).show();
                        doctor.setLinked(isAvailable);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(DoctorHome.this, "Failed to update availability", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateUIVisibility(boolean isAvailable) {
        if (isAvailable) {
            // Available mode: hide unavailable message
            TextView unavailableMsg = findViewById(R.id.unavailableMsg);
            if (unavailableMsg != null) {
                unavailableMsg.setVisibility(View.GONE);
            }

            if (isDoctorLinked) {
                // Linked with owner: show all appointment functions, hide noOwner msg
                if (noOwnerMsg != null) {
                    noOwnerMsg.setVisibility(View.GONE);
                }

                // Show chat system
                showView(R.id.view7);
                showView(R.id.textView33);
                showView(R.id.chatRecyclerView);
                showView(R.id.chatInput);
                showView(R.id.sendChatButton);
                currentChatPartnerId = ownerId;

                // Show appointment functions
                showView(R.id.textView9);
                showView(R.id.textView37);
                showView(R.id.button13);
                showView(R.id.textView42);
                showView(R.id.button14);
                showView(R.id.textView43);
                showView(R.id.button15);
                showView(R.id.imageButton9);

                refreshSlotsWithCurrentData();
            } else {
                // Available but not linked: show noOwner msg, hide appointment functions
                if (noOwnerMsg != null) {
                    noOwnerMsg.setVisibility(View.VISIBLE);
                }
                hideAppointmentFunctions();
            }
        } else {
            // Unavailable mode: Show unavailable message, hide appointment functions
            TextView unavailableMsg = findViewById(R.id.unavailableMsg);
            if (unavailableMsg != null) {
                unavailableMsg.setVisibility(View.VISIBLE);
            }
            if (noOwnerMsg != null) {
                noOwnerMsg.setVisibility(View.GONE);
            }
            hideAppointmentFunctions();
        }
    }

    private void showView(int viewId) {
        View view = findViewById(viewId);
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
    }

    private void hideView(int viewId) {
        View view = findViewById(viewId);
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    private void hideAppointmentFunctions() {
        hideView(R.id.view7);
        hideView(R.id.textView33);
        hideView(R.id.chatRecyclerView);
        hideView(R.id.chatInput);
        hideView(R.id.sendChatButton);
        hideView(R.id.textView9);
        hideView(R.id.textView37);
        hideView(R.id.button13);
        hideView(R.id.textView42);
        hideView(R.id.button14);
        hideView(R.id.textView43);
        hideView(R.id.button15);
        hideView(R.id.imageButton9);
        hideView(R.id.textView44);
        hideView(R.id.button6);
        hideView(R.id.button7);
    }

    private void checkDoctorLinkStatus(){
        if (doctorKey != null){
            DatabaseReference doctorRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(doctorKey)
                    .child("linkedOwnerId");

            doctorRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.getValue() != null) {
                        ownerId = snapshot.getValue(String.class);
                        isDoctorLinked = (ownerId != null && !ownerId.isEmpty());
                        updateUIVisibility(availabilitySwitch.isChecked());

                        if (isDoctorLinked){
                            loadChatMsgs(ownerId);
                            loadOwnerName(ownerId);
                            loadOwnerBusyTimesAndRefresh();
                        }
                    } else {
                        isDoctorLinked = false; ownerId = null;
                        updateUIVisibility(availabilitySwitch.isChecked());
                        //show empty slots when not linked
                        refreshSlotsWithCurrentData();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(DoctorHome.this, "Failed to check link status", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadOwnerName(String ownerId){
        DatabaseReference ownerRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(ownerId)
                .child("username");

        ownerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String ownerName = snapshot.getValue(String.class);
                    TextView chatTitle = findViewById(R.id.textView33);
                    if (chatTitle != null && ownerName != null) {
                        chatTitle.setText("Chat with " + ownerName);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DoctorHome.this, "Error: Owner name not retrieved", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadOwnerBusyTimesAndRefresh() {
        if (ownerId != null) {
            DatabaseReference ownerRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(ownerId);

            ownerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    ownerBusy = (ArrayList<BusyTime>) snapshot.child("busyTimes").getValue();
                    if (ownerBusy == null) ownerBusy = new ArrayList<>();

                    refreshSlotsWithCurrentData();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(DoctorHome.this, "Failed to load owner schedule", Toast.LENGTH_SHORT).show();
                    refreshSlotsWithCurrentData();
                }
            });
        } else {
            refreshSlotsWithCurrentData();
        }
    }

    private void refreshSlotsWithCurrentData() {
        ArrayList<String> slots = AppointmentSlotCalc.findAvailableSlot(
                ownerBusy, doctor, doctor.getOpenDays(), doctor.getDurationMin()
        );
        displaySlots(slots);
    }

    private void setupLinkStatusListener(){
        if (doctorKey != null){
            DatabaseReference ownersRef = FirebaseDatabase.getInstance()
                    .getReference("users");

            Query linkedDoctorQuery = ownersRef.orderByChild("linkedDoctorId").equalTo(doctorKey);

            linkedDoctorQuery.addValueEventListener(new ValueEventListener(){
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    isDoctorLinked = snapshot.exists() && snapshot.getChildrenCount() > 0;
                    updateUIVisibility(availabilitySwitch.isChecked());

                    if (isDoctorLinked) {
                        loadOwnerBusyTimesAndRefresh();//reload busy times when link changes
                        Toast.makeText(DoctorHome.this, "An owner has linked with you!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(DoctorHome.this, "Failed to check link status", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}