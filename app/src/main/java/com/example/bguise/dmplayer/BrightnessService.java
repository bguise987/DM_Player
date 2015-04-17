package com.example.bguise.dmplayer;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;

import java.io.*;

/**
 * Created by Michael Heirendt on 3/18/2015.
 */
public class BrightnessService extends IntentService {
    public static final String ACTION = "com.example.bguise.dmplayer";
    private final Handler handler = new Handler();
    Intent intent;
    Bitmap bm;
    int counter = 0; //for watchdog timer to cause periodic uploads of dimming scheme
    int recently_used;
    int brightness;

    int length;
    int frequency;
    String id;
    String name;
    int[] brightness_values; //size = length*frequency; brightness_values[timestamp*frequency] = brightness for current timestamp;

    private IBinder mBinder = new LocalBinder();

    public BrightnessService() {
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

    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    public int getBrightness(int time) {
        return brightness_values[time*frequency];
    }

    public int getFrequency() {
        return frequency;
    }

    public class LocalBinder extends Binder {
        public BrightnessService getService() {
            return BrightnessService.this;
        }
    }

    //uses a handler to start the first update to be sent to the activity
    @Override
    public void onStart(Intent intent, int startId) {
        handleIntent(intent);
    }

    // TODO: Delete this method once bitmap grabbing works, for demo purposes only
    protected int setBrightnessAtTime(Intent intent) {
        if (intent.hasExtra("dmtype") == true) {
            if (intent.getStringExtra("dmtype").compareTo("initialize") == 0) {
                length = intent.getIntExtra("length", -1);
                frequency = intent.getIntExtra("frequency", -1);
                id = intent.getStringExtra("id");
                name = intent.getStringExtra("name");
                brightness_values = new int[length * frequency];
                for (int i = 0; i < length * frequency; i++) {
                    brightness_values[i] = -1;
                }
                startTimer();

            } else {
                //bm = (Bitmap) intent.getParcelableExtra("bitmap");
                int time = intent.getIntExtra("time", -1);
                brightness = intent.getIntExtra("brightness", -1);
                brightness_values[time * frequency] = brightness;

                recently_used = 1;
                return brightness;
            }
        }
        return -1;
    }

    protected int handleIntent(Intent intent) {
        if (intent.hasExtra("dmtype") == true) {
            if (intent.getStringExtra("dmtype").compareTo("initialize") == 0) {
                length = intent.getIntExtra("length", -1);
                frequency = intent.getIntExtra("frequency", -1);
                id = intent.getStringExtra("id");
                name = intent.getStringExtra("name");
                brightness_values = new int[length * frequency];
                for (int i = 0; i < length * frequency; i++) {
                    brightness_values[i] = -1;
                }
                startTimer();

            } else {
                bm = (Bitmap) intent.getParcelableExtra("bitmap");
                int time = intent.getIntExtra("time", -1);
                brightness = calculateBrightness(bm);
                brightness_values[time * frequency] = brightness;

                recently_used = 1;
                return brightness;
            }
        }
        return -1;
    }

    private void startTimer() {
        handler.postDelayed(watchDog, 15 * 1000);
    }

    //function to be run using the handler
    private Runnable watchDog = new Runnable() {
        public void run() {
            checkRecentlyUsed();
        }
    };

    //sets up the information to be displayed by putting in an intent
    private void checkRecentlyUsed() {
        counter++;
        try {
            exportData();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(counter==4) {
            counter = 0;

            if (recently_used == 0) {
                int[] yeah = brightness_values;
                try {
                    exportData();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                this.stopSelf();
            } else {
                recently_used = 0;
            }
        }
        handler.postDelayed(watchDog, 15 * 1000);
    }

    private void exportData() throws IOException {
        String files_PATH = Environment.getExternalStorageDirectory() + "/";
        FileOutputStream fop = null;
        File file;
        String content = "";
        try {

            file = new File(files_PATH + "dim.txt");
            fop = new FileOutputStream(file);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            content = name + "," + id + "," + frequency + "," + length + System.getProperty("line.separator");
            byte[] contentInBytes = content.getBytes();
            fop.write(contentInBytes);

            for (int i = 0; i < brightness_values.length; i++) {
                content = "" + brightness_values[i] + System.getProperty("line.separator");
                contentInBytes = content.getBytes();
                fop.write(contentInBytes);
            }

            fop.flush();
            fop.close();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fop != null) {
                    fop.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Uploader uploader = new Uploader(BrightnessService.this, name, id, length, frequency, brightness_values);
        uploader.testUpload("dim.txt");
        this.stopSelf();
    }

    private int calculateBrightness(Bitmap bitmap) {
        long red = 0;
        long green = 0;
        long blue = 0;
        long pixelCount = 0;
        for (int y = bitmap.getHeight() / 3; y < 2 * bitmap.getHeight() / 3; y += 5) {
            for (int x = 0; x < bitmap.getWidth(); x += 2) {
                int c = bitmap.getPixel(x, y);

                pixelCount++;
                red += Color.red(c);
                green += Color.green(c);
                blue += Color.blue(c);
            }
        }
        bitmap.recycle();
        return (int) (100 * (0.299 * (double) (red / pixelCount / 255.0) + 0.587 * (double) (green / pixelCount / 255.0) + .114 * (double) (blue / pixelCount / 255.0)));
    }

    @Override
    public void onDestroy() {
        try {
            exportData();
        } catch (IOException e) {
            e.printStackTrace();
        }
        handler.removeCallbacks(watchDog);
        super.onDestroy();
    }

    public boolean findDimmingScheme(String n, String i){
        String files_PATH = Environment.getExternalStorageDirectory() + "/";
        Downloader downloader = new Downloader(BrightnessService.this, n, i);
        int count = 0;
        try {
            downloader.testDownload(n, i);
            Thread.sleep(5000);
            BufferedReader br = new BufferedReader(new FileReader(files_PATH+"dim2.txt"));
            try {
                String firstline = br.readLine();
                String[] parts = firstline.split(",");
                name = parts[0];
                id = parts[1];
                if(id.compareTo(i)!=0){
                    return false;
                }
                frequency = Integer.parseInt(parts[2]);
                length = Integer.parseInt(parts[3]);
                brightness_values = new int[length*frequency];
                String line = br.readLine();
                while (line != null && count<brightness_values.length) {
                    brightness_values[count]=Integer.parseInt(line);
                    line = br.readLine();
                    count++;
                }
            } finally {
                br.close();
            }
            return true;
        }catch(Exception e){

        }
        return false;
    }
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
}*/