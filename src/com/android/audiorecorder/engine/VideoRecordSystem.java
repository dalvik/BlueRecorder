/**   
 * Copyright © 2016 浙江大华. All rights reserved.
 * 
 * @title: AudioRecordSystem.java
 * @description: TODO
 * @author: 23536   
 * @date: 2016年4月25日 下午5:19:14 
 */
package com.android.audiorecorder.engine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.android.audiorecorder.BuildConfig;
import com.android.audiorecorder.DebugConfig;
import com.android.audiorecorder.engine.MultiMediaService.OnRecordListener;
import com.android.audiorecorder.provider.FileColumn;
import com.android.audiorecorder.provider.FileDetail;
import com.android.audiorecorder.provider.FileProvider;
import com.android.audiorecorder.ui.SettingsActivity;
import com.android.audiorecorder.ui.SoundRecorder;
import com.android.audiorecorder.utils.DateUtil;
import com.android.audiorecorder.utils.FileUtils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.media.AudioManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;


/** 
 * @description: TODO
 * @author: 23536
 * @date: 2016年4月25日 下午5:19:14  
 */
public class VideoRecordSystem extends AbstractVideoRecordSystem implements OnRecordListener{

    //guard camera msg
    private static final int MSG_INIT_CAMERA = 5001;
    private static final int MSG_TAKE_PICTURE = 5002;
    private static final int MSG_RELEASE_CAMERA = 5003;
    
    private boolean mRecorderStart;
    private long mVideoRecorderDuration;
    private int mCurMode;
    private Context mContext;
    private SharedPreferences mPreferences;
    private AudioManager mAudioManager;

    //video handler thread
    private MediaVideoHandlerCallBack mMediaVideoHandlerCallback;
    private HandlerThread mMediaVideoHandlerThread;
    private Handler mMediaVideoHandler;
    
    //guard camera
    private GuardCameraManager mGuardCamera;
    //private Camera camera;
    private GuardCameraSurfaceTexture mCameraSurfaceTexture;
    
    private static final String IMAGE_CACHE_DIR = "DCIM";
    private String TAG = "VideoRecordSystem";
    
    public VideoRecordSystem(Context context){
        this.mContext = context;
        this.mPreferences = mContext.getSharedPreferences(SettingsActivity.class.getName(), Context.MODE_PRIVATE);
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        init();
    }
    
    /* (non Javadoc) 
     * @title: startVideoRecord
     * @description: TODO
     * @return
     * @throws RemoteException 
     * @see com.android.audiorecorder.engine.IVideoService#startVideoRecord() 
     */
    @Override
    public int startVideoRecord() throws RemoteException {
        return 0;
    }

    /* (non Javadoc) 
     * @title: stopVideoRecord
     * @description: TODO
     * @return
     * @throws RemoteException 
     * @see com.android.audiorecorder.engine.IVideoService#stopVideoRecord() 
     */
    @Override
    public int stopVideoRecord() throws RemoteException {
        return 0;
    }

    /* (non Javadoc) 
     * @title: videoCapture
     * @description: TODO
     * @return
     * @throws RemoteException 
     * @see com.android.audiorecorder.engine.IVideoService#videoCapture() 
     */
    @Override
    public int videoCapture() throws RemoteException {
        return 0;
    }

    /* (non Javadoc) 
     * @title: videoSnap
     * @description: TODO
     * @return
     * @throws RemoteException 
     * @see com.android.audiorecorder.engine.IVideoService#videoSnap() 
     */
    @Override
    public int videoSnap() throws RemoteException {
        return 0;
    }

    /* (non Javadoc) 
     * @title: regStateListener
     * @description: TODO
     * @param listener
     * @throws RemoteException 
     * @see com.android.audiorecorder.engine.IVideoService#regStateListener(com.android.audiorecorder.engine.IVideoStateListener) 
     */
    @Override
    public void regStateListener(IVideoStateListener listener)
            throws RemoteException {
    }

    /* (non Javadoc) 
     * @title: unregStateListener
     * @description: TODO
     * @param listener
     * @throws RemoteException 
     * @see com.android.audiorecorder.engine.IVideoService#unregStateListener(com.android.audiorecorder.engine.IVideoStateListener) 
     */
    @Override
    public void unregStateListener(IVideoStateListener listener)
            throws RemoteException {
    }

    public long getRecorderTime() {
        if (mRecorderStart) {
            return (int) ((SystemClock.elapsedRealtime() - mVideoRecorderDuration) / 1000);
        }
        return 0;
    }

    @Override
    public void setMode(int mode) {
        /*if(mCurMode == MultiMediaService.LUNCH_MODE_AUTO){//reset state machine
            mHandler.removeMessages(MSG_STOP_RECORD);
            Message msg = mHandler.obtainMessage(MSG_STOP_RECORD);
            msg.arg1 = mode;
            mHandler.sendMessage(msg);
        }*/
        mCurMode = mode;
        if(DebugConfig.DEBUG){
            Log.i(TAG, "--->  setMode = " + mode);
        }
    }

