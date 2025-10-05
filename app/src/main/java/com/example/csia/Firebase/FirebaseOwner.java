package com.example.csia.Firebase;


import java.util.ArrayList;


public class FirebaseOwner {
    public String name;
    public String identity;
    public String storeName;
    public String openingHours;
    public double weight;
    public double dailyIntake;
    public String latestShoppingDate;
    public ArrayList<String> openDays;
    public boolean googleConnected;
    public String linkedDoctorId, linkedGroomerId;

    public FirebaseOwner(){} //for Firebase deserialization

    public FirebaseOwner(String name, String storeName, String openingHours, double weight, double dailyIntake,
                         String latestShoppingDate, ArrayList<String> openDays){
        this.name = name;
        this.identity = "owner";
        this.storeName = storeName;
        this.openingHours = openingHours;
        this.weight = weight;
        this.dailyIntake = dailyIntake;
        this.latestShoppingDate = latestShoppingDate;
        this.openDays = openDays;
        this.googleConnected = false;
    }

    public void setGoogleConnected(boolean googleConnected){
        this.googleConnected = googleConnected;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) { this.identity = identity; }

    public String getOpeningHours() {
        return openingHours;
    }

    public double getDailyIntake() {
        return dailyIntake;
    }

    public String getLatestShoppingDate() {
        return latestShoppingDate;
    }

    public String getStoreName() {
        return storeName;
    }

    public double getWeight() {
        return weight;
    }

    public ArrayList<String> getOpenDays() {
        return openDays;
    }

    public String getName() {
        return name;
    }

    public void setLinkedDoctorId(String doctorId){ this.linkedDoctorId = doctorId;}

    public String getLinkedDoctorId(){ return linkedDoctorId; }

    public void setLinkedGroomerId(String groomerId) { this.linkedGroomerId = groomerId; }

    public String getLinkedGroomerId() { return linkedGroomerId; }
}
