package com.example.csia.Utilities;

import java.util.Date;

public class BusyTime {
    private final Date startTime;
    private final Date endTime;

    public BusyTime(Date startTime, Date endTime){
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Date getStartTime(){
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }
}
