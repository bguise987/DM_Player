package com.example.bguise.dmplayer;


import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bguise.developerkey.DeveloperKey;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;



public class PlayActivity extends YouTubeBaseActivity implements YouTubePlayer.OnInitializedListener {
    private Intent intent;
    private final Handler handler = new Handler();
    private YouTubePlayerView youTubeView;
    private YouTubePlayer ytplayer;
    protected int frequency = 1;
    private int initialized = 0;
    private String name = "name";
    private String id = "id";
    private int useDimming = 0;

    BrightnessService mService;
    boolean mBound;


    private String videoID;

    ServiceConnection mConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBound = true;
            BrightnessService.LocalBinder binder = (BrightnessService.LocalBinder)service;
            mService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
            //mService = null;
        }
    };










    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        youTubeView = (YouTubePlayerView)findViewById(R.id.youtube_player);
        youTubeView.initialize(DeveloperKey.DEVELOPER_KEY, this);

        Intent i = new Intent(this, BrightnessService.class);
        bindService(i, mConnection, BIND_AUTO_CREATE);

        //registerReceiver(broadcastReceiver, new IntentFilter(BrightnessService.ACTION));

        Intent intent = getIntent();
        videoID = intent.getStringExtra("VIDEO_ID");

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(mBound){
            mService.unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onStop(){
        super.onStop();
    }

    private Runnable tick = new Runnable() {
        public void run() {
            handleFrame();
            handler.postDelayed(this, 1000/frequency);
        }
    };

    private void handleFrame(){


        if(ytplayer!=null && ytplayer.isPlaying()!=true){
            return;
        }
        else if(useDimming==0 && initialized == 0){
            int length = (int) (ytplayer.getDurationMillis()/1000);
            intent = new Intent(this, BrightnessService.class);
            intent.putExtra("dmtype", "initialize");
            intent.putExtra("length", length);
            intent.putExtra("frequency", frequency);
            intent.putExtra("id", videoID);
            intent.putExtra("name", "name");
            startService(intent);//starts  service
            initialized = 1;
        }
        else if(useDimming==0){
            int duration = ytplayer.getDurationMillis();
            int time = ytplayer.getCurrentTimeMillis();
            if(time==0){
                return;
            }
            else {
                time = (int) (time/1000);

                View view = (View)findViewById(R.id.view);
                view.setDrawingCacheEnabled(true);
                Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
                view.setDrawingCacheEnabled(false);

                intent = new Intent(this, BrightnessService.class);
                intent.putExtra("dmtype", "frame");
                intent.putExtra("time", time);
                intent.putExtra("duration", duration);
                intent.putExtra("bitmap", bitmap);
                intent.putExtra("name", "name");

                int bright = mService.handleIntent(intent);
                changeBrightness(bright, "From Live");
            }
        }
        else if(useDimming==1){
            int time = ytplayer.getCurrentTimeMillis();
            if(time==0){
                return;
            }
            time = (int) (time/1000);
            changeBrightness(mService.getBrightness(time), "From Dimming");
        }
    }

    private void changeBrightness(int brightness, String extra) {
        TextView text = (TextView) findViewById(R.id.textView);
        text.setText("Brightness: "+(brightness)+" "+extra);
        // WindowManager.LayoutParams lp = getWindow().getAttributes();
        // lp.screenBrightness =  (int) (brightness*100) / 100.0f;
        //getWindow().setAttributes(lp);
        // MyAsyncTask task = new MyAsyncTask();
        // task.setContext(this);
        // task.execute( (Integer) ((int) (brightness*100)));

        //android.provider.Settings.System.putInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS, (int) (brightness*100));
    }




    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player,
                                        boolean wasRestored) {
        if (!wasRestored) {
            player.cueVideo(videoID);
        }
        ytplayer = player;

        //check for dimming scheme
        if(mService.findDimmingScheme(name, id)) {
            //if dimming scheme exists
            useDimming = 1;
            frequency = mService.getFrequency();
            handler.removeCallbacks(tick);
            handler.postDelayed(tick, 1000 / frequency);
        }
        else {
            //if no dimming scheme exists
            useDimming = 0;
        }


        // Setup class to download the video & start its worker thread
        YoutubeDownloader dl = new YoutubeDownloader(videoID);
        dl.downloadVideo();

    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult result) {
        Toast.makeText(this, "Failed to Initialize!", Toast.LENGTH_LONG).show();
    }
    /*
    @Override
    protected YouTubePlayer.Provider getYouTubePlayerProvider() {
        return (YouTubePlayerView) findViewById(R.id.youtube_player);
    }*/

    public String getVideoID() {
        return this.videoID;
    }

    /*
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.hasExtra("Brightness")) {
                double brightness = (double) (Double.parseDouble(intent.getStringExtra("Brightness")));
                changeBrightness(20*brightness);
            }
        }
    };
    private void updateUI(Intent intent) {
        String brightness = intent.getStringExtra("brightness");
        changeBrightness(20*(double)(Double.parseDouble(brightness)));
    }
    @Override
    protected YouTubePlayer.Provider getYouTubePlayerProvider() {
        return (YouTubePlayerView) findViewById(R.id.youtube_player);
    }*/
}
