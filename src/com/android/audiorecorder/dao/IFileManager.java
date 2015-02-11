package com.android.audiorecorder.dao;

import java.util.List;

import com.android.audiorecorder.RecorderFile;

public interface IFileManager{
    
    public void insertRecorderFile(RecorderFile file);
    public List<RecorderFile> queryAllFileList(int mimeType, int page, int pageNumber);
    public List<RecorderFile> queryPublicFileList(int mimeType, int page, int pageNumber);
    public List<RecorderFile> queryPrivateFileList(int mimeType, int page, int pageNumber);
    public int getFileCount(int mimeType, int type);//mimeType : image audio video  -1 all;  lucher type manly tel auto -a all 
    
    public void delete(int mimeType, long id);
    public void updateUpLoadProgress(int mimeType, long progress, long id);
}
