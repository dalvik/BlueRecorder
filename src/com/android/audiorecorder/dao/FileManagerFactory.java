package com.android.audiorecorder.dao;

import android.content.Context;

public class FileManagerFactory {


    private static IFileManager fileManager;
    
    public static IFileManager getSmsManagerInstance(Context context){
        if(fileManager == null) {
            fileManager = new FileManagerImp(context);
        }
        return fileManager;
    }
}
