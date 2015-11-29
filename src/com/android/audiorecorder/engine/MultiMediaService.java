package com.android.audiorecorder.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SystemClock;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.android.audiorecorder.BuildConfig;
import com.android.audiorecorder.DebugConfig;
import com.android.audiorecorder.R;
import com.android.audiorecorder.dao.FileManagerFactory;
import com.android.audiorecorder.dao.IFileManager;
import com.android.audiorecorder.engine.ProgressOutHttpEntity.UploadResult;
import com.android.audiorecorder.provider.FileColumn;
import com.android.audiorecorder.provider.FileDetail;
import com.android.audiorecorder.provider.FileProvider;
import com.android.audiorecorder.ui.SettingsActivity;
import com.android.audiorecorder.ui.SoundRecorder;
import com.android.audiorecorder.utils.DateUtil;
import com.android.audiorecorder.utils.FileUtils;
import com.android.audiorecorder.utils.NetworkUtil;
import com.android.audiorecorder.utils.StringUtil;

public class MultiMediaService extends Service {
	
    public final static int STATE_IDLE = 0;
    public final static int STATE_BUSY = 1;
    public final static int STATE_PREPARE = 2;
    
    public int mAudioRecordState = STATE_IDLE;
    public int mVideoRecordState = STATE_IDLE;
    
    public final static int MIX_STORAGE_CAPACITY = 100;//MB
    public static final int MAX_RECORDER_DURATION = 10 * 60;
	private static final String IMAGE_CACHE_DIR = "DCIM";
	
	public final static int PAGE_NUMBER = 1;
    private static final int AM = 21;
    private static final int PM = 23;
    
    private static final int UPLOAD_START = 2;
    private static final int UPLOAD_END = 2;
    
    private static final int DELETE_START = 13;
    private static final int DELETE_END = 13;
    
    public static final int LUNCH_MODE_IDLE = 0;
    public static final int LUNCH_MODE_CALL = 1;
    public static final int LUNCH_MODE_MANLY = 2;//no allowed time and tel to recorder
    public static final int LUNCH_MODE_AUTO = 3;
    private int mCurMode;
    private boolean isScreenOn;
    
    public static final String Action_Audio_Record = "com.audio.Action_AudioRecord";
    public static final String Action_Video_Record = "com.audio.Action_VideoRecord";
    
    private int bufferSizeInBytes = 0;
    private AudioRecord mAudioRecord;
    
    public final static int AUDIO_SAMPLE_RATE = 44100;
    private static final int RECORDER_BPP = 16;
    private int mBatteryLevel;
    private final static int MIN_BATTERY_LEVEL = 40;//%

    public static final int MSG_START_RECORD = 0xE1;
    public static final int MSG_STOP_RECORD = 0xE2;
    public static final int MSG_START_AUDIO_RECORD = 0xE3;
    public static final int MSG_STOP_AUDIO_RECORD = 0xE4;

    public final static int MSG_UPDATE_TIMER = 200;
    public final static int MSG_START_TIMER = 201;
    
    public final static int MSG_START_UPLOAD = 2000;
    
    public final static int MSG_START_DELETE = 3000;
    
    private static final int MSG_BLUETOOTH_START_SCO = 4000;
    private static final int MSG_BLUETOOTH_STOP_SCO = 4001;
    private static final int MSG_BLUETOOTH_PROFILE_MATCH = 4002;

    //guard camera msg
    private static final int MSG_INIT_CAMERA = 5001;
    private static final int MSG_TAKE_PICTURE = 5002;
    private static final int MSG_RELEASE_CAMERA = 5003;
    
    private boolean mIsBluetoothConnected;
    private boolean mAtdpEnable;
    private WakeLock mRecorderWakeLock;
    private WakeLock mUploadWakeLock;
    private MediaRecorder mMediaRecorder = null;

    private String TAG = "MultiMediaService";

    private Set<IAudioStateListener> mStateSet = new HashSet<IAudioStateListener>();

    private AudioManager mAudioManager;
    private PowerManager mPowerManager;
    private int mRingerMode;

    private boolean mAudioRecorderStart;
    private boolean mRecorderStart;
    private boolean mTalkStart;
    private long mAudioRecorderDuration;
    private long mVideoRecorderDuration;
    private long mTalkTime;

    private BroadcastReceiver mStateChnageReceiver = null;
    
    private SharedPreferences mPreferences;
    private NotificationManager mNotificationManager;
    private int CUSTOM_VIEW_ID = R.layout.recorder_notification;
    private StringBuffer timerInfo = new StringBuffer();
    
    private IFileManager fileManager;
    private String mRecoderPath;
    private long mRecordStartTime;
    private int mMimeType;
    
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private int mPhoneState;
    private String mIncommingNumber;
    
