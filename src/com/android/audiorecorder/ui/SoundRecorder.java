package com.android.audiorecorder.ui;

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
import com.android.audiorecorder.DebugConfig;
import com.android.audiorecorder.R;
import com.android.audiorecorder.engine.IAudioService;
import com.android.audiorecorder.engine.IAudioStateListener;
import com.android.audiorecorder.engine.MultiMediaService;
import com.android.audiorecorder.engine.UpdateManager;
import com.android.audiorecorder.myview.ImageClock;
import com.android.audiorecorder.provider.FileProviderService;
import com.android.audiorecorder.utils.UIHelper;
import com.baidu.mobstat.StatService;

public class SoundRecorder extends SherlockActivity implements View.OnClickListener {
    
    static final String ANY_ANY = "*/*";
    static final String AUDIO_3GPP = "audio/aac";
    static final String AUDIO_AMR = "audio/amr";
    static final String AUDIO_ANY = "audio/*";
    static final int BITRATE_3GPP = 65536;
    static final int BITRATE_AMR = 12841;
    public static final int FILE_TYPE_3GPP = 0;
    public static final int FILE_TYPE_WAV = 1;
    static final String MAX_FILE_SIZE_KEY = "max_file_size";
    public static float PIXEL_DENSITY = 0.0F;
    public static final String PREFERENCE_TAG_FILE_TYPE = "filetype";
    public static final String PREFERENCE_TAG_STORAGE_LOCATION = "storagepath";
    static final String SAMPLE_INTERRUPTED_KEY = "sample_interrupted";
    static final int SETTING_TYPE_FILE_TYPE = 1;
    static final int SETTING_TYPE_STORAGE_LOCATION = 0;
    static final String STATE_FILE_NAME = "soundrecorder.state";
    public static final int STORAGE_LOCATION_LOCAL_PHONE = 0;
    public static final int STORAGE_LOCATION_SD_CARD = 1;
    static final String TAG = "SoundRecorder";
    static final boolean isCMCC = false;
    public static boolean mIsLandScape;
    ImageView imageLeft;
    ImageView imageRight;
    ImageView mAnimImg;
    AnimationDrawable mAnimation;
    String mErrorUiMessage = null;
    ImageClock mImageClock;
    ImageButton mListButton;
    long mMaxFileSize = 65535L;
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
    
    String mTimerFormat;
    TextView mTimerView;
    com.android.audiorecorder.myview.VUMeter mVUMeter;
    private AlertDialog localAlertDialog;
    private IAudioService iRecorderService;
    
    private boolean mAudioRecordStart;

