package com.android.audiorecorder.dao;

import java.util.List;

import com.android.audiorecorder.RecorderFile;

public interface IFileManager {
    
    public void createDiretory(String directory);
    public boolean createFile(String path);
    public boolean isExists(String path);
    public boolean removeFile(String path);

    public void insertRecorderFile(RecorderFile file);
    public List<RecorderFile> queryAllFileList(int page, int pageNumber);
    public List<RecorderFile> queryPublicFileList(int page, int pageNumber);
    public List<RecorderFile> queryPrivateFileList(int page, int pageNumber);
    public int getFileCount(int type);//-1 all
    
    public void delete(long id);
    public void updateUpLoadProgress(long progress, long id);
}
