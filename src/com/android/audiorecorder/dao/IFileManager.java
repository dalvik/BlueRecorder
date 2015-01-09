package com.android.audiorecorder.dao;

import java.util.List;

import com.android.audiorecorder.RecorderFile;

public interface IFileManager {
    
    public void createDiretory(String directory);
    public boolean createFile(String path);
    public boolean isExists(String path);
    public void removeFile(String path);

    public void insertRecorderFile(RecorderFile file);
    public List<RecorderFile> queryPublicFileList(int page, int pageNumber);
    public List<RecorderFile> queryPrivateFileList(int page, int pageNumber);

    public void delete(long id);
    public void updateUpLoadProgress(int progress, long id);
}