    private Handler mHandler = new Handler(){
       public void handleMessage(android.os.Message msg) {
           updateUi();
       };  
    };

    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        startService(new Intent(this, MultiMediaService.class));
        startService(new Intent(this, FileProviderService.class));
        if(bindService(new Intent(MultiMediaService.Action_Audio_Record), mServiceConnection, Context.BIND_AUTO_CREATE)){
            this.mPreferences = getSharedPreferences(SettingsActivity.class.getName(), Context.MODE_PRIVATE);
            setContentView(R.layout.layout_sound_record);
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
                localActionBar.setCustomView(R.layout.layout_actionbar_customview);
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
                iRecorderService.regStateListener(iAudioStateListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            } 
        }
        StatService.onResume(this);
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	StatService.onPause(this);
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
        //String str = getResources().getString(2131099664);
        //this.mTimerFormat = str;
    }
    
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            iRecorderService = IAudioService.Stub.asInterface(service);
            if(iRecorderService != null) {
                try {
                    iRecorderService.regStateListener(iAudioStateListener);
                    mRecorder = new Recorder();
                    initResourceRefs();
                    mAudioRecordStart = (iRecorderService.getAudioRecordState() == MultiMediaService.STATE_BUSY);
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
    
    private IAudioStateListener.Stub iAudioStateListener = new IAudioStateListener.Stub() {
        
        @Override
        public void onStateChanged(int state) throws RemoteException {
            if(state == MultiMediaService.STATE_BUSY){
                mAudioRecordStart = true;
            } else if(state == MultiMediaService.STATE_IDLE){
                mAudioRecordStart = false;
            }
            mHandler.sendEmptyMessage(state);
        }
        
    };
    
    private void openOptionDialog(int type) {
        ContextThemeWrapper localContextThemeWrapper = new ContextThemeWrapper(this, R.style.AlertDialogCustom);
        LayoutInflater localLayoutInflater = (LayoutInflater)localContextThemeWrapper.getSystemService(LAYOUT_INFLATER_SERVICE);
        if(type == 0){
            SettingAdapter adpater = new SettingAdapter(this, R.layout.setting_list_item, localLayoutInflater);
            adpater.add(R.string.storage_setting_local_item);
            adpater.add(R.string.storage_setting_sdcard_item);
            AlertDialog.Builder localBuilder1 = new AlertDialog.Builder(this).setTitle(R.string.storage_setting);
            localAlertDialog = localBuilder1.setSingleChoiceItems(adpater, mPreferences.getInt(PREFERENCE_TAG_STORAGE_LOCATION, STORAGE_LOCATION_SD_CARD), new OnClickListener() {
                
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mPreferences.edit().putInt(PREFERENCE_TAG_STORAGE_LOCATION, which==1 ? STORAGE_LOCATION_SD_CARD : STORAGE_LOCATION_LOCAL_PHONE).commit();
                    localAlertDialog.dismiss();
                }
            }).create();
            localAlertDialog.setCanceledOnTouchOutside(true);
            localAlertDialog.show();
        } else if(type == 1){
            SettingAdapter adpater = new SettingAdapter(this, R.layout.setting_list_item, localLayoutInflater);
            adpater.add(R.string.format_setting_3GPP_item);
            adpater.add(R.string.format_setting_AMR_item);
            int checkedIndex = mPreferences.getInt(PREFERENCE_TAG_FILE_TYPE, FILE_TYPE_3GPP);
            AlertDialog.Builder localBuilder1 = new AlertDialog.Builder(this).setTitle(R.string.format_setting);
            localAlertDialog = localBuilder1.setSingleChoiceItems(adpater, checkedIndex, new OnClickListener() {
                
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mPreferences.edit().putInt(PREFERENCE_TAG_FILE_TYPE, which==0 ? FILE_TYPE_3GPP : FILE_TYPE_WAV).commit();
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
        int time = mAudioRecordStart ? mRecorder.progress() : 0;
        mImageClock.setText(time);
        return 0;
    }

    private void updateUi() {
        updateTimerView();   
        updateButtonStatus();
    }

    private void updateButtonStatus() {
        if(!mAudioRecordStart) {//idle
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
    }
    
    public void onClick(View paramView) {
        switch(paramView.getId()){
            case R.id.recordButton:
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
            if(iRecorderService != null){
                if(iRecorderService.getAudioRecordState() == MultiMediaService.STATE_BUSY){
                    iRecorderService.stopRecord();
                    popToast(getString(R.string.record_saved));
                } else  if(iRecorderService.getAudioRecordState() == MultiMediaService.STATE_IDLE){
                    iRecorderService.startRecord();
                }
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
    	super.onStop();
    }
    
    public void onDestroy() {
        super.onDestroy();
        Log.v("BlueSoundRecorder", "onDestroy");
        if(iRecorderService != null) {
            try {
                iRecorderService.unregStateListener(iAudioStateListener);
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
                localIntent.setClass(this, MainThumbList.class);
                localIntent.putExtra(MainThumbList.EXTRA_FILE_TYPE, UIHelper.LISTVIEW_DATATYPE_LOCAL_AUDIO);
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
                int recordState = iRecorderService.getAudioRecordState();
                /*MenuItem localMenuItem = paramMenu.findItem(R.id.menu_item_storage);
                localMenuItem.setEnabled(recordState==MultiMediaService.STATE_IDLE);*/
                MenuItem typeMenuItem = paramMenu.findItem(R.id.menu_item_filetype);
                typeMenuItem.setEnabled(recordState==MultiMediaService.STATE_IDLE);
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
            return mAudioRecordStart;
        }
    }
}