package com.example.bguise.dmplayer;


import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
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
    private SeekBar brightnessControl = null;
    private int brightnessLevel = 0;

    // Added for seekBar
    int seek_step = 1;
    int seek_max = 100;
    int seek_min = 1;


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


        brightnessControl = (SeekBar)findViewById(R.id.seekBar);
        // Ex :
        // If you want values from 3 to 5 with a step of 0.1 (3, 3.1, 3.2, ..., 5)
        // this means that you have 21 possible values in the seekbar.
        // So the range of the seek bar will be [0 ; (5-3)/0.1 = 20].
        brightnessControl.setMax((seek_max - seek_min) / seek_step);

        brightnessControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //int progressChanged = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                brightnessLevel = progress;
                Log.d("PlayActivity", "Seekbar showing: " + brightnessLevel);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(PlayActivity.this,"seek bar progress:"+progressChanged,
                //        Toast.LENGTH_SHORT).show();
            }
        });



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
        // Sets up brightness service so it can continue running & produce values
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
        // Feeds new brightness levels to produce the dimming scheme (making the scheme)
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

                //int bright = mService.handleIntent(intent);
                // TODO: Change this back to line above this TODO
                intent.putExtra("brightness", this.brightnessLevel);
                int bright = mService.setBrightnessAtTime(intent);
                // End of demo code

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
        // TODO: Uncomment- this actually changes brightness
        // WindowManager.LayoutParams lp = getWindow().getAttributes();
        // lp.screenBrightness =  (int) (brightness*100) / 100.0f;
        //getWindow().setAttributes(lp);

        android.provider.Settings.System.putInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS, (int) (brightness*100));
    }




    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player,
                                        boolean wasRestored) {
        if (!wasRestored) {
            player.cueVideo(videoID);
        }
        ytplayer = player;

        //check for dimming scheme
        if(mService.findDimmingScheme(name, videoID)) {
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
