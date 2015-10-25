package com.android.audiorecorder.provider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.MediaStore;
import android.util.Log;

import com.android.audiorecorder.DebugConfig;
import com.android.audiorecorder.R;
import com.android.audiorecorder.engine.MultiMediaService;
import com.android.audiorecorder.utils.FileUtils;
import com.android.audiorecorder.utils.StringUtil;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest.HttpMethod;

public class FileProviderService extends Service {

    public static final String ROOT_OLD = "BlueRecorder";
    public static final String ROOT = "MediaFile";
    public static final String CATE_RECORD = "Record";
    public static final String CATE_UPLOAD = "Upload";
    public static final String CATE_DOWNLOAD = "Download";
    public static final String TYPE_JPEG = "Jpeg";
    public static final String TYPE_AUDIO = "Audio";
    public static final String TYPE_VIDEO = "Video";
    public static final String TYPE_Other = "Other";
    public static final String THUMBNAIL = "thumbnail";
    
    private static final int MSG_LOAD_TASK = 0x10;
    private static final int MSG_ANALIZE_FILE = 0x20;
    private static final int MSG_REBUILD_DATABASE = 0x30;
    private static final int MSG_CLEAR_DATABASE = 0x31;
    
    private int CUSTOM_VIEW_IMAGE_ID = 1746208400;
    
    public static final String TAG = "FileProviderService";
    private final String UPLOAD_REMOTE_URL = "http://alumb.sinaapp.com/file_recv.php";

    private HttpUtils mHttpUtils;

    private FileObserver mFileObserver;

    private UpDownloadHandlerCallback mUpDownloadHandlerCallback;
    private HandlerThread mUpDownloadThread;
    private Handler mUpDownloadHandler;
    
    private PowerManager mPowerManager;
    private WakeLock mFileProviderdWakeLock;
    
    private NotificationManager mNotificationManager;
    private BroadcastReceiver exteranalStorageStateReceiver = null;
    private BroadcastReceiver commandRecv = null;
    private Handler mHalnder = new Handler();
    
