package com.android.audiorecorder.provider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.android.audiorecorder.utils.FileUtils;
import com.android.audiorecorder.utils.StringUtil;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

public class MediaFileProducer {

    private Context mContext;
    
    private String TAG = "MediaFileProducer";
    
    public MediaFileProducer(Context context){
        this.mContext = context;
    }
    
    public void loadExistsMediaFiles(){
        String PREFIX = Environment.getExternalStorageDirectory().getPath();
        String completeDirectory = PREFIX + File.separator + FileProviderService.ROOT + File.separator;
        File parentDirectory = new File(completeDirectory);
        List<String> filePaths = new ArrayList<String>();
        String oldDirectory = PREFIX + File.separator + FileProviderService.ROOT_OLD + File.separator;
        File oldParentDirectory = new File(oldDirectory);
        if(oldParentDirectory.exists()){
            putFilePathToList(filePaths, oldParentDirectory);
        }
        if(parentDirectory.exists()){
            putFilePathToList(filePaths, parentDirectory);
        }
        insertFileListDetail(filePaths);
    }
    
    public void putFilePathToList(List<String> filePaths, File file){
        if(file.isDirectory()){
            File[] files = file.listFiles();
            for(File f:files){
                if(!f.getParent().contains(FileProviderService.THUMBNAIL) && !f.getName().contains(".nomedia")){
                    putFilePathToList(filePaths, f);
                }
            }
        } else {
            filePaths.add(file.getPath());
        }
    }
    
    public void insertFileListDetail(List<String> filePaths){
        if(filePaths.size()>0){
            Log.d(TAG, "reloadMediaFile number = " + filePaths.size());
            mContext.getContentResolver().bulkInsert(FileProvider.JPEGS_URI, generalFileDetails(filePaths));
        } else {
            Log.w(TAG, "reloadMediaFile none." );
        }
    }
    
    
    private ContentValues[] generalFileDetails(List<String> filePaths){
        int length = filePaths.size();
        ContentValues[] valueArray = new ContentValues[length];
        for (int i=0; i<length; i++) {
            ContentValues values = new ContentValues();
            //putContentValues(filePaths.get(i), values);
            putContentValuesDefault(filePaths.get(i), values);
            valueArray[i] = values;
        }
        return valueArray;
    }
    
    private void putContentValuesDefault(String path, ContentValues values){
        FileDetail detail = new FileDetail(path);
        values.put(FileColumn.COLUMN_LOCAL_PATH, path);
        values.put(FileColumn.COLUMN_FILE_TYPE, detail.getFileType());
        values.put(FileColumn.COLUMN_MIME_TYPE, detail.getMimeType());
        values.put(FileColumn.COLUMN_FILE_SIZE, detail.getLength());
        if(path.contains(FileProviderService.CATE_DOWNLOAD)){
            values.put(FileColumn.COLUMN_UP_OR_DOWN, 1);
            values.put(FileColumn.COLUMN_UP_DOWN_LOAD_STATUS, 2);
        }
        values.put(FileColumn.COLUMN_THUMB_NAME, StringUtil.getYearMonthWeek(detail.getLastModifyTime()));
        values.put(FileColumn.COLUMN_FILE_DURATION, detail.getDuration());
        values.put(FileColumn.COLUMN_DOWN_LOAD_TIME, detail.getLastModifyTime());
        values.put(FileColumn.COLUMN_UP_LOAD_TIME, detail.getLastModifyTime());
        values.put(FileColumn.COLUMN_FILE_RESOLUTION_X, detail.getFileResolutionX());
        values.put(FileColumn.COLUMN_FILE_RESOLUTION_Y, detail.getFileResolutionY());
        values.put(FileColumn.COLUMN_FILE_THUMBNAIL, detail.getThumbnailPath());
        values.put(FileColumn.COLUMN_LAUNCH_MODE, 2);
    }
    
