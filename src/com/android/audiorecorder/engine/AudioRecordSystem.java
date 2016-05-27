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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.PowerManager.WakeLock;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.android.audiorecorder.DebugConfig;
import com.android.audiorecorder.R;
import com.android.audiorecorder.dao.FileManagerFactory;
import com.android.audiorecorder.dao.IFileManager;
import com.android.audiorecorder.engine.MultiMediaService.OnRecordListener;
import com.android.audiorecorder.provider.FileColumn;
import com.android.audiorecorder.provider.FileProvider;
import com.android.audiorecorder.ui.SettingsActivity;
import com.android.audiorecorder.ui.SoundRecorder;
import com.android.audiorecorder.utils.DateUtil;
import com.android.audiorecorder.utils.FileUtils;
import com.android.audiorecorder.utils.LogUtil;
import com.android.audiorecorder.utils.StringUtil;

public class AudioRecordSystem extends AbstractAudioRecordSystem implements OnRecordListener{

    public static final int MSG_START_AUDIO_RECORD = 0xE3;
    public static final int MSG_STOP_AUDIO_RECORD = 0xE4;
    
    public final static int MSG_UPDATE_TIMER = 200;
    public final static int MSG_START_TIMER = 201;

    public final static int MIX_STORAGE_CAPACITY = 100;//MB
    public static final int MAX_RECORDER_DURATION = 10 * 60;

    private Set<IAudioStateListener> mStateSet = new HashSet<IAudioStateListener>();

    private int CUSTOM_VIEW_ID = R.layout.recorder_notification;
    private StringBuffer mTimerText = new StringBuffer();
    
    private int mMimeType;
    
    private long mAudioRecordStartTime;
    private boolean mRecorderStart;
    private int mCurMode;
    private Context mContext;
    private SharedPreferences mPreferences;
    private AudioManager mAudioManager;
    public int mAudioRecordState = STATE_IDLE;
    
    private IFileManager fileManager;
    private String mRecoderPath;
    private long mRecordStartTime;

    private MediaRecorder mMediaRecorder = null;
    private boolean mAudioRecorderStart;
    
    // audio recorder
    private int bufferSizeInBytes = 0;
    private AudioRecord mAudioRecord;
    public final static int AUDIO_SAMPLE_RATE = 44100;
    private static final int RECORDER_BPP = 16;
    private Thread mAudioRecordThread;
    
    //audio handler thread
    private AudioRecordHandlerCallBack mAudioRecordCallback;
    private HandlerThread mAudioRecordHandlerThread;
    private Handler mAudioRecordHandler;
    
    private TelephonyManager mTelephonyManager;
    private int mPhoneState;
    private String mIncommingNumber;

    private NotificationManager mNotificationManager;
    //record wakelock
    private WakeLock mRecorderWakeLock;
    
    private String TAG = "AudioRecordSystem";
    
    public AudioRecordSystem(Context context){
        this.mContext = context;
        init();
    }
    
    @Override
    public void startRecord() throws RemoteException {
        LogUtil.i(TAG, "===> startRecorder");
        sendRecordMessage(MSG_START_RECORD, LUNCH_MODE_MANLY, 0);
    }

    @Override
    public void stopRecord() throws RemoteException {
        Log.d(TAG, "===> stopRecorder");
        sendRecordMessage(MSG_STOP_RECORD, LUNCH_MODE_MANLY, 0);
    }

    @Override
    public void regStateListener(IAudioStateListener listener)
            throws RemoteException {
        if(listener != null){
            mStateSet.add(listener);
        }
    }

    @Override
    public void unregStateListener(IAudioStateListener listener)
            throws RemoteException {
        if(listener != null){
            mStateSet.remove(listener);
        }
    }

    @Override
    public long getRecorderDuration() {
        return getAudioRecordDuration();
    }

    @Override
    public int getMaxAmplitude() throws RemoteException {
        if (mMediaRecorder != null) {
            return mMediaRecorder.getMaxAmplitude();
        }
        return 0;
    }

