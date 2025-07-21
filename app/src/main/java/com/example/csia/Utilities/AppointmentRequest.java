package com.example.csia.Utilities;

public class AppointmentRequest {
    public String ownerName;
    public String providerName;
    public String slotTime;
    public String type; //doctor/ groomer
    public String status; // pending/ accepted/ rejected

    public AppointmentRequest(){}

    public AppointmentRequest(String ownerName,
                              String providerName,
                              String slotTime,
                              String type,
                              String status){
        this.ownerName = ownerName;
        this.providerName = providerName;
        this.slotTime = slotTime;
        this.type = type;
        this.status = status;
    }
}
