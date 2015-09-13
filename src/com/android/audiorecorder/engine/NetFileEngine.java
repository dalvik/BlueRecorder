package com.android.audiorecorder.engine;

import java.util.List;

import com.android.audiorecorder.dao.Alumb;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.http.ResponseStream;
import com.lidroid.xutils.http.client.HttpRequest;

public class NetFileEngine {

    private HttpUtils mHttpUtils;
    
    public NetFileEngine(){
        mHttpUtils = new HttpUtils();
    }
    
    public List<Alumb> downLoadThumbList(String url, RequestParams param) throws HttpException{
        ResponseStream responseStream = mHttpUtils.sendSync(HttpRequest.HttpMethod.GET, url, param);
        return null;
    }
    
    public void downloadFile(){
        
    }
    
    public void uploadFile(){
        
    }
}
