package com.android.audiorecorder;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.android.audiorecorder.engine.AudioService;
import com.android.audiorecorder.engine.IRecordListener;
import com.android.audiorecorder.engine.IStateListener;
import com.android.audiorecorder.engine.UpdateManager;
import com.android.audiorecorder.utils.FileUtils;

public class SoundRecorder extends SherlockActivity implements View.OnClickListener {
    
    public final static int MIX_STORAGE_CAPACITY = 500;//MB
    public final static int MSG_RECORDER_UPDATE_UI = 10;
    public final static int MSG_CHECK_MODE = 20;

    static final String ANY_ANY = "*/*";
    static final String AUDIO_3GPP = "audio/aac";
    static final String AUDIO_AMR = "audio/amr";
    static final String AUDIO_ANY = "audio/*";
    static final int BITRATE_3GPP = 65536;
    static final int BITRATE_AMR = 12841;
    public static final int FILE_TYPE_3GPP = 1;
    public static final int FILE_TYPE_AMR = 0;
    public static final int FILE_TYPE_DEFAULT = 1;
    static final String MAX_FILE_SIZE_KEY = "max_file_size";
    public static float PIXEL_DENSITY = 0.0F;
    public static final String PREFERENCE_TAG_FILE_TYPE = "filetype";
    public static final String PREFERENCE_TAG_STORAGE_LOCATION = "storagepath";
    private static final int Phone_Storage = 1;
    private static final int Position_Setting = 0;
    private static final int QUIT = 2;
    static final String RECORDER_STATE_KEY = "recorder_state";
    private static final int REFRESH = 1;
    static final String SAMPLE_INTERRUPTED_KEY = "sample_interrupted";
    static final int SETTING_TYPE_FILE_TYPE = 1;
    static final int SETTING_TYPE_STORAGE_LOCATION = 0;
    static final String STATE_FILE_NAME = "soundrecorder.state";
    public static final int STORAGE_LOCATION_DEFAULT = 0;
    public static final int STORAGE_LOCATION_LOCAL_PHONE = 0;
    public static final int STORAGE_LOCATION_SD_CARD = 1;
    static final String STORAGE_PATH_LOCAL_PHONE = null;
    static final String STORAGE_PATH_SD_CARD = null;
    static final String TAG = "SoundRecorder";
    private static final int TF_Storage = 0;
    static final boolean isCMCC = false;
    public static boolean mIsLandScape;
    private boolean DEBUG = true;
    ImageView imageLeft;
    ImageView imageRight;
    private boolean isPauseSupport = false;
    private boolean isStop = true;
    ImageView mAnimImg;
    AnimationDrawable mAnimation;
    String mErrorUiMessage = null;
    private int mFileType = 1;
    ImageClock mImageClock;
    ImageButton mListButton;
    long mMaxFileSize = 65535L;
    private boolean mOneShot;
    private SharedPreferences mPreferences;
    ImageButton mRecordButton;
    ImageButton mStopButton;
    ImageButton mPauseButton;
    Recorder mRecorder;
    //RemainingTimeCalculator mRemainingTimeCalculator;
    String mRequestedType = "audio/*";
    //private BroadcastReceiver mSDCardMountEventReceiver;
    boolean mSampleInterrupted;
    TextView mStateMessage1;
    ProgressBar mStateProgressBar;
    
    private int mStoragePath;
    String mTimerFormat;
    TextView mTimerView;
    com.android.audiorecorder.myview.VUMeter mVUMeter;
    private AlertDialog localAlertDialog;
    private IRecordListener iRecorderService;

