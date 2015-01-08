package com.android.audiorecorder;

import java.io.File;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.YuvImage;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.StatFs;
import android.provider.MediaStore;
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

public class SoundRecorder extends SherlockActivity implements View.OnClickListener {
    
    public final static int MIX_STORAGE_CAPACITY = 500;//MB
    public final static int MSG_RECORDER_UPDATE_UI = 10;
    
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
    private IRecordListener iRecordListener;

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
               default:
                   break;
           }
       };  
    };
    /*static {
        String str1 = String.valueOf(Environment.getExternalStorageDirectory()
                .toString());
        STORAGE_PATH_SD_CARD = str1 + "/Audio/Record";
        Object localObject = phoneStrorage();
        if (localObject != null) {
            String str2 = String.valueOf(phoneStrorage().toString());
            localObject = str2 + "/Audio/Record";
        }
        while (true) {
            STORAGE_PATH_LOCAL_PHONE = (String) localObject;
            //PIXEL_DENSITY = null;
            return;
            int i = 0;
        }
    }*/

    private Uri addToMediaDB(File paramFile, int paramInt)
{
  /*Resources localResources = getResources();
  ContentValues localContentValues = new ContentValues();
  long l1 = System.currentTimeMillis();
  long l2 = paramFile.lastModified();
  Object localObject1;
  Date localDate = new Date(localObject1);
  int i = 2131099666;
  String str1 = localResources.getString(i);
  String str2 = new SimpleDateFormat(str1).format(localDate);
  StringBuilder localStringBuilder1 = new StringBuilder("recording ");
  String str3 = str2;
  String str4 = str3;
  String str5 = "is_music";
  String str6 = "1";
  localContentValues.put(str5, str6);
  String str7 = getFileNameNoEx(paramFile.getName());
  String str8 = "title";
  String str9 = str7;
  localContentValues.put(str8, str9);
  String str10 = paramFile.getAbsolutePath();
  String str11 = "_data";
  String str12 = str10;
  localContentValues.put(str11, str12);
  Long localLong1 = Long.valueOf(paramInt * 1000L);
  String str13 = "duration";
  Long localLong2 = localLong1;
  localContentValues.put(str13, localLong2);
  Integer localInteger1 = Integer.valueOf((int)(localObject1 / 1000L));
  String str14 = "date_added";
  Integer localInteger2 = localInteger1;
  localContentValues.put(str14, localInteger2);
  Object localObject2;
  Integer localInteger3 = Integer.valueOf((int)(localObject2 / 1000L));
  String str15 = "date_modified";
  Integer localInteger4 = localInteger3;
  localContentValues.put(str15, localInteger4);
  String str16 = this.mRequestedType;
  String str17 = "mime_type";
  String str18 = str16;
  localContentValues.put(str17, str18);
  if (this.DEBUG)
  {
    StringBuilder localStringBuilder2 = new StringBuilder("Inserting audio record: ");
    String str19 = localContentValues.toString();
    String str20 = str19;
    Log.v("SoundRecorder", str20);
  }
  ContentResolver localContentResolver = getContentResolver();
  Uri localUri1 = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
  if (this.DEBUG)
  {
    String str21 = "ContentURI: " + localUri1;
    Log.v("SoundRecorder", str21);
  }
  Uri localUri2 = localContentResolver.insert(localUri1, localContentValues);
  if (this.DEBUG)
  {
    String str22 = "resolver.insert result URI=" + localUri2;
    Log.v("SoundRecorder", str22);
  }
  int j;
  if (localUri2 == null)
  {
    AlertDialog.Builder localBuilder1 = new android/app/AlertDialog$Builder;
    AlertDialog.Builder localBuilder2 = localBuilder1;
    SoundRecorder localSoundRecorder1 = this;
    localBuilder2.<init>(localSoundRecorder1);
    localBuilder1.setTitle(2131099648).setMessage(2131099672).setPositiveButton(2131099663, null).setCancelable(null).show();
    j = 0;
  }*/
 /* while (true)
  {
    return j;
    int k = getPlaylistId(localResources);
    int m = 65535;
    if (k == m)
      createPlaylist(localResources, localContentResolver);
    int n = Integer.valueOf(j.getLastPathSegment()).intValue();
    long l3 = getPlaylistId(localResources);
    SoundRecorder localSoundRecorder2 = this;
    long l4 = l3;
    localSoundRecorder2.addToPlaylist(localContentResolver, n, l4);
    Intent localIntent1 = new android/content/Intent;
    Intent localIntent2 = localIntent1;
    String str23 = "android.intent.action.MEDIA_SCANNER_SCAN_FILE";
    localIntent2.<init>(str23, j);
    SoundRecorder localSoundRecorder3 = this;
    Intent localIntent3 = localIntent1;
    localSoundRecorder3.sendBroadcast(localIntent3);
  }*/
        return null;
}

    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        if(bindService(new Intent(AudioService.Action_RecordListen), serviceConnection, Context.BIND_AUTO_CREATE)){
            startService(new Intent(this, AudioService.class));
            this.mPreferences = getSharedPreferences("SoundRecorder", Context.MODE_PRIVATE);
            setContentView(R.layout.main1);
            reloadQueue();
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
            registerExternalStorageListener();
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
        if(iRecordListener != null) {
            try {
                iRecordListener.regStateListener(iStateListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            } 
        }
        if (this.DEBUG)
            Log.v("SoundRecorder", "onResume");
        /*this.mRecorder.resetState();
        stopFmPlayback();*/
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
    
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            iRecordListener = IRecordListener.Stub.asInterface(service);
            if(iRecordListener != null) {
                try {
                    iRecordListener.regStateListener(iStateListener);
                    mRecorder = new Recorder();
                    initResourceRefs();
                    boolean start = iRecordListener.isRecorderStart();
                    mHandler.sendEmptyMessage(MSG_RECORDER_UPDATE_UI);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }else {
                if(DebugConfig.DEBUG) {
                    Log.e(TAG, "===> onServiceConnected error iRecordListener = " + iRecordListener);
                }
            }
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            iRecordListener = null;
            if(DebugConfig.DEBUG) {
                Log.d(TAG, "===> onServiceDisconnected");
            }
        }
        
    };
    
    private IStateListener.Stub iStateListener = new IStateListener.Stub() {
        
        @Override
        public void onStateChanged(int state) throws RemoteException {
            Log.d(TAG, "===> onStateChanged  state = " + state);
            mHandler.sendEmptyMessage(state);
        }
        
    };
    
    private void addToPlaylist(ContentResolver paramContentResolver,
            int paramInt, long paramLong) {
        String[] arrayOfString1 = new String[1];
        arrayOfString1[0] = "count(*)";
        Uri localUri = MediaStore.Audio.Playlists.Members.getContentUri(
                "external", paramLong);
        ContentResolver localContentResolver = paramContentResolver;
        String[] arrayOfString2 = null;
        String str = null;
        Cursor localCursor = localContentResolver.query(localUri,
                arrayOfString1, null, arrayOfString2, str);
        localCursor.moveToFirst();
        int i = localCursor.getInt(0);
        localCursor.close();
        ContentValues localContentValues = new ContentValues();
        Integer localInteger1 = Integer.valueOf(i + paramInt);
        localContentValues.put("play_order", localInteger1);
        Integer localInteger2 = Integer.valueOf(paramInt);
        localContentValues.put("audio_id", localInteger2);
        paramContentResolver.insert(localUri, localContentValues);
    }

    private Uri createPlaylist(Resources paramResources,
            ContentResolver paramContentResolver) {
        int i = 2131099669;
        ContentValues localContentValues = new ContentValues();
        String str1 = paramResources.getString(i);
        localContentValues.put("name", str1);
        Uri localUri1 = MediaStore.Audio.Playlists.getContentUri("external");
        Uri localUri2 = paramContentResolver.insert(localUri1,
                localContentValues);
        if (this.DEBUG) {
            StringBuilder localStringBuilder = new StringBuilder(
                    "createPlaylist Playlists.NAME = ");
            String str2 = paramResources.getString(i);
            String str3 = str2;
            Log.i("SoundRecorder", str3);
        }
        if (localUri2 == null)
            new AlertDialog.Builder(this).setTitle(2131099648)
                    .setMessage(2131099672).setPositiveButton(2131099663, null)
                    .setCancelable(false).show();
        return localUri2;
    }

    public static String getFileNameNoEx(String paramString) {
        if ((paramString != null) && (paramString.length() > 0)) {
            int i = paramString.lastIndexOf('.');
            if (i > -1) {
                int j = paramString.length();
                if (i < j)
                    paramString = paramString.substring(0, i);
            }
        }
        return paramString;
    }

    private int getPlaylistId(Resources paramResources) {
        int i = 0;
        int j = -1;
        Uri localUri = MediaStore.Audio.Playlists.getContentUri("external");
        String[] arrayOfString1 = new String[1];
        arrayOfString1[i] = "_id";
        String[] arrayOfString2 = new String[1];
        String str = paramResources.getString(2131099669);
        arrayOfString2[i] = str;
        Cursor localCursor = query(localUri, arrayOfString1, "name=?",
                arrayOfString2, null);
        if ((localCursor == null) && (this.DEBUG))
            Log.v("SoundRecorder", "query returns null");
        int k = 0;
        if (localCursor != null) {
            localCursor.moveToFirst();
            if (!localCursor.isAfterLast())
                k = localCursor.getInt(i);
            localCursor.close();
        }
        return k;
    }


    private void openOptionDialog(int type) {
        ContextThemeWrapper localContextThemeWrapper = new ContextThemeWrapper(this, R.style.AlertDialogCustom);
        LayoutInflater localLayoutInflater = (LayoutInflater)localContextThemeWrapper.getSystemService(LAYOUT_INFLATER_SERVICE);
        SettingAdapter adpater = new SettingAdapter(this, R.layout.setting_list_item, localLayoutInflater);
        adpater.clear();
        if(type == 0){
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
            adpater.add(R.string.format_setting_AMR_item);
            adpater.add(R.string.format_setting_3GPP_item);
            AlertDialog.Builder localBuilder1 = new AlertDialog.Builder(this).setTitle(R.string.format_setting);
            localAlertDialog = localBuilder1.setSingleChoiceItems(adpater, mPreferences.getInt(PREFERENCE_TAG_FILE_TYPE, FILE_TYPE_DEFAULT), new OnClickListener() {
                
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

    private Cursor query(Uri paramUri, String[] paramArrayOfString1, String paramString1, String[] paramArrayOfString2, String paramString2)
{
  /*int i = 0;
  try
  {
    ContentResolver localContentResolver = getContentResolver();
    if (localContentResolver == null);
    String[] arrayOfString1;
    String str1;
    String[] arrayOfString2;
    String str2;
    for (localObject = i; ; localObject = localContentResolver.query((Uri)localObject, arrayOfString1, str1, arrayOfString2, str2))
    {
      return localObject;
      localObject = paramUri;
      arrayOfString1 = paramArrayOfString1;
      str1 = paramString1;
      arrayOfString2 = paramArrayOfString2;
      str2 = paramString2;
    }
  }
  catch (UnsupportedOperationException localUnsupportedOperationException)
  {
    while (true)
      Object localObject = i;
  }*/
        return null;
}

    private void queueNextRefresh(long paramLong) {
        /*int i = 1;
        if (this.mRecorder.state() != 0) {
            Message localMessage = this.mHandler.obtainMessage(i);
            this.mHandler.removeMessages(i);
            this.mHandler.sendMessageDelayed(localMessage, paramLong);
        }*/
    }

    private void registerExternalStorageListener()
{
  /*if (this.mSDCardMountEventReceiver == null)
  {
    SoundRecorder.3 local3 = new SoundRecorder.3(this);
    this.mSDCardMountEventReceiver = local3;
    IntentFilter localIntentFilter = new IntentFilter();
    localIntentFilter.addAction("android.intent.action.MEDIA_EJECT");
    localIntentFilter.addAction("android.intent.action.MEDIA_MOUNTED");
    localIntentFilter.addAction("android.intent.action.MEDIA_REMOVED");
    localIntentFilter.addDataScheme("file");
    BroadcastReceiver localBroadcastReceiver = this.mSDCardMountEventReceiver;
    registerReceiver(localBroadcastReceiver, localIntentFilter);
  }*/
}

    private void reloadQueue() {
        /*int i = this.mPreferences.getInt("storagepath", 0);
        this.mStoragePath = i;
        Recorder localRecorder = this.mRecorder;
        int j = this.mStoragePath;
        localRecorder.setStorage(j);
        //RemainingTimeCalculator localRemainingTimeCalculator = this.mRemainingTimeCalculator;
        int k = this.mStoragePath;
        localRemainingTimeCalculator.setStoragePath(k);
        int m = this.mPreferences.getInt("filetype", 1);
        this.mFileType = m;
        if (this.mFileType == 0)
            ;
        for (this.mRequestedType = "audio/amr";; this.mRequestedType = "audio/aac")
            return;*/
    }

    private void saveQueue() {
        SharedPreferences.Editor localEditor = this.mPreferences.edit();
        int i = this.mStoragePath;
        localEditor.putInt("storagepath", i);
        int j = this.mFileType;
        localEditor.putInt("filetype", j);
        localEditor.commit();
        if (this.DEBUG) {
            StringBuilder localStringBuilder = new StringBuilder(
                    "---mStoragePath = ");
            int k = this.mStoragePath;
        }
    }

    private void saveSample() {
        /*if (this.mRecorder.sampleLength() <= 0) {
            if (this.DEBUG)
                Log.v("SoundRecorder",
                        "saveSample()  mRecorder.sampleLength() <= 0");
            this.mRecorder.delete();
        }
        while (true) {
            return;
            if (this.mRecorder.sampleFile() == null) {
                if (!this.DEBUG)
                    continue;
                Log.v("SoundRecorder", "samplefile is null---");
                continue;
            }
            if (this.DEBUG)
                Log.v("SoundRecorder",
                        "saveSample()  mRecorder.sampleLength() != 0");
            String str = getString(2131099708);
            popToast(str);
            Uri localUri = null;
            try {
                File localFile = this.mRecorder.sampleFile();
                int i = this.mRecorder.sampleLength();
                localUri = addToMediaDB(localFile, i);
                this.mRecorder.resetState();
                if (localUri == null)
                    continue;
                Intent localIntent = new Intent().setData(localUri);
                setResult(-1, localIntent);
                if (!this.mOneShot)
                    continue;
                finish();
            } catch (UnsupportedOperationException localUnsupportedOperationException) {
            }
        }*/
    }

    private long updateTimerView() {
        boolean start = false;
        if(iRecordListener != null) {
            try {
                start = iRecordListener.isRecorderStart();
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
        if(iRecordListener != null) {
            try {
                boolean start = iRecordListener.isRecorderStart();
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
            if(iRecordListener.isRecorderStart()){
                iRecordListener.stopRecord();
            } else {
                iRecordListener.startRecord();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    
    public boolean onCreateOptionsMenu(Menu paramMenu) {
        getSupportMenuInflater().inflate(R.menu.main_menu, paramMenu);
        return super.onCreateOptionsMenu(paramMenu);
    }

    public void onDestroy() {
        super.onDestroy();
        Log.v("SoundRecorder", "onDestroy");
        /*if (this.mSDCardMountEventReceiver != null) {
            BroadcastReceiver localBroadcastReceiver = this.mSDCardMountEventReceiver;
            unregisterReceiver(localBroadcastReceiver);
            this.mSDCardMountEventReceiver = null;
        }*/
        if(iRecordListener != null) {
            try {
                iRecordListener.unregStateListener(iStateListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
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
        if(iRecordListener != null) {
            try {
                boolean start = iRecordListener.isRecorderStart();
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
                ((TextView)convertView).setText((Integer)getItem(position));
            }
            return convertView;
        }
        
    }
    public class Recorder {
        
        public Recorder() {
            
        }
        
        public int getMaxAmplitude() {
            if (iRecordListener == null) {
                Log.d(TAG, "===> audio record state stop");
                return 0;
            }
            try {
                return iRecordListener.getMaxAmplitude();
            } catch (RemoteException e) {
                Log.d(TAG, "===> getMaxAmplitude error " + e.getMessage());
                e.printStackTrace();
            }
            return 0;
        }
        
        public int progress() {
            if (iRecordListener != null) {
                try {
                    return iRecordListener.getRecorderTime();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            return 0;
        }
        
        public boolean state() {
            if(iStateListener != null) {
                try {
                    return iRecordListener.isRecorderStart();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
    }
}