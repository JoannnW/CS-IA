package com.example.csia.Firebase;

import java.util.List;

public class FirebaseDoctor {
    public String name;
    public String identity;
    public List<String> openDays;
    public String businessHrs;
    public int durationMin;

    public FirebaseDoctor(){} //for Firebase deserialization

    public FirebaseDoctor(String name, List<String> openDays, String businessHrs, int durationMin){
        this.name = name;
        this.identity = "doctor";
        this.openDays = openDays;
        this.businessHrs = businessHrs;
        this.durationMin = durationMin;
    }

    public String getName() {
        return name;
    }

    public List<String> getOpenDays() {
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
}
