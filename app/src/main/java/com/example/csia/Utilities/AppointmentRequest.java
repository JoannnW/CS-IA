package com.example.csia.Utilities;

public class AppointmentRequest {
    private String ownerId;
    private String providerName;
    private String type; // "groomer" or "doctor"
    private AppointmentOption requestedOption;
    private String status; // "pending", "accepted", "rejected"

    public AppointmentRequest() {} // Needed for Firebase

    public AppointmentRequest(String ownerId, String providerName, String type, AppointmentOption option) {
        this.ownerId = ownerId;
        this.providerName = providerName;
        this.type = type;
        this.requestedOption = option;
        this.status = "pending";
    }

    // Add getters and setters for all fields
    public String getOwnerId() { return ownerId; }
    public String getProviderName() { return providerName; }
    public String getType() { return type; }
    public AppointmentOption getRequestedOption() { return requestedOption; }
    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }
}