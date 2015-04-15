package com.example.bguise.dmplayer;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import java.util.Date;

/**
 * Created by Michael Heirendt on 2/18/2015.
 */
public class TimerService extends Service {
    public static final String ACTION = "com.example.bguise.Timer";
    private final Handler handler = new Handler();
    Intent intent;
    int start = 0;
    int frequency;

    //Initializes the intent to be used for the remainder of this service's lifetime
    @Override
    public void onCreate() {
        super.onCreate();
        intent = new Intent(ACTION);
    }

    //uses a handler to start the first update to be sent to the activity
    @Override
    public void onStart(Intent intent, int startId) {
        frequency = intent.getIntExtra("frequency", -1);
        handler.removeCallbacks(sendUpdatesToUI);
        handler.postDelayed(sendUpdatesToUI, 1000/frequency);

    }

    //function to be run using the handler
    private Runnable sendUpdatesToUI = new Runnable() {
        public void run() {
            DisplayLoggingInfo();
            handler.postDelayed(this, 1000/frequency);
        }
    };

    //sets up the information to be displayed by putting in an intent
    private void DisplayLoggingInfo() {
        intent.putExtra("time", new Date().toLocaleString());
        intent.putExtra("counter", String.valueOf(start+=5));
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(sendUpdatesToUI);
        super.onDestroy();
    }
}