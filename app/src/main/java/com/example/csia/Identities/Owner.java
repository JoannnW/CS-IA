package com.example.csia.Identities;

import android.content.Context;
import android.content.Intent;

import com.example.csia.OwnerHome;
import com.example.csia.OwnerRegistration;

public class Owner extends Identity {
    public Owner(String name){
        super(name, "owner");
    }

    @Override
    public void goToHome(Context context){
        Intent intent = new Intent(context, OwnerHome.class);
        intent.putExtra("userName", name); //store data as a key-value pair/ bundle data object
        context.startActivity(intent);
    }

    @Override
    public void goToRegistration(Context context){
        Intent intent = new Intent(context, OwnerRegistration.class);
        intent.putExtra("userName", name);
        context.startActivity(intent);
    }
}
