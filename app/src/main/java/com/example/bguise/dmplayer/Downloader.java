package com.example.bguise.dmplayer;


import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.HashMap;

public class Downloader {
    private Context mContext;

    private static String TeamName="DMSliders";
    private static String VideoName="vn1";
    private static String VideoURL="vl1";
    private static String VideoLgh="vh1";
    private static String OptStepLgh="oh1";
    private int[] brightness_levels;
    private String returned_string;

    public Downloader(Context c, String vname, String vURL){
        mContext = c;
        VideoName = vname;
        VideoURL = vURL;
    }

    public void testDownload(String TeamName1,String VideoURL1){
        Handler myHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    String resStr=msg.getData().getString("jgstr").toString();
                    HashMap<String, String> resmap;
                    try{
                        resmap=tranStr(resStr);
                    }catch(Exception e){
                        return;
                    }
                    String OptPath=resmap.get("OptPath");
                    Log.d("Downloader", resStr);
                    if(OptPath!=null && OptPath.length()>1) {
                        Handler myHandler2 = new Handler() {
                            public void handleMessage(Message msg) {
                                if (msg.what == 1) {
                                    String resStr=msg.getData().getString("jgstr").toString();
                                    returned_string = resStr;
                                    Log.d("Downloader", returned_string);
                                }
                            }
                        };
                        String url="http://"+getRemoteData_WebService.serverIP+OptPath.substring(1);
                        new getRemoteData_WebService(mContext, url, myHandler2);
                    }
                }
            }
        };
        try {
            new getRemoteData_WebService(mContext, "downloadSchemedb", new String[] {
                    "TeamName","VideoURL" }, new String[] {TeamName,VideoURL1},
                    myHandler);

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    private HashMap<String, String> tranStr(String ss){
        HashMap<String, String> res=new HashMap<String, String>();
        String[] sg=ss.split("><");

        for (int i = 0; i < sg.length; i++) {
            String fieldname=sg[i].substring(0, sg[i].indexOf(">"));
            if(fieldname.substring(0, 1).equalsIgnoreCase("<"))fieldname=fieldname.substring(1);
            String fieldvalue=sg[i].substring(sg[i].indexOf(">")+1, sg[i].indexOf("</"));
            if(fieldname!=null && fieldname.length()>0 && fieldvalue!=null && fieldvalue.length()>0)
            {
                res.put(fieldname, fieldvalue);
            }
        }
        return res;
    }

}