package com.example.csia.Firebase;

import java.util.ArrayList;

public class FirebaseGroomer {
    public String name;
    public String identity;
    public ArrayList<String> openDays;
    public String businessHrs;
    public int durationMin = 0;
    public boolean googleConnected, isLinked;
    public String linkedOwnerid;

    public FirebaseGroomer(){} //for Firebase deserialization

    public FirebaseGroomer(String name, ArrayList<String> openDays, String businessHrs, int durationMin){
        this.name = name;
        this.identity = "groomer";
        this.openDays = openDays;
        this.businessHrs = businessHrs;
        this.durationMin = durationMin;
        this.googleConnected = false;
        this.isLinked = false;
    }

    public int getDurationMin() {
        return durationMin;
    }

    public String getBusinessHrs() {
        return businessHrs;
    }

    public ArrayList<String> getOpenDays() {
        return openDays;
    }

    public String getIdentity() {
        return identity;
    }

    public String getName() {
        return name;
    }

    public void setGoogleConnected(boolean googleConnected) {
        this.googleConnected = googleConnected;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public void setLinked(boolean linked) { isLinked = linked; }

    public boolean getLinked (){ return isLinked; }

    public String getLinkedOwnerid() {
        return linkedOwnerid;
    }

    public void setLinkedOwnerid(String linkedOwnerid) {
        this.linkedOwnerid = linkedOwnerid;
    }
}
