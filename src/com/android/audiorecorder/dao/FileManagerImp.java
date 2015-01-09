package com.android.audiorecorder.dao;

import java.io.File;
import java.io.IOException;
import java.util.List;

import android.content.Context;

import com.android.audiorecorder.RecorderFile;

public class FileManagerImp implements IFileManager {

    private DBHelper dbHelper;
    
    public FileManagerImp(Context context) {
        dbHelper = new DBHelper(context);
    }
    
    @Override
    public void createDiretory(String directory) {
        File file = new File(directory);
        if(!file.exists()){
            file.mkdirs();
        }
    }
    
    @Override
    public boolean isExists(String path) {
        File file = new File(path);
        return file.exists();
    }
    
    @Override
    public boolean createFile(String path) {
        File file = new File(path);
        if(!file.exists()){
            try {
                return file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    
    @Override
    public void removeFile(String path) {
        File file = new File(path);
        if(file.exists()){
            file.delete();
        }
    }
    
    @Override
    public void delete(long id) {
        dbHelper.delete(id);
    }
    
    @Override
    public void insertRecorderFile(RecorderFile file) {
        dbHelper.insertRecorderFile(file);
    }
    
    @Override
    public List<RecorderFile> queryPrivateFileList(int page, int pageNumber) {
        return dbHelper.queryPrivateFileList(page, pageNumber);
    }
    
    @Override
    public List<RecorderFile> queryPublicFileList(int page, int pageNumber) {
        return dbHelper.queryPublicFileList(page, pageNumber);
    }
    
    @Override
    public void updateUpLoadProgress(int progress, long id) {
        dbHelper.updateUpLoadProgress(progress, id);
    }

}
