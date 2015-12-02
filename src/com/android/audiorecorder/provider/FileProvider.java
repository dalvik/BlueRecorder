
package com.android.audiorecorder.provider;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class FileProvider extends ContentProvider {

    //download /storage/sdcard0/MediaFileManager/DownLoad/YYYY/MONTH/WEEK/*.jpg
    //upload /storage/sdcard0/MediaFileManager/UpLoad/YYYY/MONTH/WEEK/file_name.jpeg
    //record /storage/sdcard0/MediaFileManager/Record/JPG/YYYY/MONTH/WEEK/file_name.jpg
    //record /storage/sdcard0/MediaFileManager/Record/AUDIO/YYYY/MONTH/WEEK/file_name.wav
    //record /storage/sdcard0/MediaFileManager/Record/VIDEO/YYYY/MONTH/WEEK/file_name.wmv
    
    public final static String ACTION_PROVIDER_ONCREATE = "android.intent.action.PROVIDER_ONCREATE";
    public final static int FILE_TYPE_JEPG = 0;
    public final static int FILE_TYPE_AUDIO = 1;
    public final static int FILE_TYPE_VIDEO = 2;
    public final static int FILE_TYPE_TEXT = 3;
    public final static int FILE_TYPE_APK = 4;
    public final static int FILE_TYPE_ZIP = 5;
    public final static int FILE_TYPE_OTHER = 6;
    
    protected static final String DB_TABLE_FILES = "files";
    protected static final String DB_TABLE_TASKS = "down_up_load_tasks";//up or download tasks
    protected static final String DB_TABLE_SETTINGS = "settings";
    
    protected static final String TABLE_JPEG_FILES = "jpeg";
    protected static final String TABLE_AUDIO_FILES = "audio";
    protected static final String TABLE_VIDEO_FILES = "video";
    public static final String TABLE_DELETE_FILES = "deleted";

    private static final int TASK = 6;
    
    private static final int ALL_FILE_INFO = 7;
    
    private static final int SETTINGS = 10;
    
    private static final int JPEG_FILES = 15;
    private static final int AUDIO_FILES = 16;
    private static final int VIDEO_FILES = 17;
    private static final int DELETE_FILES = 18;
    
    private final static String authority = "com.android.audiorecorder.provider.FileProvider";
    
    public static final Uri ALL_URI = Uri.parse("content://" + authority + "/all_file_info");
    public static final Uri TASK_URI = Uri.parse("content://" + authority + "/tasks");
    public static final Uri SETTINGS_URI = Uri.parse("content://" + authority + "/settings");
    
    public static final Uri JPEGS_URI = Uri.parse("content://" + authority + "/jpeg");
    public static final Uri AUDIOS_URI = Uri.parse("content://" + authority + "/audio");
    public static final Uri VIDEOS_URI = Uri.parse("content://" + authority + "/video");
    
    public static final Uri DELETE_URI = Uri.parse("content://" + authority + "/deleted");
    
    private DatabaseHelper mDatabaseHelper;

    /** URI matcher used to recognize URIs sent by applications */
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private static final String TAG = "FileProvider";

    static {
        sURIMatcher.addURI(authority, "tasks", TASK);
        sURIMatcher.addURI(authority, "all_file_info", ALL_FILE_INFO);
        sURIMatcher.addURI(authority, "settings", SETTINGS);
        
        sURIMatcher.addURI(authority, "jpeg", JPEG_FILES);
        sURIMatcher.addURI(authority, "audio", AUDIO_FILES);
        sURIMatcher.addURI(authority, "video", VIDEO_FILES);
        sURIMatcher.addURI(authority, "deleted", DELETE_FILES);
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new DatabaseHelper(getContext());
        Log.i(TAG, "===> FileProvider onCreate.");
        return true;
    }
    
    @Override
    public String getType(Uri uri) {
        int match = sURIMatcher.match(uri);
        /*switch (match) {
            case DOWNLOADS_ID:
            case UPLOAD_ID:
                return "vnd.android.cursor.item";
            case DOWNLOAD:
            case UPLOAD:
                return "vnd.android.cursor.dir";
        }*/
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        int type = sURIMatcher.match(uri);
        long rowid = 0;
        Uri newUri = null;
        switch(type){
            case JPEG_FILES:
                rowid = db.insert(DB_TABLE_FILES, null, values);
                if (rowid <= 0) {
                    Log.d(TAG, "couldn't insert into jpeg files database. " + uri);
                    return null;
                }
                newUri = ContentUris.withAppendedId(uri, rowid);
                getContext().getContentResolver().notifyChange(newUri, null);
                break;
            case VIDEO_FILES:
                rowid = db.insert(DB_TABLE_FILES, null, values);
                if (rowid <= 0) {
                    Log.d(TAG, "couldn't insert into video files database. " + uri);
                    return null;
                }
                newUri = ContentUris.withAppendedId(uri, rowid);
                getContext().getContentResolver().notifyChange(newUri, null);
                break;
            case AUDIO_FILES:
                rowid = db.insert(DB_TABLE_FILES, null, values);
                if (rowid <= 0) {
                    Log.d(TAG, "couldn't insert into audio files database. " + uri);
                    return null;
                }
                newUri = ContentUris.withAppendedId(uri, rowid);
                getContext().getContentResolver().notifyChange(newUri, null);
                break;
            case TASK:
                int id = 0;
                if(values.containsKey(FileColumn.COLUMN_ID)){
                    id = values.getAsInteger(FileColumn.COLUMN_ID);
                }
                String where = FileColumn.COLUMN_ID + " = " + id;
                rowid =  db.update(DB_TABLE_FILES, values, where, null);
                if (rowid <= 0) {
                    Log.d(TAG, "id = " + id + " couldn't insert into updownloads task database. " + uri);
                    return null;
                }
                newUri = ContentUris.withAppendedId(uri, rowid);
                getContext().getContentResolver().notifyChange(newUri, null);
                break;
                default:
                    Log.d(TAG, "calling insert on an unknown/invalid URI: " + uri);
                    throw new IllegalArgumentException("Unknown/Invalid URI " + uri);
                
        }
        return newUri;
    }
    
    @Override
    public Cursor query(Uri uri, String[] projectionIn, String selection,
            String[] selectionArgs, String sort) {
        int type = sURIMatcher.match(uri);
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        List<String> prependArgs = new ArrayList<String>();
        if (uri.getQueryParameter("distinct") != null) {
            qb.setDistinct(true);
        }
        switch (type) {
            case TASK:
                qb.setTables(DB_TABLE_TASKS);
                break;
            case ALL_FILE_INFO:
                qb.setTables(DB_TABLE_FILES);
                break;
            case SETTINGS:
                qb.setTables(DB_TABLE_SETTINGS);
                break;
            case JPEG_FILES:
                qb.setTables(TABLE_JPEG_FILES);
                break;
            case AUDIO_FILES:
                qb.setTables(TABLE_AUDIO_FILES);
                break;
            case VIDEO_FILES:
                qb.setTables(TABLE_VIDEO_FILES);
                break;
            default:
                break;
        }
        Cursor c = qb.query(db, projectionIn, selection,
                combine(prependArgs, selectionArgs), null, null, sort, null);

        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        int count = 0;
        int match = sURIMatcher.match(uri);
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        boolean notification = false;
        switch (match) {
            case JPEG_FILES:
                count = db.update(DB_TABLE_FILES, values, selection, selectionArgs);
                if (count <= 0) {
                    Log.d(TAG, "couldn't update jpeg in files database. " + uri);
                    return count;
                }
                notification = true;
                break;
            case VIDEO_FILES:
                count = db.update(DB_TABLE_FILES, values, selection, selectionArgs);
                if (count <= 0) {
                    Log.d(TAG, "couldn't update video in  files database. " + uri);
                    return count;
                }
                notification = true;
                break;
            case AUDIO_FILES:
                count = db.update(DB_TABLE_FILES, values, selection, selectionArgs);
                if (count <= 0) {
                    Log.d(TAG, "couldn't update audio in  files database");
                    return count;
                }
                notification = true;
                break;
			case SETTINGS:
                count = db.update(DB_TABLE_SETTINGS, values, selection, selectionArgs);
                break;
            case TASK:
                count = db.update(DB_TABLE_FILES, values, selection, selectionArgs);
                if (count <= 0) {
                    Log.d(TAG, "couldn't update task state in files database. " + uri);
                    return count;
                }
                notification = false;
                break;
            case DELETE_FILES:
            	count = db.update(DB_TABLE_FILES, values, selection, selectionArgs);
                if (count <= 0) {
                    Log.d(TAG, "couldn't update audio in  files database");
                    return count;
                }
                notification = true;
            	break;
            default:
                Log.d(TAG, "calling update on an unknown/invalid URI: " + uri);
                throw new IllegalArgumentException("Unknown/Invalid URI " + uri);
        }
        if (count > 0 && notification) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }
    
    
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        int match = sURIMatcher.match(uri);
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        switch (match) {
            case ALL_FILE_INFO:
                sendToTargetService(uri, selection, selectionArgs);
                count = db.delete(DB_TABLE_FILES, selection, selectionArgs);
                break;
            case JPEG_FILES:
                sendToTargetService(uri, selection, selectionArgs);
                count = db.delete(DB_TABLE_FILES, selection, selectionArgs);
                if (count <= 0) {
                    Log.w(TAG, "couldn't delete jpeg files from database");
                    return 0;
                }
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            case VIDEO_FILES:
                sendToTargetService(uri, selection, selectionArgs);
                count = db.delete(DB_TABLE_FILES, selection, selectionArgs);
                if (count <= 0) {
                    Log.w(TAG, "couldn't delete video files from database");
                    return 0;
                }
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            case AUDIO_FILES:
                sendToTargetService(uri, selection, selectionArgs);
                count = db.delete(DB_TABLE_FILES, selection, selectionArgs);
                if (count <= 0) {
                    Log.w(TAG, "couldn't delete audio files from database");
                    return 0;
                }
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            case DELETE_FILES:
            	count = db.delete(DB_TABLE_FILES, selection, selectionArgs);
                if (count <= 0) {
                    Log.w(TAG, "couldn't delete audio files from database");
                    return 0;
                }
                //getContext().getContentResolver().notifyChange(uri, null);
            	break;
            default:
                Log.w(TAG, "calling delete on an unknown/invalid URI: " + uri);
                throw new IllegalArgumentException("Unknown/Invalid URI " + uri);
        }
        return count;
    }
    
    private String[] combine(List<String> prepend, String[] userArgs) {
        int presize = prepend.size();
        if (presize == 0) {
            return userArgs;
        }

        int usersize = (userArgs != null) ? userArgs.length : 0;
        String[] combined = new String[presize + usersize];
        for (int i = 0; i < presize; i++) {
            combined[i] = prepend.get(i);
        }
        for (int i = 0; i < usersize; i++) {
            combined[presize + i] = userArgs[i];
        }
        return combined;
    }
    
    private void sendToTargetService(Uri uri, String selection, String[] selectionArgs){
        String[] list = queryFilePathList(uri, selection, selectionArgs, null);
        if(list != null){
            Intent intent = new Intent(this.getContext(), FileProviderService.class);
            intent.putExtra("_list", list);
            getContext().startService(intent);
        } else {
            Log.w(TAG, "---> none find will deleted files.");
        }
    }
    
    private String[] queryFilePathList(Uri uri, String selection, String[] selectionArgs, String sort){
        String[] list = null;
        String[] projection = { FileColumn.COLUMN_LOCAL_PATH };
        Cursor cursor = query(uri, projection, selection, selectionArgs, sort);
        if(cursor != null){
            int count = cursor.getCount();
            if(count>0){
                list = new String[count];
                int index = 0;
                while(cursor.moveToNext()){
                    list[index++] = cursor.getString(0);
                }
            }
            cursor.close();
        }
        return list;
    }

}
