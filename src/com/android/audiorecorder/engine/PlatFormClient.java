/**   
 * Copyright © 2016 浙江大华. All rights reserved.
 * 
 * @title: PlatFormClient.java
 * @description: TODO
 * @author: 23536   
 * @date: 2016年4月13日 下午3:02:47 
 */
package com.android.audiorecorder.engine;

import java.util.List;

import com.android.media.PlatFormManager;
import com.android.media.image.tiango.TiangoImageListResp;
import com.android.media.image.tiango.TiangoImageThumbListResp;
import com.android.media.image.tiango.TiangoImageType;

/** 
 * @description: TODO
 * @author: 23536
 * @date: 2016年4月13日 下午3:02:47  
 */
public enum PlatFormClient {
    
    instance;//单实例模式
    
    private PlatFormManager mPlatFormManager;
    
    private PlatFormClient(){
        mPlatFormManager = new PlatFormManager(null);
    }
    
    public List<TiangoImageType> getTypeList(){
        return mPlatFormManager.getTypeList();
    }
    
    public List<TiangoImageThumbListResp> getThumbList(String json){
        return mPlatFormManager.getThumbList(json);
    }
    
    public List<TiangoImageListResp> getFileList(String json){
        return mPlatFormManager.getImageList(json);
    }
}
