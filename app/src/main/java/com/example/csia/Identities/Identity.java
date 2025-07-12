package com.example.csia.Identities;
import android.content.Context;


public abstract class Identity{
    protected String name;
    protected String identity;

    public Identity(String name, String identity){
        this.name = name;
        this.identity = identity;
    }

    public abstract void goToHome(Context context);
    public abstract void goToRegistration(Context context);
}