    private Handler mHandler = new Handler(){
       public void handleMessage(android.os.Message msg) {
           switch(msg.what){
               case MSG_RECORDER_UPDATE_UI:
               case AudioService.MSG_START_RECORD:
               case AudioService.MSG_STOP_RECORD:
                   updateUi();
                   break;
               case AudioService.MSG_UPDATE_TIMER:
                   updateTimerView();
                   break;
               case MSG_CHECK_MODE:
                   try {
                       if(iRecorderService != null && iRecorderService.isRecorderStart() && iRecorderService.getMode() == AudioService.LUNCH_MODE_AUTO){
                    	   iRecorderService.stopRecord();
                    	   iRecorderService.setMode(AudioService.LUNCH_MODE_MANLY);
                       }
                   } catch (RemoteException e) {
                        e.printStackTrace();
                   }
                   break;
               default:
                   break;
           }
       };  
    };

    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        startService(new Intent(this, AudioService.class));
        if(bindService(new Intent(AudioService.Action_RecordListen), mServiceConnection, Context.BIND_AUTO_CREATE)){
            this.mPreferences = getSharedPreferences(SettingsActivity.class.getName(), Context.MODE_PRIVATE);
            setContentView(R.layout.main1);
            UpdateManager.getUpdateManager().checkAppUpdate(this, false);
            Intent localIntent = getIntent();
            String type = "";
            if (localIntent != null) {
                type = localIntent.getType();
                if ((!"audio/amr".equals(type)) && (!"audio/aac".equals(type)) && (!"audio/*".equals(type)) && (!"*/*".equals(type))){
                    //SoundRecorder.this.finish();
                }
                this.mRequestedType = type;
            }
            String str3 = this.mRequestedType;
            Log.i(TAG, "---> mRequestedType " + mRequestedType);
            if (!"audio/*".equals(str3)) {
                this.mRequestedType = "audio/aac";
            }
            com.actionbarsherlock.app.ActionBar localActionBar = getSupportActionBar();
            if (localActionBar != null) {
                Drawable localDrawable = getResources().getDrawable(R.drawable.title_background);
                localActionBar.setBackgroundDrawable(localDrawable);
                localActionBar.setCustomView(R.layout.actionbar_customview);
                localActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
                //localActionBar.setDisplayOptions(0, ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
                localActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
                localActionBar.setHomeButtonEnabled(false);
            }
        }else {
            Toast.makeText(this, getText(R.string.audio_bind_error), Toast.LENGTH_LONG).show();
            SoundRecorder.this.finish();
        }
    }
    
    public void onResume() {
        super.onResume();
        if(iRecorderService != null) {
            try {
                iRecorderService.regStateListener(iStateListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            } 
        }
    }
    
    private void initResourceRefs() {
        this.mRecordButton = (ImageButton) findViewById(R.id.recordButton);
        this.mPauseButton = (ImageButton) findViewById(R.id.pauseButton);
        this.mStopButton = (ImageButton) findViewById(R.id.stopButton);
        this.mImageClock = (ImageClock) findViewById(R.id.timerView);
        this.mAnimImg = (ImageView) findViewById(R.id.image_d);
        this.mAnimImg.setBackgroundResource(R.anim.animation_d);
        this.mAnimation = (AnimationDrawable) this.mAnimImg.getBackground();
        this.mVUMeter = (com.android.audiorecorder.myview.VUMeter) findViewById(R.id.uvMeter);
        this.mVUMeter.setRecorder(mRecorder);
        this.mRecordButton.setOnClickListener(this);
        this.mStopButton.setOnClickListener(this);
        this.mPauseButton.setOnClickListener(this);
        String str = getResources().getString(2131099664);
        this.mTimerFormat = str;
    }
    
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            iRecorderService = IRecordListener.Stub.asInterface(service);
            if(iRecorderService != null) {
                try {
                    iRecorderService.regStateListener(iStateListener);
                    mRecorder = new Recorder();
                    initResourceRefs();
                    boolean start = iRecorderService.isRecorderStart();
                    mHandler.sendEmptyMessage(MSG_RECORDER_UPDATE_UI);
                    mHandler.sendEmptyMessage(MSG_CHECK_MODE);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }else {
                if(DebugConfig.DEBUG) {
                    Log.e(TAG, "===> onServiceConnected error iRecordListener = " + iRecorderService);
                }
            }
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            iRecorderService = null;
            if(DebugConfig.DEBUG) {
                Log.d(TAG, "===> onServiceDisconnected");
            }
        }
        
    };
    
    private IStateListener.Stub iStateListener = new IStateListener.Stub() {
        
        @Override
        public void onStateChanged(int state) throws RemoteException {
            mHandler.sendEmptyMessage(state);
        }
        
    };
    
    private void openOptionDialog(int type) {
        ContextThemeWrapper localContextThemeWrapper = new ContextThemeWrapper(this, R.style.AlertDialogCustom);
        LayoutInflater localLayoutInflater = (LayoutInflater)localContextThemeWrapper.getSystemService(LAYOUT_INFLATER_SERVICE);
        if(type == 0){
            SettingAdapter adpater = new SettingAdapter(this, R.layout.setting_list_item, localLayoutInflater);
            adpater.add(R.string.storage_setting_Local_item);
            adpater.add(R.string.storage_setting_sdcard_item);
            AlertDialog.Builder localBuilder1 = new AlertDialog.Builder(this).setTitle(R.string.storage_setting);
            localAlertDialog = localBuilder1.setSingleChoiceItems(adpater, mPreferences.getInt(PREFERENCE_TAG_STORAGE_LOCATION, STORAGE_LOCATION_DEFAULT), new OnClickListener() {
                
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mPreferences.edit().putInt(PREFERENCE_TAG_STORAGE_LOCATION, which).commit();
                    localAlertDialog.dismiss();
                }
            }).create();
            localAlertDialog.setCanceledOnTouchOutside(true);
            localAlertDialog.show();
        } else if(type == 1){
            SettingAdapter adpater = new SettingAdapter(this, R.layout.setting_list_item, localLayoutInflater);
            adpater.add(R.string.format_setting_AMR_item);
            adpater.add(R.string.format_setting_3GPP_item);
            int checkedIndex = mPreferences.getInt(PREFERENCE_TAG_FILE_TYPE, FILE_TYPE_DEFAULT);
            AlertDialog.Builder localBuilder1 = new AlertDialog.Builder(this).setTitle(R.string.format_setting);
            localAlertDialog = localBuilder1.setSingleChoiceItems(adpater, checkedIndex, new OnClickListener() {
                
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mPreferences.edit().putInt(PREFERENCE_TAG_FILE_TYPE, which).commit();
                    localAlertDialog.dismiss();
                }
            }).create();
            localAlertDialog.setCanceledOnTouchOutside(true);
            localAlertDialog.show();
        }
    }

    private void popToast(String paramString) {
        Toast localToast = Toast.makeText(this, paramString, 0);
        localToast.setGravity(17, 0, 0);
        localToast.show();
    }

    private long updateTimerView() {
        boolean start = false;
        if(iRecorderService != null) {
            try {
                start = iRecorderService.isRecorderStart();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        int time = start ? mRecorder.progress() : 0;
        mImageClock.setText(time);
        return 0;
    }

    private void updateUi() {
        updateTimerView();   
        updateButtonStatus();
        mVUMeter.invalidate();
    }

    private void updateButtonStatus() {
        if(iRecorderService != null) {
            try {
                boolean start = iRecorderService.isRecorderStart();
                if(!start) {
                    this.mRecordButton.setEnabled(true);
                    this.mRecordButton.setFocusable(true);
                    this.mRecordButton.setVisibility(View.VISIBLE);
                    this.mStopButton.setVisibility(View.GONE);
                    this.mStopButton.setEnabled(false);
                    this.mStopButton.setFocusable(false);
                    this.mAnimation.stop();
                }else {
                    this.mRecordButton.setEnabled(false);
                    this.mRecordButton.setFocusable(false);
                    this.mRecordButton.setVisibility(View.GONE);
                    this.mStopButton.setVisibility(View.VISIBLE);
                    this.mStopButton.setEnabled(true);
                    this.mStopButton.setFocusable(true);
                    this.mAnimation.start();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            } 
        }
    }
    
    public void onClick(View paramView) {
        switch(paramView.getId()){
            case R.id.recordButton:
                int storage = mPreferences.getInt(PREFERENCE_TAG_STORAGE_LOCATION, STORAGE_LOCATION_DEFAULT);
                if(storage == STORAGE_LOCATION_LOCAL_PHONE){
                    int availSize = FileUtils.getAvailableSize(Environment.getDataDirectory().getPath());
                    if(availSize<MIX_STORAGE_CAPACITY){
                        popToast(getString(R.string.storage_is_full));
                        return;
                    }
                } else if(storage == STORAGE_LOCATION_SD_CARD){
                    if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                        popToast(getString(R.string.insert_sd_card));
                        return;
                    }
                    int availSize = FileUtils.getAvailableSize(Environment.getExternalStorageDirectory().getPath());
                    if(availSize<MIX_STORAGE_CAPACITY){
                        popToast(getString(R.string.storage_is_full));
                        return;
                    }
                }
                recordOperation();
                break;
            case R.id.pauseButton:
            case R.id.stopButton:
                recordOperation();
                break;
                default:
                    break;
        }
    }

    
    private void recordOperation(){
        try {
            if(iRecorderService.isRecorderStart()){
                iRecorderService.stopRecord();
                popToast(getString(R.string.record_saved));
            } else {
                iRecorderService.startRecord();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    
    public boolean onCreateOptionsMenu(Menu paramMenu) {
        getSupportMenuInflater().inflate(R.menu.main_menu, paramMenu);
        return super.onCreateOptionsMenu(paramMenu);
    }

    @Override
    protected void onStop() {
    	if(iRecorderService != null) {
            try {
                if(!iRecorderService.isRecorderStart() && iRecorderService.getMode() == AudioService.LUNCH_MODE_MANLY){
                    iRecorderService.setMode(AudioService.LUNCH_MODE_AUTO);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    	super.onStop();
    }
    
    public void onDestroy() {
        super.onDestroy();
        Log.v("BlueSoundRecorder", "onDestroy");
        if(iRecorderService != null) {
            try {
                iRecorderService.unregStateListener(iStateListener);
                if(!iRecorderService.isRecorderStart() && iRecorderService.getMode() == AudioService.LUNCH_MODE_MANLY){
                    iRecorderService.setMode(AudioService.LUNCH_MODE_AUTO);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        if(mServiceConnection != null){
            unbindService(mServiceConnection);
            mServiceConnection = null;
        }
    }

    public boolean onOptionsItemSelected(MenuItem paramMenuItem) {
        switch (paramMenuItem.getItemId()) {
            case R.id.menu_item_storage:
                openOptionDialog(0);
                break;
            case R.id.menu_item_filetype:
                openOptionDialog(1);
                break;
            case R.id.menu_item_setting:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.list:
                Intent localIntent = new Intent();
                localIntent.setClass(this, RecordList.class);
                startActivity(localIntent);
                break;
                default:
                    break;
        }
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu paramMenu) {
        if(iRecorderService != null) {
            try {
                boolean start = iRecorderService.isRecorderStart();
                MenuItem localMenuItem = paramMenu.findItem(R.id.menu_item_storage);
                localMenuItem.setEnabled(!start);
                MenuItem typeMenuItem = paramMenu.findItem(R.id.menu_item_filetype);
                typeMenuItem.setEnabled(!start);
            } catch(RemoteException e){
                
            }
        }
        return true;
    }

    public void onStateChanged(int paramInt) {
        if ((paramInt == 2) || (paramInt == 1)) {
            this.mSampleInterrupted = false;
            this.mErrorUiMessage = null;
        }
        /*while (true) {
            updateUi();
            return;
            if (!this.mWakeLock.isHeld())
                continue;
            this.mWakeLock.release();
        }*/
    }

    public void stopAudioPlayback() {
        Intent localIntent = new Intent("com.android.music.musicservicecommand");
        localIntent.putExtra("command", "pause");
        sendBroadcast(localIntent);
    }

    public void stopFmPlayback() {
        Intent localIntent = new Intent("com.quicinc.fmradio.fmoff");
        sendBroadcast(localIntent);
    }
    
    private class SettingAdapter extends ArrayAdapter<Integer>{

        private LayoutInflater mLayoutInflater;
        private int mResource;
        
        public SettingAdapter(Context context, int resource, LayoutInflater layoutInflater) {
            super(context, resource);
            this.mLayoutInflater = layoutInflater;
            this.mResource = resource;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null){
                convertView = mLayoutInflater.inflate(mResource, null);
            }
            ((TextView)convertView).setText((Integer)getItem(position));
            return convertView;
        }
        
    }
    public class Recorder {
        
        public Recorder() {
            
        }
        
        public int getMaxAmplitude() {
            if (iRecorderService == null) {
                Log.d(TAG, "===> audio record state stop");
                return 0;
            }
            try {
                return iRecorderService.getMaxAmplitude();
            } catch (RemoteException e) {
                Log.d(TAG, "===> getMaxAmplitude error " + e.getMessage());
                e.printStackTrace();
            }
            return 0;
        }
        
        public int progress() {
            if (iRecorderService != null) {
                try {
                    return iRecorderService.getRecorderTime();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            return 0;
        }
        
        public boolean state() {
            if(iStateListener != null) {
                try {
                    return iRecorderService.isRecorderStart();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
    }
}