    @Override
    public int getMode(){
        return mCurMode;
    }

    @Override
    public void adjustStreamVolume(int streamType, int direct, int flag)
            throws RemoteException {
        mAudioManager.adjustStreamVolume(streamType, direct, flag);
    }

    @Override
    public long checkDiskCapacity() throws RemoteException {
        int where = mPreferences.getInt(SoundRecorder.PREFERENCE_TAG_STORAGE_LOCATION, SoundRecorder.STORAGE_LOCATION_SD_CARD);
        return FileUtils.avliableDiskSize(mContext, where);
    }

    private void init(){

        mGuardCamera = new GuardCameraManager(mContext);
        
        mMediaVideoHandlerThread = new HandlerThread("MediaVideoThread", HandlerThread.MAX_PRIORITY);
        mMediaVideoHandlerThread.start();
        mMediaVideoHandlerCallback = new MediaVideoHandlerCallBack();
        //mMediaVideoHandler = getHandler(mMediaVideoHandlerThread.getLooper(), mMediaVideoHandlerCallback);
    }
    
    private void initCamera(){
        mGuardCamera.getCameraInstance(CameraInfo.CAMERA_FACING_BACK);
        mGuardCamera.getCameraInfo();
        mGuardCamera.startPreview(CameraInfo.CAMERA_FACING_BACK, mAutoFocusCallback);
        //mRingerMode = mAudioManager.getRingerMode();
        Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        System.out.println("orientation = " + display.getRotation());
        if(BuildConfig.DEBUG){
            //Log.i(TAG, "---> mRingerMode = " + mRingerMode);
        }
        mAudioManager.setRingerMode(AudioManager.MODE_IN_CALL);
        if(!mGuardCamera.isSupportAutoFocus()){
            mGuardCamera.takePicture(null, null, jpegCallback);
        }
    }
    
    private void tackPicture(){
        mGuardCamera.takePicture(null, null, jpegCallback);
    }
    
    private void releaseCamera(){
        mGuardCamera.releaseCamera();
    }
    private class MediaVideoHandlerCallBack implements Handler.Callback{

        @Override
        public boolean handleMessage(Message arg0) {
            return false;
        }
        
    }
    
    private AutoFocusCallback mAutoFocusCallback = new AutoFocusCallback() {
        
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if(DebugConfig.DEBUG){
                Log.i(TAG, "---> onAutoFocus : " + success);
            }
            mGuardCamera.takePicture(null, null, jpegCallback);
            /*if(!mGuardCamera.isSupportAutoFocus()){
            } else {
                if(success){
                    mGuardCamera.takePicture(null, null, jpegCallback);
                }
            }*/
        }
    };
    
    private PictureCallback jpegCallback = new PictureCallback() {
        
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File catchPath = FileUtils.getDiskCacheDir(mContext, IMAGE_CACHE_DIR);
            if(!catchPath.exists()){
                catchPath.mkdirs();
            }
            String path = "";//catchPath.getPath() + File.separator + getNamePrefix() + DateUtil.formatyyMMDDHHmmss(System.currentTimeMillis())+".jpg";
            if(DebugConfig.DEBUG){
                Log.d(TAG, "file path = " + path);
            }
            try {
                FileOutputStream fos = new FileOutputStream(path);
                fos.write(data);
                fos.close();
                
                FileDetail detail = new FileDetail(path);
                ContentValues values = new ContentValues();
                values.put(FileColumn.COLUMN_LOCAL_PATH, path);
                values.put(FileColumn.COLUMN_FILE_TYPE, detail.getFileType());
                values.put(FileColumn.COLUMN_MIME_TYPE, detail.getMimeType());
                values.put(FileColumn.COLUMN_FILE_SIZE, data.length);
                values.put(FileColumn.COLUMN_LAUNCH_MODE, LUNCH_MODE_MANLY);
                values.put(FileColumn.COLUMN_DOWN_LOAD_TIME, System.currentTimeMillis());
                values.put(FileColumn.COLUMN_THUMB_NAME, DateUtil.getYearMonthWeek(System.currentTimeMillis()));
                values.put(FileColumn.COLUMN_FILE_RESOLUTION_X, detail.getFileResolutionX());
                values.put(FileColumn.COLUMN_FILE_RESOLUTION_Y, detail.getFileResolutionY());
                mContext.getContentResolver().insert(FileProvider.JPEGS_URI, values);      
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                //mUploadHandler.sendEmptyMessage(MSG_RELEASE_CAMERA);
                //mAudioManager.setRingerMode(mRingerMode);
            }
        }
    };

}
