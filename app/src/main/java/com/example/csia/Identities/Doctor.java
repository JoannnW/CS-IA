package com.example.csia.Identities;

import android.content.Context;
import android.content.Intent;

import com.example.csia.DoctorHome;
import com.example.csia.DoctorRegistration;

public class Doctor extends Identity {
    public Doctor(String name){
        super(name, "doctor");
    }

    @Override
    public void goToHome(Context context){
        Intent intent = new Intent(context, DoctorHome.class);
        intent.putExtra("userName", name); //store data as a key-value pair/ bundle data object
        context.startActivity(intent);
    }

    @Override
    public void goToRegistration(Context context){
        Intent intent = new Intent(context, DoctorRegistration.class);
        intent.putExtra("userName", name);
        context.startActivity(intent);
    }
}
