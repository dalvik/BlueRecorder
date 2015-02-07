package com.android.audiorecorder.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.opengl.GLES11Ext;
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
import android.view.TextureView.SurfaceTextureListener;
import android.widget.RemoteViews;

import com.android.audiorecorder.BuildConfig;
import com.android.audiorecorder.DebugConfig;
import com.android.audiorecorder.R;
import com.android.audiorecorder.RecorderFile;
import com.android.audiorecorder.SettingsActivity;
import com.android.audiorecorder.SoundRecorder;
import com.android.audiorecorder.dao.FileManagerFactory;
import com.android.audiorecorder.dao.IFileManager;
import com.android.audiorecorder.engine.GuardCameraSurfaceTexture.ISurfaceReady;
import com.android.audiorecorder.engine.ProgressOutHttpEntity.ProgressListener;
import com.android.audiorecorder.engine.ProgressOutHttpEntity.UploadResult;
import com.android.audiorecorder.utils.DateUtil;
import com.android.audiorecorder.utils.FileUtils;
import com.android.audiorecorder.utils.NetworkUtil;

public class AudioService extends Service{
	
    public final static int MIX_STORAGE_CAPACITY = 100;//MB
	private static final String Recorder_CACHE_DIR = "Recorder";
	private static final String IMAGE_CACHE_DIR = "DCIM";
	
	public final static int PAGE_NUMBER = 1;
    private static final int AM = 22;
    private static final int PM = 22;
    
    private static final int UPLOAD_START = 2;
    private static final int UPLOAD_END = 2;
    
    private static final int DELETE_START = 13;
    private static final int DELETE_END = 13;
    
    public static final int LUNCH_MODE_IDLE = 0;
    public static final int LUNCH_MODE_CALL = 1;
    public static final int LUNCH_MODE_MANLY = 2;//no allowed time and tel to recorder
    public static final int LUNCH_MODE_AUTO = 3;
    private int mCurMode;
    
    public static final String Action_RecordListen = "com.audio.Action_BluetoothRecord";
    
    private int bufferSizeInBytes = 0;
    private AudioRecord mAudioRecord;
    public final static int AUDIO_SAMPLE_RATE = 44100;
    private static final int RECORDER_BPP = 16;

    public static final int MSG_START_RECORD = 0xE1;
    public static final int MSG_STOP_RECORD = 0xE2;
    public static final int STATE_PREPARE = 0x01;

    public final static int MSG_UPDATE_TIMER = 200;
    
    public final static int MSG_TIMER_ALARM = 1000;
    
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
    private WakeLock mAlarmWakeLock;
    private WakeLock mUploadWakeLock;
    private MediaRecorder mMediaRecorder = null;

    private String TAG = "AudioService";

    private Set<IStateListener> mStateSet = new HashSet<IStateListener>();

    private AudioManager mAudioManager;
    private PowerManager mPowerManager;

    private boolean mRecorderStart;
    private boolean mTalkStart;
    private long mRecorderTime;
    private long mTalkTime;

    private BroadcastReceiver mStateChnageReceiver = null;
    
    private SharedPreferences mPreferences;
    private NotificationManager mNotificationManager;
    private int CUSTOM_VIEW_ID = R.layout.recorder_notification;
    private StringBuffer timerInfo = new StringBuffer();
    
    private IFileManager fileManager;
    private String mRecoderPath;
    private long mStartTime;
    private int mMimeType;
    
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private int mPhoneState;
    private String mIncommingNumber;
    
