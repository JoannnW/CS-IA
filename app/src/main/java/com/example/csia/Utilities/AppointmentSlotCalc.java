package com.example.csia.Utilities;

import android.util.Log;

import com.example.csia.Firebase.FirebaseDoctor;
import com.example.csia.Firebase.FirebaseGroomer;
import com.example.csia.Utilities.BusyTime; // New helper class

import org.checkerframework.checker.units.qual.C;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AppointmentSlotCalc {
    public static ArrayList<String> findAvailableSlot(ArrayList<BusyTime> ownerBusyTimes,
                                                 Object serviceProvider,
                                                 ArrayList<String> openDays,
                                                 int durationMin){
        ArrayList<String> availableSlots = new ArrayList<>();

        //debug technique ("Understand logging", "Logging Suggestions")
        Log.d("SlotCalc", "Starting slot calculation");
        Log.d("SlotCalc", "Open days: " + openDays + ", Duration: " + durationMin);

        //if opendays is null or empty, use default days
        if (openDays == null || openDays.isEmpty()){
            openDays = new ArrayList<>(Arrays.asList("Mon", "TUE", "WED", "THU", "FRI"));
            Log.d("SlotCalc", "Using default open days: " + openDays);//("Understand logging", "Logging Suggestions")
        }

        // If duration is invalid, use default 60 minutes
        if (durationMin <= 0) {
            durationMin = 60;
            //debug to figure out why slots might be empty ("Understand logging", "Logging Suggestions")
            Log.d("SlotCalc", "Using default duration: " + durationMin);//("Understand logging", "Logging Suggestions")
        }

        // If ownerBusyTimes is null, initialize empty list
        if (ownerBusyTimes == null) {
            ownerBusyTimes = new ArrayList<>();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Get business hours with fallback
        String businessHours = "09:00-17:00";
        if (serviceProvider instanceof FirebaseDoctor) {
            String hrs = ((FirebaseDoctor) serviceProvider).businessHrs;
            if (hrs != null && !hrs.trim().isEmpty()) {
                businessHours = hrs;
            }
        } else if (serviceProvider instanceof FirebaseGroomer) {
            String hrs = ((FirebaseGroomer) serviceProvider).businessHrs;
            if (hrs != null && !hrs.trim().isEmpty()) {
                businessHours = hrs;
            }
        }
        Log.d("SlotCalc", "Business hours: " + businessHours);//("Understand logging", "Logging Suggestions")


        //split the business hours string into start and end times
        String[] hours = businessHours.split("-");
        if (hours.length < 2) {
            Log.d("SlotCalc", "Invalid business hours format, using defaults");//("Understand logging", "Logging Suggestions")
            hours = new String[]{"09:00", "17:00"};
        }

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat slotFormat = new SimpleDateFormat("EEE, MMM d, yyyy HH:mm", Locale.getDefault());

        try {
            //parse the start and end times into Date objects
            Date openTime = timeFormat.parse(hours[0].trim());
            Date closeTime = timeFormat.parse(hours[1].trim());

            //extract hour/min from parsed times into separate Cal instances
            Calendar openCal = Calendar.getInstance();
            openCal.setTime(openTime);

            Calendar closeCal = Calendar.getInstance();
            closeCal.setTime(closeTime);

            //Loop through the next 7 days (starting from tmr)
            for (int i = 1; i < 7; i ++){
                Calendar dayCalendar = (Calendar) calendar.clone();
                dayCalendar.add(Calendar.DAY_OF_MONTH, i);

                //get day of the week
                String dayOfWeek = dayCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US).toUpperCase();

                //check if provider is open this day
                if (!openDays.contains(dayOfWeek)) {
                    Log.d("SlotCalc", "Skipping " + dayOfWeek + " - not in open days");//("Understand logging", "Logging Suggestions")
                    continue; // Skips entire day if not listed in open days
                }

                //set calendar to open times
                Calendar slotCalendar = (Calendar) calendar.clone();
                slotCalendar.set(Calendar.HOUR_OF_DAY, openCal.get(Calendar.HOUR_OF_DAY));
                slotCalendar.set(Calendar.MINUTE, openCal.get(Calendar.MINUTE));
                slotCalendar.set(Calendar.SECOND, 0);
                slotCalendar.set(Calendar.MILLISECOND, 0);

                //set the closing time for that same day
                Calendar dayClose = (Calendar) calendar.clone();
                dayClose.set(Calendar.HOUR_OF_DAY, closeCal.get(Calendar.HOUR_OF_DAY));
                dayClose.set(Calendar.MINUTE, closeCal.get(Calendar.MINUTE));
                dayClose.set(Calendar.SECOND, 0);
                dayClose.set(Calendar.MILLISECOND, 0);

                Log.d("SlotCalc", "Checking " + dayOfWeek + " from " + slotCalendar.getTime() + " to " + dayClose.getTime());//("Understand logging", "Logging Suggestions")

                //Generate open slots for this day
                while (slotCalendar.before(dayClose)){
                    Calendar endCalendar = (Calendar) slotCalendar.clone();
                    endCalendar.add(Calendar.MINUTE, durationMin);

                    // Check if slot extends beyond closing time
                    if (endCalendar.after(dayClose)) {
                        break;
                    }

                    //check if slot is available in owner's calendar
                   if (isSlotAvailable(slotCalendar.getTime(), endCalendar.getTime(), ownerBusyTimes)){
                       String slot = slotFormat.format(slotCalendar.getTime());
                       availableSlots.add(slot);
                       Log.d("SlotCalc", "Added available slot: " + slot);//("Understand logging", "Logging Suggestions")
                   }

                    //Move to next slot by the appointment duration to find non-overlapping slots
                    slotCalendar.add(Calendar.MINUTE, durationMin);

                }

            }
        } catch (ParseException e){
            Log.e("SlotCalc", "Error parsing business hours", e);//("Understand logging", "Logging Suggestions")
            e.printStackTrace();
        }

        // If no slots were generated, create some default slots
        if (availableSlots.isEmpty()) {
            Log.d("SlotCalc", "No slots generated, creating default slots");//("Understand logging", "Logging Suggestions")
            createDefaultSlots(availableSlots, calendar, durationMin);
        }

        ArrayList<String> topSlots = new ArrayList<>();
        int count = Math.min(3, availableSlots.size());//in case there are less than 3 available slots
        for (int i = 0; i < count; i++) {
            topSlots.add(availableSlots.get(i));
        }
        Log.d("SlotCalc", "Returning " + topSlots.size() + " slots");//("Understand logging", "Logging Suggestions")
        //Return top 3 slots
        return topSlots;
    }

    private static boolean isSlotAvailable(Date start, Date end, ArrayList<BusyTime> busyTimes){
        for (BusyTime busy : busyTimes){
           if (start.before(busy.getEndTime()) && end.after(busy.getStartTime())){
               return false; //there is a conflict
           }
        }
        return true; //no conflict, is available
    }

    private static void createDefaultSlots(ArrayList<String> availableSlots, Calendar calendar, int durationMin) {
        SimpleDateFormat slotFormat = new SimpleDateFormat("EEE, MMM d, yyyy HH:mm", Locale.getDefault());

        // Add slots for next 3 days at 10:00 AM, 2:00 PM, and 4:00 PM
        for (int i = 1; i <= 3; i++) {
            Calendar slotCal = (Calendar) calendar.clone();
            slotCal.add(Calendar.DAY_OF_MONTH, i);

            // Morning slot
            slotCal.set(Calendar.HOUR_OF_DAY, 10);
            slotCal.set(Calendar.MINUTE, 0);
            availableSlots.add(slotFormat.format(slotCal.getTime()));

            // Afternoon slot
            slotCal.set(Calendar.HOUR_OF_DAY, 14);
            availableSlots.add(slotFormat.format(slotCal.getTime()));

            // Late afternoon slot
            slotCal.set(Calendar.HOUR_OF_DAY, 16);
            availableSlots.add(slotFormat.format(slotCal.getTime()));
        }
    }
}

