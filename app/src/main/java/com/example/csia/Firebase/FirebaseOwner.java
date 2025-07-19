package com.example.csia.Firebase;

import java.util.List;

public class FirebaseOwner {
    public String name;
    public String identity;
    public String storeName;
    public String openingHours;
    public double weight;
    public List<String> openDays;

    public FirebaseOwner(){} //for Firebase deserialization

    public FirebaseOwner(String name, String storeName, String openingHours, double weight, List<String> openDays){
        this.name = name;
        this.identity = "owner";
        this.storeName = storeName;
        this.openingHours = openingHours;
        this.weight = weight;
        this.openDays = openDays;
    }
}
