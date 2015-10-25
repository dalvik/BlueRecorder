
package com.android.audiorecorder.provider;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
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
    
    private static final String DB_NAME = "files.db";

    private static final int DB_VERSION = 1;

    private static final String DB_TABLE_FILES = "files";
    private static final String DB_TABLE_TASKS = "down_up_load_tasks";//up or download tasks
    private static final String DB_TABLE_SETTINGS = "settings";
    
    private static final String TABLE_JPEG_FILES = "jpeg";
    private static final String TABLE_AUDIO_FILES = "audio";
    private static final String TABLE_VIDEO_FILES = "video";
    public static final String TABLE_DELETE_FILES = "deleted";

    private static final int DOWNLOAD = 1;
    private static final int DOWNLOADS_ID = 2;

    private static final int UPLOAD = 3;
    private static final int UPLOAD_ID = 4;

    private static final int TASK = 6;
    
    private static final int ALL_FILE_INFO = 7;
    
    private static final int SETTINGS = 10;
    
    private static final int JPEG_FILES = 15;
    private static final int AUDIO_FILES = 16;
    private static final int VIDEO_FILES = 17;
    private static final int DELETE_FILES = 18;
    
    private final static String authority = "com.android.audiorecorder.provider.FileProvider";
    
    public static final Uri ALL_URI = Uri.parse("content://" + authority + "/all_file_info");
    public static final Uri DOWNLOAD_URI = Uri.parse("content://" + authority + "/download");
    public static final Uri UPLOAD_URI = Uri.parse("content://" + authority + "/upload");
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
        sURIMatcher.addURI(authority, "download", DOWNLOAD);
        sURIMatcher.addURI(authority, "download/#", DOWNLOADS_ID);
        sURIMatcher.addURI(authority, "upload", UPLOAD);
        sURIMatcher.addURI(authority, "upload/#", UPLOAD_ID);
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
        switch (match) {
            case DOWNLOADS_ID:
            case UPLOAD_ID:
                return "vnd.android.cursor.item";
            case DOWNLOAD:
            case UPLOAD:
                return "vnd.android.cursor.dir";
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        int type = sURIMatcher.match(uri);
        long rowid = 0;
        Uri newUri = null;
        switch(type){
            case DOWNLOAD:
            case UPLOAD:
            case TASK:
                values.put(FileColumn.COLUMN_UP_OR_DOWN, (type==DOWNLOAD)?1:0);
                rowid = db.insert(DB_TABLE_FILES, null, values);
                if (rowid <= 0) {
                    Log.d(TAG, "couldn't insert into downloads database. " + uri);
                    return null;
                }
                newUri = ContentUris.withAppendedId(uri, rowid);
                getContext().getContentResolver().notifyChange(newUri, null);
                break;
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
            case DOWNLOAD:
            case UPLOAD:
            case DOWNLOADS_ID:
            case UPLOAD_ID:
            case TASK:
                if(type == DOWNLOAD){
                    qb.appendWhere(FileColumn.COLUMN_UP_OR_DOWN + "=1");//download
                } else if(type == UPLOAD){
                    qb.appendWhere(FileColumn.COLUMN_UP_OR_DOWN + "=0");//upload
                } else if(type == DOWNLOADS_ID){
                    qb.appendWhere(FileColumn.COLUMN_UP_OR_DOWN + "=1");//download
                    qb.appendWhere(FileColumn.COLUMN_ID + "=?");
                    prependArgs.add(uri.getPathSegments().get(1));
                } else if(type == UPLOAD_ID){
                    qb.appendWhere(FileColumn.COLUMN_UP_OR_DOWN + "=0");//upload
                    qb.appendWhere(FileColumn.COLUMN_ID + "=?");
                    prependArgs.add(uri.getPathSegments().get(1));
                }
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
            case DELETE_FILES:
            	qb.setTables(TABLE_DELETE_FILES);
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
        String extraSelection = null;
        String finalSelection = null;
        boolean notification = false;
        switch (match) {
            case DOWNLOADS_ID:
            case UPLOAD_ID:
            case UPLOAD:
            case DOWNLOAD:
                count = db.update(DB_TABLE_FILES, values, selection, selectionArgs);
                if (count <= 0) {
                    Log.d(TAG, "couldn't update task in files database. " + uri);
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
                notification = true;
                break;
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
        String extraSelection = null;
        String finalSelection = null;
        switch (match) {
            case UPLOAD_ID:
            case DOWNLOADS_ID: {
                extraSelection = FileColumn.COLUMN_ID + "=" + uri.getPathSegments().get(1);
            }
            case UPLOAD:
            case DOWNLOAD: {
                finalSelection = TextUtils.isEmpty(selection)
                        ? extraSelection : extraSelection + " AND " + selection;

                count = db.delete(DB_TABLE_FILES, finalSelection, selectionArgs);
            }
                break;
            case ALL_FILE_INFO:
                count = db.delete(DB_TABLE_FILES, selection, selectionArgs);
                break;
            case JPEG_FILES:
                count = db.delete(DB_TABLE_FILES, selection, selectionArgs);
                if (count <= 0) {
                    Log.w(TAG, "couldn't delete jpeg files from database");
                    return 0;
                }
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            case VIDEO_FILES:
                count = db.delete(DB_TABLE_FILES, selection, selectionArgs);
                if (count <= 0) {
                    Log.w(TAG, "couldn't delete video files from database");
                    return 0;
                }
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            case AUDIO_FILES:
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

    private final class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(final Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        private void updateDatabase(SQLiteDatabase db, int fromVersion, int toVersion) {
            try {
                db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE_FILES);
                db.execSQL("CREATE TABLE " + DB_TABLE_FILES +
                    "('" + FileColumn.COLUMN_ID+"' INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "'" + FileColumn.COLUMN_FILE_TYPE+"' INTEGER NOT NULL DEFAULT " + FILE_TYPE_OTHER + ", " +
                    "'" + FileColumn.COLUMN_MIME_TYPE+"' TEXT, " +
                    
                    "'" + FileColumn.COLUMN_LOCAL_PATH + "' TEXT NOT NULL, " +
                    "'" + FileColumn.COLUMN_REMOTE_PATH + "' TEXT, " +
                    "'" + FileColumn.COLUMN_UP_OR_DOWN + "' INTEGER DEFAULT 0, " +
                    "'" + FileColumn.COLUMN_THUMB_NAME + "' TEXT, " +
                    
                    "'" + FileColumn.COLUMN_FILE_SIZE + "' INTEGER DEFAULT 0, " +
                    "'" + FileColumn.COLUMN_FILE_DURATION + "' INTEGER DEFAULT 0, " +
                    "'" + FileColumn.COLUMN_LAUNCH_MODE + "' INTEGER, " +
                    
                    "'" + FileColumn.COLUMN_FILE_RESOLUTION_X + "' INTEGER, " +
                    "'" + FileColumn.COLUMN_FILE_RESOLUTION_Y + "' INTEGER, " +
                    "'" + FileColumn.COLUMN_FILE_THUMBNAIL + "' TEXT, " +
                    
                    "'" + FileColumn.COLUMN_UP_DOWN_LOAD_STATUS + "' INTEGER NOT NULL DEFAULT 0, " +
                    "'" + FileColumn.COLUMN_DOWN_LOAD_TIME + "' LONG, " +
                    "'" + FileColumn.COLUMN_UP_LOAD_TIME + "' LONG DEFAULT 0, " +
                    "'" + FileColumn.COLUMN_UP_LOAD_BYTE + "' LONG DEFAULT 0, " +
                    "'" + FileColumn.COLUMN_SHOW_NOTIFICATION + "' INTEGER DEFAULT 0, " +
                    "'" + FileColumn.COLUMN_UP_LOAD_MESSAGE + "' TEXT);");
                
                db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE_SETTINGS);
                db.execSQL("CREATE TABLE " + DB_TABLE_SETTINGS +
                        "('" + FileColumn.COLUMN_ID+"' INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "'" + FileColumn.COLUMN_FILE_INIT+"' INTEGER NOT NULL DEFAULT 0 );" );
                db.execSQL("DROP VIEW IF EXISTS " + DB_TABLE_TASKS);
                
                String createTasksView = "CREATE VIEW " + DB_TABLE_TASKS + " AS SELECT " 
                + FileColumn.COLUMN_ID + ", " + FileColumn.COLUMN_LOCAL_PATH + ", " + FileColumn.COLUMN_REMOTE_PATH + ", "  + FileColumn.COLUMN_UP_DOWN_LOAD_STATUS +", " 
                + FileColumn.COLUMN_UP_OR_DOWN + ", " + FileColumn.COLUMN_LAUNCH_MODE + ", " + FileColumn.COLUMN_SHOW_NOTIFICATION + ", " + FileColumn.COLUMN_UP_LOAD_BYTE + ", " + FileColumn.COLUMN_UP_LOAD_MESSAGE
                + " FROM " + DB_TABLE_FILES + " WHERE " + FileColumn.COLUMN_UP_OR_DOWN + " !=0 ";
                db.execSQL(createTasksView);
                
                //init jpeg audio video view
                db.execSQL("DROP VIEW IF EXISTS " + TABLE_JPEG_FILES);
                String createJpegView = "CREATE VIEW " + TABLE_JPEG_FILES + " AS SELECT " 
                + FileColumn.COLUMN_ID + ", " + FileColumn.COLUMN_FILE_TYPE + ", " + FileColumn.COLUMN_MIME_TYPE + ", " + FileColumn.COLUMN_LOCAL_PATH + ", " + FileColumn.COLUMN_THUMB_NAME + ", " + FileColumn.COLUMN_LAUNCH_MODE + ", "
                + FileColumn.COLUMN_FILE_THUMBNAIL + ", " + FileColumn.COLUMN_FILE_RESOLUTION_X + ", " + FileColumn.COLUMN_FILE_RESOLUTION_Y + ", " + FileColumn.COLUMN_UP_OR_DOWN + ", " + FileColumn.COLUMN_SHOW_NOTIFICATION + ", "
                + FileColumn.COLUMN_FILE_SIZE + ", " + FileColumn.COLUMN_FILE_DURATION + ", " + FileColumn.COLUMN_DOWN_LOAD_TIME + ", "  + FileColumn.COLUMN_UP_DOWN_LOAD_STATUS +", "  + FileColumn.COLUMN_UP_LOAD_TIME + " FROM " + DB_TABLE_FILES
                + " WHERE " + FileColumn.COLUMN_FILE_TYPE + "=" + FILE_TYPE_JEPG;
                db.execSQL(createJpegView);
                
                db.execSQL("DROP VIEW IF EXISTS " + TABLE_AUDIO_FILES);
                String createAudioView = "CREATE VIEW " + TABLE_AUDIO_FILES + " AS SELECT " 
                + FileColumn.COLUMN_ID + ", " + FileColumn.COLUMN_FILE_TYPE + ", " + FileColumn.COLUMN_MIME_TYPE + ", " + FileColumn.COLUMN_LOCAL_PATH +", " + FileColumn.COLUMN_THUMB_NAME + ", " + FileColumn.COLUMN_LAUNCH_MODE + ", "
                + FileColumn.COLUMN_FILE_THUMBNAIL + ", " + FileColumn.COLUMN_FILE_RESOLUTION_X + ", " + FileColumn.COLUMN_FILE_RESOLUTION_Y + ", " + FileColumn.COLUMN_UP_OR_DOWN + ", " + FileColumn.COLUMN_SHOW_NOTIFICATION + ", "
                        +FileColumn.COLUMN_FILE_SIZE + ", " + FileColumn.COLUMN_FILE_DURATION + ", " + FileColumn.COLUMN_DOWN_LOAD_TIME + ", "  + FileColumn.COLUMN_UP_DOWN_LOAD_STATUS +", "  + FileColumn.COLUMN_UP_LOAD_TIME +  " FROM " + DB_TABLE_FILES
                + " WHERE " + FileColumn.COLUMN_FILE_TYPE + "=" + FILE_TYPE_AUDIO;
                db.execSQL(createAudioView);
                
                db.execSQL("DROP VIEW IF EXISTS " + TABLE_VIDEO_FILES);
                String createVideoView = "CREATE VIEW " + TABLE_VIDEO_FILES + " AS SELECT " 
                + FileColumn.COLUMN_ID + ", " + FileColumn.COLUMN_FILE_TYPE + ", " + FileColumn.COLUMN_MIME_TYPE + ", " + FileColumn.COLUMN_LOCAL_PATH +", " + FileColumn.COLUMN_THUMB_NAME + ", " + FileColumn.COLUMN_LAUNCH_MODE + ", "
                + FileColumn.COLUMN_FILE_THUMBNAIL + ", " + FileColumn.COLUMN_FILE_RESOLUTION_X + ", " + FileColumn.COLUMN_FILE_RESOLUTION_Y + ", " + FileColumn.COLUMN_UP_OR_DOWN + ", " + FileColumn.COLUMN_SHOW_NOTIFICATION + ", "
                + FileColumn.COLUMN_FILE_SIZE + ", " + FileColumn.COLUMN_FILE_DURATION + ", " + FileColumn.COLUMN_DOWN_LOAD_TIME + ", "  + FileColumn.COLUMN_UP_DOWN_LOAD_STATUS +", "  + FileColumn.COLUMN_UP_LOAD_TIME +  " FROM " + DB_TABLE_FILES
                + " WHERE " + FileColumn.COLUMN_FILE_TYPE + "=" + FILE_TYPE_VIDEO;
                db.execSQL(createVideoView);
                
                db.execSQL("DROP VIEW IF EXISTS " + TABLE_DELETE_FILES);
                String createDeleteView = "CREATE VIEW " + TABLE_DELETE_FILES + " AS SELECT " 
                + FileColumn.COLUMN_ID + ", " + FileColumn.COLUMN_LOCAL_PATH +", " + FileColumn.COLUMN_FILE_THUMBNAIL + ", " + FileColumn.COLUMN_UP_DOWN_LOAD_STATUS + " FROM " + DB_TABLE_FILES
                + " WHERE " + FileColumn.COLUMN_UP_DOWN_LOAD_STATUS + "=-1";
                db.execSQL(createDeleteView);
                Log.i(TAG, "===> SQLiteOpenHelper Oncreate.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            updateDatabase(db, 0, DB_VERSION);
            getContext().sendStickyBroadcast(new Intent(ACTION_PROVIDER_ONCREATE));
            /*ContentValues values = new ContentValues();
            values.put(FileColumn.COLUMN_FILE_INIT, 0);//db onreate, 0:frist 1:is create before
            long id = db.insert(DB_TABLE_SETTINGS, null, values);
            Log.i(TAG, "===> db init id : " + id);*/
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {
            updateDatabase(db, arg1, arg2);
        }

    }

}
