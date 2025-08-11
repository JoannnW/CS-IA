package com.example.csia.Firebase;

import java.util.ArrayList;

public class FirebaseDoctor {
    public String name;
    public String identity;
    public ArrayList<String> openDays;
    public String businessHrs;
    public int durationMin;
    public boolean googleConnected;

    public FirebaseDoctor(){} //for Firebase deserialization

    public FirebaseDoctor(String name, ArrayList<String> openDays, String businessHrs, int durationMin){
        this.name = name;
        this.identity = "doctor";
        this.openDays = openDays;
        this.businessHrs = businessHrs;
        this.durationMin = durationMin;
    }

    public String getName() {
        return name;
    }

    public ArrayList<String> getOpenDays() {
        return openDays;
    }

    public int getDurationMin() {
        return durationMin;
    }

    public String getBusinessHrs() {
        return businessHrs;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public void setGoogleConnected(boolean googleConnected) {
        this.googleConnected = googleConnected;
    }
}