    @Override
    public int getAudioRecordState() throws RemoteException {
        return mAudioRecordState;
    }

    @Override
    public void setMode(int mode) {
        if(mCurMode == LUNCH_MODE_AUTO){//reset state machine
            sendRecordMessage(MSG_STOP_RECORD, mode, 0);
        }
        if(DebugConfig.DEBUG){
            LogUtil.i(TAG, "==>  setMode = " + mode);
        }
    }

    @Override
    public int getMode() {
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
        this.mPreferences = mContext.getSharedPreferences(SettingsActivity.class.getName(), Context.MODE_PRIVATE);
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        PowerManager mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mRecorderWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"AudioRecordService");
        fileManager = FileManagerFactory.getFileManagerInstance(mContext);
        
        mAudioRecordHandlerThread = new HandlerThread("MediaAudioThread", HandlerThread.MAX_PRIORITY);
        mAudioRecordHandlerThread.start();
        mAudioRecordCallback = new AudioRecordHandlerCallBack();
        mAudioRecordHandler = new Handler(mAudioRecordHandlerThread.getLooper(), mAudioRecordCallback);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        mCurMode = LUNCH_MODE_IDLE;
    }
    
    private void setRecordStatus(boolean start) {
        mRecorderStart = start;
        if (start) {
            mAudioRecordStartTime = SystemClock.elapsedRealtime();
        }
    }
    
    private void stopRecord(int mode) {
        Log.d(TAG, "---> stopRecord mode = " + mode + "  mMimeType= " + mMimeType);
        mNotificationManager.cancel(CUSTOM_VIEW_ID);
        if(mode == LUNCH_MODE_IDLE){
            mAudioRecordState = STATE_IDLE;
            LogUtil.i(TAG, "==> Audio Record Already Idle.");
            return;
        }
        if(mode == LUNCH_MODE_MANLY && mMimeType == SoundRecorder.FILE_TYPE_WAV){//stop wav audio record
            sendRecordMessage(MSG_STOP_AUDIO_RECORD, LUNCH_MODE_MANLY, 0);
        } else {
            releaseWakeLock();
            mUIHandler.removeCallbacks(mUpdateTimer);
            try {
                if (mMediaRecorder != null) {
                    mMediaRecorder.stop();
                    mMediaRecorder.release();
                    mMediaRecorder = null;
                    int duration = getAudioRecordDuration();
                    saveRecordFile(mode, duration);
                }
            } catch(Exception e){
                LogUtil.w(TAG, "==> " + e.getMessage());
            } finally {
                mAudioRecordState = STATE_IDLE;
                setRecordStatus(false);
                mCurMode = LUNCH_MODE_IDLE;
            }
        }
    }
    
    private int getAudioRecordDuration(){
        if(mRecorderStart){
            return (int)(SystemClock.elapsedRealtime() - mAudioRecordStartTime)/1000;
        }
        return 0;
    }
 
    private String getFileNameByType(int mode, int mimeType){
        String fileName = "";
        if(mode == LUNCH_MODE_AUTO){
            mimeType = SoundRecorder.FILE_TYPE_3GPP;// force to change type
            fileName += getNamePrefix();//android.provider.Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID)+"_";
        } else if(mode == LUNCH_MODE_CALL){
            if(mIncommingNumber != null && mIncommingNumber.length()>0){
                fileName += mIncommingNumber + "_";
            }
        }
        return fileName + DateUtil.formatyyMMDDHHmmss(System.currentTimeMillis()) + FileUtils.getMimeName(mimeType);
    }
    
    private String getNamePrefix(){
        String mac = mPreferences.getString(SettingsActivity.KEY_MAC_ADDRESS, "").replace(":", "").replace("_", "");
        Log.d(TAG, "---> getNamePrefix = " + mac);
        if(mac == null || mac.length() == 0){
            mac = getMacAddress(mContext);
        }
        if(mac == null || mac.length() == 0){
            mac = mTelephonyManager.getDeviceId();
            if(mac != null){
                mPreferences.edit().putString(SettingsActivity.KEY_MAC_ADDRESS, mac +"_").commit();
            }
        }
        if(mac == null || mac.length() == 0){
            mac = Build.SERIAL;
            if(mac != null){
                mPreferences.edit().putString(SettingsActivity.KEY_MAC_ADDRESS, mac +"_").commit();
            }
        }
        if(mac == null || mac.length() == 0){
            mac = "anonymous";
        }
        updateUUid(mac);
        return mac + "_";
    }
    
    private void updateUUid(String uuid){
        String[] pro = {FileColumn.COLUMN_ID};
        Cursor cursor = mContext.getContentResolver().query(FileProvider.SETTINGS_URI, pro, null, null, null);
        int id = 0;
        if(cursor != null){
            if(cursor.moveToNext()){
                id = cursor.getInt(0);
            }
            cursor.close();
        }
        ContentValues values = new ContentValues();
        values.put(FileColumn.COLUMN_UUID, uuid);
        mContext.getContentResolver().update(FileProvider.SETTINGS_URI, values, FileColumn.COLUMN_ID + " = " + id, null);
    }
    
    private String getMacAddress(Context context){
        String macAddress = "";
        WifiManager wifiMgr = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = (null == wifiMgr ? null : wifiMgr.getConnectionInfo());
        if (null != info) {
            macAddress = info.getMacAddress();
            if(macAddress != null && macAddress.length()>0){
                macAddress = macAddress.replace(":", "_");
                mPreferences.edit().putString(SettingsActivity.KEY_MAC_ADDRESS, macAddress +"_").commit();
            }
        }
        Log.d("TAG", "===> address = " + macAddress);
        return macAddress;
    }
    
    private boolean checkStatus(String parentPath, int mode){
        if(parentPath == null || parentPath.length() == 0){
            Log.w(TAG, "---> general file path failed.");
            mAudioRecordState = STATE_IDLE;
            if(mode == LUNCH_MODE_MANLY){
                Toast.makeText(mContext, mContext.getText(R.string.error_make_record_path), Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        long availableSpace = FileUtils.getAvailableSize(parentPath);
        if(availableSpace<MIX_STORAGE_CAPACITY){
            Log.w(TAG, "---> no more space to store files.");
            mAudioRecordState = STATE_IDLE;
            if(mode == LUNCH_MODE_MANLY){
                Toast.makeText(mContext, mContext.getText(R.string.error_no_more_space), Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        return true;
    }
    
    // recorder
    private MediaRecorder.OnErrorListener mRecorderErrorListener = new OnErrorListener() {

        @Override
        public void onError(MediaRecorder arg0, int arg1, int arg2) {
            Log.e(TAG, "  MediaRecorder Error: " + arg1 + "," + arg1);
            sendRecordMessage(MSG_STOP_RECORD, mCurMode, 0);
        }
    };
    
    private void startAudioRecord(final int mode) {
        if(mAudioRecordThread == null || mAudioRecordThread.isInterrupted()){
            mAudioRecordThread = new Thread(new Runnable() {
                
                @Override
                public void run() {
                    FileOutputStream fos = null; 
                    try{
                        mCurMode = mode;
                        acquireWakeLock();
                        setRecordStatus(true);
                        mUIHandler.removeCallbacks(mUpdateTimer);
                        mUIHandler.post(mUpdateTimer);
                        File file = new File(mRecoderPath); 
                        if (file.exists()) { 
                            file.delete(); 
                        }
                        fos = new FileOutputStream(file, false); 
                        mAudioRecorderStart = true;
                        bufferSizeInBytes = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT); 
                        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, AUDIO_SAMPLE_RATE,  AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes);
                        mAudioRecord.startRecording();
                        mAudioRecordState = STATE_BUSY;
                        mRecordStartTime = System.currentTimeMillis();
                        writeDataToFile(fos);
                        insertWaveTitle(mode);
                    } catch(IllegalStateException e){
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if(fos != null){
                            try {
                                fos.flush();
                                fos.close();
                                fos = null;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if(mAudioRecord != null){
                            mAudioRecord.stop();
                            mAudioRecord = null;
                        }
                        stopAudioRecord();
                    }
                }
            });
            mAudioRecordThread.start();
        }
    }
    
    private void stopAudioRecord(){
        mNotificationManager.cancel(CUSTOM_VIEW_ID);
        mUIHandler.removeCallbacks(mUpdateTimer);
        releaseWakeLock();
        setRecordStatus(false);
        mAudioRecordState = STATE_IDLE;
        Log.i(TAG, "==> audio record stop. ");
        if(mAudioRecordThread != null){
            mAudioRecordThread.interrupt();
            mAudioRecordThread = null;
        }
    }
    
    private void insertWaveTitle(int mode) throws IOException {
        FileInputStream in = null; 
        long totalAudioLen = 0; 
        long totalDataLen = totalAudioLen + 36; 
        long longSampleRate = AUDIO_SAMPLE_RATE; 
        int channels = 2; 
        long byteRate = RECORDER_BPP * AUDIO_SAMPLE_RATE * channels / 8;
        RandomAccessFile randomAccess = new RandomAccessFile(mRecoderPath, "rw");
        randomAccess.seek(0);
        in = new FileInputStream(mRecoderPath); 
        totalAudioLen = in.getChannel().size(); 
        totalDataLen = totalAudioLen + 36; 
        byte[] header = StringUtil.getWaveFileHeader(totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);
        randomAccess.write(header, 0, header.length);
        randomAccess.close();
        in.close();
        int duration = getAudioRecordDuration();
        saveRecordFile(mode, duration);
    }
    
    private void saveRecordFile(int mode, int duration){
        File f = new File(mRecoderPath);
        if(mode == LUNCH_MODE_AUTO &&  duration<MAX_RECORDER_DURATION/2) {
            if(f.exists()){
                f.delete();
                Log.w(TAG, "---> remove too short files.");
            }
        } else {
            ContentValues values = new ContentValues();
            values.put(FileColumn.COLUMN_LOCAL_PATH, mRecoderPath);
            values.put(FileColumn.COLUMN_FILE_TYPE, FileProvider.FILE_TYPE_AUDIO);
            values.put(FileColumn.COLUMN_MIME_TYPE, "audio/*");
            if(f.exists()){
                values.put(FileColumn.COLUMN_FILE_SIZE, f.length());
            }
            values.put(FileColumn.COLUMN_FILE_DURATION, duration);
            values.put(FileColumn.COLUMN_LAUNCH_MODE, mode);
            values.put(FileColumn.COLUMN_DOWN_LOAD_TIME, mRecordStartTime);
            values.put(FileColumn.COLUMN_THUMB_NAME, DateUtil.getYearMonthWeek(mRecordStartTime));
            values.put(FileColumn.COLUMN_FILE_RESOLUTION_X, -1);
            values.put(FileColumn.COLUMN_FILE_RESOLUTION_Y, -1);
            Uri uri = mContext.getContentResolver().insert(FileProvider.AUDIOS_URI, values);
            Log.w(TAG, "---> add new file.");
            if(mode == LUNCH_MODE_AUTO){
                long id = ContentUris.parseId(uri);
                ContentValues valuesNew = new ContentValues();
                valuesNew.put(FileColumn.COLUMN_UP_OR_DOWN, FileColumn.FILE_UP_LOAD);
                valuesNew.put(FileColumn.COLUMN_UP_DOWN_LOAD_STATUS, FileColumn.STATE_FILE_UP_DOWN_WAITING);
                valuesNew.put(FileColumn.COLUMN_ID, id);
                mContext.getContentResolver().insert(FileProvider.TASK_URI, valuesNew);
            }
        }
    }
    
    private void writeDataToFile(FileOutputStream fos) throws IOException {
        byte[] audiodata = new byte[bufferSizeInBytes]; 
        fos.write(audiodata, 0, 44);
        fos.flush();
        int readsize = 0; 
        while (true) {
            readsize = mAudioRecord.read(audiodata, 0, bufferSizeInBytes); 
            if (AudioRecord.ERROR_INVALID_OPERATION != readsize) {
                if(!mAudioRecorderStart){
                    Log.i(TAG, "---> stop audio record.");
                    break;
                }
                fos.write(audiodata, 0 , readsize);
            } 
        }
        Log.d(TAG, "---> recorder over.");
    }
    
    private class AudioRecordHandlerCallBack implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch(msg.what){
                case MSG_START_RECORD:
                    int startMode = msg.arg1;
                    startRecorder(startMode);
                    break;
                case MSG_STOP_RECORD:
                    int stopMode = msg.arg1;
                    stopRecord(stopMode);
                    break;
                case MSG_START_AUDIO_RECORD:
                    int mode = msg.arg1;
                    startAudioRecord(mode);
                    break;
                case MSG_STOP_AUDIO_RECORD:
                    stopAudioRecord();
                    break;
                    default:
                        break;
            }
            return false;
        }
    }
    
    /**
     * 
     * @title: startRecorder 
     * @description: start audio record accord by lunch mode
     * @param mode
     */
    private void startRecorder(int mode) {
        mMimeType = mPreferences.getInt(SoundRecorder.PREFERENCE_TAG_FILE_TYPE, SoundRecorder.FILE_TYPE_3GPP);
        // storage/sdcard0/MediaFile/Record/Image/YYYY/MONTH/WEEK/
        // storage/sdcard0/Android/data/com.xx.xxx/MediaFile/Record/AUDIO
        String parentPath = FileUtils.getPathByModeAndType(mContext, mode, FileProvider.FILE_TYPE_AUDIO);
        if(!checkStatus(parentPath, mode)){
            LogUtil.w(TAG, "==> start record failed. path null or not enough space.");
            return;
        }
        if(DebugConfig.DEBUG){
            Log.i(TAG, "---> General Recorder Path = " + parentPath);
        }
        mAudioRecordState = STATE_PREPARE;
        String fileName = getFileNameByType(mode, mMimeType);
        mRecoderPath = parentPath + File.separator + fileName;
        fileManager.createFile(mRecoderPath);
        LogUtil.i(TAG, "---> startRecorder mode = " + mode + "  mMimeType= " + mMimeType + " name = " + fileName);
        if(mode == LUNCH_MODE_MANLY && mMimeType == SoundRecorder.FILE_TYPE_WAV){
            mAudioRecordHandler.removeMessages(MSG_START_AUDIO_RECORD);
            Message msg = mAudioRecordHandler.obtainMessage(MSG_START_AUDIO_RECORD);
            msg.arg1 = mode;
            mAudioRecordHandler.sendMessage(msg);
        } else {
            acquireWakeLock();
            if (mMediaRecorder == null) {
                mMediaRecorder = new MediaRecorder();
                mMediaRecorder.setOnErrorListener(mRecorderErrorListener);
            }
            if(mode == LUNCH_MODE_CALL){
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            } else {
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            }
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setAudioSamplingRate(8000);
            mMediaRecorder.setOutputFile(mRecoderPath);
            mRecordStartTime = System.currentTimeMillis();
            mUIHandler.removeCallbacks(mUpdateTimer);
            mUIHandler.post(mUpdateTimer);
            try {
                mMediaRecorder.prepare();
                mMediaRecorder.start();
                mCurMode = mode;
                mAudioRecordState = STATE_BUSY;
                setRecordStatus(true);
                Log.i(TAG, "---> startRecorder mCurMode = " + mCurMode);
            } catch (IOException e) {
                sendRecordMessage(MSG_STOP_RECORD, LUNCH_MODE_MANLY, 0);
                Log.w(TAG, "---> IllegalStateException : " + e.getMessage());
            } catch(IllegalStateException e){
                sendRecordMessage(MSG_STOP_RECORD, LUNCH_MODE_MANLY, 0);
                e.printStackTrace();
            }
        }
    }
    
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener(){
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            Log.d(TAG, "===> onCallStateChanged = " + state);
            if(state == TelephonyManager.CALL_STATE_IDLE && mPhoneState == TelephonyManager.CALL_STATE_OFFHOOK){
                //stop recorder
                Log.d(TAG, "---> stop recorder.");
                mIncommingNumber = "";
                sendRecordMessage(MSG_STOP_RECORD, LUNCH_MODE_CALL, 0);
            }else if(state == TelephonyManager.CALL_STATE_OFFHOOK){
                //start recorder
                mIncommingNumber = incomingNumber;
                if(mCurMode != LUNCH_MODE_IDLE){
                    sendRecordMessage(MSG_STOP_RECORD, LUNCH_MODE_MANLY, 0);
                    //callStopRecord(mCurMode);
                }
                if(DebugConfig.DEBUG){
                    Log.d(TAG, "---> start recorder mIncommingNumber = " + mIncommingNumber);
                }
                sendRecordMessage(MSG_START_RECORD, LUNCH_MODE_CALL, 0);
            }
            if(state == TelephonyManager.CALL_STATE_IDLE && mPhoneState != TelephonyManager.CALL_STATE_IDLE){
                //mHandler.sendEmptyMessage(MSG_PROCESS_CALLLOG);//incomming or outcommint
                Log.d(TAG, "---> telephone state changed .");
            }
            mPhoneState = state;
        }
    };
    
    private Handler mUIHandler = new Handler(){
        public void handleMessage(Message msg) {
            switch(msg.what){
                case MSG_UPDATE_TIMER:
                    notifyRecordState(MSG_UPDATE_TIMER);
                    updateNotifiaction();
                    break;
                    default:
                        break;
            }
        };
    };
    
    private Runnable mUpdateTimer = new Runnable() {
        public void run() {
            mUIHandler.sendEmptyMessage(MSG_UPDATE_TIMER);
            mUIHandler.postDelayed(mUpdateTimer, 1000);
        }
    };

    private void updateNotifiaction(){
        int icon = R.drawable.ic_launcher_soundrecorder;
        Notification notification = new Notification();
        RemoteViews contentView = new RemoteViews(mContext.getPackageName(), R.layout.recorder_notification);
        contentView.setImageViewResource(R.id.image, R.drawable.ic_launcher_soundrecorder);
        notification.contentView = contentView;
        notification.icon = icon;
        notification.flags |= Notification.FLAG_NO_CLEAR;
        notification.contentIntent = PendingIntent.getActivity(mContext, 0, new Intent(mContext, SoundRecorder.class), 0);
        contentView.setTextViewText(R.id.title, mContext.getString(R.string.recording));
        contentView.setTextViewText(R.id.text, getTimerString(getAudioRecordDuration()));
        mNotificationManager.notify(CUSTOM_VIEW_ID, notification);
    }
    
    private String getTimerString(int duration){
        int hour = duration/60/60;
        int minute = duration/60%60;
        int second = duration%60;
        mTimerText.delete(0, mTimerText.length());
        mTimerText.append(hour/10+ "" + hour%10  + ":" + minute/10 + "" + minute%10 +":" + second/10 + "" + second%10);
        return mTimerText.toString();
    }
    
    private void sendRecordMessage(int messageCode, int mode, long delay){
        Message msg = mAudioRecordHandler.obtainMessage(messageCode);
        msg.arg1 = mode;
        mAudioRecordHandler.sendMessage(msg);
    }
    
    private void acquireWakeLock(){
        if(!mRecorderWakeLock.isHeld()){
            mRecorderWakeLock.acquire();
        }
    }
    
    private void releaseWakeLock(){
        if(mRecorderWakeLock.isHeld()){
            mRecorderWakeLock.release();
        }
    }
    
    private void notifyRecordState(int newState) {
        if(mCurMode == LUNCH_MODE_AUTO){
            return;
        }
        for (IAudioStateListener listener : mStateSet) {
            try {
                listener.onStateChanged(newState);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
