package com.example.bguise.dmplayer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.bguise.developerkey.DeveloperKey;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;



public class PlayActivity extends YouTubeBaseActivity implements YouTubePlayer.OnInitializedListener {

    private String videoID;
    YouTubePlayerView youTubeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        youTubeView = (YouTubePlayerView)findViewById(R.id.youtube_player);
        youTubeView.initialize(DeveloperKey.DEVELOPER_KEY, this);

        Intent intent = getIntent();
        videoID = intent.getStringExtra("VIDEO_ID");

    }


    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player,
                                        boolean wasRestored) {
        if (!wasRestored) {
            player.cueVideo(videoID);
        }
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
}
