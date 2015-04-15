package com.example.bguise.dmplayer;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

/**
 * Created by Michael Heirendt on 3/18/2015.
 */
public class BrightnessService extends IntentService {
    public static final String ACTION = "com.example.bguise.dmplayer";
    private final Handler handler = new Handler();
    Intent intent;
    Bitmap bm;

    int recently_used;
    int brightness;

    int length;
    int frequency;
    String id;
    String name;
    int[] brightness_values; //size = length*frequency; brightness_values[timestamp*frequency] = brightness for current timestamp;

    private IBinder mBinder = new LocalBinder();

    public BrightnessService(){
        super("BrightnessService");
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public BrightnessService(String name) {
        super(name);
    }

    public IBinder onBind(Intent intent){
        return mBinder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    public class LocalBinder extends Binder {
        public BrightnessService getService(){
            return BrightnessService.this;
        }
    }

    //uses a handler to start the first update to be sent to the activity
    @Override
    public void onStart(Intent intent, int startId) {
        handleIntent(intent);
    }

    protected int handleIntent(Intent intent) {
        if(intent.hasExtra("dmtype")==true){
            if(intent.getStringExtra("dmtype").compareTo("initialize")==0){
                length = intent.getIntExtra("length", -1);
                frequency = intent.getIntExtra("frequency", -1);
                id = intent.getStringExtra("id");
                name = intent.getStringExtra("name");
                brightness_values = new int[length*frequency];
                for(int i = 0; i < length*frequency; i++){
                    brightness_values[i] = -1;
                }
                startTimer();

            }
            else{
                bm = (Bitmap) intent.getParcelableExtra("bitmap");
                int time = intent.getIntExtra("time", -1);
                brightness = calculateBrightness(bm);
                brightness_values[time*frequency] = brightness;

                recently_used = 1;
                return brightness;
            }
        }
        return -1;
    }

    private void startTimer() {
        handler.postDelayed(watchDog, 15*1000);
    }

    //function to be run using the handler
    private Runnable watchDog = new Runnable() {
        public void run() {
            checkRecentlyUsed();
        }
    };

    //sets up the information to be displayed by putting in an intent
    private void checkRecentlyUsed() {
        if(recently_used == 0){

            exportData();
            // this.stopSelf();
        }
        else{
            recently_used = 0;
        }
        handler.postDelayed(watchDog, 60*1000);
    }

    private void exportData() {

    }

    private int calculateBrightness(Bitmap bitmap) {
        long red = 0;
        long green = 0;
        long blue = 0;
        long pixelCount = 0;
        for (int y = bitmap.getHeight()/3; y < 2*bitmap.getHeight()/3; y+=5)
        {
            for (int x = 0; x < bitmap.getWidth(); x+=2)
            {
                int c = bitmap.getPixel(x, y);

                pixelCount++;
                red += Color.red(c);
                green += Color.green(c);
                blue += Color.blue(c);
            }
        }
        bitmap.recycle();
        return (int) (10000*(0.299*(double) (red/pixelCount / 255.0)+ 0.587*(double) (green / pixelCount / 255.0)+ .114*(double) (blue / pixelCount / 255.0)));
    }

    //--------------------------------------------------------------------------

  /*  //Initializes the intent to be used for the remainder of this service's lifetime
    @Override
    public void onCreate() {
        super.onCreate();
        intent = new Intent(ACTION);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        handler.removeCallbacks(sendUpdatesToUI);
        super.onDestroy();
    }*/
}