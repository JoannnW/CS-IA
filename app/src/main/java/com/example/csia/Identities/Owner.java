package com.example.csia.Identities;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.example.csia.OwnerHome;
import com.example.csia.OwnerRegistration;

import java.util.List;

public class Owner extends Identity {
    private String storeName;
    private String openingHours;
    private double weight;
    private List<String> openDays;

    public Owner(String name, String storeName, String openingHours, double weight, List<String> openDays){
        super(name, "owner");
        this.storeName = storeName;
        this.openingHours = openingHours;
        this.weight = weight;
        this.openDays = openDays;
    }

    public String getStoreName(){
        return storeName;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public double getWeight() {
        return weight;
    }

    public List<String> getOpenDays() {
        return openDays;
    }

    @NonNull
    @Override
    public String toString() {
        return "Name: "+ name + ", Store: " + storeName + ", Opening Hours: " + openingHours + ", Weight: " + weight + ", Days: " + openDays.toString();
    }

    public Owner(String name){
        super(name, "owner");
    }

    @Override
    public void goToHome(Context context){
        Intent intent = new Intent(context, OwnerHome.class);
        intent.putExtra("username", name); //store data as a key-value pair/ bundle data object
        context.startActivity(intent);
    }

    @Override
    public void goToRegistration(Context context){
        Intent intent = new Intent(context, OwnerRegistration.class);
        intent.putExtra("username", name);
        context.startActivity(intent);
    }

    public static boolean isValidTimeRange(String str) {
        return str.matches("\\d{2}:\\d{2}-\\d{2}:\\d{2}"); //"\\d{2} means exactly 2 digits
    }
    public static boolean isValidWeight(double weight) {
        return weight >= 10.00 && weight <= 999.99;
    }
}
