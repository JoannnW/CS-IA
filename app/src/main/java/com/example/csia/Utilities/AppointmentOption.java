package com.example.csia.Utilities;

public class AppointmentOption {
    private String dateTimeRange;
    private boolean isDeferred;

    public AppointmentOption(){}

    public AppointmentOption(String dateTimeRange){
        this.dateTimeRange = dateTimeRange;
        this.isDeferred = false;
    }

    public String getDateTimeRange(){ return dateTimeRange; }
    public void setDateTimeRange(String dateTimeRange){ this.dateTimeRange = dateTimeRange; }

    public boolean isDeferred(){ return isDeferred; }
    public void setDeferred(boolean deferred) { isDeferred = deferred; }
}
