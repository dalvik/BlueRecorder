package com.android.audiorecorder.dao;

import android.content.Context;

public class FileManagerFactory {


    private static IFileManager mFileManager;
    
    private static MediaFileManager mMediaFileManager;
    
    public static IFileManager getFileManagerInstance(Context context){
        if(mFileManager == null) {
            mFileManager = new FileManagerImp(context);
        }
        return mFileManager;
    }
    
    
    public static MediaFileManager getMediaFileManagerInstance(Context context){
        if(mMediaFileManager == null) {
            mMediaFileManager = new MediaFileManagerImp(context);
        }
        return mMediaFileManager;
    }
}
