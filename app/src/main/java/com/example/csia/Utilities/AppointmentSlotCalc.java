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
import java.util.List;
import java.util.Locale;

public class AppointmentSlotCalc {
    public static List<String> findAvailableSlot(List<BusyTime> ownerBusyTimes,
                                                 Object serviceProvider,
                                                 List<String> openDays,
                                                 int durationMin){
        List<String> availableSlots = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        //get business hours
        String businessHours = "";
        if (serviceProvider instanceof FirebaseDoctor){
            businessHours = ((FirebaseDoctor) serviceProvider).businessHrs;
        } else if (serviceProvider instanceof FirebaseGroomer){
            businessHours = ((FirebaseGroomer) serviceProvider).businessHrs;
        }

        String[] hours = businessHours.split("-");
        if (hours.length < 2) return availableSlots;

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat slotFormat = new SimpleDateFormat("EEE, MMM d, yyyy HH:mm", Locale.getDefault());

        try {
            Date openTime = timeFormat.parse(hours[0].trim());
            Date closeTime = timeFormat.parse(hours[1].trim());

            //Check for next 7 days
            for (int i = 0; i < 7; i ++){
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                String dayOfWeek = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US).toUpperCase();

                //check if provider is open this day
                if (!openDays.contains(dayOfWeek)) continue;

                //set calendar to open times
                Calendar slotCalendar = (Calendar) calendar.clone();
                slotCalendar.set(Calendar.HOUR_OF_DAY, openTime.getHours());
                slotCalendar.set(Calendar.MINUTE, openTime.getMinutes());
                slotCalendar.set(Calendar.SECOND, 0);
                slotCalendar.set(Calendar.MILLISECOND, 0);

                //Generate open slots for this day
                while (slotCalendar.getTime().before(closeTime)){
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

        //Return top 3 slots
        return availableSlots.subList(0,Math.min(3,availableSlots.size()));
    }

    private static boolean isSlotAvailable(Date start, Date end, List<BusyTime> busyTimes){
        for (BusyTime busy : busyTimes){
           if (start.before(busy.getEndTime()) && end.after(busy.getStartTime())){
               return false;
           }
        }
        return true;
    }
}
