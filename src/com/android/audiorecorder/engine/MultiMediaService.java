package com.android.audiorecorder.engine;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.audiorecorder.DebugConfig;
import com.android.audiorecorder.R;
import com.android.audiorecorder.provider.FileColumn;
import com.android.audiorecorder.provider.FileProvider;
import com.android.audiorecorder.ui.SettingsActivity;
import com.android.audiorecorder.ui.SoundRecorder;

public class MultiMediaService extends Service {
	
    public final static int STATE_IDLE = 0;
    public final static int STATE_BUSY = 1;
    public final static int STATE_PREPARE = 2;
    
    public static final int LUNCH_MODE_IDLE = OnRecordListener.LUNCH_MODE_IDLE;
    public static final int LUNCH_MODE_CALL = OnRecordListener.LUNCH_MODE_CALL;
    public static final int LUNCH_MODE_MANLY = OnRecordListener.LUNCH_MODE_MANLY;//no allowed time and tel to recorder
    public static final int LUNCH_MODE_AUTO = OnRecordListener.LUNCH_MODE_AUTO;
    
    public int mAudioRecordState = STATE_IDLE;
    public int mVideoRecordState = STATE_IDLE;
    
	
	public final static int PAGE_NUMBER = 1;
    private static final int AM = 21;
    private static final int PM = 23;
    
    private static final int UPLOAD_START = 2;
    private static final int UPLOAD_END = 2;
    
    private static final int DELETE_START = 13;
    private static final int DELETE_END = 13;
    
    private int mCurMode;
    private boolean isScreenOn;
    
    public static final String Action_Audio_Record = "com.audio.Action_AudioRecord";
    public static final String Action_Video_Record = "com.audio.Action_VideoRecord";
    
    
    private int mBatteryLevel;
    private final static int MIN_BATTERY_LEVEL = 40;//%

    
    public final static int MSG_START_UPLOAD = 2000;
    
    public final static int MSG_START_DELETE = 3000;
    
    private static final int MSG_BLUETOOTH_START_SCO = 4000;
    private static final int MSG_BLUETOOTH_STOP_SCO = 4001;
    private static final int MSG_BLUETOOTH_PROFILE_MATCH = 4002;

    private boolean mIsBluetoothConnected;
    private boolean mAtdpEnable;

    private String TAG = "MultiMediaService";

    private AudioManager mAudioManager;
    private PowerManager mPowerManager;
    private int mRingerMode;

    private boolean mRecorderStart;
    private boolean mTalkStart;
    private long mAudioRecorderDuration;
    private long mTalkTime;

    private BroadcastReceiver mStateChnageReceiver = null;
    
    private SharedPreferences mPreferences;
    
    private TimeSchedule mTimeSchedule;
    
    private long mTransferedBytes;
    
