package com.example.bguise.dmplayer;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Environment;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by ben on 4/15/15.
 * This class was created to help demonstrate the project goals for ECE 1161, Spring 2015.
 * This code is NOT intended to be used for copyright infringement!
 * Based on code from the following webpage:
 * http://stackoverflow.com/questions/7203047/code-for-download-video-from-youtube-on-java-android
 */
public class YoutubeDownloader {

    private static String youtubeID;
    private static final String youtubeAddr = "https://www.youtube.com/watch?v=";
    private static final String completeURL = youtubeAddr + youtubeID;

    YoutubeDownloader(String videoid) {
        this.youtubeID = videoid;
    }

    public static void downloadVideo() {

        // Create new thread to handle the HTTP request, as Android will not allow this
        Thread thread = new Thread(new Runnable() {

            InputStream is;

            @Override
            public void run() {

                try {
                    URL u = new URL(completeURL);

                    is = u.openStream();
                    HttpURLConnection huc = (HttpURLConnection) u.openConnection(); //to know the size of video
                    int size = huc.getContentLength();

                    if (huc != null) {
                        String fileName = "FILE.mp4";
                        String storagePath = Environment.getExternalStorageDirectory().toString();
                        File f = new File(storagePath, fileName);

                        FileOutputStream fos = new FileOutputStream(f);
                        byte[] buffer = new byte[1024];
                        int len1 = 0;
                        if (is != null) {
                            while ((len1 = is.read(buffer)) > 0) {
                                fos.write(buffer, 0, len1);
                            }
                        }
                        if (fos != null) {
                            fos.close();


                            /*
                            Attempt to extract Bitmap from downloaded file
                            */

                            MediaMetadataRetriever bitmapGrabber = new MediaMetadataRetriever();

                            //String fileName = "FILE.mp4";
                            //String storagePath = Environment.getExternalStorageDirectory().toString();
                            File movie = new File(storagePath, fileName);

                            FileInputStream movieStream;
                            FileDescriptor fd;
                            try {
                                movieStream = new FileInputStream(movie);
                                fd = movieStream.getFD();

                                bitmapGrabber.setDataSource(fd);
                                if (movieStream != null)
                                    movieStream.close();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException io) {
                                io.printStackTrace();
                            }


                            Bitmap frame = bitmapGrabber.getFrameAtTime(2000000);
                            FileOutputStream outFile = null;
                            try {
                                outFile = new FileOutputStream("Frame.png");
                                frame.compress(Bitmap.CompressFormat.PNG, 100, outFile);
                            } catch (FileNotFoundException fnof) {
                                fnof.printStackTrace();
                            } finally {
                                try {
                                    if (outFile != null) {
                                        outFile.close();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }


                        }
                    }
                } catch (MalformedURLException mue) {
                    mue.printStackTrace();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                    } catch (IOException ioe) {
                        // just going to ignore this one
                    }
                }



            }
        });

        thread.start();
    }
}