    private UploadHandlerCallback mUploadHandlerCallback;
    private HandlerThread mUpHandlerThread;
    private Handler mUploadHandler;
    
    private long mTransferedBytes;
    
    private BluetoothA2dp mService;
    private BluetoothHeadset mBluetoothHeadset;
    private BluetoothHeadsetListener mBluetoothHeadsetListener = new BluetoothHeadsetListener();
    
    //guard camera
    private GuardCameraManager mGuardCamera;
    //private Camera camera;
    private GuardCameraSurfaceTexture mCameraSurfaceTexture;
    
    //audio handler thread
    private MediaAudioHandlerCallBack mMediaAudioHandlerCallback;
    private HandlerThread mMediaAudioHandlerThread;
    private Handler mMediaAudioHandler;
    
    //video handler thread
    private MediaVideoHandlerCallBack mMediaVideoHandlerCallback;
    private HandlerThread mMediaVideoHandlerThread;
    private Handler mMediaVideoHandler;
    
    private Thread mAudioRecordThread;
    
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_TIMER:
                    notifyRecordState(MSG_UPDATE_TIMER);//only update recorder time on UI
                    updateNotifiaction();
                    break;
                case MSG_BLUETOOTH_PROFILE_MATCH:
                    if (mService != null) {
                        List<BluetoothDevice> devs = getConnectedDevices();
                        if(DebugConfig.DEBUG) {
                            Log.d(TAG, "---> buletooth connected number = " + devs.size());
                        }
                    }
                    break;
                case MSG_BLUETOOTH_START_SCO:
                    if (mIsBluetoothConnected) {
                       mAudioManager.startBluetoothSco();
                       mAudioManager.setMode(AudioManager.MODE_IN_CALL);
                       Log.d(TAG, "---> start bluetooth sco : " + mAudioManager.isBluetoothScoOn());
                    }
                  break;

