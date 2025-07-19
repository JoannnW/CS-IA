package com.example.csia.Identities;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.example.csia.GroomerHome;
import com.example.csia.GroomerRegistration;

import java.util.List;

public class Groomer extends Identity {
    private List<String> openDays;
    private String businessHrs;
    private int durationMin = 0;

    //used for hours/minutes validation
    public static final int VALID_TIME = 0;
    public static final int EMPTY_TIME = 1;
    public static final int MISSING_H = 2;
    public static final int INVALID_FORMAT = 3;
    public static final int HOURS_NOT_NUMBER = 4;
    public static final int MINUTES_NOT_NUMBER = 5;
    public static final int HOURS_NEGATIVE = 6;
    public static final int MINUTES_INVALID = 7;
    public static final int MISSING_M = 8;


    public Groomer(String name, List<String> openDays, String businessHrs, int durationMin){
        super(name, "groomer");
        this.openDays = openDays;
        this.businessHrs = businessHrs;
        this.durationMin += durationMin;
    }

    public List<String> getOpenDays() {
        return openDays;
    }

    public String getBusinessHrs() {
        return businessHrs;
    }

    public int getDurationMin() {
        return durationMin;
    }

    @NonNull
    @Override
    public String toString() {
        return "Name: " + name + ", OpenDays: " + openDays.toString() + ", businessHrs: " + businessHrs + ", Appointment duration: " + duration;
    }

    public Groomer(String name){
        super(name, "groomer");
    }

    @Override
    public void goToHome(Context context){
        Intent intent = new Intent(context, GroomerHome.class);
        intent.putExtra("username", name); //store data as a key-value pair/ bundle data object
        context.startActivity(intent);
    }

    @Override
    public void goToRegistration(Context context){
        Intent intent = new Intent(context, GroomerRegistration.class);
        intent.putExtra("username", name);
        context.startActivity(intent);
    }

    public static boolean isValidTimeRange(String str) {
        return str.matches("\\d{2}:\\d{2}-\\d{2}:\\d{2}"); //"\\d{2} means exactly 2 digits
    }
    public static boolean isValidHours(String str) {
        return str.matches("\\d{1}h\\d{1,2}m");
    }
    public static int isValidHrMin(String timeStr) {    // --- Basic Checks ---
        if (timeStr == null || timeStr.isEmpty()) {
            return EMPTY_TIME;
        }

        String lowerStr = timeStr.toLowerCase();

        // --- Check for 'h' ---
        if (!lowerStr.contains("h")) {
            return MISSING_H;
        }

        // Validate minutes=
        if (!lowerStr.contains("m")) {
            return MISSING_M;
        }

        // --- Split Hours & Minutes ---
        String[] parts = lowerStr.split("h");
        if (parts.length != 2) {
            return INVALID_FORMAT;
        }

        // --- Parse Hours ---
        int hours;
        try {
            hours = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return HOURS_NOT_NUMBER;
        }

        // --- Parse Minutes ---
        String minutePart = parts[1].replace("m", ""); // Remove 'm' if present
        int minutes;
        try {
            minutes = minutePart.isEmpty() ? 0 : Integer.parseInt(minutePart);
        } catch (NumberFormatException e) {
            return MINUTES_NOT_NUMBER;
        }

        // --- Validate Ranges ---
        if (hours < 0) {
            return HOURS_NEGATIVE;
        }
        if (minutes < 0 || minutes > 59) {
            return MINUTES_INVALID;
        }

        return VALID_TIME; // All checks passed!
    }
}