    private BluetoothA2dp mService;
    private BluetoothHeadset mBluetoothHeadset;
    private BluetoothHeadsetListener mBluetoothHeadsetListener = new BluetoothHeadsetListener();
    
    
    private IAudioService.Stub mAudioService;
    private IVideoService.Stub mVideoService;
    
    
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                /*case MSG_UPDATE_TIMER:
                    notifyRecordState(MSG_UPDATE_TIMER);//only update recorder time on UI
                    updateNotifiaction();
                    break;*/
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
              /*case MSG_START_TIMER:
            	  mHandler.sendEmptyMessageDelayed(MSG_START_TIMER, MAX_RECORDER_DURATION*1000);
            	  processAutoTimerAlarm();
            	  break;*/
                default:
                    break;
            }
        };
    };

    @Override
    public IBinder onBind(Intent intent) {
        if (Action_Audio_Record.equalsIgnoreCase(intent.getAction())) {
            return getAudioService();
        } else if (Action_Video_Record.equalsIgnoreCase(intent.getAction())) {
            return getVideoService();
        }
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        isScreenOn = mPowerManager.isScreenOn();
        this.mPreferences = getSharedPreferences(SettingsActivity.class.getName(), Context.MODE_PRIVATE);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
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
                    if(DebugConfig.DEBUG){
                        Log.i(TAG, "---> action = " + action);
                    }
                    if(TimeSchedule.ACTION_TIMER_ALARM.equalsIgnoreCase(action)){
                    	 Log.i(TAG, "---> alarm.");
                    } else if(Intent.ACTION_USER_PRESENT.equalsIgnoreCase(action)){//user login, screen on
                        isScreenOn = true;
                        try {
                            if(getAudioService().getMode() == LUNCH_MODE_AUTO){//stop record
                                getAudioService().stopRecord();
                                Log.i(TAG, "---> user present.");
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
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
                        //mHandler.removeMessages(MSG_START_TIMER);
                        //mHandler.sendEmptyMessageDelayed(MSG_START_TIMER, 3000);
                    }
                }
            };
            registerReceiver(mStateChnageReceiver, filter);
        }
        mCurMode = LUNCH_MODE_IDLE;
        Log.d(TAG, "===> onCreate. screen state : " + isScreenOn);
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
                /*if(mCurMode == LUNCH_MODE_IDLE){
                    mMediaAudioHandler.removeMessages(MSG_START_RECORD);
                    Message msg = mMediaAudioHandler.obtainMessage(MSG_START_RECORD);
                    msg.arg1 = LUNCH_MODE_AUTO;
                    mMediaAudioHandler.sendMessage(msg);
                }*/
                /**
                 * if curmode is auto, stop record and restart with auto mode
                 */
                /*if(mCurMode == LUNCH_MODE_AUTO){
                    mMediaAudioHandler.removeMessages(MSG_STOP_RECORD);
                    Message msg = mMediaAudioHandler.obtainMessage(MSG_STOP_RECORD);
                    msg.arg1 = LUNCH_MODE_AUTO;
                    mMediaAudioHandler.sendMessage(msg);
                    mMediaAudioHandler.removeMessages(MSG_START_RECORD);
                    Message msg2 = mMediaAudioHandler.obtainMessage(MSG_START_RECORD);
                    msg2.arg1 = LUNCH_MODE_AUTO;
                    mMediaAudioHandler.sendMessage(msg2);
                }*/
            } else {// stop auto recorder
                /*if(mCurMode == LUNCH_MODE_AUTO){
                    Log.i(TAG, "---> auto stop.");
                    mMediaAudioHandler.removeMessages(MSG_STOP_RECORD);
                    Message msg = mMediaAudioHandler.obtainMessage(MSG_STOP_RECORD);
                    msg.arg1 = LUNCH_MODE_AUTO;
                    mMediaAudioHandler.sendMessage(msg);
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
   
    private List<BluetoothDevice> getConnectedDevices() {
        return mService.getDevicesMatchingConnectionStates(new int[] {BluetoothProfile.STATE_CONNECTED});
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
        //Notification note = new Notification(0, null, System.currentTimeMillis() );
        //note.flags |= Notification.FLAG_NO_CLEAR;
        //startForeground(42, note);
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
    
    private IAudioService.Stub getAudioService(){
        if(mAudioService == null){
            mAudioService = new AudioRecordSystem(MultiMediaService.this);
        }
        return mAudioService;
    }
    
    private IVideoService.Stub getVideoService(){
        if(mVideoService == null){
            mVideoService = new VideoRecordSystem(MultiMediaService.this);
        }
        return mVideoService;
    }
    
    public interface OnRecordListener{
        
        public final static int STATE_IDLE = 0;
        public final static int STATE_BUSY = 1;
        public final static int STATE_PREPARE = 2;
        
        public static final int MSG_START_RECORD = 0xE1;
        public static final int MSG_STOP_RECORD = 0xE2;
        
        public static final int LUNCH_MODE_IDLE = 0;
        public static final int LUNCH_MODE_CALL = 1;
        public static final int LUNCH_MODE_MANLY = 2;//no allowed time and tel to recorder
        public static final int LUNCH_MODE_AUTO = 3;
        
        public void setMode(int mode);
        
    }
}
