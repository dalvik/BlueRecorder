package com.android.audiorecorder.dao;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.android.audiorecorder.DebugConfig;
import com.android.audiorecorder.RecorderFile;
import com.android.audiorecorder.engine.MultiMediaService;
import com.android.audiorecorder.engine.MediaProvider;
import com.android.audiorecorder.provider.FileDetail;

public class MediaFileManagerImp extends MediaFileManager implements IFileManager {

    private Context mContext;
    
    private String TAG = "MediaFileManager";
    
    public MediaFileManagerImp(Context context){
        this.mContext = context;
    }
    
    public void createDiretory(String directory) {
        File file = new File(directory);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public boolean createFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            try {
                return file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean isExists(String path) {
        File file = new File(path);
        return file.exists();
    }

    public boolean removeFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            return file.delete();
        }
        return true;
    }
    
    @Override
    public void insertRecorderFile(RecorderFile file) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaProvider.FILE_COLUMN_PATH, file.getPath());
        contentValues.put(MediaProvider.FILE_COLUMN_LENGTH, file.getSize());
        contentValues.put(MediaProvider.FILE_COLUMN_DURATION, file.getDuration());
        contentValues.put(MediaProvider.FILE_COLUMN_MIME_TYPE, file.getMimeType());
        contentValues.put(MediaProvider.FILE_COLUMN_LAUNCH_TYPE, file.getLaunchType());
        contentValues.put(MediaProvider.FILE_COLUMN_TIME, file.getTime());
        contentValues.put(MediaProvider.FILE_COLUMN_PROGRESS, 0);
        contentValues.put(MediaProvider.FILE_COLUMN_BACKUP, 0);
        contentValues.put(MediaProvider.FILE_COLUMN_MEDIA_TYPE, file.getMediaType());
        contentValues.put(MediaProvider.FILE_COLUMN_WIDTH, file.getWidth());
        contentValues.put(MediaProvider.FILE_COLUMN_HEIGHT, file.getHeight());
        contentValues.put(MediaProvider.FILE_COLUMN_SUMMARY, file.getSummary());
        Uri uri = getMediaUri(file.getMediaType());
        if(DebugConfig.DEBUG){
            Log.i(TAG, "---> uri = " + uri + " file = " + file.toString());
        }
        mContext.getContentResolver().insert(uri, contentValues);
    }

    @Override
    public List<RecorderFile> queryAllFileList(int mimeType, int page, int pageNumber) {
        Uri uri = getMediaUri(mimeType);
        List<RecorderFile> list = new ArrayList<RecorderFile>();
        String[] columns = {MediaProvider.BASE_COLUMN_ID, MediaProvider.FILE_COLUMN_PATH, MediaProvider.FILE_COLUMN_LENGTH, MediaProvider.FILE_COLUMN_DURATION, 
                MediaProvider.FILE_COLUMN_MIME_TYPE, MediaProvider.FILE_COLUMN_LAUNCH_TYPE, MediaProvider.FILE_COLUMN_TIME, 
                MediaProvider.FILE_COLUMN_PROGRESS, MediaProvider.FILE_COLUMN_BACKUP, MediaProvider.FILE_COLUMN_MEDIA_TYPE, MediaProvider.FILE_COLUMN_WIDTH, MediaProvider.FILE_COLUMN_HEIGHT, MediaProvider.FILE_COLUMN_SUMMARY};
        Cursor cursor = mContext.getContentResolver().query(uri, columns, null, null, MediaProvider.FILE_COLUMN_TIME +" desc limit " + (page * pageNumber) + "," + pageNumber);
        if(cursor != null) {
            cursor.moveToFirst();
            while(!cursor.isAfterLast()) {
                int index = 0;
                RecorderFile file = new RecorderFile();
                file.setId(cursor.getInt(index++));
                String path = cursor.getString(index++); 
                file.setPath(path);
                String name = path.substring(path.lastIndexOf("/")+1, path.lastIndexOf("."));
                file.setName(name);
                file.setSize(cursor.getInt(index++));
                file.setDuration(cursor.getInt(index++));
                file.setMimeType(cursor.getString(index++));
                file.setLaunchType(cursor.getInt(index++));
                file.setTime(cursor.getLong(index++));
                file.setProgress(cursor.getInt(index++));
                file.setMediaType(cursor.getInt(index++));
                file.setWidth(cursor.getInt(index++));
                file.setHeight(cursor.getInt(index++));
                file.setSummary(cursor.getString(index++));
                File f = new File(path);
                if(f.exists()){
                   list.add(file);
                }else{
                    delete(mimeType, file.getId());
                }
                cursor.moveToNext();
            }
            cursor.close();
        }
        return list;
    }

    @Override
    public List<RecorderFile> queryPublicFileList(int mimeType, int page, int pageNumber) {
        Uri uri = getMediaUri(mimeType);
        List<RecorderFile> list = new ArrayList<RecorderFile>();
        String[] columns = {MediaProvider.BASE_COLUMN_ID, MediaProvider.FILE_COLUMN_PATH, MediaProvider.FILE_COLUMN_LENGTH, MediaProvider.FILE_COLUMN_DURATION, 
                MediaProvider.FILE_COLUMN_MIME_TYPE, MediaProvider.FILE_COLUMN_LAUNCH_TYPE, MediaProvider.FILE_COLUMN_TIME, 
                MediaProvider.FILE_COLUMN_PROGRESS, MediaProvider.FILE_COLUMN_BACKUP, MediaProvider.FILE_COLUMN_MEDIA_TYPE, MediaProvider.FILE_COLUMN_WIDTH, MediaProvider.FILE_COLUMN_HEIGHT, MediaProvider.FILE_COLUMN_SUMMARY};
        Cursor cursor = mContext.getContentResolver().query(uri, columns, MediaProvider.FILE_COLUMN_LAUNCH_TYPE + " != " + MultiMediaService.LUNCH_MODE_AUTO, null, MediaProvider.FILE_COLUMN_TIME +" desc limit " + (page * pageNumber) + "," + pageNumber);
        if(cursor != null) {
            cursor.moveToFirst();
            while(!cursor.isAfterLast()) {
                int index = 0;
                RecorderFile file = new RecorderFile();
                file.setId(cursor.getInt(index++));
                String path = cursor.getString(index++); 
                file.setPath(path);
                String name = path.substring(path.lastIndexOf("/")+1, path.lastIndexOf("."));
                file.setName(name);
                file.setSize(cursor.getInt(index++));
                file.setDuration(cursor.getInt(index++));
                file.setMimeType(cursor.getString(index++));
                file.setLaunchType(cursor.getInt(index++));
                file.setTime(cursor.getLong(index++));
                file.setProgress(cursor.getInt(index++));
                file.setMediaType(cursor.getInt(index++));
                file.setWidth(cursor.getInt(index++));
                file.setHeight(cursor.getInt(index++));
                file.setSummary(cursor.getString(index++));
                File f = new File(path);
                if(f.exists()){
                   list.add(file);
                }else{
                    delete(mimeType, file.getId());
                }
                cursor.moveToNext();
            }
            cursor.close();
        }
        return list;
    }

    @Override
    public List<RecorderFile> queryPrivateFileList(int mimeType, int page, int pageNumber) {
        Uri uri = getMediaUri(mimeType);
        List<RecorderFile> list = new ArrayList<RecorderFile>();
        String[] columns = {MediaProvider.BASE_COLUMN_ID, MediaProvider.FILE_COLUMN_PATH, MediaProvider.FILE_COLUMN_LENGTH, MediaProvider.FILE_COLUMN_DURATION, 
                MediaProvider.FILE_COLUMN_MIME_TYPE, MediaProvider.FILE_COLUMN_LAUNCH_TYPE, MediaProvider.FILE_COLUMN_TIME, 
                MediaProvider.FILE_COLUMN_PROGRESS, MediaProvider.FILE_COLUMN_BACKUP, MediaProvider.FILE_COLUMN_MEDIA_TYPE, MediaProvider.FILE_COLUMN_WIDTH, MediaProvider.FILE_COLUMN_HEIGHT, MediaProvider.FILE_COLUMN_SUMMARY};
        Cursor cursor = mContext.getContentResolver().query(uri, columns, MediaProvider.FILE_COLUMN_LAUNCH_TYPE + " = " + MultiMediaService.LUNCH_MODE_AUTO, null, MediaProvider.FILE_COLUMN_TIME +" desc limit " + (page * pageNumber) + "," + pageNumber);
        if(cursor != null) {
            cursor.moveToFirst();
            while(!cursor.isAfterLast()) {
                int index = 0;
                RecorderFile file = new RecorderFile();
                file.setId(cursor.getInt(index++));
                String path = cursor.getString(index++); 
                file.setPath(path);
                String name = path.substring(path.lastIndexOf("/")+1, path.lastIndexOf("."));
                file.setName(name);
                file.setSize(cursor.getInt(index++));
                file.setDuration(cursor.getInt(index++));
                file.setMimeType(cursor.getString(index++));
                file.setLaunchType(cursor.getInt(index++));
                file.setTime(cursor.getLong(index++));
                file.setProgress(cursor.getInt(index++));
                file.setMediaType(cursor.getInt(index++));
                file.setWidth(cursor.getInt(index++));
                file.setHeight(cursor.getInt(index++));
                file.setSummary(cursor.getString(index++));
                File f = new File(path);
                if(f.exists()){
                   list.add(file);
                }else{
                    delete(mimeType, file.getId());
                }
                cursor.moveToNext();
            }
            cursor.close();
        }
        return list;
    }

    @Override
    public int getFileCount(int mimeType, int type) {
        String[] columns = {"count(*) as a_count"};
        int count = 0;
        Uri uri = getMediaUri(mimeType);
        Cursor cursor  = mContext.getContentResolver().query(uri, columns, MediaProvider.FILE_COLUMN_LAUNCH_TYPE +" = " + type, null, null);
        if(cursor != null){
            count = cursor.getCount();
            cursor.close();
        }
        return count;
    }

    @Override
    public long delete(int mimeType, long id) {
        Uri uri = getMediaUri(mimeType);
        return mContext.getContentResolver().delete(uri, MediaProvider.BASE_COLUMN_ID + " = " + id, null);
    }

    @Override
    public void updateUpLoadProgress(int mimeType, long progress, long id) {
        Uri uri = getMediaUri(mimeType);
        ContentValues values = new ContentValues();
        values.put(MediaProvider.FILE_COLUMN_PROGRESS, progress);
        mContext.getContentResolver().update(uri, values, MediaProvider.BASE_COLUMN_ID +" = " + id, null);
    }

    private Uri getMediaUri(int mimeType){
        Uri uri = null;
        if(mimeType == RecorderFile.MEDIA_TYPE_IMAGE){
            uri = Uri.parse(MediaProvider.IMAGE_Content_URI);
        } else if(mimeType == RecorderFile.MEDIA_TYPE_VIDEO){
            uri = Uri.parse(MediaProvider.VIDEO_Content_URI);
        } else if(mimeType == RecorderFile.MEDIA_TYPE_AUDIO){
            uri = Uri.parse(MediaProvider.AUDIO_Content_URI);
        } else {
            uri = Uri.parse(MediaProvider.ALL_Content_URI);
        }
        return uri;
    }

    @Override
    public long addTask(long id, boolean download) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<FileThumb> loadFileThumbList(boolean isLocal, int mediaType, int pageIndex, int pageNumber, Set<Integer> launchType) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public List<FileDetail> loadFileList(boolean isLocal, int mediaType,
            String thumbName, int pageIndex, int pageNumber, Set<Integer> launchType) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public int getFileThumbCount(int fileType, int type, Set<Integer> launchType) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public int getFileListCount(int fileType, String thumbName, Set<Integer> launchType) {
        // TODO Auto-generated method stub
        return 0;
    }
}
