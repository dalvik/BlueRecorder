package com.android.audiorecorder.engine;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import com.android.audiorecorder.DateUtil;

public class AudioService extends Service {


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
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "DHSoundRecorderService");
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
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
        }

        @Override
        public void stopRecord() throws RemoteException {
            Log.d(TAG, "===> stopRecorder");
            mHandler.removeMessages(MSG_STOP_RECORD);
            mHandler.sendEmptyMessage(MSG_STOP_RECORD);
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
            ;
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
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setAudioSamplingRate(8000);
            String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/audio/";
            File file = new File(path);
            if(!file.exists()){
                file.mkdirs();
            }
            File newFile = new File(path + DateUtil.formatyyMMDDHHmmss(System.currentTimeMillis())+".mp3");
            if(newFile.exists()){
                newFile.delete();
            }
            newFile.createNewFile();
            mMediaRecorder.setOutputFile(path + DateUtil.formatyyMMDDHHmmss(System.currentTimeMillis())+".mp3");
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
            try {
                mMediaRecorder.stop();
            } catch (IllegalStateException e) {
                Log.e(TAG, "===> IllegalStateException.");
            }
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
        setRecordStatus(false);
        notifyUpdate(MSG_STOP_RECORD);
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

}
