package com.example.csia.Firebase;

import java.util.ArrayList;
import java.util.List;

public class FirebaseGroomer {
    public String name;
    public String identity;
    public ArrayList<String> openDays;
    public String businessHrs;
    public int durationMin = 0;
    public boolean isGoogleConnected;

    public FirebaseGroomer(){} //for Firebase deserialization

    public FirebaseGroomer(String name, ArrayList<String> openDays, String businessHrs, int durationMin){
        this.name = name;
        this.identity = "groomer";
        this.openDays = openDays;
        this.businessHrs = businessHrs;
        this.durationMin = durationMin;
    }

    public int getDurationMin() {
        return durationMin;
    }

    public String getBusinessHrs() {
        return businessHrs;
    }

    public List<String> getOpenDays() {
        return openDays;
    }

    public String getIdentity() {
        return identity;
    }

    public String getName() {
        return name;
    }

    public void setGoogleConnected(boolean googleConnected) {
        isGoogleConnected = googleConnected;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }
}
