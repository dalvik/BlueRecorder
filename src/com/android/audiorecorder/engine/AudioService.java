package com.android.audiorecorder.engine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.HTTP;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SystemClock;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.audiorecorder.DebugConfig;
import com.android.audiorecorder.R;
import com.android.audiorecorder.RecorderFile;
import com.android.audiorecorder.SettingsActivity;
import com.android.audiorecorder.SoundRecorder;
import com.android.audiorecorder.dao.FileManagerFactory;
import com.android.audiorecorder.dao.IFileManager;
import com.android.audiorecorder.engine.ProgressOutHttpEntity.ProgressListener;
import com.android.audiorecorder.engine.RecorderUploader.UploadResult;
import com.android.audiorecorder.utils.DateUtil;
import com.android.audiorecorder.utils.FileUtils;
import com.android.audiorecorder.utils.NetworkUtil;
import com.android.audiorecorder.utils.Utils;

public class AudioService extends Service{
	
	public final static int PAGE_NUMBER = 1;
    private static final int AM = 21;
    private static final int PM = 23;
    
    private static final int UPLOAD_START = 2;
    private static final int UPLOAD_END = 4;
    
    private static final int DELETE_START = 13;
    private static final int DELETE_END = 15;
    
    public static final int LUNCH_MODE_MANLY = 0;//no allowed time and tel to recorder
    public static final int LUNCH_MODE_AUTO = 1;
    public static final int LUNCH_MODE_CALL = 2;
    private int mCurMode;
    int mTempMode = LUNCH_MODE_AUTO;
    
    public static final String Action_RecordListen = "com.audio.Action_BluetoothRecord";


    public static final String Action_Record_Extral_Start = "com.dahuatech.audio.extral_start";// false
    public static final String Action_Record_Extral_Channel = "com.dahuatech.audio.extral_channel";// 0
    public static final String Action_Record_Extral_Stream = "com.dahuatech.audio.extral_stream";// 1

    public static final String Action_TALK_Extral_Start = "com.dahuatech.audio.extral_start";// false
    public static final String Action_TALK_Extral_Channel = "com.dahuatech.audio.extral_channel";// 0
    public static final String Action_TALK_Extral_Stream = "com.dahuatech.audio.extral_stream";// 1

    private boolean mAudioRecordStart;
    private int mAudioRecordChannel;
    private int mAudioRecordStream;

    private boolean mAudioTalkStart;
    private int mAudioTalkChannel;
    private int mAudioTalkStream;

    public static final int MSG_START_RECORD = 0xE1;
    public static final int MSG_STOP_RECORD = 0xE2;
    public static final int STATE_PREPARE = 0x01;

    public static final int MSG_ATDP_CONNECTED = 0x10;
    public static final int MSG_ATDP_DISCONNECTED = 0x11;
    
    public final static int MSG_UPDATE_TIMER = 200;
    
    public final static int MSG_TIMER_ALARM = 1000;
    
    public final static int MSG_START_UPLOAD = 2000;
    
    public final static int MSG_START_DELETE = 3000;

    private WakeLock mWakeLock;
    private WakeLock mAlarmWakeLock;
    private MediaRecorder mMediaRecorder = null;

    private MediaPlayer mMediaPlayer = null;
    private OnCompletionListener mOnCompletionListener;

    private String TAG = "AudioService";

    private Set<IStateListener> mStateSet = new HashSet<IStateListener>();

    private int audioStartId;
    private int audioStopId;

    private AudioManager mAudioManager;
    private PowerManager mPowerManager;
    private boolean mIsSpeekPhoneOn;
    private Object lock = new Object();

    private boolean mRecorderStart;
    private boolean mTalkStart;
    private long mRecorderTime;
    private long mTalkTime;

    private BroadcastReceiver receiver = null;
    private BroadcastReceiver mStateChnageReceiver = null;
    private BroadcastReceiver bluetoothStateReceiver = null;
    
    private SharedPreferences mPreferences;
    private NotificationManager mNotificationManager;
    private int CUSTOM_VIEW_ID = R.layout.recorder_notification;
    private StringBuffer timerInfo = new StringBuffer();
    
    private IFileManager fileManager;
    private String mRecoderPath;
    private long startTime;
    private int mMimeType;
    
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private int mPhoneState;
    private String mIncommingNumber;
    
