package com.example.csia.Utilities;

import com.example.csia.Firebase.FirebaseDoctor;
import com.example.csia.Firebase.FirebaseGroomer;
import com.example.csia.Utilities.BusyTime; // New helper class

import org.checkerframework.checker.units.qual.C;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AppointmentSlotCalc {
    public static ArrayList<String> findAvailableSlot(ArrayList<BusyTime> ownerBusyTimes,
                                                 Object serviceProvider,
                                                 ArrayList<String> openDays,
                                                 int durationMin){
        ArrayList<String> availableSlots = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        //get business hours
        String businessHours = "09:00-17:00";
        if (serviceProvider instanceof FirebaseDoctor){
            String hrs = ((FirebaseDoctor) serviceProvider).businessHrs;
            if (hrs != null && !hrs.trim().isEmpty()){
                businessHours = hrs;
            }
        } else if (serviceProvider instanceof FirebaseGroomer){
            String hrs = ((FirebaseGroomer) serviceProvider).businessHrs;
        if (hrs != null && !hrs.trim().isEmpty()){
            businessHours = hrs;
        }
        }

        //split the business hours string into start and end times
        String[] hours = businessHours.split("-");
        if (hours.length < 2) return availableSlots;

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
            for (int i = 0; i < 7; i ++){
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                String dayOfWeek = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US).toUpperCase();

                //check if provider is open this day
                if (!openDays.contains(dayOfWeek)) continue;

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

                //Generate open slots for this day
                while (slotCalendar.before(dayClose)){
                    Calendar endCalendar = (Calendar) slotCalendar.clone();
                    endCalendar.add(Calendar.MINUTE, durationMin);

                    //check if slot is available in owner's calendar
                   if (isSlotAvailable(slotCalendar.getTime(), endCalendar.getTime(), ownerBusyTimes)){
                       availableSlots.add(slotFormat.format(slotCalendar.getTime()));
                   }

                   //Move to next slot
                    slotCalendar.add(Calendar.MINUTE, 15);
                }

            }
        } catch (ParseException e){
            e.printStackTrace();
        }

        ArrayList<String> topSlots = new ArrayList<>();
        int count = Math.min(3, availableSlots.size());//in case there are less than 3 available slots
        for (int i = 0; i < count; i++) {
            topSlots.add(availableSlots.get(i));
        }
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
}