    private TimeSchedule mTimeSchedule;
    
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
    
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_START_RECORD:
                    int startMode = msg.arg1;
                    startRecorder(startMode);
                    break;
                case MSG_STOP_RECORD:
                    int stopMode = msg.arg1;
                    stopRecord(stopMode);
                    break;
                case MSG_UPDATE_TIMER:
                    notifyUpdate(MSG_UPDATE_TIMER);
                    updateNotifiaction();
                    break;
                case MSG_TIMER_ALARM:
                    processAutoTimerAlarm();
                    break;
                case MSG_BLUETOOTH_PROFILE_MATCH:
                    if (mService != null) {
                        List<BluetoothDevice> devs = getConnectedDevices();
                        if(DebugConfig.DEBUG) {
                            Log.d(TAG, "---> buletooth connected number = " + devs.size());
                        }
                        /*for (final BluetoothDevice dev : devs) {
                            BluetoothClass cl = dev.getBluetoothClass();
                            try {
                                Class<?> classObj = cl.getClass();
                                System.out.println("classObj = " + classObj);
                                Method doesClassMatch = classObj.getDeclaredMethod("doesClassMatch", new Class[]{int.class});
                                System.out.println("doesClassMatch = " + doesClassMatch);
                                doesClassMatch.setAccessible(true);
                                Field profile = classObj.getDeclaredField("PROFILE_A2DP");
                                System.out.println("profile = " + profile);
                                profile.setAccessible(true);
                                Integer profileType = (Integer)profile.get(cl);
                                Boolean result = (Boolean)doesClassMatch.invoke(doesClassMatch, profileType);
                                Log.d(TAG, "------> invoke doesClassMatch result = " + result);
                                if(result){
                                    Log.d(TAG, "BluetoothClass.PROFILE_A2DP");
                                    mAtdpEnable =  true;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "--------> " + e.getMessage());
                                e.printStackTrace();
                            }
                        }*/
                        /*Log.i(TAG, "---> mAtdpEnable = " + mAtdpEnable);
                        if (!mAtdpEnable) {
                            mHandler.removeMessages(MSG_BLUETOOTH_START_SCO);
                            mHandler.sendEmptyMessageDelayed(MSG_BLUETOOTH_START_SCO, 1500);
                        }*/
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
        mGuardCamera = new GuardCameraManager(this);
        this.mPreferences = getSharedPreferences(SettingsActivity.class.getName(), Context.MODE_PRIVATE);
        mRecorderWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"SoundRecorderService");
        mAlarmWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlarmWakeLock");
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
                        mUploadHandler.sendEmptyMessage(MSG_INIT_CAMERA);
                    } else if (action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
                        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
                        if(state == BluetoothAdapter.STATE_CONNECTED){
                             mIsBluetoothConnected = true;
                             BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                             if(bluetoothAdapter != null){
                                  bluetoothAdapter.getProfileProxy(AudioService.this, mBluetoothHeadsetListener, BluetoothProfile.A2DP);
                                  bluetoothAdapter.getProfileProxy(AudioService.this, mBluetoothHeadsetListener, BluetoothProfile.HEADSET);
                             }
                        } else if(state == BluetoothAdapter.STATE_DISCONNECTED || state == BluetoothAdapter.STATE_DISCONNECTING){
                             mAtdpEnable =  false;
                             mIsBluetoothConnected = false;
                             mAudioManager.stopBluetoothSco();
                             mAudioManager.setBluetoothScoOn(false);
                             mAudioManager.setMode(AudioManager.MODE_NORMAL);
                             Log.d(TAG,"==> recv bluetooth connected  state = " + state + " atdp enable = " + mAtdpEnable);
                        }
                    } else if(action.equals(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)) {
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
        fileManager = FileManagerFactory.getSmsManagerInstance(this);
        mTimeSchedule = new TimeSchedule(this);
        mTimeSchedule.start();
        mUploadHandlerCallback = new UploadHandlerCallback();
        mUpHandlerThread = new HandlerThread("upload_thread", HandlerThread.MAX_PRIORITY);
        mUpHandlerThread.start();
        mUploadHandler = new Handler(mUpHandlerThread.getLooper(), mUploadHandlerCallback);
        mCurMode = LUNCH_MODE_IDLE;
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
            Message msgStop = mHandler.obtainMessage(MSG_STOP_RECORD);
            msgStop.arg1 = mCurMode;
            mHandler.sendMessage(msgStop);
            mHandler.removeMessages(MSG_START_RECORD);
            Message msg = mHandler.obtainMessage(MSG_START_RECORD);
            msg.arg1 = LUNCH_MODE_MANLY;
            mHandler.sendMessage(msg);
            mHandler.removeCallbacks(mUpdateTimer);
            mHandler.post(mUpdateTimer);
        }

        @Override
        public void stopRecord() throws RemoteException {
            Log.d(TAG, "===> stopRecorder");
            mHandler.removeMessages(MSG_STOP_RECORD);
            Message msg = mHandler.obtainMessage(MSG_STOP_RECORD);
            msg.arg1 = LUNCH_MODE_MANLY;
            mHandler.sendMessage(msg);
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
        
        @Override
        public long checkDiskCapacity() throws RemoteException {
            return avliableDiskSize();
        }
    };

    private void setRecordStatus(boolean start) {
        mRecorderStart = start;
        if (start) {
            mRecorderTime = SystemClock.uptimeMillis();
        }
    }

    private void startRecorder(int mode) {
        mMimeType = mPreferences.getInt(SoundRecorder.PREFERENCE_TAG_FILE_TYPE, SoundRecorder.FILE_TYPE_3GPP);
        createDir(mode);
        Log.d(TAG, "---> startRecorder mode = " + mode + "  mMimeType= " + mMimeType);
        if(!mRecorderWakeLock.isHeld()){
            mRecorderWakeLock.acquire();
        }
        mHandler.sendEmptyMessage(MSG_BLUETOOTH_START_SCO);
        if(mode == LUNCH_MODE_MANLY && mMimeType == SoundRecorder.FILE_TYPE_WAV){
            creatAudioRecord(mode);
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
            try {
                mMediaRecorder.prepare();
                mMediaRecorder.start();
                if(mode == LUNCH_MODE_MANLY){
                    setRecordStatus(true);
                    notifyUpdate(MSG_START_RECORD);
                }else{
                    mStartTime = System.currentTimeMillis();
                }
                mCurMode = mode;
                Log.i(TAG, "---> startRecorder mCurMode = " + mCurMode);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecord(int mode) {
        Log.d(TAG, "---> stopRecord mode = " + mode + "  mMimeType= " + mMimeType);
        if(mode == LUNCH_MODE_IDLE){
            return;
        }
        if(mode == LUNCH_MODE_MANLY && mMimeType == SoundRecorder.FILE_TYPE_WAV){
            setRecordStatus(false);
            notifyUpdate(MSG_STOP_RECORD);
        } else {
            if (mMediaRecorder != null) {
                mMediaRecorder.stop();
                mMediaRecorder.release();
                mMediaRecorder = null;
                RecorderFile file = new RecorderFile();
                file.setDuration((int) ((SystemClock.uptimeMillis() - mRecorderTime) / 1000));
                file.setPath(mRecoderPath);
                file.setTime(mStartTime);
                file.setType(mode);
                File f = new File(mRecoderPath);
                if(f.exists()){
                    file.setSize(f.length());
                }
                file.setMimeType("3gpp");
                fileManager.insertRecorderFile(file);
                notifyUpdate(MSG_STOP_RECORD);
                setRecordStatus(false);
            }
        }
        mHandler.sendEmptyMessage(MSG_BLUETOOTH_STOP_SCO);
        if (mRecorderWakeLock.isHeld()) {
            mRecorderWakeLock.release();
        }
        mCurMode = LUNCH_MODE_IDLE;
    }

    private void createDir(int mode){
        
        String storagePath;
        int storage = mPreferences.getInt(SoundRecorder.PREFERENCE_TAG_STORAGE_LOCATION, SoundRecorder.STORAGE_LOCATION_SD_CARD);
        if(storage == SoundRecorder.STORAGE_LOCATION_SD_CARD){
            storagePath = Environment.getExternalStorageDirectory().getPath();
            if(DebugConfig.DEBUG){
                Log.i(TAG, "---> external storage path = " + storagePath);
            }
        } else {
            storagePath = FileUtils.getExternalStoragePath(this);
            if(storagePath.length() > 0){
                if(DebugConfig.DEBUG){
                    Log.i(TAG, "---> internal storage path = " + storagePath);
                }
            } else {
                storagePath = Environment.getExternalStorageDirectory().getPath();
                if(DebugConfig.DEBUG){
                    Log.i(TAG, "---> internal storage do not exist, get default path = " + storagePath);    
                }
            }
        }
        if(mode != LUNCH_MODE_AUTO){
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
             }
             String pre = "";
             if(mIncommingNumber != null && mIncommingNumber.length()>0){
         		pre = mIncommingNumber+"_";
         	 }
             if(mMimeType == SoundRecorder.FILE_TYPE_3GPP){
                 mRecoderPath += File.separator + pre + DateUtil.formatyyMMDDHHmmss(System.currentTimeMillis())+".3gp";
             } else {
                 mRecoderPath += File.separator + pre + DateUtil.formatyyMMDDHHmmss(System.currentTimeMillis())+".wav";
             }
        } else {
        	File catchPath = FileUtils.getDiskCacheDir(this, Recorder_CACHE_DIR);
        	if(!catchPath.exists()){
        		catchPath.mkdirs();
        	}
        	String pre = getNamePrefix();
        	mRecoderPath = catchPath.getPath() + File.separator + pre + DateUtil.formatyyMMDDHHmmss(System.currentTimeMillis())+".wav";
        }
        if(DebugConfig.DEBUG){
        	Log.d(TAG, "---> Recorder Path = " + mRecoderPath);
        }
        fileManager.createFile(mRecoderPath);
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
    
    private void callStartRecord(int mode){
        if(DebugConfig.DEBUG){
            Log.i(TAG, "---> call in come , start recorder");
        }
        Message msg = mHandler.obtainMessage(MSG_START_RECORD);
        msg.arg1 = mode;
        mHandler.sendMessage(msg);
    }
    
    private void callStopRecord(int mode){
        if(DebugConfig.DEBUG){
            Log.i(TAG, "---> call over , stop recorder");
        }
        Message msg = mHandler.obtainMessage(MSG_STOP_RECORD);
        msg.arg1 = mode;
        mHandler.sendMessage(msg);
    }
    
    private void processAutoTimerAlarm(){
        acquireWakeLock();
        if(isValidRecorderTime()){//start
            if(DebugConfig.DEBUG){
                Log.i(TAG, "processTimerAlarm mCurMode = " + mCurMode + " mRecorderStart = " + mRecorderStart);
            }
            if(mCurMode == LUNCH_MODE_IDLE){
                mHandler.removeMessages(MSG_START_RECORD);
                Message msg = mHandler.obtainMessage(MSG_START_RECORD);
                msg.arg1 = LUNCH_MODE_AUTO;
                mHandler.sendMessage(msg);
            }
            if(mCurMode == LUNCH_MODE_AUTO){
                mHandler.removeMessages(MSG_STOP_RECORD);
                Message msg = mHandler.obtainMessage(MSG_STOP_RECORD);
                msg.arg1 = LUNCH_MODE_AUTO;
                mHandler.sendMessage(msg);
                mHandler.removeMessages(MSG_START_RECORD);
                Message msg2 = mHandler.obtainMessage(MSG_START_RECORD);
                msg2.arg1 = LUNCH_MODE_AUTO;
                mHandler.sendMessage(msg2);
            }
        } else {
            if(mCurMode == LUNCH_MODE_AUTO){
                mHandler.removeMessages(MSG_STOP_RECORD);
                Message msg = mHandler.obtainMessage(MSG_STOP_RECORD);
                msg.arg1 = LUNCH_MODE_AUTO;
                mHandler.sendMessage(msg);
            }
            if(isValidUploadTime() && !mPowerManager.isScreenOn() && NetworkUtil.isWifiDataEnable(this)){//start
                mUploadHandler.removeMessages(MSG_START_UPLOAD);
                mUploadHandler.sendEmptyMessage(MSG_START_UPLOAD);
            }else{
            	mUploadHandler.removeMessages(MSG_START_UPLOAD);
            }
            if(isValidDeleteTime()){
                mUploadHandler.removeMessages(MSG_START_UPLOAD);
                mUploadHandler.removeMessages(MSG_START_DELETE);
                mUploadHandler.sendEmptyMessage(MSG_START_DELETE);
            }else{
            	mUploadHandler.removeMessages(MSG_START_DELETE);
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
			if(!mUploadWakeLock.isHeld()){
				mUploadWakeLock.acquire();
			}
			initUploadFile(file.getId(), file.getPath(), file.getType());
		}else{
			if(mUploadWakeLock.isHeld()){
				mUploadWakeLock.release();
			}
		    if(DebugConfig.DEBUG){
		        Log.w(TAG, "---> No Files Upload.");
		    }
		}
    }
    
    private void startDeleteTask(){
        List<RecorderFile> list = fileManager.queryPrivateFileList(0, PAGE_NUMBER);
        if(list.size()>0){
        	if(DebugConfig.DEBUG){
				Log.i(TAG, "---> startDeleteTask  = " + mPowerManager.isScreenOn() + "  wifi state = " + NetworkUtil.isWifiDataEnable(this));
			}
            if(!mPowerManager.isScreenOn() && NetworkUtil.isWifiDataEnable(this)){//start upload try
                mUploadHandler.removeMessages(MSG_START_UPLOAD);
                mUploadHandler.sendEmptyMessage(MSG_START_UPLOAD);
            } else {
                RecorderFile file = list.get(0);
                if(file.getType() == LUNCH_MODE_AUTO){
                	if(fileManager.removeFile(file.getPath())){
                		fileManager.delete(file.getId());
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
    }
    
    
    private void acquireWakeLock(){
        if (mAlarmWakeLock.isHeld()) {
            mAlarmWakeLock.release();
        }
        mAlarmWakeLock.acquire(2000);
    }
    
    private void initUploadFile(long id, String path, int type){
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
            fileManager.delete(id);
            if(DebugConfig.DEBUG){
                Log.i(TAG, "upload success. path = " + path);
            }
            mUploadHandler.removeMessages(MSG_START_UPLOAD); //wait for next callback
            mUploadHandler.sendEmptyMessageDelayed(MSG_START_UPLOAD, 1000);
        }
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
        	if(DebugConfig.DEBUG){
                Log.i(TAG, "---> start upload url = " + url);
            }
            HttpResponse httpResponse = httpClient.execute(httpPost);
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                if(!ProgressOutHttpEntity.mCancle){
                    mResult = UploadResult.SUCCESS;
                } else {
                    mResult = UploadResult.FAIL;
                }
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
    
    private String getNamePrefix(){
        String mac = mPreferences.getString(SettingsActivity.KEY_MAC_ADDRESS, "").replace(":", "_");
        Log.d(TAG, "---> getNamePrefix = " + mac);
        if(mac == null || mac.length() == 0){
            mac = getMacAddress(this);
        }
        if(mac == null || mac.length() == 0){
            mac = telephonyManager.getDeviceId();
        }
        return mac;
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
    
    private void creatAudioRecord(final int mode) { 
        bufferSizeInBytes = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT); 
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, AUDIO_SAMPLE_RATE,  AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes);
        mAudioRecord.startRecording();
        mStartTime = System.currentTimeMillis();
        mRecorderTime = SystemClock.uptimeMillis();
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                mCurMode = mode;
                Log.i(TAG, "---> startRecorder creatAudioRecord mCurMode = " + mCurMode);
                setRecordStatus(true);
                notifyUpdate(MSG_STOP_RECORD);
                writeDataToFile();
                insertWaveTitle(mode);
            }
        }).start();
    }
    
    private void writeDataToFile() { 
        byte[] audiodata = new byte[bufferSizeInBytes]; 
        FileOutputStream fos = null; 
        int readsize = 0; 
        File file = new File(mRecoderPath); 
        try { 
            if (file.exists()) { 
                file.delete(); 
            }
            fos = new FileOutputStream(file, false); 
            fos.write(audiodata, 0, 44);
            fos.flush();
        } catch (Exception e) { 
            e.printStackTrace(); 
        } 
        while (mRecorderStart) {
            readsize = mAudioRecord.read(audiodata, 0, bufferSizeInBytes); 
            if (AudioRecord.ERROR_INVALID_OPERATION != readsize && fos!=null) { 
                try { 
                    fos.write(audiodata, 0 , readsize);
                } catch (IOException e) { 
                    e.printStackTrace(); 
                } 
            } 
        }
        if(mAudioRecord != null){
            mAudioRecord.stop();
            mAudioRecord = null;
        }
        try {
            if(fos != null){
                fos.flush();
                fos.close();// ¹Ø±ÕÐ´ÈëÁ÷
                fos = null;
            }
            Log.d(TAG, "---> recorder over.");
        } catch (IOException e) { 
            e.printStackTrace(); 
        } 
    } 
   
    private void insertWaveTitle(int mode) { 
        FileInputStream in = null; 
        long totalAudioLen = 0; 
        long totalDataLen = totalAudioLen + 36; 
        long longSampleRate = AUDIO_SAMPLE_RATE; 
        int channels = 2; 
        long byteRate = RECORDER_BPP * AUDIO_SAMPLE_RATE * channels / 8;
        try {
            RandomAccessFile randomAccess = new RandomAccessFile(mRecoderPath, "rw");
            randomAccess.seek(0);
            in = new FileInputStream(mRecoderPath); 
            totalAudioLen = in.getChannel().size(); 
            totalDataLen = totalAudioLen + 36; 
            byte[] header = writeWaveFileHeader(totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);
            randomAccess.write(header, 0, header.length);
            randomAccess.close();
            in.close();
            RecorderFile file = new RecorderFile();
            file.setDuration((int) ((SystemClock.uptimeMillis() - mRecorderTime) / 1000));
            file.setPath(mRecoderPath);
            file.setTime(mStartTime);
            file.setType(mode);
            file.setSize(totalDataLen);
            file.setMimeType("wav");
            fileManager.insertRecorderFile(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace(); 
        } 
    } 
   
    private byte[] writeWaveFileHeader(long totalAudioLen, 
            long totalDataLen, long longSampleRate, int channels, long byteRate) 
            throws IOException { 
        byte[] header = new byte[44]; 
        header[0] = 'R'; // RIFF/WAVE header 
        header[1] = 'I'; 
        header[2] = 'F'; 
        header[3] = 'F'; 
        header[4] = (byte) (totalDataLen & 0xff); 
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);
        header[33] = 0; 
        header[34] = RECORDER_BPP; 
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        return header;
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
        if(!mGuardCamera.isSupportAutoFocus()){
            mGuardCamera.takePicture(null, null, jpegCallback);
        }
        /*Camera camera = mGuardCamera.getFrontCameraInstance(CameraInfo.CAMERA_FACING_BACK);
        System.out.println("camera = " + camera);
        Camera.Parameters params = camera.getParameters();
        System.out.println("params = " + params);
        List<String> focusModes = params.getSupportedFocusModes();
        if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)){
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        camera.setParameters(params);
        GuardCameraPreview cameraPreview = new GuardCameraPreview(this, camera);
        mUploadHandler.sendEmptyMessage(MSG_RELEASE_CAMERA);*/
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
            File catchPath = FileUtils.getDiskCacheDir(AudioService.this, IMAGE_CACHE_DIR);
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
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mUploadHandler.sendEmptyMessage(MSG_RELEASE_CAMERA);
            }
        }
    };
    
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
    
    private long avliableDiskSize(){
        int storage = mPreferences.getInt(SoundRecorder.PREFERENCE_TAG_STORAGE_LOCATION, SoundRecorder.STORAGE_LOCATION_SD_CARD);
        if(storage == SoundRecorder.STORAGE_LOCATION_SD_CARD){
            if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                return 0;
            }
            String extStoragePath = Environment.getExternalStorageDirectory().getPath();
            if(DebugConfig.DEBUG){
                Log.i(TAG, "---> external storage path = " + extStoragePath);
            }
            return FileUtils.getAvailableSize(extStoragePath);
        } else {
            String interStoragePath = FileUtils.getExternalStoragePath(this);
            if(interStoragePath.length() > 0){
                if(DebugConfig.DEBUG){
                    Log.i(TAG, "---> internal storage path = " + interStoragePath);
                }
            } else {
                interStoragePath = Environment.getExternalStorageDirectory().getPath();
                if(DebugConfig.DEBUG){
                    Log.i(TAG, "---> internal storage do not exist, get default path = " + interStoragePath);    
                }
            }
            return FileUtils.getAvailableSize(interStoragePath);
        }
    }
}
