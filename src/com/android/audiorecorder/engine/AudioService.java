package com.android.audiorecorder.engine;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

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
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.audiorecorder.DateUtil;
import com.android.audiorecorder.FileUtils;
import com.android.audiorecorder.R;
import com.android.audiorecorder.RecorderFile;
import com.android.audiorecorder.SoundRecorder;
import com.android.audiorecorder.Utils;
import com.android.audiorecorder.dao.FileManagerFactory;
import com.android.audiorecorder.dao.FileManagerImp;
import com.android.audiorecorder.dao.IFileManager;

public class AudioService extends Service {
    
    public static final int TYPE_MANLY = 0;
    public static final int TYPE_TEL = 1;
    public static final int TYPE_AUTO = 2;
    
    private String STORAGE_PATH_LOCAL_PHONE;
    private String STORAGE_PATH_SD_CARD;
    
    public static final String Action_RecordListen = "com.audio.Action_BluetoothRecord";

    /* action */
    public static final String Action_Record = "com.dahuatech.audio.Action_Record";
    public static final String Action_Talk = "com.dahuatech.audio.Action_Talk";

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

    public static final String Action_SetParemeter = "com.dahuatech.audio.Action_SetParameter";

    public static final int MSG_START_RECORD = 0xE1;

    public static final int MSG_STOP_RECORD = 0xE2;

    public static final int STATE_PREPARE = 0x01;

    public static final int MSG_ATDP_CONNECTED = 0x10;
    public static final int MSG_ATDP_DISCONNECTED = 0x11;
    
    public final static int MSG_UPDATE_TIMER = 20;

    private WakeLock mWakeLock;
    private MediaRecorder mMediaRecorder = null;

    private MediaPlayer mMediaPlayer = null;
    private OnCompletionListener mOnCompletionListener;

    private String TAG = "AudioService";

    private Set<IStateListener> mStateSet = new HashSet<IStateListener>();

    private int audioStartId;
    private int audioStopId;

    private AudioManager mAudioManager;
    private int mCurAudoMode;
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
    private int mType;
    
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
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mPreferences = getSharedPreferences("SoundRecorder", Context.MODE_PRIVATE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "DHSoundRecorderService");
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        String str1 = String.valueOf(Environment.getExternalStorageDirectory().toString());
        STORAGE_PATH_SD_CARD = str1 + "/Audio/Record/";
        //String str2 = String.valueOf(phoneStrorage().toString());
        //STORAGE_PATH_LOCAL_PHONE = str2 + "/Audio/Record";
        if (mStateChnageReceiver == null) {
            mStateChnageReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    final String action = intent.getAction();
                    if (Intent.ACTION_HEADSET_PLUG.equals(intent
                            .getAction())) {
                        int headSetState = intent.getIntExtra("state", -1);
                        if (headSetState == 1) {
                            mAudioManager.setSpeakerphoneOn(false);
                        } else {
                            mAudioManager.setSpeakerphoneOn(true);
                        }
                    }
                }
            };
            registerReceiver(mStateChnageReceiver, filter);
        }
        fileManager = FileManagerFactory.getSmsManagerInstance(this);
        Log.d(TAG, "===> onCreate.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return super.onStartCommand(intent, flags, startId);
        }
        Log.d(TAG, "===> Action name = " + intent.getAction() + " <====");
        if (Action_SetParemeter.equalsIgnoreCase(intent.getAction())) {
            Log.e(TAG, "===> " + intent.getAction());
        } else if (Action_Record.equalsIgnoreCase(intent.getAction())) {
            mAudioRecordStart = intent.getBooleanExtra(
                    Action_Record_Extral_Start, false);
            mAudioRecordChannel = intent.getIntExtra(
                    Action_Record_Extral_Channel, 0);
            mAudioRecordStream = intent.getIntExtra(
                    Action_Record_Extral_Stream, 1);
            if (mAudioRecordStart) {
                mHandler.sendEmptyMessage(MSG_START_RECORD);
            } else {
                mHandler.sendEmptyMessage(MSG_STOP_RECORD);
            }
            Log.d(TAG, "===>  mAudioRecordStart = " + mAudioRecordStart);
        } else if (Action_Talk.equalsIgnoreCase(intent.getAction())) {
            mAudioTalkStart = intent.getBooleanExtra(
                    Action_Record_Extral_Start, false);
            mAudioTalkChannel = intent.getIntExtra(
                    Action_Record_Extral_Channel, 0);
            mAudioTalkStream = intent.getIntExtra(Action_Record_Extral_Stream,
                    1);
            Log.d(TAG, "===> mAudioTalkStart = " + mAudioTalkStart);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mStateChnageReceiver != null) {
            unregisterReceiver(mStateChnageReceiver);
            mStateChnageReceiver = null;
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
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            /*if(mPreferences.getInt(SoundRecorder.PREFERENCE_TAG_FILE_TYPE, SoundRecorder.FILE_TYPE_DEFAULT) == SoundRecorder.FILE_TYPE_3GPP){
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            }else{
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            }*/
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setAudioSamplingRate(8000);
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
                if (bluetoothStateReceiver == null) {
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
                file.setDuration((int)(System.currentTimeMillis() - startTime)/1000);
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
            file.setType(mType);
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
        }
        setRecordStatus(false);
        notifyUpdate(MSG_STOP_RECORD);
    }

    private void createDir(){
        int storage = mPreferences.getInt(SoundRecorder.PREFERENCE_TAG_STORAGE_LOCATION, SoundRecorder.STORAGE_LOCATION_DEFAULT);
        String storagePath = FileUtils.getExternalStoragePath(this);
        Log.i(TAG, "---> external storage path = " + storagePath);
        if(storagePath.length() == 0){
            storagePath = Environment.getExternalStorageDirectory().getPath();
            Log.i(TAG, "---> default path = " + storagePath);    
        }
        if(storage == SoundRecorder.STORAGE_LOCATION_DEFAULT){
            
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
            for(int i=1;i<10;i++){
                fileManager.createDiretory(mRecoderPath+i);
            }
        }
        if(mMimeType == SoundRecorder.FILE_TYPE_AMR){
            mRecoderPath += File.separator + DateUtil.formatyyMMDDHHmmss(System.currentTimeMillis())+".mp3";
        } else {
            mRecoderPath += File.separator +DateUtil.formatyyMMDDHHmmss(System.currentTimeMillis())+".mp3";
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
}
