package com.android.audiorecorder.dao;

import java.util.List;

import android.content.Context;

import com.android.audiorecorder.RecorderFile;

public class FileManagerImp extends MediaFileManager {

    private DBHelper dbHelper;
    
    public FileManagerImp(Context context) {
        dbHelper = new DBHelper(context);
    }
    
    @Override
    public void delete(int mimeType, long id) {
        dbHelper.delete(id);
    }
    
    @Override
    public int getFileCount(int mimeType, int type) {
        return dbHelper.getCount(type);
    }
    
    @Override
    public void insertRecorderFile(RecorderFile file) {
        dbHelper.insertRecorderFile(file);
    }
    
    @Override
    public List<RecorderFile> queryAllFileList(int mimeType, int page, int pageNumber) {
        return dbHelper.queryAllFileList(page, pageNumber);
    }
    
    @Override
    public List<RecorderFile> queryPrivateFileList(int mimeType, int page, int pageNumber) {
        return dbHelper.queryPrivateFileList(page, pageNumber);
    }
    
    @Override
    public List<RecorderFile> queryPublicFileList(int mimeType, int page, int pageNumber) {
        return dbHelper.queryPublicFileList(page, pageNumber);
    }
    
    @Override
    public void updateUpLoadProgress(int mimeType, long progress, long id) {
        dbHelper.updateUpLoadProgress(progress, id);
    }

}
