package com.example.csia.Firebase;

public class FirebaseAppointmentReq {
    public String ownerId;
    public String serviceProviderId;
    public String type; //doctor or groomer or owner's schedule
    public String status; //pending, accepted or rejected
    public String dateTime;
    public int duration;

    public FirebaseAppointmentReq(){}

    public FirebaseAppointmentReq(String ownerId, String serviceProviderId, String type, String dateTime, int duration){
        this.ownerId = ownerId;
        this.serviceProviderId = serviceProviderId;
        this.type = type;
        this.status = "pending";
        this.dateTime = dateTime;
        this.duration = duration;
    }
}
