package com.example.csia.Identities;

import android.content.Context;
import android.content.Intent;

import com.example.csia.GroomerHome;
import com.example.csia.GroomerRegistration;

public class Groomer extends Identity {
    public Groomer(String name){
        super(name, "groomer");
    }

    @Override
    public void goToHome(Context context){
        Intent intent = new Intent(context, GroomerHome.class);
        intent.putExtra("userName", name); //store data as a key-value pair/ bundle data object
        context.startActivity(intent);
    }

    @Override
    public void goToRegistration(Context context){
        Intent intent = new Intent(context, GroomerRegistration.class);
        intent.putExtra("userName", name);
        context.startActivity(intent);
    }
}
