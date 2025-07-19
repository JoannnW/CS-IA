package com.example.csia;

import android.os.Bundle;
import android.provider.CalendarContract;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.firebase.events.Event;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class OwnerHome extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.owner_homepage);


        String username = getIntent().getStringExtra("username");
        String storeName = getIntent().getStringExtra("storeName");
        String openingHours = getIntent().getStringExtra("openingHours");
        double weight = getIntent().getDoubleExtra("weight", 0.0);
        ArrayList<String> openDays = getIntent().getStringArrayListExtra("openDays");

        TextView welcomeTxt = findViewById(R.id.textView2);
        welcomeTxt.setText("Welcome, " + username);

        TextView storeNameTxt = findViewById(R.id.textView16);
        storeNameTxt.setText("Food supply from " + storeName);
    }

    //called after user signs in with their Google account
    private void fetchCalendarEvents(GoogleSignInAccount account){

        //helps Google identify user's account
        GoogleAccountCredential cred = GoogleAccountCredential.usingOAuth2(
                this, Collections.singleton(CalendarScopes.CALENDAR) //ask for Calendar permission
        );

        cred.setSelectedAccount(account.getAccount()); //set the signed-in Google account (modifier method)

        //create Calendar service using cred (use Google's good stuff and link it to my app)
        Calendar service = new Calendar.Builder(
                AndroidHttp.newCompatibleTransport(), //access Android's network Library
                JacksonFactory.getDefaultInstance(),  //Use Google's default JSON convertor
                cred)                                 //use the signed-in acc
                .setApplicationName("CSIA").build();

        //avoid freezing screen with Thread
        new Thread(()->{
            try {
                //set time to now
                DateTime now = new DateTime(System.currentTimeMillis());

                //ask Google Calendar for upcoming events
                CalendarContract.Events events = service.events().list("primary") //access events in "primary"/ main cal
                        .setMaxResults(5) //retrieve up to 5 events
                        .setTimeMin(now)  //only get future events
                        .setOrderBy("startTime")   //sort by when they start
                        .setSingleEvents(true)     //Ignore repeating events
                        .execute();

                //Store events in an ArrayList
                List<Event> items = events.getItems();

                //update buttons on screen (need to use onUIThread)
                runOnUiThread(()-> {
                    
                });
            }
        })
    }
}