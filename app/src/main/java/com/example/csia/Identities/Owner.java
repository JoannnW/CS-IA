package com.example.csia.Identities;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.example.csia.OwnerHome;
import com.example.csia.OwnerRegistration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Owner extends Identity {
    private String storeName;
    private String openingHours;
    private double weight;
    private double dailyIntake;
    private String latestShoppingDate;
    private List<String> openDays;
    private boolean googleConnected;

    public Owner(String name,
                 String storeName,
                 String openingHours,
                 double weight,
                 double dailyIntake,
                 String latestShoppingDate,
                 List<String> openDays,
                 boolean googleConnected){
        super(name, "owner");
        this.storeName = storeName;
        this.openingHours = openingHours;
        this.weight = weight;
        this.dailyIntake = dailyIntake;
        this.latestShoppingDate = latestShoppingDate;
        this.openDays = openDays;
        this.googleConnected = googleConnected;
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

    public double getDailyIntake() { return dailyIntake; }

    public String getLatestShoppingDate() { return latestShoppingDate; }

    public void setGoogleConnected(boolean googleConnected) {
        this.googleConnected = googleConnected;
    }

    public boolean isGoogleConnected(){
        return googleConnected;
    }

    @NonNull
    @Override
    public String toString() {
        return "Name: "+ name + ", Store: " + storeName + ", Opening Hours: " + openingHours + ", Weight: " + weight + ", Daily Intake: " + dailyIntake + ", Days: " + openDays.toString();
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
        if (!str.matches("\\d{2}:\\d{2}-\\d{2}:\\d{2}")){//"\\d{2} means exactly 2 digits
            return false;
        }
        String[] parts = str.split("-");
        String openTime = parts[0];
        String closeTime = parts[1];

        try{
            int openHour = Integer.parseInt(openTime.substring(0,2));
            int openMin = Integer.parseInt(openTime.substring(3));
            int closeHour = Integer.parseInt(closeTime.substring(0,2));
            int closeMin = Integer.parseInt(closeTime.substring(3));

            return  openHour   >= 0 && openHour < 24 &&
                    openMin    >= 0 && openMin  < 60 &&
                    closeHour  >= 0 && closeHour < 24 &&
                    closeMin   >= 0 && closeMin  < 60;
        } catch (Exception e){
            return false;
        }
    }
    public static boolean isValidWeight(double weight) {
        return weight > 0.00 && weight <= 999.99;
    }

    public static boolean isValidDate(String dateStr){
        if (dateStr == null || dateStr.isEmpty()) return false;

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        sdf.setLenient(false); // strict parsing
        try {
            sdf.parse(dateStr);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }
}