    @Override
    public void onCreate() {
        super.onCreate();
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mFileProviderdWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"FileProviderService");
        if (mFileObserver == null) {
            mFileObserver = new FileObserver(new Handler());
            getContentResolver().registerContentObserver(FileProvider.DOWNLOAD_URI,
                    true, mFileObserver);
            getContentResolver().registerContentObserver(FileProvider.UPLOAD_URI,
                    true, mFileObserver);
            getContentResolver().registerContentObserver(FileProvider.DELETE_URI,
                    true, mFileObserver);
        }
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mUpDownloadThread = new HandlerThread("DOWN_UP_LOAD_THREAD");
        mUpDownloadHandlerCallback = new UpDownloadHandlerCallback();
        mUpDownloadThread.start();
        mUpDownloadHandler = new Handler(mUpDownloadThread.getLooper(), mUpDownloadHandlerCallback);
        initReceiver();
        if (mHttpUtils == null) {
            mHttpUtils = new HttpUtils();
            ContentValues values = new ContentValues();
            values.put(FileColumn.COLUMN_UP_DOWN_LOAD_STATUS, FileColumn.STATE_FILE_UP_DOWN_WAITING);
            getContentResolver().update(FileProvider.TASK_URI, values, FileColumn.COLUMN_UP_DOWN_LOAD_STATUS + "==? or " + FileColumn.COLUMN_UP_DOWN_LOAD_STATUS + "==?", new String[]{String.valueOf(FileColumn.STATE_FILE_UP_DOWN_FAILED), String.valueOf(FileColumn.STATE_FILE_UP_DOWN_ING) });
        }
        Log.i(TAG, "---> onCreate.");
    }

    private void netfileRequest(final int id, final boolean isDownload, String remotePath, String localPath, final boolean isShowNotifiaction, final int mode) {
        final String fileName;
        if(isDownload){
            fileName = remotePath.substring(remotePath.lastIndexOf("/")+1);
        } else {
            fileName = localPath.substring(localPath.lastIndexOf("/")+1);
        }
        
        if(isDownload){
        	RequestCallBack<File> requestCallBack = new RequestCallBack<File>() {
        		@Override
        		public void onStart() {
        			super.onStart();
        			Log.d(TAG, "start...");
        			ContentValues values = new ContentValues();
        			values.put(FileColumn.COLUMN_UP_DOWN_LOAD_STATUS, FileColumn.STATE_FILE_UP_DOWN_ING);
        			updateTaskStatus(id, values);
        		}
        		
        		@Override
        		public void onLoading(long total, long current, boolean isUploading) {
        			super.onLoading(total, current, isUploading);
        			if(isShowNotifiaction){
        				updateNotifiaction(isUploading, total, current, fileName);
        			}
        			/*if (isUploading) {
        				Log.d(TAG, "upload: " + current + "/" + total);
        			} else {
        				Log.d(TAG, "reply: " + current + "/" + total);
        			}
        			ContentValues values = new ContentValues();
        			values.put(FileColumn.COLUMN_UP_LOAD_BYTE, current);
        			updateTaskStatus(id, values);*/
        		}
        		
        		@Override
        		public void onSuccess(ResponseInfo<File> responseInfo) {
        			Log.d(TAG, "reply: " + responseInfo.result);
        			mNotificationManager.cancel(CUSTOM_VIEW_IMAGE_ID);
        			Message message = mUpDownloadHandler.obtainMessage(MSG_ANALIZE_FILE);
        			message.obj = responseInfo.result.getPath();
        			message.arg1 = isDownload?1:2;
        			message.arg2 = id;
        			mUpDownloadHandler.sendMessage(message);
        		}
        		
        		@Override
        		public void onFailure(HttpException arg0, String msg) {
        			Log.e(TAG, msg);
        			mNotificationManager.cancel(CUSTOM_VIEW_IMAGE_ID);
        			ContentValues values = new ContentValues();
        			values.put(FileColumn.COLUMN_UP_DOWN_LOAD_STATUS, FileColumn.STATE_FILE_UP_DOWN_FAILED);
        			values.put(FileColumn.COLUMN_UP_LOAD_TIME, System.currentTimeMillis());
        			values.put(FileColumn.COLUMN_UP_LOAD_MESSAGE, msg);
        			updateTaskStatus(id, values);
        		}
        	};
            mHttpUtils.download(remotePath, Environment.getExternalStorageDirectory().getPath() + "/" + ROOT + "/" + CATE_DOWNLOAD +"/"+fileName, requestCallBack);
        } else {
            RequestParams params = new RequestParams();
            params.addBodyParameter("file", new File(localPath));
            mHttpUtils.send(HttpMethod.POST, UPLOAD_REMOTE_URL, params, new RequestCallBack<String>() {
                @Override
                public void onStart() {
                    super.onStart();
                    Log.d(TAG, "start...");
                    ContentValues values = new ContentValues();
                    values.put(FileColumn.COLUMN_UP_DOWN_LOAD_STATUS, 1);
                    updateTaskStatus(id, values);
                }

                @Override
                public void onLoading(long total, long current, boolean isUploading) {
                    super.onLoading(total, current, isUploading);
                    if(isShowNotifiaction){
                        updateNotifiaction(isUploading, total, current, fileName);
                    }
                    /*if (isUploading) {
                        Log.d(TAG, "upload: " + current + "/" + total);
                    } else {
                        Log.d(TAG, "reply: " + current + "/" + total);
                    }
                    ContentValues values = new ContentValues();
                    values.put(FileColumn.COLUMN_UP_LOAD_BYTE, current);
                    updateTaskStatus(id, values);*/
                }

                @Override
                public void onSuccess(ResponseInfo<String> responseInfo) {
                    Log.d(TAG, "reply: " + responseInfo.result);
                    mNotificationManager.cancel(CUSTOM_VIEW_IMAGE_ID);
                    if(mode == MultiMediaService.LUNCH_MODE_AUTO){
                    	
                    } else {
                    	ContentValues values = new ContentValues();
                    	values.put(FileColumn.COLUMN_UP_DOWN_LOAD_STATUS, 2);
                    	values.put(FileColumn.COLUMN_UP_LOAD_TIME, System.currentTimeMillis());
                    	String result = responseInfo.result;
                    	values.put(FileColumn.COLUMN_REMOTE_PATH, result);
                    	updateTaskStatus(id, values);
                    }
                }

                @Override
                public void onFailure(HttpException arg0, String msg) {
                    Log.e(TAG, msg);
                    mNotificationManager.cancel(CUSTOM_VIEW_IMAGE_ID);
                    ContentValues values = new ContentValues();
                    values.put(FileColumn.COLUMN_UP_DOWN_LOAD_STATUS, 3);
                    values.put(FileColumn.COLUMN_UP_LOAD_TIME, System.currentTimeMillis());
                    values.put(FileColumn.COLUMN_UP_LOAD_MESSAGE, msg);
                    updateTaskStatus(id, values);
                }
            });
        }
    }

    private void loadNewTasks(){
        if(hasDownLoadingTask()){
        	Log.w(TAG, "---> exists loading task.");
           return; 
        }
        String[] pro = {FileColumn.COLUMN_ID, FileColumn.COLUMN_LOCAL_PATH, FileColumn.COLUMN_REMOTE_PATH, FileColumn.COLUMN_UP_OR_DOWN,
                FileColumn.COLUMN_UP_DOWN_LOAD_STATUS, FileColumn.COLUMN_SHOW_NOTIFICATION, FileColumn.COLUMN_LAUNCH_MODE};
        String selection = FileColumn.COLUMN_UP_DOWN_LOAD_STATUS + " = " + FileColumn.STATE_FILE_UP_DOWN_WAITING;
        String sortOrder = FileColumn.COLUMN_SHOW_NOTIFICATION + " desc ";
        Cursor cursor = getContentResolver().query(FileProvider.TASK_URI, pro, selection, null, sortOrder);
        if(cursor != null){
            if(cursor.moveToNext()){
                int status = cursor.getInt(cursor.getColumnIndex(FileColumn.COLUMN_UP_DOWN_LOAD_STATUS));
                int mode = cursor.getInt(cursor.getColumnIndex(FileColumn.COLUMN_LAUNCH_MODE));
                String remote = cursor.getString(cursor.getColumnIndex(FileColumn.COLUMN_REMOTE_PATH));
                Log.i(TAG, "load new task. status : " + status + " luanch mode = " + mode);
                if(status == FileColumn.STATE_FILE_UP_DOWN_WAITING ){//&& remote != null
                	int id = cursor.getInt(cursor.getColumnIndex(FileColumn.COLUMN_ID));
                	int upOrDown = cursor.getInt(cursor.getColumnIndex(FileColumn.COLUMN_UP_OR_DOWN));//0 up 1 down
                	String local = cursor.getString(cursor.getColumnIndex(FileColumn.COLUMN_LOCAL_PATH));
                	int notifiaction = cursor.getInt(cursor.getColumnIndex(FileColumn.COLUMN_SHOW_NOTIFICATION));
                	if(mode == MultiMediaService.LUNCH_MODE_AUTO){
                		
                	} else {
                		netfileRequest(id, upOrDown==1 ? true : false, remote, local, notifiaction == 1 ? true : false, mode);
                	}
                }
            }
        }
        cursor.close();
    }
    
    private boolean hasDownLoadingTask(){
        String[] pro = {FileColumn.COLUMN_ID, FileColumn.COLUMN_UP_OR_DOWN};
        String selection = FileColumn.COLUMN_UP_DOWN_LOAD_STATUS + " = " + FileColumn.STATE_FILE_UP_DOWN_ING;
        Cursor cursor = getContentResolver().query(FileProvider.TASK_URI, pro, selection, null, null);
        if(cursor != null && cursor.getCount()>0){
        	if(cursor.moveToNext()){
        		int id = cursor.getInt(0);
        		int up_down_load = cursor.getInt(1);
        		Log.w(TAG, "---> task id " + id + " is " + ((up_down_load==FileColumn.FILE_DOWN_LOAD) ? " download.":" upload."));
        	}
            cursor.close();
            return true;
        }
        return false;
    }
    

    
    private class UpDownloadHandlerCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOAD_TASK:
                    loadNewTasks();
                    break;
                case MSG_ANALIZE_FILE:
                    String path = (String)msg.obj;
                    int arg1 = msg.arg1;
                    int id = msg.arg2;
                    updateFileDetail(arg1, path, id);
                    break;
                case MSG_REBUILD_DATABASE:
                    cleanMediaFile();
                    loadExistsMediaFiles();
                    break;
                case MSG_CLEAR_DATABASE:
                    cleanMediaFile();
                    break;
                default:
                    break;
            }
            return false;
        }
    }

    private void updateFileDetail(int arg1, String path, int id){
        ContentValues values = new ContentValues();
        values.put(FileColumn.COLUMN_UP_DOWN_LOAD_STATUS, FileColumn.STATE_FILE_UP_DOWN_SUCCESS);
        if(arg1 == 1){//download
            putContentValues(path, values);
        } else if(arg1 == 2){//upload
        }
        updateTaskStatus(id, values);
    }
    
    private class FileObserver extends ContentObserver {

        public FileObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.d(TAG, "onChange:" + uri.toString());
            sendMessage(mUpDownloadHandler, MSG_LOAD_TASK, 1000);
            List<String> segs = uri.getPathSegments();
            if(segs.size()>0){
            	String tableName = segs.get(0);
            	if(DebugConfig.DEBUG)Log.i(TAG, "---> path segments = " + tableName);
            	if(FileProvider.TABLE_DELETE_FILES.equalsIgnoreCase(tableName)){
            		mHalnder.removeCallbacks(autoDeleteFileTask);
            		mHalnder.postDelayed(autoDeleteFileTask, 1000);
            	}
            }
        }

    }
    
    private void updateTaskStatus(int id, ContentValues values){
        getContentResolver().update(FileProvider.TASK_URI, values, FileColumn.COLUMN_ID + "=" + id, null);
    }
    
    private void sendMessage(Handler handler, int msgCode, long delayMillis){
        handler.removeMessages(msgCode);
        handler.sendEmptyMessageDelayed(msgCode, delayMillis);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification note = new Notification(0, null,
                System.currentTimeMillis());
        note.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(42, note);
        Log.i(TAG, "onStartCommand.");
        sendMessage(mUpDownloadHandler, MSG_LOAD_TASK, 2000);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        if (mFileObserver != null) {
            getContentResolver().unregisterContentObserver(mFileObserver);
            mFileObserver = null;
        }
        unregisterReceiver();
    }
    
    private void initReceiver(){
        if(exteranalStorageStateReceiver == null){
            exteranalStorageStateReceiver = new BroadcastReceiver(){
              @Override
                public void onReceive(Context arg0, Intent intent) {
                  Log.i(TAG, "===> Action : " + intent.getAction());
                    if(Intent.ACTION_MEDIA_MOUNTED.equals(intent.getAction()) && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                        sendMessage(mUpDownloadHandler, MSG_REBUILD_DATABASE, 3000);
                    } else if ((Intent.ACTION_MEDIA_REMOVED.equals(intent.getAction()) || Intent.ACTION_MEDIA_EJECT.equals(intent.getAction())) && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                        sendMessage(mUpDownloadHandler, MSG_CLEAR_DATABASE, 1000);
                    }
                }  
            };
            IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_EJECT);
            filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            filter.addAction(Intent.ACTION_MEDIA_REMOVED);
            filter.addAction(Intent.ACTION_MEDIA_SHARED);
            filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
            filter.addDataScheme("file");
            registerReceiver(exteranalStorageStateReceiver, filter);
        }
        if(commandRecv == null){
            commandRecv = new BroadcastReceiver(){
                @Override
                public void onReceive(Context arg0, Intent intent) {
                    if(FileProvider.ACTION_PROVIDER_ONCREATE.equals(intent.getAction())){
                        Log.i(TAG, "===> Recv Provider OnCreate Action.");
                        sendMessage(mUpDownloadHandler, MSG_REBUILD_DATABASE, 10);
                    }
                }  
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction(FileProvider.ACTION_PROVIDER_ONCREATE);
            registerReceiver(commandRecv, filter);
        }
    }
    
    private void loadExistsMediaFiles(){
    	List<String> filePaths = new ArrayList<String>();
        String PREFIX = Environment.getExternalStorageDirectory().getPath();
        String oldCompleteDirectory = PREFIX + File.separator + ROOT_OLD + File.separator;
        File oldParentDirectory = new File(oldCompleteDirectory);
        if(oldParentDirectory.exists()){
            putFilePathToList(filePaths, oldParentDirectory);
        }
        /**
         * auto recorder dir
         */
        String fileTypeFoldName = FileUtils.getFileTypePath( FileProvider.FILE_TYPE_AUDIO);
        File cachePath = FileUtils.getDiskCacheDir(this, FileProviderService.CATE_RECORD + File.separator + fileTypeFoldName);
        if(cachePath != null && cachePath.exists()){
        	putFilePathToList(filePaths, cachePath);
        }
        /**
         * special path
         */
        String completeDirectory = PREFIX + File.separator + ROOT + File.separator;
        File parentDirectory = new File(completeDirectory);
        if(parentDirectory.exists()){
            putFilePathToList(filePaths, parentDirectory);
        }
        insertFileListDetail(filePaths);
    }
    
    
    private ContentValues[] generalFileDetails(List<String> filePaths, int mode){
        int length = filePaths.size();
        ContentValues[] valueArray = new ContentValues[length];
        for (int i=0; i<length; i++) {
            ContentValues values = new ContentValues();
            //putContentValues(filePaths.get(i), values);
            putContentValuesDefault(filePaths.get(i), values);
            valueArray[i] = values;
            values.put(FileColumn.COLUMN_LAUNCH_MODE, mode);
        }
        return valueArray;
    }
    
    private void insertFileListDetail(List<String> filePaths){
        if(filePaths.size()>0){
            Log.d(TAG, "reloadMediaFile number = " + filePaths.size());
            getContentResolver().bulkInsert(FileProvider.JPEGS_URI, generalFileDetails(filePaths, MultiMediaService.LUNCH_MODE_MANLY));
        } else {
            Log.w(TAG, "reloadMediaFile none." );
        }
    }
    
    private void cleanMediaFile(){
        getContentResolver().delete(FileProvider.JPEGS_URI, null, null);
    }
    
    private void putContentValues(String path, ContentValues values){
        String[] projection = { MediaStore.Files.FileColumns.MEDIA_TYPE, MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.SIZE, 
                MediaStore.Audio.Media.DURATION, MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.Audio.Media.WIDTH, MediaStore.Audio.Media.HEIGHT, MediaStore.Files.FileColumns._ID};
        String where = MediaStore.Files.FileColumns.DATA + " like '%" + path + "'";
        Uri uri = MediaStore.Files.getContentUri("external");
        Cursor cursor = getContentResolver().query(uri, projection, where, null, null);
        values.put(FileColumn.COLUMN_LOCAL_PATH, path);
        if(cursor != null && cursor.getCount()>0){//if exist this file,use query info, else process myself
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
                values.put(FileColumn.COLUMN_FILE_DURATION, cursor.getInt(index++)/1000);
                long addTime = cursor.getInt(index++)*1000;
                values.put(FileColumn.COLUMN_DOWN_LOAD_TIME, addTime);
                values.put(FileColumn.COLUMN_THUMB_NAME, StringUtil.getYearMonthWeek(addTime));
                values.put(FileColumn.COLUMN_FILE_RESOLUTION_X, cursor.getInt(index++));
                values.put(FileColumn.COLUMN_FILE_RESOLUTION_Y, cursor.getInt(index++));
                if(mediaType == FileProvider.FILE_TYPE_VIDEO){
                    int id = cursor.getInt(index++);
                    String selection = MediaStore.Video.Thumbnails.VIDEO_ID +"=?";
                    String[] selectionArgs = new String[]{String.valueOf(id)};
                    Cursor thumbCursor = getContentResolver().query(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Video.Thumbnails.DATA}, selection, selectionArgs, null);
                    if(thumbCursor != null){
                        if(thumbCursor.moveToNext()){
                            values.put(FileColumn.COLUMN_FILE_THUMBNAIL, thumbCursor.getString(0));
                        }
                        thumbCursor.close();
                    }
                }
                if(path.contains(CATE_DOWNLOAD)){
                    values.put(FileColumn.COLUMN_UP_LOAD_TIME, addTime);
                    values.put(FileColumn.COLUMN_UP_OR_DOWN, 1);
                    values.put(FileColumn.COLUMN_UP_DOWN_LOAD_STATUS, 2);
                }
            }
            cursor.close();
        } else {
            putContentValuesDefault(path, values);
        }
    }
    
    private void putContentValuesDefault(String path, ContentValues values){
        FileDetail detail = new FileDetail(path);
        values.put(FileColumn.COLUMN_LOCAL_PATH, path);
        values.put(FileColumn.COLUMN_FILE_TYPE, detail.getFileType());
        values.put(FileColumn.COLUMN_MIME_TYPE, detail.getMimeType());
        values.put(FileColumn.COLUMN_FILE_SIZE, detail.getLength());
        if(path.contains(CATE_DOWNLOAD)){
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
    }
    
    private Runnable autoDeleteFileTask = new Runnable() {
		
		@Override
		public void run() {
			 String[] pro = {FileColumn.COLUMN_ID,  FileColumn.COLUMN_LOCAL_PATH, FileColumn.COLUMN_FILE_THUMBNAIL };
		     Cursor cursor = getContentResolver().query(FileProvider.DELETE_URI, pro, null, null, null);
		     if(cursor != null){
		    	 while(cursor.moveToNext()){
		    		 long id = cursor.getLong(0);
		    		 String path = cursor.getString(1);
		    		 String thunbnailPath = cursor.getString(2);
		    		 System.out.println("id = " + id + " path = " + path + " " + thunbnailPath);
		    		 getContentResolver().delete(FileProvider.ALL_URI, FileColumn.COLUMN_ID + "=" + id, null);
		    		 if(path != null){
		    			 File file = new File(path);
		    			 if(file.exists()){
		    				 file.delete();
		    			 }
		    			 deleteEmptyFolder(file.getParentFile());
		    		 }
		    		 if(thunbnailPath != null){
		    			 File file = new File(thunbnailPath);
		    			 if(file.exists()){
		    				 file.delete();
		    			 }
		    			 deleteEmptyFolder(file.getParentFile());
		    		 }
		    		 
		    	 }
		    	 cursor.close();
		     }
		}
	};
	
	private void putFilePathToList(List<String> filePaths, File file){
        if(file.isDirectory()){
            File[] files = file.listFiles();
            for(File f:files){
                if(!f.getParent().contains(THUMBNAIL)){
                    putFilePathToList(filePaths, f);
                }
            }
        } else {
            filePaths.add(file.getPath());
        }
    }
	
	private void updateNotifiaction(boolean isUpload, long max, long progress, String title) {
        Notification notification = new Notification.Builder(this)
        .setSmallIcon(R.drawable.ic_launcher)
        .setContentTitle(title)
        .setContentText(progress+" %")
        .setOngoing(true)
        .setWhen(0)
        //.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, FileUploadTaskActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
        .build();

        // Notification.Builder will helpfully fill these out for you no
        // matter what you do
        notification.tickerView = null;
        notification.tickerText = null;
        
        notification.priority = Notification.PRIORITY_HIGH;
        notification.flags |= Notification.FLAG_NO_CLEAR;
        
        //notification.contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, FileManagerActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        mNotificationManager.notify(CUSTOM_VIEW_IMAGE_ID, notification);
    }
	
	private void deleteEmptyFolder(File file){
        if(file.isDirectory()){
            File[] files = file.listFiles();
            if(files.length == 0){
                file.delete();
                if(file.getParent() != null){
                    deleteEmptyFolder(file.getParentFile());
                }
            } else {
                for(File f:files){
                    deleteEmptyFolder(f);
                }
            }
        }
    }
	
	    private void unregisterReceiver(){
        if(exteranalStorageStateReceiver != null){
            unregisterReceiver(exteranalStorageStateReceiver);
            exteranalStorageStateReceiver = null;
        }
        if(commandRecv != null){
            unregisterReceiver(commandRecv);
            commandRecv = null;
        }
    }
    /*
    public static final int MEDIA_TYPE_NONE = 0;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_AUDIO = 2;
    public static final int MEDIA_TYPE_VIDEO = 3;
    public static final int MEDIA_TYPE_PLAYLIST = 4;
    */
}
