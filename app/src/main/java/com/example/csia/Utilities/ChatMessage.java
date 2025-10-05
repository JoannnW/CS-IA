package com.example.csia.Utilities;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatMessage {
    private String senderId;
    private String senderName;
    private String receiverId;
    private String message;
    private String timestamp;

    // Default constructor for firebase
    public ChatMessage() {}

    public ChatMessage(String senderId, String senderName, String receiverId, String message) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
    }

    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getReceiverId() { return receiverId; }
    public String getMessage() { return message; }
    public String getTimestamp() { return timestamp; }
}