              case MSG_BLUETOOTH_STOP_SCO:
                  if (mIsBluetoothConnected) {
                      mAudioManager.stopBluetoothSco();
                      mAudioManager.setBluetoothScoOn(false);
                      mAudioManager.setMode(AudioManager.MODE_NORMAL);
                  }
                  break;
              case MSG_START_TIMER:
            	  mHandler.sendEmptyMessageDelayed(MSG_START_TIMER, MAX_RECORDER_DURATION*1000);
            	  processAutoTimerAlarm();
            	  break;
                default:
                    break;
            }
        };
    };

    @Override
    public IBinder onBind(Intent intent) {
        if (Action_Audio_Record.equalsIgnoreCase(intent.getAction())) {
            return iAudioService;
        } else if (Action_Video_Record.equalsIgnoreCase(intent.getAction())) {
            return iVideoService;
        }
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        isScreenOn = mPowerManager.isScreenOn();
        mGuardCamera = new GuardCameraManager(this);
        this.mPreferences = getSharedPreferences(SettingsActivity.class.getName(), Context.MODE_PRIVATE);
        mRecorderWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MutilMediaService");
        mUploadWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "UploadWakeLock");
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mCurMode = LUNCH_MODE_MANLY;
        IntentFilter filter = new IntentFilter();
        filter.addAction(TimeSchedule.ACTION_TIMER_ALARM);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        if (mStateChnageReceiver == null) {
            mStateChnageReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    final String action = intent.getAction();
                    if(TimeSchedule.ACTION_TIMER_ALARM.equalsIgnoreCase(action)){
                        Log.i(TAG, "---> alarm.");
                    } else if(Intent.ACTION_USER_PRESENT.equalsIgnoreCase(action)){//user login, screen on
                        isScreenOn = true;
                        //mUploadHandler.sendEmptyMessage(MSG_INIT_CAMERA);
                        if(mCurMode == LUNCH_MODE_AUTO){//stop record
                            Message msgStop = mMediaAudioHandler.obtainMessage(MSG_STOP_RECORD);
                            msgStop.arg1 = mCurMode;
                            mMediaAudioHandler.sendMessage(msgStop);
                            
                            mHandler.removeMessages(MSG_START_TIMER);
                            Log.i(TAG, "---> user present.");
                        }
                    }/* else if (action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
                        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
                        if(state == BluetoothAdapter.STATE_CONNECTED){
                             mIsBluetoothConnected = true;
                             BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                             if(bluetoothAdapter != null){
                                  bluetoothAdapter.getProfileProxy(MultiMediaService.this, mBluetoothHeadsetListener, BluetoothProfile.A2DP);
                                  bluetoothAdapter.getProfileProxy(MultiMediaService.this, mBluetoothHeadsetListener, BluetoothProfile.HEADSET);
                             }
                        } else if(state == BluetoothAdapter.STATE_DISCONNECTED || state == BluetoothAdapter.STATE_DISCONNECTING){
                             mAtdpEnable =  false;
                             mIsBluetoothConnected = false;
                             mAudioManager.stopBluetoothSco();
                             mAudioManager.setBluetoothScoOn(false);
                             mAudioManager.setMode(AudioManager.MODE_NORMAL);
                             Log.d(TAG,"==> recv bluetooth connected  state = " + state + " atdp enable = " + mAtdpEnable);
                        }
                    }*/ else if(action.equals(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)) {
                        int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_ERROR);
                        Log.d(TAG,"===> bluetooth sco state = " + state + " mIsBluetoothConnected = " + mIsBluetoothConnected + " mAtdpEnable = " + mAtdpEnable);
                        if(state == AudioManager.SCO_AUDIO_STATE_CONNECTED){
                             mAudioManager.setBluetoothScoOn(true);
                        } else if(state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED){
                             if(mAudioManager.isBluetoothScoOn()) {
                                 mAudioManager.stopBluetoothSco();
                                   mAudioManager.setBluetoothScoOn(false);
                                   mAudioManager.setMode(AudioManager.MODE_NORMAL);
                             }
                             if(mIsBluetoothConnected && !mAtdpEnable){
                                   Log.d(TAG, "===> start reconnected bluetooth sco.");
                                   mHandler.removeMessages(MSG_BLUETOOTH_START_SCO);
                                   mHandler.sendEmptyMessageDelayed(MSG_BLUETOOTH_START_SCO, 1500);
                             }
                        }
                    } else if(action.equals(Intent.ACTION_BATTERY_CHANGED)){
                        mBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                        Log.i(TAG, "--> mBatteryLevel = " + mBatteryLevel);
                    } else if(action.equals(Intent.ACTION_SCREEN_OFF)){
                        isScreenOn = false;
                        mHandler.removeMessages(MSG_START_TIMER);
                        mHandler.sendEmptyMessageDelayed(MSG_START_TIMER, 30000);
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
                        callStopRecord(LUNCH_MODE_CALL);
                    }else if(state == TelephonyManager.CALL_STATE_OFFHOOK){
                        //start recorder
                        mIncommingNumber = incomingNumber;
                        if(mCurMode != LUNCH_MODE_IDLE){
                            callStopRecord(mCurMode);
                        }
                        if(DebugConfig.DEBUG){
                            Log.d(TAG, "---> start recorder mIncommingNumber = " + mIncommingNumber);
                        }
                        callStartRecord(LUNCH_MODE_CALL);
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
        fileManager = FileManagerFactory.getFileManagerInstance(this);
        init();
        mCurMode = LUNCH_MODE_IDLE;
        Log.d(TAG, "===> onCreate. screen state : " + isScreenOn);
    }

    private IAudioService.Stub iAudioService = new IAudioService.Stub() {

        @Override
        public void startRecord() throws RemoteException {
            Log.i(TAG, "===> startRecorder");
            mMediaAudioHandler.removeMessages(MSG_START_RECORD);
            Message msg = mMediaAudioHandler.obtainMessage(MSG_START_RECORD);
            msg.arg1 = LUNCH_MODE_MANLY;
            mMediaAudioHandler.sendMessage(msg);
            mHandler.removeCallbacks(mUpdateTimer);
            mHandler.post(mUpdateTimer);
        }

        @Override
        public void stopRecord() throws RemoteException {
            Log.d(TAG, "===> stopRecorder");
            mMediaAudioHandler.removeMessages(MSG_STOP_RECORD);
            Message msg = mMediaAudioHandler.obtainMessage(MSG_STOP_RECORD);
            msg.arg1 = LUNCH_MODE_MANLY;
            mMediaAudioHandler.sendMessage(msg);
            mHandler.removeCallbacks(mUpdateTimer);
            mNotificationManager.cancel(CUSTOM_VIEW_ID);
        }

        @Override
        public int getRecorderTime() throws RemoteException {
            return getAudioRecordDuration();
        }

        @Override
        public int getTalkTime() throws RemoteException {
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
        public boolean isTalkStart() throws RemoteException {
            return mTalkStart;
        }

        public void setMode(int mode) throws RemoteException {
            if(mCurMode == LUNCH_MODE_AUTO){//reset state machine
                mHandler.removeMessages(MSG_STOP_RECORD);
                Message msg = mHandler.obtainMessage(MSG_STOP_RECORD);
                msg.arg1 = mode;
                mHandler.sendMessage(msg);
            }
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
        public void regStateListener(IAudioStateListener listener)
                throws RemoteException {
            if (listener != null) {
                mStateSet.add(listener);
                Log.d(TAG, "===> IRecordListener regStateListener");
            }
        }

        @Override
        public void unregStateListener(IAudioStateListener listener)
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
        
        @Override
        public long checkDiskCapacity() throws RemoteException {
            int where = mPreferences.getInt(SoundRecorder.PREFERENCE_TAG_STORAGE_LOCATION, SoundRecorder.STORAGE_LOCATION_SD_CARD);
            return FileUtils.avliableDiskSize(MultiMediaService.this, where);
        }
    };

    private IVideoService.Stub iVideoService = new IVideoService.Stub() {

        @Override
        public int getRecorderTime() throws RemoteException {
            if (mRecorderStart) {
                return (int) ((SystemClock.elapsedRealtime() - mVideoRecorderDuration) / 1000);
            }
            return 0;
        }

        @Override
        public boolean isRecorderStart() throws RemoteException {
            return mRecorderStart;
        }

        public void setMode(int mode) throws RemoteException {
            if(mCurMode == LUNCH_MODE_AUTO){//reset state machine
                mHandler.removeMessages(MSG_STOP_RECORD);
                Message msg = mHandler.obtainMessage(MSG_STOP_RECORD);
                msg.arg1 = mode;
                mHandler.sendMessage(msg);
            }
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
        public void adjustStreamVolume(int streamType, int direct, int flag)
                throws RemoteException {
            mAudioManager.adjustStreamVolume(streamType, direct, flag);
        }
        
        @Override
        public long checkDiskCapacity() throws RemoteException {
            int where = mPreferences.getInt(SoundRecorder.PREFERENCE_TAG_STORAGE_LOCATION, SoundRecorder.STORAGE_LOCATION_SD_CARD);
            return FileUtils.avliableDiskSize(MultiMediaService.this, where);
        }

        @Override
        public int startVideoRecord() throws RemoteException {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public int stopVideoRecord() throws RemoteException {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public int videoCapture() throws RemoteException {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public int videoSnap() throws RemoteException {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void regStateListener(IVideoStateListener listener)
                throws RemoteException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void unregStateListener(IVideoStateListener listener)
                throws RemoteException {
            // TODO Auto-generated method stub
            
        }
    };
    
    private void setRecordStatus(boolean start) {
        mRecorderStart = start;
        if (start) {
            mAudioRecorderDuration = SystemClock.elapsedRealtime();
        }
        notifyRecordState(mAudioRecordState);
    }

    private void startRecorder(int mode) {
        mMimeType = mPreferences.getInt(SoundRecorder.PREFERENCE_TAG_FILE_TYPE, SoundRecorder.FILE_TYPE_3GPP);
        //mRecoderPath = FileUtils.generalFilePath(this, mode, SoundRecorder.STORAGE_LOCATION_SD_CARD, mMimeType, mIncommingNumber, getNamePrefix());
        String parentPath = FileUtils.getPathByModeAndType(this, mode, FileProvider.FILE_TYPE_AUDIO);
        if(!checkStatus(parentPath, mode)){
            return;
        }
        if(DebugConfig.DEBUG){
            Log.i(TAG, "---> General Recorder Path = " + parentPath);
        }
        mAudioRecordState = STATE_PREPARE;
        String fileName = getFileNameByType(mode, mMimeType);
        mRecoderPath = parentPath + File.separator + fileName;
        fileManager.createFile(mRecoderPath);
        Log.i(TAG, "---> startRecorder mode = " + mode + "  mMimeType= " + mMimeType + " name = " + fileName);
        if(!mRecorderWakeLock.isHeld()){
            mRecorderWakeLock.acquire();
        }
        mHandler.sendEmptyMessage(MSG_BLUETOOTH_START_SCO);
        if(mode == LUNCH_MODE_MANLY && mMimeType == SoundRecorder.FILE_TYPE_WAV){
            mMediaAudioHandler.removeMessages(MSG_START_AUDIO_RECORD);
            Message msg = mMediaAudioHandler.obtainMessage(MSG_START_AUDIO_RECORD);
            msg.arg1 = mode;
            mMediaAudioHandler.sendMessage(msg);
        } else {
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
            try {
                mMediaRecorder.prepare();
                mMediaRecorder.start();
                mCurMode = mode;
                mAudioRecordState = STATE_BUSY;
                setRecordStatus(true);
                Log.i(TAG, "---> startRecorder mCurMode = " + mCurMode);
            } catch (IOException e) {
                e.printStackTrace();
                mAudioRecordState = STATE_IDLE;
            } catch(IllegalStateException e){
                mAudioRecordState = STATE_IDLE;
                Log.w(TAG, "---> IllegalStateException : " + e.getMessage());
            }
        }
    }

    private void stopRecord(int mode) {
        Log.d(TAG, "---> stopRecord mode = " + mode + "  mMimeType= " + mMimeType);
        if(mode == LUNCH_MODE_IDLE){
            mAudioRecordState = STATE_IDLE;
            return;
        }
        if(mode == LUNCH_MODE_MANLY && mMimeType == SoundRecorder.FILE_TYPE_WAV){
            mMediaAudioHandler.sendEmptyMessage(MSG_STOP_AUDIO_RECORD);
        } else {
            if (mMediaRecorder != null) {
                mMediaRecorder.stop();
                mMediaRecorder.release();
                mMediaRecorder = null;
                int duration = getAudioRecordDuration();
                saveRecordFile(mode, duration);
                mAudioRecordState = STATE_IDLE;
                setRecordStatus(false);
            }
            mCurMode = LUNCH_MODE_IDLE;
        }
        mHandler.sendEmptyMessage(MSG_BLUETOOTH_STOP_SCO);
        if (mRecorderWakeLock.isHeld()) {
            mRecorderWakeLock.release();
        }
    }
  
    // recorder
    private MediaRecorder.OnErrorListener mRecorderErrorListener = new OnErrorListener() {

        @Override
        public void onError(MediaRecorder arg0, int arg1, int arg2) {
            Log.e(TAG, "  MediaRecorder Error: " + arg1 + "," + arg1);
            Message msg = mHandler.obtainMessage(MSG_STOP_RECORD);
            msg.arg1 = mCurMode;
            mHandler.sendMessage(msg);
        }
    };

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
    
    private Runnable mUpdateTimer = new Runnable() {
        public void run() {
            mHandler.sendEmptyMessage(MSG_UPDATE_TIMER);
            mHandler.postDelayed(mUpdateTimer, 1000);
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
        contentView.setTextViewText(R.id.text, getTimerString(getAudioRecordDuration()));
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
    
    private void callStartRecord(int mode){
        if(DebugConfig.DEBUG){
            Log.i(TAG, "---> call in come , start recorder");
        }
        mMediaAudioHandler.sendEmptyMessage(MSG_STOP_RECORD);
        Message msg = mMediaAudioHandler.obtainMessage(MSG_START_RECORD);
        msg.arg1 = mode;
        mMediaAudioHandler.sendMessage(msg);
    }
    
    private void callStopRecord(int mode){
        if(DebugConfig.DEBUG){
            Log.i(TAG, "---> call over , stop recorder");
        }
        Message msg = mMediaAudioHandler.obtainMessage(MSG_STOP_RECORD);
        msg.arg1 = mode;
        mMediaAudioHandler.sendMessage(msg);
    }
    
    private void processAutoTimerAlarm(){
        if(!isScreenOn){//srceen off
            if(DebugConfig.DEBUG){
                Log.i(TAG, "--> isValidRecorderTime : " + isValidAudoRecorderTime() + " BatteryLevel : " + mBatteryLevel + " isValidDeleteTime : " + isValidDeleteTime());
            }
            /**
             * check is valid recorder time and batter level is enough
             */
            if(isValidAudoRecorderTime() && mBatteryLevel>=MIN_BATTERY_LEVEL){//start
                if(DebugConfig.DEBUG){
                    Log.i(TAG, "processTimerAlarm mCurMode = " + mCurMode + " mRecorderStart = " + mRecorderStart);
                }
                /**
                 * if curmode is idle, start record with auto mode
                 */
                if(mCurMode == LUNCH_MODE_IDLE){
                    mMediaAudioHandler.removeMessages(MSG_START_RECORD);
                    Message msg = mMediaAudioHandler.obtainMessage(MSG_START_RECORD);
                    msg.arg1 = LUNCH_MODE_AUTO;
                    mMediaAudioHandler.sendMessage(msg);
                }
                /**
                 * if curmode is auto, stop record and restart with auto mode
                 */
                if(mCurMode == LUNCH_MODE_AUTO){
                    mMediaAudioHandler.removeMessages(MSG_STOP_RECORD);
                    Message msg = mMediaAudioHandler.obtainMessage(MSG_STOP_RECORD);
                    msg.arg1 = LUNCH_MODE_AUTO;
                    mMediaAudioHandler.sendMessage(msg);
                    mMediaAudioHandler.removeMessages(MSG_START_RECORD);
                    Message msg2 = mMediaAudioHandler.obtainMessage(MSG_START_RECORD);
                    msg2.arg1 = LUNCH_MODE_AUTO;
                    mMediaAudioHandler.sendMessage(msg2);
                }
            } else {// stop auto recorder
                if(mCurMode == LUNCH_MODE_AUTO){
                    Log.i(TAG, "---> auto stop.");
                    mMediaAudioHandler.removeMessages(MSG_STOP_RECORD);
                    Message msg = mMediaAudioHandler.obtainMessage(MSG_STOP_RECORD);
                    msg.arg1 = LUNCH_MODE_AUTO;
                    mMediaAudioHandler.sendMessage(msg);
                }
                if(NetworkUtil.isWifiDataEnable(this)){//start upload
                    mUploadHandler.removeMessages(MSG_START_UPLOAD);
                    mUploadHandler.sendEmptyMessage(MSG_START_UPLOAD);
                }
                /*if(isValidDeleteTime()){
                    mUploadHandler.removeMessages(MSG_START_UPLOAD);
                    mUploadHandler.removeMessages(MSG_START_DELETE);
                    mUploadHandler.sendEmptyMessage(MSG_START_DELETE);
                }*/
            }
        }
    }
    
    private boolean isValidAudoRecorderTime(){
        Calendar rightNow = Calendar.getInstance();
        int dayOfHour = rightNow.get(Calendar.HOUR_OF_DAY);
        return dayOfHour>=mPreferences.getInt(SettingsActivity.KEY_RECORDER_START, AM) && dayOfHour<=mPreferences.getInt(SettingsActivity.KEY_RECORDER_END, PM);
    }
    
    private boolean isValidDeleteTime(){
        Calendar rightNow = Calendar.getInstance();
        int dayOfHour = rightNow.get(Calendar.HOUR_OF_DAY);
        return dayOfHour>=DELETE_START && dayOfHour<=DELETE_END;
    }
    
    /*private void startUploadTask(){
    	List<RecorderFile> list = fileManager.queryPrivateFileList(RecorderFile.MEDIA_TYPE_AUDIO, 0, PAGE_NUMBER);
		if(list.size()>0){
			RecorderFile file = list.get(0);
			if(!mUploadWakeLock.isHeld()){
				mUploadWakeLock.acquire();
			}
			initUploadFile(file.getId(), file.getPath(), file.getLaunchType());
		}else{
			if(mUploadWakeLock.isHeld()){
				mUploadWakeLock.release();
			}
		    if(DebugConfig.DEBUG){
		        Log.w(TAG, "---> No Files Upload.");
		    }
		}
    }*/
    
    /*private void startDeleteTask(){
        List<RecorderFile> list = fileManager.queryPrivateFileList(RecorderFile.MEDIA_TYPE_AUDIO, 0, PAGE_NUMBER);
        if(list.size()>0){
        	if(DebugConfig.DEBUG){
				Log.i(TAG, "---> startDeleteTask  = " + mPowerManager.isScreenOn() + "  wifi state = " + NetworkUtil.isWifiDataEnable(this));
			}
            if(!mPowerManager.isScreenOn() && NetworkUtil.isWifiDataEnable(this)){//start upload try
                mUploadHandler.removeMessages(MSG_START_UPLOAD);
                mUploadHandler.sendEmptyMessage(MSG_START_UPLOAD);
            } else {
                RecorderFile file = list.get(0);
                if(file.getLaunchType() == LUNCH_MODE_AUTO){
                	if(fileManager.removeFile(file.getPath())){
                		fileManager.delete(RecorderFile.MEDIA_TYPE_AUDIO, file.getId());
                	}
                }
                mUploadHandler.removeMessages(MSG_START_DELETE);
                mUploadHandler.sendEmptyMessage(MSG_START_DELETE);
            }
        }else{
            if(DebugConfig.DEBUG){
                Log.w(TAG, "---> No Files Delete.");
            }
        }
    }*/
    
    /*private void initUploadFile(long id, String path, int type){
        if(DebugConfig.DEBUG){
            Log.i(TAG, "initUploadFile id = " + id + " path = " + path + " type = " + type);
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
        
        int result = uploadFile(mPreferences.getString(SettingsActivity.KEY_UPLOAD_URL, SettingsActivity.DEFAULT_UPLOAD_URL), progressHttpEntity);
        if(DebugConfig.DEBUG){
            Log.i(TAG, "---> uploadFile result = " + result);
        }
        if(result == UploadResult.SUCCESS){
        	if(type == LUNCH_MODE_AUTO){
        		File f = new File(path);
        		f.delete();
        	}
            fileManager.delete(RecorderFile.MEDIA_TYPE_AUDIO, id);
            if(DebugConfig.DEBUG){
                Log.i(TAG, "upload success. path = " + path);
            }
            mUploadHandler.removeMessages(MSG_START_UPLOAD); //wait for next callback
            mUploadHandler.sendEmptyMessageDelayed(MSG_START_UPLOAD, 1000);
        }
    }*/
    
    private int uploadFile(String url, ProgressOutHttpEntity entity) {
        int mResult;
        HttpClient httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(entity);
        mResult = UploadResult.PROCESS;
        try {
        	if(DebugConfig.DEBUG){
                Log.i(TAG, "---> start upload url = " + url);
            }
            HttpResponse httpResponse = httpClient.execute(httpPost);
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                mResult = UploadResult.SUCCESS;
            } else {
            	mResult = UploadResult.FAIL;
            }
            if(DebugConfig.DEBUG){
                Log.i(TAG, "upload result = " + mResult);
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
        }
        return mResult;
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
   
    private List<BluetoothDevice> getConnectedDevices() {
        return mService.getDevicesMatchingConnectionStates(new int[] {BluetoothProfile.STATE_CONNECTED});
    }
    
    private void startAudioRecord(final int mode) { 
        if(mAudioRecordThread == null || mAudioRecordThread.isInterrupted()){
            mAudioRecordThread = new Thread(new Runnable() {
                
                @Override
                public void run() {
                    FileOutputStream fos = null; 
                    try{
                        mCurMode = mode;
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
                        setRecordStatus(true);
                        writeDataToFile(fos);
                        insertWaveTitle(mode);
                    } catch(IllegalStateException e){
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        setRecordStatus(false);
                        mAudioRecorderStart = false;
                        mAudioRecordState = STATE_IDLE;
                        notifyRecordState(mAudioRecordState);
                        if(fos != null){
                            try {
                                fos.flush();
                                fos.close();// ¹Ø±ÕÐ´ÈëÁ÷
                                fos = null;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if(mAudioRecord != null){
                            mAudioRecord.stop();
                            mAudioRecord = null;
                        }
                    }
                }
            });
            mAudioRecordThread.start();
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
            Uri uri = getContentResolver().insert(FileProvider.AUDIOS_URI, values);
            Log.w(TAG, "---> add new file.");
            if(mode == LUNCH_MODE_AUTO){
                long id = ContentUris.parseId(uri);
                ContentValues valuesNew = new ContentValues();
                valuesNew.put(FileColumn.COLUMN_UP_OR_DOWN, FileColumn.FILE_UP_LOAD);
                valuesNew.put(FileColumn.COLUMN_UP_DOWN_LOAD_STATUS, FileColumn.STATE_FILE_UP_DOWN_WAITING);
                valuesNew.put(FileColumn.COLUMN_ID, id);
                getContentResolver().insert(FileProvider.TASK_URI, valuesNew);
            }
        }
    }
   
    private class UploadHandlerCallback implements Handler.Callback{
        
        @Override
        public boolean handleMessage(Message msg) {
            switch(msg.what){
                case MSG_START_UPLOAD:
                    //startUploadTask();
                    break;
                case MSG_START_DELETE:
                    //startDeleteTask();
                    break;
                case MSG_INIT_CAMERA:
                    System.out.println("front = " + mGuardCamera.checkCameraHardware(PackageManager.FEATURE_CAMERA_FRONT));
                    if(!mGuardCamera.checkCameraHardware(PackageManager.FEATURE_CAMERA_FRONT)){
                        //has front camera
                        if(BuildConfig.DEBUG){
                            Log.d(TAG, "---> platform has front camera.");
                        }
                    }
                    initCamera();
                    break;
                case MSG_TAKE_PICTURE:
                    tackPicture();
                    break;
                case MSG_RELEASE_CAMERA:
                    releaseCamera();
                    break;
                    default:
                        break;
            }
            return true;
        }
    }
    
    private void initCamera(){
        mGuardCamera.getCameraInstance(CameraInfo.CAMERA_FACING_BACK);
        mGuardCamera.getCameraInfo();
        mGuardCamera.startPreview(CameraInfo.CAMERA_FACING_BACK, mAutoFocusCallback);
        mRingerMode = mAudioManager.getRingerMode();
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        System.out.println("orientation = " + display.getRotation());
        if(BuildConfig.DEBUG){
            Log.i(TAG, "---> mRingerMode = " + mRingerMode);
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
            File catchPath = FileUtils.getDiskCacheDir(MultiMediaService.this, IMAGE_CACHE_DIR);
            if(!catchPath.exists()){
                catchPath.mkdirs();
            }
            String path = catchPath.getPath() + File.separator + getNamePrefix() + DateUtil.formatyyMMDDHHmmss(System.currentTimeMillis())+".jpg";
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
                getContentResolver().insert(FileProvider.JPEGS_URI, values);      
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mUploadHandler.sendEmptyMessage(MSG_RELEASE_CAMERA);
                mAudioManager.setRingerMode(mRingerMode);
            }
        }
    };
    
    private void init(){
        mUploadHandlerCallback = new UploadHandlerCallback();
        mUpHandlerThread = new HandlerThread("upload_thread", HandlerThread.MAX_PRIORITY);
        mUpHandlerThread.start();
        mUploadHandler = new Handler(mUpHandlerThread.getLooper(), mUploadHandlerCallback);
        
        mMediaAudioHandlerThread = new HandlerThread("MediaAudioThread", HandlerThread.MAX_PRIORITY);
        mMediaVideoHandlerThread = new HandlerThread("MediaVideoThread", HandlerThread.MAX_PRIORITY);
        mMediaAudioHandlerThread.start();
        mMediaVideoHandlerThread.start();
        mMediaAudioHandlerCallback = new MediaAudioHandlerCallBack();
        mMediaVideoHandlerCallback = new MediaVideoHandlerCallBack();
        mMediaAudioHandler = getHandler(mMediaAudioHandlerThread.getLooper(), mMediaAudioHandlerCallback);
        mMediaVideoHandler = getHandler(mMediaVideoHandlerThread.getLooper(), mMediaVideoHandlerCallback);
    }
    
    private Handler getHandler(Looper looper, Handler.Callback callback) {
        return new Handler(looper, callback){
             public void handleMessage(android.os.Message msg) {
                 switch (msg.what) {
                     default:
                         break;
                 }
             }
         };
    }
    
    private class MediaAudioHandlerCallBack implements Handler.Callback{

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
                    if(mAudioRecorderStart){
                        mAudioRecorderStart = false;
                    }
                    Log.i(TAG, "---> audio record stop. ");
                    if(mAudioRecordThread != null){
                        mAudioRecordThread.interrupt();
                        mAudioRecordThread = null;
                    }
                    break;
                    default:
                        break;
            }
            return false;
        }
        
    }
    
    private class MediaVideoHandlerCallBack implements Handler.Callback{

        @Override
        public boolean handleMessage(Message arg0) {
            return false;
        }
        
    }
    
    private class BluetoothHeadsetListener implements BluetoothProfile.ServiceListener {

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if(profile == BluetoothProfile.A2DP){
                mAtdpEnable = true;
                mService = (BluetoothA2dp) proxy;
                List<BluetoothDevice> list = mService.getConnectedDevices();
                Log.d(TAG, "a2dp list size =  " + list);
                /*for(BluetoothDevice device:list){
                    boolean state = mService.isA2dpPlaying(device);
                    Log.d(TAG, "a2dp state = " + state);
                }*/
            } else if(profile == BluetoothProfile.HEADSET){
                mAtdpEnable = false;
                mBluetoothHeadset =  (BluetoothHeadset) proxy;
                List<BluetoothDevice> list = mBluetoothHeadset.getConnectedDevices();
                Log.d(TAG, "no a2dp list size =  " + list);
                for(BluetoothDevice device:list){
                    BluetoothClass bluetoothClass = device.getBluetoothClass();
                    String bluetoothDeviceClass = bluetoothClass.toString();
                    boolean isAudioConnected = mBluetoothHeadset.isAudioConnected(device);
                    Log.d(TAG, "isAudioConnected = " + isAudioConnected + " bluetoothDeviceClass = " + bluetoothDeviceClass);
                }
            }
            mHandler.removeMessages(MSG_BLUETOOTH_PROFILE_MATCH);
            mHandler.sendEmptyMessageDelayed(MSG_BLUETOOTH_PROFILE_MATCH, 500);
        }

        @Override
        public void onServiceDisconnected(int profile) {
            
        }
        
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
            mac = getMacAddress(this);
        }
        if(mac == null || mac.length() == 0){
            mac = telephonyManager.getDeviceId();
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
        return mac + "_";
    }
    
    private boolean checkStatus(String parentPath, int mode){
        if(parentPath == null || parentPath.length() == 0){
            Log.w(TAG, "---> general file path failed.");
            mAudioRecordState = STATE_IDLE;
            if(mode == LUNCH_MODE_MANLY){
                Toast.makeText(this, getText(R.string.error_make_record_path), Toast.LENGTH_SHORT).show();
                notifyRecordState(mAudioRecordState);
            }
            return false;
        }
        long availableSpace = FileUtils.getAvailableSize(parentPath);
        if(availableSpace<MultiMediaService.MIX_STORAGE_CAPACITY){
            Log.w(TAG, "---> no more space to store files.");
            mAudioRecordState = STATE_IDLE;
            if(mode == LUNCH_MODE_MANLY){
                Toast.makeText(this, getText(R.string.error_no_more_space), Toast.LENGTH_SHORT).show();
                notifyRecordState(mAudioRecordState);
            }
            return false;
        }
        return true;
    }
    
    private int getAudioRecordDuration(){
        if(mRecorderStart){
            return (int)((SystemClock.elapsedRealtime() - mAudioRecorderDuration)/1000l);
        }
        return 0;
    }
}
