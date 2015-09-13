package com.android.audiorecorder.utils;


public class URLS {
    
    private static final java.lang.String HOST = "10.0.2.2";
    private static final java.lang.String HTTP = "http://";
    private static final java.lang.String HTTPS = "https://";
    private final static String URL_HOST = "www.drovik.com";
    private final static String URL_WWW_HOST = "www."+URL_HOST;
    private final static String URL_MY_HOST = "my."+URL_HOST;
    private final static String URL_SPLITTER = "/";
    private final static String URL_API_HOST = HTTP + HOST + URL_SPLITTER;
    public final static String THUMB_LIST = URL_API_HOST+"action/api/thumb_list.php";
    public final static String FILE_LIST = URL_API_HOST+"action/api/cloud_file_list.php";
    
    private URLS(){
        
    }
    
}
