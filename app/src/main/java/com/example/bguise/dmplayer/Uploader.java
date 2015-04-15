package com.example.bguise.dmplayer;


import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import org.kobjects.base64.Base64;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;

public class Uploader {
    private Context mContext;

    private static String TeamName="DMSliders";
    private static String VideoName="vn1";
    private static String VideoURL="vl1";
    private static String VideoLgh="vh1";
    private static String OptStepLgh="oh1";
    private static String OptDimLv="dm1";
    private int[] brightness_levels;

    public Uploader(Context c, String vname, String vURL, int len, int freq, int[] brightnesses){
        mContext = c;
        VideoName = vname;
        VideoURL = vURL;
        VideoLgh = ""+len;
        OptStepLgh = ""+freq;
        brightness_levels = brightnesses;
    }

    public void testUpload(String fileName){
        try{
            FileInputStream fis = new FileInputStream(getRemoteData_WebService.files_PATH+fileName);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int count = 0;
            while((count = fis.read(buffer)) >= 0){
                baos.write(buffer, 0, count);
            }
            String uploadBuffer;  //Base64 encode
            uploadBuffer = new String(Base64.encode(baos.toByteArray()));
            uploadfile(uploadBuffer);
            fis.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    private void uploadfile(String fileBytes)
    {
        Handler myHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    String resStr = msg.getData().getString("jgstr").toString();
                    Log.d("Uploader", resStr);
                }
            }
        };
        try {
            new getRemoteData_WebService(mContext, "UploadSchemedb", new String[] {
                    "TeamName","VideoName" ,"VideoURL","VideoLgh","OptStepLgh","OptDimLv","fileBase64Datas"},
                    new String[] {TeamName,VideoName,VideoURL,VideoLgh,OptStepLgh,OptDimLv,fileBytes},
                    myHandler);
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }
}