    private TimeSchedule mTimeSchedule;
    //private RecorderUploader mUploader;
    
    private UploadHandlerCallback mUploadHandlerCallback;
    private HandlerThread mUpHandlerThread;
    private Handler mUploadHandler;
    
    private long mTransferedBytes;
    
    private String mPath;
    
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_START_RECORD:
                    startRecorder();
                    break;
                case MSG_STOP_RECORD:
                    stopRecord();
                    break;
                case MSG_ATDP_CONNECTED:
                     mAudioManager.setStreamMute(AudioManager.STREAM_VOICE_CALL, true);
                     mAudioManager.setBluetoothScoOn(true);
                     mAudioManager.startBluetoothSco();
                    break;
                case MSG_ATDP_DISCONNECTED:
                    if(mAudioManager.isBluetoothA2dpOn()){
                        mAudioManager.setBluetoothScoOn(false);
                        mAudioManager.stopBluetoothSco();
                    }
                    if(!mAudioManager.isWiredHeadsetOn() && !mAudioManager.isBluetoothA2dpOn()){
                        mAudioManager.setSpeakerphoneOn(true);
                    }
                    break;
                case MSG_UPDATE_TIMER:
                    notifyUpdate(MSG_UPDATE_TIMER);
                    updateNotifiaction();
                    break;
                case MSG_TIMER_ALARM:
                    processTimerAlarm();
                    break;
                default:
                    break;
            }
        };
    };

    @Override
    public IBinder onBind(Intent intent) {
        if (Action_RecordListen.equalsIgnoreCase(intent.getAction())) {
            return iRecordListener;
        }
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mPreferences = getSharedPreferences("SoundRecorder", Context.MODE_PRIVATE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"SoundRecorderService");
        mAlarmWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlarmWakeLock");
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mCurMode = LUNCH_MODE_MANLY;
        IntentFilter filter = new IntentFilter();
        filter.addAction(TimeSchedule.ACTION_TIMER_ALARM);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        if (mStateChnageReceiver == null) {
            mStateChnageReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    final String action = intent.getAction();
                    if(DebugConfig.DEBUG){
                        Log.i(TAG, "---> action = " + action);
                    }
                    if(TimeSchedule.ACTION_TIMER_ALARM.equalsIgnoreCase(action)){
                        mHandler.sendEmptyMessage(MSG_TIMER_ALARM);
                    } else if(Intent.ACTION_USER_PRESENT.equalsIgnoreCase(action)){
                        ProgressOutHttpEntity.mCancle = true;
                    }
                }
            };
            registerReceiver(mStateChnageReceiver, filter);
        }
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if(phoneStateListener == null){
            phoneStateListener = new PhoneStateListener(){
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    super.onCallStateChanged(state, incomingNumber);
                    Log.d(TAG, "===> onCallStateChanged = " + state);
                    if(state == TelephonyManager.CALL_STATE_IDLE && mPhoneState == TelephonyManager.CALL_STATE_OFFHOOK){
                        //stop recorder
                        Log.d(TAG, "---> stop recorder.");
                        mIncommingNumber = "";
                        mCurMode = mTempMode;
                        callStopRecord();
                    }else if(state == TelephonyManager.CALL_STATE_OFFHOOK){
                        //start recorder
                        mIncommingNumber = incomingNumber;
                        mTempMode = mCurMode;
                        mCurMode = LUNCH_MODE_CALL;
                        if(DebugConfig.DEBUG){
                            Log.d(TAG, "---> start recorder mIncommingNumber = " + mIncommingNumber);
                        }
                        callStartRecord();
                    }
                    if(state == TelephonyManager.CALL_STATE_IDLE && mPhoneState != TelephonyManager.CALL_STATE_IDLE){
                        //mHandler.sendEmptyMessage(MSG_PROCESS_CALLLOG);//incomming or outcommint
                        Log.d(TAG, "---> telephone state changed .");
                    }
                    mPhoneState = state;
                }
            };
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        fileManager = FileManagerFactory.getSmsManagerInstance(this);
        mTimeSchedule = new TimeSchedule(this);
        mTimeSchedule.start();
        mUploadHandlerCallback = new UploadHandlerCallback();
        mUpHandlerThread = new HandlerThread("upload_thread", HandlerThread.MAX_PRIORITY);
        mUpHandlerThread.start();
        mUploadHandler = new Handler(mUpHandlerThread.getLooper(), mUploadHandlerCallback);
        mCurMode = LUNCH_MODE_AUTO;
        Log.d(TAG, "===> onCreate.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	//startForeground(CUSTOM_VIEW_ID, new Notification());
    	Notification note = new Notification(0, null, System.currentTimeMillis() );
        note.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(42, note);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mStateChnageReceiver != null) {
            unregisterReceiver(mStateChnageReceiver);
            mStateChnageReceiver = null;
        }
        if(mUpHandlerThread != null){
            mUpHandlerThread.quit();
            mUpHandlerThread = null;
        }
        Log.d(TAG, "===> onDestroy.");
    }

    private IRecordListener.Stub iRecordListener = new IRecordListener.Stub() {

        @Override
        public void startRecord() throws RemoteException {
            Log.i(TAG, "===>Call startRecorder");
            mHandler.removeMessages(MSG_START_RECORD);
            mHandler.sendEmptyMessage(MSG_START_RECORD);
            mHandler.removeCallbacks(mUpdateTimer);
            mHandler.post(mUpdateTimer);
            mCurMode = LUNCH_MODE_MANLY;
        }

        @Override
        public void stopRecord() throws RemoteException {
            Log.d(TAG, "===> stopRecorder");
            mHandler.removeMessages(MSG_STOP_RECORD);
            mHandler.sendEmptyMessage(MSG_STOP_RECORD);
            mHandler.removeCallbacks(mUpdateTimer);
            mNotificationManager.cancel(CUSTOM_VIEW_ID);
        }

        @Override
        public int getRecorderTime() throws RemoteException {
            if (mRecorderStart) {
                return (int) ((SystemClock.uptimeMillis() - mRecorderTime) / 1000);
            }
            return 0;
        }

        @Override
        public int getTalkTime() throws RemoteException {
            return (int) ((SystemClock.uptimeMillis() - mTalkTime) / 1000);
        }

        @Override
        public int getMaxAmplitude() throws RemoteException {
            if (mMediaRecorder != null) {
                return mMediaRecorder.getMaxAmplitude();
            }
            return 0;
        }

        @Override
        public boolean isRecorderStart() throws RemoteException {
            return mRecorderStart;
        }

        @Override
        public boolean isTalkStart() throws RemoteException {
            return mTalkStart;
        }

        public void setMode(int mode) throws RemoteException {
            mCurMode = mode;
            if(DebugConfig.DEBUG){
                Log.i(TAG, "--->  setMode = " + mode);
            }
        }
        
        public int getMode() throws RemoteException {
            if(DebugConfig.DEBUG){
                Log.i(TAG, "---> current mode = " + mCurMode);
            }
            return mCurMode;
        };
        
        @Override
        public void regStateListener(IStateListener listener)
                throws RemoteException {
            if (listener != null) {
                mStateSet.add(listener);
                Log.d(TAG, "===> IRecordListener regStateListener");
            }
        }

        @Override
        public void unregStateListener(IStateListener listener)
                throws RemoteException {
            if (listener != null) {
                mStateSet.remove(listener);
                Log.d(TAG, "===> IRecordListener unregStateListener");
            }
        }

        @Override
        public void adjustStreamVolume(int streamType, int direct, int flag)
                throws RemoteException {
            mAudioManager.adjustStreamVolume(streamType, direct, flag);
        }
    };

    private void setRecordStatus(boolean start) {
        mRecorderStart = start;
        if (start) {
            mRecorderTime = SystemClock.uptimeMillis();
        }
    }

    private void startRecorder() {
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setOnErrorListener(new OnErrorListener() {

                @Override
                public void onError(MediaRecorder mr, int what, int extra) {
                    Log.d(TAG, "===> startRecorder onError : what = " + what);
                    stopRecord();
                }
            });

        }
        try {
            if(mCurMode == LUNCH_MODE_CALL){
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            } else {
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            }
            /*if(mPreferences.getInt(SoundRecorder.PREFERENCE_TAG_FILE_TYPE, SoundRecorder.FILE_TYPE_DEFAULT) == SoundRecorder.FILE_TYPE_3GPP){
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            }else{
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            }*/
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            if(mCurMode == LUNCH_MODE_MANLY){
                mMediaRecorder.setAudioSamplingRate(44100);
            } else {
                mMediaRecorder.setAudioSamplingRate(8000);
            }
            createDir();
            mMediaRecorder.setOutputFile(mRecoderPath);
            startTime = System.currentTimeMillis();
            mMediaRecorder.setOnErrorListener(mRecorderErrorListener);
            mMediaRecorder.prepare();
            IntentFilter filter = new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
            if (receiver == null) {
                receiver = new BroadcastReceiver() {
                    public void onReceive(Context context, Intent intent) {
                        int state = intent.getIntExtra(
                                BluetoothA2dp.EXTRA_STATE, -1);
                        Log.d(TAG, "===>BluetoothA2dp state = " + state);
                        int di = BluetoothA2dp.STATE_DISCONNECTING;
                        if (state == BluetoothA2dp.STATE_CONNECTED) {
                            mHandler.sendEmptyMessageDelayed(
                                    MSG_ATDP_CONNECTED, 2000);
                        } else if (state == BluetoothA2dp.STATE_DISCONNECTED) {
                            mHandler.sendEmptyMessage(MSG_ATDP_DISCONNECTED);
                        }
                    }
                };
                registerReceiver(receiver, filter);
            }
            if (mAudioManager.isBluetoothScoAvailableOffCall()) {
                mHandler.sendEmptyMessage(MSG_ATDP_CONNECTED);
                Log.d(TAG,
                        "===> startRecorder isBluetoothA2dpOn = "
                                + mAudioManager.isBluetoothA2dpOn()
                                + " isBluetoothScoOn  = "
                                + mAudioManager.isBluetoothScoOn());
                if (bluetoothStateReceiver == null && Utils.hasHoneycomb()) {
                    bluetoothStateReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if (intent != null) {
                                int state = intent.getIntExtra(
                                        AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                                Log.d(TAG, "===> audio sco state = " + state);
                                if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {
                                    Log.i(TAG,
                                            "AudioManager.SCO_AUDIO_STATE_CONNECTED");
                                    mAudioManager.setStreamMute(
                                            AudioManager.STREAM_VOICE_CALL,
                                            true);
                                    // mAudioManager.setMode(AudioManager.STREAM_MUSIC);
                                    if (bluetoothStateReceiver != null) {
                                        unregisterReceiver(bluetoothStateReceiver);
                                        bluetoothStateReceiver = null;
                                    }
                                }
                            }
                        }
                    };
                    registerReceiver(bluetoothStateReceiver, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
                }
            }
            mMediaRecorder.start();
            mWakeLock.acquire();
            setRecordStatus(true);
            notifyUpdate(MSG_START_RECORD);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "===> startRecorder error --- " + e.getMessage());
            stopRecord();
        } 
    }

    private void stopRecord() {
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        mHandler.sendEmptyMessage(MSG_ATDP_DISCONNECTED);
        if (bluetoothStateReceiver != null) {
            unregisterReceiver(bluetoothStateReceiver);
            bluetoothStateReceiver = null;
        }
        Log.d(TAG,
                "===>stopRecord isBluetoothA2dpOn = "
                        + mAudioManager.isBluetoothA2dpOn()
                        + " isBluetoothScoOn  = "
                        + mAudioManager.isBluetoothScoOn());
        if (mMediaRecorder != null) {
            RecorderFile file = new RecorderFile();
            try {
                file.setDuration((int) ((SystemClock.uptimeMillis() - mRecorderTime) / 1000));
                mMediaRecorder.stop();
            } catch (IllegalStateException e) {
                Log.e(TAG, "===> IllegalStateException.");
            }
            mMediaRecorder.release();
            mMediaRecorder = null;
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            file.setPath(mRecoderPath);
            file.setTime(startTime);
            file.setType(mCurMode);
            File f = new File(mRecoderPath);
            if(f.exists()){
                file.setSize(f.length());
            }
            if(mMimeType == SoundRecorder.FILE_TYPE_AMR) {
                file.setMimeType("amr");
            } else {
                file.setMimeType("3gpp");
            }
            fileManager.insertRecorderFile(file);
            setRecordStatus(false);
            notifyUpdate(MSG_STOP_RECORD);
        }
    }

    private void createDir(){
        int storage = mPreferences.getInt(SoundRecorder.PREFERENCE_TAG_STORAGE_LOCATION, SoundRecorder.STORAGE_LOCATION_DEFAULT);
        String storagePath = FileUtils.getExternalStoragePath(this);
        Log.i(TAG, "---> external storage path = " + storagePath);
        if(storagePath.length() == 0){
            storagePath = Environment.getExternalStorageDirectory().getPath();
            Log.i(TAG, "---> default path = " + storagePath);    
        }
        if(storage == SoundRecorder.STORAGE_LOCATION_SD_CARD){
            
        } else {
            
        }
        String parent = "/BlueRecorder/";
        String child = "Audio";
        mRecoderPath = storagePath + parent;
        if(!fileManager.isExists(mRecoderPath)){
            fileManager.createDiretory(mRecoderPath);
            fileManager.createFile(mRecoderPath+".nomedia");
        }
        mRecoderPath += child;
        if(!fileManager.isExists(mRecoderPath)){
            fileManager.createDiretory(mRecoderPath);
            for(int i=1;i<5;i++){
                fileManager.createDiretory(mRecoderPath+i);
            }
        }
        String pre = "";
        if(mPhoneState == TelephonyManager.CALL_STATE_OFFHOOK){
            pre = mIncommingNumber+"_";
        }else {
            if(mCurMode == LUNCH_MODE_AUTO){
                pre = getNamePrefix();
            }
        }
        if(mMimeType == SoundRecorder.FILE_TYPE_AMR){
            mRecoderPath += File.separator + pre + DateUtil.formatyyMMDDHHmmss(System.currentTimeMillis())+".mp3";
        } else {
            mRecoderPath += File.separator + pre + DateUtil.formatyyMMDDHHmmss(System.currentTimeMillis())+".3gp";
        }
        fileManager.createFile(mRecoderPath);
    }
    
    // recorder
    private MediaRecorder.OnErrorListener mRecorderErrorListener = new OnErrorListener() {

        @Override
        public void onError(MediaRecorder arg0, int arg1, int arg2) {
            Log.e(TAG, "  MediaRecorder Error: " + arg1 + "," + arg1);
            mAudioRecordStart = false;
            mHandler.sendEmptyMessage(MSG_STOP_RECORD);
        }
    };

    // talk
    private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            mAudioTalkStart = false;
            mHandler.sendEmptyMessage(MSG_STOP_RECORD);
            if (mOnCompletionListener != null) {
                mOnCompletionListener.onCompletion(mMediaPlayer);
            }
        }
    };

    // talk
    private MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
        public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
            Log.e(TAG, "  MediaPlayer Error: " + framework_err + "," + impl_err);
            mAudioTalkStart = false;
            mHandler.sendEmptyMessage(MSG_STOP_RECORD);
            return true;
        }
    };

    private void notifyUpdate(int msg) {
    	if(mCurMode == LUNCH_MODE_AUTO){
    		return;
    	}
        for (IStateListener listener : mStateSet) {
            try {
                listener.onStateChanged(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
    
    private Runnable mUpdateTimer = new Runnable() {
        public void run() {
            mHandler.postDelayed(mUpdateTimer, 1000);
            mHandler.sendEmptyMessage(MSG_UPDATE_TIMER);
        }
    };

    private void updateNotifiaction(){
        int icon = R.drawable.ic_launcher_soundrecorder;
        Notification notification = new Notification();
        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.recorder_notification);
        contentView.setImageViewResource(R.id.image, R.drawable.ic_launcher_soundrecorder);
        notification.contentView = contentView;
        notification.icon = icon;
        notification.flags |= Notification.FLAG_NO_CLEAR;
        notification.contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, SoundRecorder.class), 0);
        contentView.setTextViewText(R.id.title, getString(R.string.recording));
        if (mRecorderStart) {
            contentView.setTextViewText(R.id.text, getTimerString((int) ((SystemClock.uptimeMillis() - mRecorderTime) / 1000)));
        } else {
            contentView.setTextViewText(R.id.text, getTimerString(0));
        }
        mNotificationManager.notify(CUSTOM_VIEW_ID, notification);
    }
    
    
    private String getTimerString(int duration){
        int hour = duration/60/60;
        int minute = duration/60%60;
        int second = duration%60;
        timerInfo.delete(0, timerInfo.length());
        timerInfo.append(hour/10+ "" + hour%10  + ":" + minute/10 + "" + minute%10 +":" + second/10 + "" + second%10);
        return timerInfo.toString();
    }
    
    private void callStartRecord(){
        if(!isValidRecorderTime()){
            if(mCurMode == LUNCH_MODE_AUTO && !mRecorderStart){
                if(DebugConfig.DEBUG){
                    Log.i(TAG, "---> call in come , start recorder");
                }
                mHandler.sendEmptyMessage(MSG_START_RECORD);
            }
        }
    }
    
    private void callStopRecord(){
        if(!isValidRecorderTime()){
            if(mCurMode == LUNCH_MODE_AUTO && mRecorderStart){
                if(DebugConfig.DEBUG){
                    Log.i(TAG, "---> call over , stop recorder");
                }
                mHandler.sendEmptyMessage(MSG_STOP_RECORD);
            }
        }
    }
    
    private void processTimerAlarm(){
        acquireWakeLock();
        if(isValidRecorderTime()){//start
            if(DebugConfig.DEBUG){
                Log.i(TAG, "processTimerAlarm mCurMode = " + mCurMode + " mRecorderStart = " + mRecorderStart);
            }
            if(mCurMode == LUNCH_MODE_AUTO){
                if(mRecorderStart){
                    mHandler.sendEmptyMessage(MSG_STOP_RECORD);
                }
                mHandler.sendEmptyMessage(MSG_START_RECORD);
            }
        } else {
            if(mCurMode == LUNCH_MODE_AUTO && mRecorderStart){
                mHandler.sendEmptyMessage(MSG_STOP_RECORD);
            }
            if(isValidUploadTime() && !mPowerManager.isScreenOn() && NetworkUtil.isWifiDataEnable(this)){//start
                mUploadHandler.removeMessages(MSG_START_UPLOAD);
                mUploadHandler.sendEmptyMessage(MSG_START_UPLOAD);
            }
            if(isValidDeleteTime()){
                mUploadHandler.removeMessages(MSG_START_DELETE);
                mUploadHandler.sendEmptyMessage(MSG_START_DELETE);
            }
        }
        mTimeSchedule.setRtcTimerAlarm();
    }
    
    private boolean isValidRecorderTime(){
        Calendar rightNow = Calendar.getInstance();
        int dayOfHour = rightNow.get(Calendar.HOUR_OF_DAY);
        return dayOfHour>=mPreferences.getInt(SettingsActivity.KEY_RECORDER_START, AM) && dayOfHour<=mPreferences.getInt(SettingsActivity.KEY_RECORDER_END, PM);
    }
    
    private boolean isValidUploadTime(){
    	Calendar rightNow = Calendar.getInstance();
        int dayOfHour = rightNow.get(Calendar.HOUR_OF_DAY);
        return dayOfHour>=mPreferences.getInt(SettingsActivity.KEY_UPLOAD_START, UPLOAD_START) && dayOfHour<=mPreferences.getInt(SettingsActivity.KEY_UPLOAD_END, UPLOAD_END);
    }
    
    private boolean isValidDeleteTime(){
        Calendar rightNow = Calendar.getInstance();
        int dayOfHour = rightNow.get(Calendar.HOUR_OF_DAY);
        return dayOfHour>=DELETE_START && dayOfHour<=DELETE_END;
    }
    
    private void startUploadTask(){
    	List<RecorderFile> list = fileManager.queryPrivateFileList(0, PAGE_NUMBER);
		if(list.size()>0){
			RecorderFile file = list.get(0);
			initUploadFile(file.getId(), file.getPath());
		}else{
		    if(DebugConfig.DEBUG){
		        Log.w(TAG, "---> No Files.");
		    }
		}
    }
    
    private void startDeleteTask(){
        List<RecorderFile> list = fileManager.queryPrivateFileList(0, PAGE_NUMBER);
        if(list.size()>0){
            if(!mPowerManager.isScreenOn() && NetworkUtil.isWifiDataEnable(this)){//start upload try
                mUploadHandler.removeMessages(MSG_START_UPLOAD);
                mUploadHandler.sendEmptyMessage(MSG_START_UPLOAD);
            } else {
                RecorderFile file = list.get(0);
                if(fileManager.removeFile(file.getPath())){
                    fileManager.delete(file.getId());
                }
                mUploadHandler.removeMessages(MSG_START_DELETE);
                mUploadHandler.sendEmptyMessage(MSG_START_DELETE);
            }
        }else{
            if(DebugConfig.DEBUG){
                Log.w(TAG, "---> No Files.");
            }
        }
    }
    
    
    private void acquireWakeLock(){
        if (mAlarmWakeLock.isHeld()) {
            mAlarmWakeLock.release();
        }
        mAlarmWakeLock.acquire(2000);
    }
    
    private void initUploadFile(long id, String path){
        if(DebugConfig.DEBUG){
            Log.i(TAG, "initUploadFile id = " + id + " path = " + path);
        }
        File file = new File(path);
        MultipartEntityBuilder entitys = MultipartEntityBuilder.create();
        entitys.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        entitys.setCharset(Charset.forName(HTTP.UTF_8));

        entitys.addPart("file", new FileBody(file));
        HttpEntity httpEntity = entitys.build();
        //long totalSize = httpEntity.getContentLength();
        ProgressOutHttpEntity progressHttpEntity = new ProgressOutHttpEntity(
                httpEntity, new ProgressListener() {
                    @Override
                    public void transferred(long transferedBytes) {
                        // publishProgress((int) (100 * transferedBytes / totalSize));
                        mTransferedBytes = transferedBytes;
                    }
                });
        if(uploadFile(mPreferences.getString(SettingsActivity.KEY_UPLOAD_URL, SettingsActivity.DEFAULT_UPLOAD_URL), progressHttpEntity) == UploadResult.SUCCESS){
            File f = new File(path);
            f.delete();
            fileManager.delete(id);
            if(DebugConfig.DEBUG){
                Log.i(TAG, "upload success. path = " + path);
            }
        }
        mUploadHandler.removeMessages(MSG_START_UPLOAD);
        mUploadHandler.sendEmptyMessage(MSG_START_UPLOAD);
    }
    
    private int uploadFile(String url, ProgressOutHttpEntity entity) {
        int mResult;
        HttpClient httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(entity);
        mResult = UploadResult.PROCESS;
        try {
            HttpResponse httpResponse = httpClient.execute(httpPost);
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                if(!ProgressOutHttpEntity.mCancle){
                    mResult = UploadResult.SUCCESS;
                } else {
                    mResult = UploadResult.FAIL;
                }
            }
        } catch (Exception e) {
            mResult = UploadResult.FAIL;
            e.printStackTrace();
        } finally {
            if (httpClient != null && httpClient.getConnectionManager() != null) {
                httpClient.getConnectionManager().shutdown();
            }
            if(DebugConfig.DEBUG){
                Log.i(TAG, "upload size = " + mTransferedBytes);
            }
            mResult = UploadResult.IDLE;
        }
        return mResult;
    }
    
    private String getNamePrefix(){
        String mac = mPreferences.getString(SettingsActivity.KEY_MAC_ADDRESS, "");
        if(mac == null || mac.length() == 0){
            mac = getMacAddress(this);
        }
        if(mac == null || mac.length() == 0){
            mac = telephonyManager.getDeviceId();
        }
        if(mac == null || mac.length() == 0){
            return "_";
        }
        return mac + "_";
    }
    
    private String getMacAddress(Context context){
        String macAddress = "";
        WifiManager wifiMgr = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = (null == wifiMgr ? null : wifiMgr.getConnectionInfo());
        if (null != info) {
            macAddress = info.getMacAddress();
            if(macAddress != null && macAddress.length()>0){
                mPreferences.edit().putString(SettingsActivity.KEY_MAC_ADDRESS, macAddress+"_").commit();
            }
        }
        Log.d("TAG", "===> address = " + macAddress);
        return macAddress;
   }
    
    private class UploadHandlerCallback implements Handler.Callback{
        
        @Override
        public boolean handleMessage(Message msg) {
            switch(msg.what){
                case MSG_START_UPLOAD:
                    startUploadTask();
                    break;
                case MSG_START_DELETE:
                    startDeleteTask();
                    break;
                    default:
                        break;
            }
            return true;
        }
        
    }
}