    public void updateFileDetail(int arg1, String path, int id){
        ContentValues values = new ContentValues();
        values.put(FileColumn.COLUMN_UP_DOWN_LOAD_STATUS, FileColumn.STATE_FILE_UP_DOWN_SUCCESS);
        if(arg1 == 1){//download
            putContentValues(path, values);
        } else if(arg1 == 2){//upload
        }
        mContext.getContentResolver().update(FileProvider.TASK_URI, values, FileColumn.COLUMN_ID + "=" + id, null);
    }
    
    private void putContentValues(String path, ContentValues values){
        String[] projection = { MediaStore.Files.FileColumns.MEDIA_TYPE, MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.SIZE, 
                MediaStore.Audio.Media.DURATION, MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.Audio.Media.WIDTH, MediaStore.Audio.Media.HEIGHT, MediaStore.Files.FileColumns._ID};
        String where = MediaStore.Files.FileColumns.DATA + " like '%" + path + "'";
        Uri uri = MediaStore.Files.getContentUri("external");
        Cursor cursor = mContext.getContentResolver().query(uri, projection, where, null, null);
        values.put(FileColumn.COLUMN_LOCAL_PATH, path);
        if(cursor != null && cursor.getCount()>0){
            if(cursor.moveToNext()){
                int index = 0;
                int mediaType = cursor.getInt(index++);
                if(mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_NONE){
                    mediaType = FileProvider.FILE_TYPE_OTHER;
                } else if(mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE){
                    mediaType = FileProvider.FILE_TYPE_JEPG;
                } else if(mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO){
                    mediaType = FileProvider.FILE_TYPE_AUDIO;
                } else if(mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO){
                    mediaType = FileProvider.FILE_TYPE_VIDEO;
                } else if(mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_PLAYLIST){
                    mediaType = FileProvider.FILE_TYPE_AUDIO;
                }
                values.put(FileColumn.COLUMN_FILE_TYPE, mediaType);
                values.put(FileColumn.COLUMN_MIME_TYPE, cursor.getString(index++));
                values.put(FileColumn.COLUMN_FILE_SIZE, cursor.getInt(index++));
                values.put(FileColumn.COLUMN_FILE_DURATION, cursor.getInt(index++));
                long createTime = cursor.getInt(index++) * 1000;
                values.put(FileColumn.COLUMN_DOWN_LOAD_TIME, createTime);
                values.put(FileColumn.COLUMN_THUMB_NAME, StringUtil.getYearMonthWeek(createTime));
                values.put(FileColumn.COLUMN_FILE_RESOLUTION_X, cursor.getInt(index++));
                values.put(FileColumn.COLUMN_FILE_RESOLUTION_Y, cursor.getInt(index++));
                
                if(mediaType == FileProvider.FILE_TYPE_VIDEO){
                    int id = cursor.getInt(index++);
                    String selection = MediaStore.Video.Thumbnails.VIDEO_ID +"=?";
                    String[] selectionArgs = new String[]{String.valueOf(id)};
                    Cursor thumbCursor = mContext.getContentResolver().query(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Video.Thumbnails.DATA}, selection, selectionArgs, null);
                    if(thumbCursor != null){
                        if(thumbCursor.moveToNext()){
                            values.put(FileColumn.COLUMN_FILE_THUMBNAIL, thumbCursor.getString(0));
                        }
                        thumbCursor.close();
                    }
                }
                if(path.contains(FileProviderService.CATE_DOWNLOAD)){
                    values.put(FileColumn.COLUMN_UP_LOAD_TIME, createTime);
                    values.put(FileColumn.COLUMN_UP_OR_DOWN, 1);
                    values.put(FileColumn.COLUMN_UP_DOWN_LOAD_STATUS, 2);
                }
                values.put(FileColumn.COLUMN_LAUNCH_MODE, 2);
            }
            cursor.close();
        } else {
            putContentValuesDefault(path, values);
        }
    }
    
    public void deleteFiles(String[] fileList){
        if(fileList != null && fileList.length>0){
            for(String name:fileList){
                File file = new File(name);
                if(file.delete()){
                    Log.i(TAG, "---> delete " + name);
                }
                FileUtils.deleteEmptyFolder(file.getParent());
            }
        }
    }
    
    
    public void cleanMediaFile(){
        mContext.getContentResolver().delete(FileProvider.JPEGS_URI, null, null);
    }
}

