package com.android.audiorecorder;

import java.util.List;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.android.audiorecorder.RecordListAdapter.ITaskClickListener;
import com.android.audiorecorder.audio.IMediaPlaybackService;
import com.android.audiorecorder.audio.MediaPlaybackService;
import com.android.audiorecorder.audio.MusicUtils;
import com.android.audiorecorder.audio.MusicUtils.ServiceToken;
import com.android.audiorecorder.dao.FileManagerFactory;
import com.android.audiorecorder.dao.IFileManager;
import com.android.audiorecorder.engine.AudioService;
import com.android.audiorecorder.utils.FileUtils;
import com.baidu.mobstat.StatService;

public class RecordList extends SherlockListActivity implements
        View.OnCreateContextMenuListener, OnItemClickListener, ITaskClickListener{
    
    public final static int PAGE_NUMBER = 100;
    
    public final static int IDLE = 0;
    public final static int PLAY = 1;
    public final static int PAUSE = 2;
    
    private int mCurState = IDLE;
    
    public final static int MSG_TOGGLE_UI = 5;
    public final static int MSG_REFRESH_LIST = 10;
    
    public static final int ITEM_OPERATION_PLAY = 1;
    public static final int ITEM_OPERATION_RENAME = 2;
    public static final int ITEM_OPERATION_DETAILS = 3;
    public static final int ITEM_OPERATION_DELETE = 4;
    
    private int mCurPlayIndex;
    
    private BroadcastReceiver mPlayCompleteReciBroadcastReceiver;
    
    /*
     *
     arrayOfString1[0] = "_id";
        arrayOfString1[1] = "title";
        arrayOfString1[2] = "_data";
        arrayOfString1[3] = "album";
        arrayOfString1[4] = "artist";
        arrayOfString1[5] = "artist_id";
        arrayOfString1[6] = "mime_type";
        arrayOfString1[7] = "duration";
        arrayOfString1[8] = "play_order";
        arrayOfString1[9] = "audio_id";
        arrayOfString1[10] = "_size";
     
     */
    private static final int ALBUM_ART_DECODED = 4;
    public static final int CURSOR_COLUMN_INDEX_ALBUM = 3;
    public static final int CURSOR_COLUMN_INDEX_ARTIST = 4;
    public static final int CURSOR_COLUMN_INDEX_ARTIST_ID = 5;
    public static final int CURSOR_COLUMN_INDEX_AUDIO_ID = 9;
    public static final int CURSOR_COLUMN_INDEX_DATA = 2;
    public static final int CURSOR_COLUMN_INDEX_DURATION = 7;
    public static final int CURSOR_COLUMN_INDEX_ID = 0;
    public static final int CURSOR_COLUMN_INDEX_MIME_TYPE = 6;
    public static final int CURSOR_COLUMN_INDEX_PLAY_ORDER = 8;
    public static final int CURSOR_COLUMN_INDEX_SIZE = 10;
    public static final int CURSOR_COLUMN_INDEX_TITLE = 1;
    private static final boolean DEBUG = true;
    private static final int FADEDOWN = 5;
    private static final int FADEUP = 6;
    private static final int FOCUSCHANGE = 4;
    private static final int GET_ALBUM_ART = 3;

    private static final int MAX_HISTORY_SIZE = 100;
    private static final int QUIT = 2;
    private static final int REFRESH = 1;
    private static final int RELEASE_WAKELOCK = 2;
    private static final int SERVER_DIED = 3;
    private static final int TRACK_ENDED = 1;
    private static Cursor mCursor = null;
    protected int[] NameCheckList;
    protected String NameCheckList_Str;
    private String TAG = "RecordList";
    private RecordListAdapter mAdapter;
    private List<RecorderFile> mFileList;
    private AudioManager mAudioManager;
    public Handler mCounterhandler;
    private TextView mCurrentTime;
    private String mCurrentTrackName;
    private long mDuration;
    StringBuilder mFormatBuilder;
    Formatter mFormatter;
    private boolean mFromTouch;
    //private final Handler mHandler;
    private TextView mIndicator;
    public boolean mIsSupposedToBePlaying;
    private long mLastSeekEventTime;
    long[] mListToDelete;
    //private MultiPlayer mPlayer;
    public long mPlayingId = 0L;
    private long mPosOverride = -1;
    private ProgressBar mProgress;
    private LinearLayout mProgressLayout;
    //private TrackQueryHandler mQueryHandler;

    public long mSelectedId;
    public int mSelectedPosition;
    private TextView mTotalTime;
    private ListView mTrackList;
    private PowerManager.WakeLock mWakeLock;

    //start audio play
    private IMediaPlaybackService mService;
    private ServiceToken mToken;
    private boolean paused;

    //end audo play
    
    private IFileManager mFileManager;
    
    private Handler mHandler = new Handler(){
        public void handleMessage(android.os.Message msg) {
            switch(msg.what){
                case MSG_REFRESH_LIST:
                    mAdapter.notifyDataSetChanged();
                    break;
                case REFRESH:
                    if(!mFromTouch){
                        long next = refreshNow();
                        queueNextRefresh(next);
                    }
                    break;
                case MSG_TOGGLE_UI:
                    int position = msg.arg1;
                    mAdapter.setPlayId(position, mCurState);
                    mProgressLayout.setVisibility(View.VISIBLE);
                    mHandler.sendEmptyMessage(MSG_REFRESH_LIST);
                    startPlayback(position);
                    mCurPlayIndex = position;
                    break;
                    default:
                        break;
            }
        };
    };

    public void onCreate(Bundle paramBundle) {
        //ModeCallback localModeCallback1 = null;
        super.onCreate(paramBundle);
        setVolumeControlStream(3);
        setContentView(R.layout.recordlist_view);
        this.mTrackList = getListView();
        this.mTrackList.setOnItemClickListener(this);
        //this.mTrackList.setChoiceMode(3);
        //ModeCallback localModeCallback2 = new ModeCallback(localModeCallback1);
        //localListView2.setMultiChoiceModeListener(localModeCallback2);
        setTitle(R.string.list);
        registerForContextMenu(this.mTrackList);
        this.mIndicator = (TextView) findViewById(R.id.indicator);
        this.mProgress = (ProgressBar) findViewById(R.id.progress);
        this.mProgressLayout = (LinearLayout) findViewById(R.id.progresslayout);
        this.mCurrentTime = (TextView) findViewById(R.id.currenttime);
        this.mTotalTime = (TextView) findViewById(R.id.totaltime);
        //int i = getThemeColor(this);
        //localLinearLayout2.setBackgroundColor(i);
        this.mProgressLayout.setVisibility(View.GONE);
        PowerManager localPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
        this.mWakeLock = localPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RecorderPlayer");
        this.mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if ((this.mProgress instanceof SeekBar)) {
            Log.d(this.TAG, "setOnSeekBarChangeListener");
            SeekBar seeker = (SeekBar) this.mProgress;
            seeker.setOnSeekBarChangeListener(mSeekListener);
        }
        this.mProgress.setMax(1000);
        mFileManager = FileManagerFactory.getSmsManagerInstance(this);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            Drawable localDrawable = getResources().getDrawable(R.drawable.title_background);
            actionBar.setBackgroundDrawable(localDrawable);
            actionBar.setCustomView(R.layout.recordlist_customview);
            actionBar.setDisplayOptions(18);
        }
        if(mPlayCompleteReciBroadcastReceiver == null){
            mPlayCompleteReciBroadcastReceiver = new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(MediaPlaybackService.PLAY_COMPLETE_ACTION.equals(intent.getAction())){
                        mProgress.setProgress(1000);
                        Message msg = mHandler.obtainMessage(MSG_TOGGLE_UI);
                        msg.arg1 = mCurPlayIndex;
                        mCurState = PLAY;
                        mHandler.sendMessage(msg);
                    }
                }
            };
            registerReceiver(mPlayCompleteReciBroadcastReceiver, new IntentFilter(MediaPlaybackService.PLAY_COMPLETE_ACTION));
        }
        init();
    }

    public boolean onCreateOptionsMenu(Menu paramMenu) {
        //getSupportMenuInflater().inflate(R.menu.recordlist_menu, paramMenu);
        return super.onCreateOptionsMenu(paramMenu);
    }
    
    public boolean onPrepareOptionsMenu(Menu paramMenu) {
        /*if(mFileList.size() == 0){
            paramMenu.findItem(R.id.menu_item_delete).setEnabled(false);
        }*/
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_item_delete:
                if(mCurPlayIndex == -1 && mCurPlayIndex>=mFileList.size()){
                    Toast.makeText(this, getString(R.string.select_delete_file), Toast.LENGTH_SHORT).show();
                    return true;
                }
                deleteItem(mCurPlayIndex);
                break;
                default:
                    break;
        }
        return true;
    }
    public void init() {
        mFileList = DebugConfig.DEBUG ? mFileManager.queryAllFileList(0, PAGE_NUMBER) : mFileManager.queryPublicFileList(0, PAGE_NUMBER);
        updateCounter();
        mCurPlayIndex = -1;
        this.mAdapter = new RecordListAdapter(this, mFileList);
        setListAdapter(this.mAdapter);
        mAdapter.setPlayId(-1, mCurState);
        mAdapter.setTaskClickListener(this);
    }
    
    private void updateCounter() {
      if ((mFileList.size() == 0)) {
          this.mIndicator.setVisibility(View.GONE);
      } else {
          this.mIndicator.setVisibility(View.VISIBLE);
          int count = mFileManager.getFileCount(DebugConfig.DEBUG ? -1 : AudioService.LUNCH_MODE_MANLY);
          this.mIndicator.setText(getResources().getQuantityString(R.plurals.NNNtrackscount, count, count));
      }
    }
    
	@Override
	protected void onResume() {
		super.onResume();
		StatService.onResume(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		StatService.onPause(this);
	}
	
    @Override
    public void onStart() {
        super.onStart();
        paused = false;

        mToken = MusicUtils.bindToService(this, osc);
        if (mToken == null) {
            mHandler.sendEmptyMessage(QUIT);
        }
    }
    
    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "===> onStop.");
        paused = true;
        mHandler.removeMessages(REFRESH);
        if(mService != null) {
            try {
                mService.stop();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        MusicUtils.unbindFromService(mToken);
        mService = null;
    }
    
    @Override
    public void onDestroy()
    {
        Log.d(TAG, "===> onDestroy.");
        if(mPlayCompleteReciBroadcastReceiver != null){
        	unregisterReceiver(mPlayCompleteReciBroadcastReceiver);
        	mPlayCompleteReciBroadcastReceiver = null;
        }
        super.onDestroy();
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        toggleAudio(position);
    }
    
    @Override
    public void onTaskClick(int index, int action) {
        switch(action){
            case ITEM_OPERATION_PLAY:
                toggleAudio(index);
                break;
            case ITEM_OPERATION_DETAILS:
                mCurPlayIndex = index;
                showInfomationDlg(index);
                break;
            case ITEM_OPERATION_DELETE:
                deleteItem(index);
                mCurPlayIndex = -1;
                break;
                default:
                    break;
        }
    }
    
    private void toggleAudio(int position){
        if(mCurPlayIndex == position){
            if(mCurState == PLAY){
                mCurState = PAUSE;
            } else {
                mCurState = PLAY;
            }
        }else{
            mCurState = PAUSE;
        }
        Message msg = mHandler.obtainMessage(MSG_TOGGLE_UI);
        msg.arg1 = position;
        mHandler.sendMessage(msg);
    }
    
    private void deleteItem(int position){
        if(mCurPlayIndex == position){
            try {
                if(mService != null && mService.isPlaying()){
                    mService.stop();
                    toggleAudio(position);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        RecorderFile file = mFileList.get(position);
        mFileManager.removeFile(file.getPath());
        mFileManager.delete(file.getId());
        mFileList.remove(position);
        mHandler.sendEmptyMessage(MSG_REFRESH_LIST);
        updateCounter();
        Toast.makeText(this, getResources().getQuantityString(R.plurals.NNNtracksdeleted, 1, 1), Toast.LENGTH_SHORT).show();
    }
    
    private ServiceConnection osc = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            mService = IMediaPlaybackService.Stub.asInterface(obj);
            Log.i(TAG, "---> AudioMediaService bind success.");
        }
        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };
    
    private void startPlayback(int position) {
        if(mService == null || mFileList.size()<=position){
            return;
        }
        String filename = mFileList.get(position).getPath();
        if (filename != null && filename.length() > 0) {
            if(position == mCurPlayIndex){
                try {
                    if(mService.isPlaying()){
                        mService.pause();
                    }else {
                        mService.play();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }else {
                if(DebugConfig.DEBUG) {
                    Log.d(TAG, "===> open audio filename = " + filename);
                }
                try {
                    mService.stop();
                    mService.openFile(filename);
                    mService.play();
                } catch (Exception ex) {
                    Log.d("MediaPlaybackActivity", "couldn't start playback: " + ex);
                }
            }
        }
        updateTrackInfo();
        long next = refreshNow();
        queueNextRefresh(next);
    }
    
    private void updateTrackInfo() {
        if (mService == null) {
            return;
        }
        try {
            String path = mService.getPath();
            if (path == null) {
                Toast.makeText(this, R.string.service_open_error_msg, Toast.LENGTH_SHORT).show();
                return;
            }
            mDuration = mService.duration();
            long remain =  mDuration % 1000;
            long totalSeconds = (remain == 0) ? mDuration / 1000 : (mDuration / 1000);
            mTotalTime.setText(MusicUtils.makeTimeString(this, totalSeconds));
        } catch (RemoteException ex) {
        }
    }
    
    private long refreshNow() {
        if(mService == null)
            return 500;
        try {
            long pos = mPosOverride < 0 ? mService.position() : mPosOverride;
            if ((pos >= 0) && (mDuration > 0)) {
                mCurrentTime.setText(MusicUtils.makeTimeString(this, pos / 1000));
                int progress = (int) (1000 * pos / mDuration);
                mProgress.setProgress(progress);
                
                if (mService.isPlaying()) {
                    mCurrentTime.setVisibility(View.VISIBLE);
                } else {
                    // blink the counter
                    int vis = mCurrentTime.getVisibility();
                    mCurrentTime.setVisibility(vis == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
                    return 500;
                }
            } else {
                mCurrentTime.setText("0:00");
                mProgress.setProgress(1000);
            }
            // calculate the number of milliseconds until the next full second, so
            // the counter can be updated at just the right time
            long remaining = 1000 - (pos % 1000);
            // approximate how often we would need to refresh the slider to
            // move it smoothly
            int width = mProgress.getWidth();
            if (width == 0) width = 320;
            long smoothrefreshtime = mDuration / width;
            if(mService.getPlayState() == MediaPlaybackService.PLAY_STATE_COMPLETE && mService.getSeekState() != MediaPlaybackService._STATE_SEEKING) {
                return 500;
            }
            if (smoothrefreshtime > remaining){
                return remaining;
            }
            if (smoothrefreshtime <= 20) {
                return 20;
            }
            return smoothrefreshtime;
        } catch (RemoteException ex) {
        }
        return 500;
    }
    
    private void queueNextRefresh(long delay) {
        if (!paused) {
            Message msg = mHandler.obtainMessage(REFRESH);
            mHandler.removeMessages(REFRESH);
            mHandler.sendMessageDelayed(msg, delay);
        }
    }
    
    
    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            if (mService == null) return;
            mLastSeekEventTime = 0;
            mFromTouch = true;
        }
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser || (mService == null)) return;
            long now = SystemClock.elapsedRealtime();
            if ((now - mLastSeekEventTime) > 250) {
                mLastSeekEventTime = now;
                mPosOverride = mDuration * progress / 1000;
                try {
                    if(mPosOverride<=mDuration){
                        mCurrentTime.setText(MusicUtils.makeTimeString(RecordList.this, mPosOverride / 1000));
                        mService.seek(mPosOverride);
                    }
                } catch (RemoteException ex) {
                }

                if (!mFromTouch) {
                    refreshNow();
                    mPosOverride = -1;
                }
            }
        }
        
        public void onStopTrackingTouch(SeekBar bar) {
            if (mService == null) return;
            long seekTo = mDuration * bar.getProgress() / 1000;
            try {
                if(seekTo<=mDuration){
                    mCurrentTime.setText(MusicUtils.makeTimeString(RecordList.this, mPosOverride / 1000));
                    mService.seek(seekTo);
                }
            } catch (RemoteException ex) {
            }
            mPosOverride = -1;
            mFromTouch = false;
            queueNextRefresh(refreshNow());
        }
    };

    private void showInfomationDlg(int index) {
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService("layout_inflater");
        int i = (int) this.mSelectedId;
        AlertDialog.Builder localBuilder = new AlertDialog.Builder(this);
        localBuilder.setTitle(R.string.information);
        localBuilder.setIcon(android.R.drawable.ic_menu_info_details);
        localBuilder.setPositiveButton(R.string.button_ok, null);
        RecorderFile file = mFileList.get(index);
        View localView2 = layoutInflater.inflate(R.layout.information_dlg, null);
        TextView title = (TextView) localView2.findViewById(R.id.title);
        title.setText(file.getName());
        ImageView thumb = (ImageView) localView2.findViewById(R.id.thumb);
        Drawable localDrawable = getResources().getDrawable(R.drawable.ic_default_thumb);
        thumb.setImageDrawable(localDrawable);
        TextView duration = (TextView) localView2.findViewById(R.id.duration);
        duration.setText(MusicUtils.makeTimeString(this, file.getDuration()));
        TextView size = (TextView) localView2.findViewById(R.id.size);
        size.setText(FileUtils.formetFileSize(file.getSize()));
        TextView type = (TextView) localView2.findViewById(R.id.type);
        type.setText(file.getMimeType());
        TextView pathTextView = (TextView) localView2.findViewById(R.id.path);
        pathTextView.setText(file.getPath());
        localBuilder.setView(localView2);
        localBuilder.show();
    }
    
    /*

    private void exitMultiSelectView() {
        Log.d(this.TAG, "_____exitMultiSelectedView_____");
        Class localClass = this.mTrackList.getClass();
        try {
            Class[] arrayOfClass = new Class[1];
            arrayOfClass[0] = ActionMode.class;
            Method localMethod = localClass.getMethod("setActionMode",
                    arrayOfClass);
            localMethod.setAccessible(true);
            ListView localListView = this.mTrackList;
            Object[] arrayOfObject = new Object[1];
            arrayOfObject[0] = null;
            localMethod.invoke(localListView, arrayOfObject);
            this.mTrackList.clearChoices();
            this.mTrackList.invalidateViews();
            return;
        } catch (IllegalArgumentException localIllegalArgumentException) {
            while (true)
                localIllegalArgumentException.printStackTrace();
        } catch (InvocationTargetException localInvocationTargetException) {
            while (true)
                localInvocationTargetException.printStackTrace();
        } catch (NoSuchMethodException localNoSuchMethodException) {
            while (true)
                localNoSuchMethodException.printStackTrace();
        } catch (IllegalAccessException localIllegalAccessException) {
            while (true)
                localIllegalAccessException.printStackTrace();
        }
    }

    private void holdDialog(DialogInterface paramDialogInterface,
            boolean paramBoolean) {
        try {
            Field localField = paramDialogInterface.getClass().getSuperclass()
                    .getDeclaredField("mShowing");
            localField.setAccessible(true);
            Boolean localBoolean = Boolean.valueOf(paramBoolean);
            localField.set(paramDialogInterface, localBoolean);
            return;
        } catch (Exception localException) {
            while (true)
                localException.printStackTrace();
        }
    }

    private void queueNextRefresh(long paramLong) {
        int i = 1;
        if (!this.isStop) {
            Message localMessage = this.mHandler.obtainMessage(i);
            this.mHandler.removeMessages(i);
            this.mHandler.sendMessageDelayed(localMessage, paramLong);
        }
    }

    protected void onActivityResult(int paramInt1, int paramInt2,
            Intent paramIntent) {
        if ((paramIntent == null) || (paramInt2 != -1))
            ;
        while (true) {
            return;
            Uri localUri = paramIntent.getData();
            if (localUri == null)
                continue;
            String str1 = this.TAG;
            StringBuilder localStringBuilder = new StringBuilder(
                    "requsetcode: ").append(paramInt1).append(" result uri: ");
            String str2 = localUri.toString();
            String str3 = str2;
            Log.d(str1, str3);
            switch (paramInt1) {
                case 0:
            }
        }
    }


    public boolean onContextItemSelected(MenuItem paramMenuItem)
{
  boolean bool1 = false;
  int i = 1;
  boolean bool2;
  switch (paramMenuItem.getItemId())
  {
  default:
    bool2 = super.onContextItemSelected(paramMenuItem);
  case 0:
  case 1:
  case 3:
  case 2:
  }
  while (true)
  {
    return bool2;
    boolean bool3 = bool2;
    String str1 = this.TAG;
    StringBuilder localStringBuilder = new StringBuilder("ITEM_OPERATION_DELETE mSelectedId= ");
    long l1 = this.mSelectedId;
    String str2 = l1;
    Log.d(str1, str2);
    long l2 = this.mSelectedId;
    bool3[bool1] = l2;
    long l3 = this.mPlayingId;
    long l4 = this.mSelectedId;
    if ((l3 == l4) && (this.mIsSupposedToBePlaying))
    {
      long l5 = this.mSelectedId;
      stopByItemId(l5);
    }
    String str3 = getString(2131099688);
    Object[] arrayOfObject = new Object[bool2];
    String str4 = this.mCurrentTrackName;
    arrayOfObject[bool1] = str4;
    String str5 = String.format(str3, arrayOfObject);
    AlertDialog.Builder localBuilder = new AlertDialog.Builder(this).setTitle(2131099689).setMessage(str5).setIcon(17301543);
    RecordList.7 local7 = new RecordList.7(this, bool3);
    localBuilder.setPositiveButton(17039370, local7).setNegativeButton(17039360, null).show();
    continue;
    if (this.mIsSupposedToBePlaying)
    {
      long l6 = this.mSelectedId;
      playByItemId(l6, bool1);
      continue;
    }
    long l7 = this.mSelectedId;
    playByItemId(l7, bool2);
    continue;
    showInfomationDlg();
    continue;
    showRenameDlg();
  }
}

    public boolean onOptionsItemSelected(MenuItem paramMenuItem) {
        switch (paramMenuItem.getItemId()) {
            default:
            case 2131427373:
            case 16908332:
        }
        while (true) {
            return super.onOptionsItemSelected(paramMenuItem);
            Class localClass = this.mTrackList.getClass();
            try {
                Class[] arrayOfClass = new Class[1];
                arrayOfClass[0] = ActionMode.class;
                Method localMethod = localClass.getMethod("setActionMode",
                        arrayOfClass);
                localMethod.setAccessible(true);
                ModeCallback localModeCallback = new ModeCallback(null);
                ListView localListView = this.mTrackList;
                Object[] arrayOfObject = new Object[1];
                ActionMode localActionMode = this.mTrackList
                        .startActionMode(localModeCallback);
                arrayOfObject[0] = localActionMode;
                localMethod.invoke(localListView, arrayOfObject);
                this.mTrackList.setMultiChoiceModeListener(localModeCallback);
            } catch (IllegalArgumentException localIllegalArgumentException) {
                localIllegalArgumentException.printStackTrace();
            } catch (InvocationTargetException localInvocationTargetException) {
                localInvocationTargetException.printStackTrace();
            } catch (NoSuchMethodException localNoSuchMethodException) {
                localNoSuchMethodException.printStackTrace();
            } catch (IllegalAccessException localIllegalAccessException) {
                localIllegalAccessException.printStackTrace();
            }
            continue;
            finish();
        }
    }

    protected void onPause() {
        PowerManager localPowerManager = (PowerManager) getSystemService("power");
        Log.d(this.TAG, "on onPause()");
        if (localPowerManager.isScreenOn()) {
            long l = this.mPlayingId;
            pauseByItemId(l);
        }
        super.onPause();
    }

    public boolean onPrepareOptionsMenu(Menu paramMenu) {
        super.onPrepareOptionsMenu(paramMenu);
        return true;
    }

    public void onResume() {
        super.onResume();
        Log.d(this.TAG, "on onResume()");
    }

    public void onStart() {
        super.onStart();
        this.isStop = null;
        long l = refreshNow();
        Object localObject;
        queueNextRefresh(localObject);
        Log.d(this.TAG, "on onStart()");
    }

    public void onStop() {
        int i = 1;
        PowerManager localPowerManager = (PowerManager) getSystemService("power");
        Log.d(this.TAG, "on onStop()");
        if (localPowerManager.isScreenOn()) {
            this.mIsSupposedToBePlaying = null;
            this.mHandler.removeMessages(i);
            this.isStop = i;
        }
        super.onStop();
    }

    public void pause()
{
  monitorenter;
  try
  {
    this.mMediaplayerHandler.removeMessages(6);
    boolean bool = this.mIsSupposedToBePlaying;
    if (bool)
    {
      this.mPlayer.pause();
      Object localObject1 = null;
      this.mIsSupposedToBePlaying = localObject1;
    }
    monitorexit;
    return;
  }
  finally
  {
    localObject2 = finally;
    monitorexit;
  }
  throw localObject2;
}

    public void pauseByItemId(long paramLong) {
        String str1 = this.TAG;
        String str2 = "pauseByItemId itemid= " + paramLong;
        Log.d(str1, str2);
        ListView localListView = this.mTrackList;
        int i = (int) paramLong;
        View localView = localListView.findViewById(i);
        if (localView != null) {
            RecordListAdapter.ViewHolder localViewHolder = (RecordListAdapter.ViewHolder) localView
                    .getTag();
            localViewHolder.pause.setVisibility(4);
            localViewHolder.play.setVisibility(0);
        }
        pause();
    }

    public void play() {
        int i = 1;
        AudioManager localAudioManager = this.mAudioManager;
        AudioManager.OnAudioFocusChangeListener localOnAudioFocusChangeListener = this.mAudioFocusListener;
        localAudioManager.requestAudioFocus(localOnAudioFocusChangeListener, 3,
                i);
        if (this.mPlayer.isInitialized()) {
            long l = this.mPlayer.duration();
            this.mPlayer.start();
            if (!this.mIsSupposedToBePlaying)
                this.mIsSupposedToBePlaying = i;
            this.mMediaplayerHandler.removeMessages(5);
            this.mMediaplayerHandler.sendEmptyMessage(6);
        }
    }

    public void playByItemId(long paramLong, boolean paramBoolean)
{
  paramBoolean = true;
  int i = 0;
  String str1 = this.TAG;
  String str2 = "supdateItemView itemid= " + paramLong + " fromPlayButton=" + ???;
  Log.d(str1, str2);
  ListView localListView1 = this.mTrackList;
  int j = (int)this.mPlayingId;
  View localView1 = localListView1.findViewById(j);
  int k = 0;
  RecordListAdapter.ViewHolder localViewHolder1;
  if (localView1 != null)
    localViewHolder1 = (RecordListAdapter.ViewHolder)localView1.getTag();
  ListView localListView2 = this.mTrackList;
  int m = (int)paramLong;
  View localView2 = localListView2.findViewById(m);
  if (localView2 == null)
  {
    String str3 = this.TAG;
    String str4 = "playByItemId v == null " + paramLong + " fromPlayButton=" + ???;
    Log.d(str3, str4);
    return;
  }
  RecordListAdapter.ViewHolder localViewHolder2 = (RecordListAdapter.ViewHolder)localView2.getTag();
  }
  while (true)
  {
    if (localViewHolder1 != 0)
      localViewHolder1.title.setSelected(i);
    localViewHolder2.title.setSelected(true);
    localViewHolder2.play_indicator.setVisibility(i);
    this.mPlayingId = paramLong;
    break;
    doPauseResume();
    break label229;
    long l2 = this.mPlayingId;
    doPauseResume();
    if (this.mIsSupposedToBePlaying)
    {
      localViewHolder2.pause.setVisibility(i);
      localViewHolder2.play.setVisibility(paramBoolean);
      continue;
    }
    localViewHolder2.pause.setVisibility(paramBoolean);
    localViewHolder2.play.setVisibility(i);
  }
}

    public long position()
{
  boolean bool = this.mPlayer.isInitialized();
  if (bool)
    long l1 = this.mPlayer.position();
  while (true)
  {
    Object localObject;
    return localObject;
    long l2 = 65535L;
  }
}

    public Cursor query(Uri paramUri, String[] paramArrayOfString1,
            String paramString1, String[] paramArrayOfString2,
            String paramString2) {
        RecordList localRecordList = this;
        Uri localUri = paramUri;
        String[] arrayOfString1 = paramArrayOfString1;
        String str1 = paramString1;
        String[] arrayOfString2 = paramArrayOfString2;
        String str2 = paramString2;
        return localRecordList.query(localUri, arrayOfString1, str1,
                arrayOfString2, str2, 0);
    }

    public Cursor query(Uri paramUri, String[] paramArrayOfString1, String paramString1, String[] paramArrayOfString2, String paramString2, int paramInt)
{
  int i = 0;
  try
  {
    ContentResolver localContentResolver = getContentResolver();
    if (localContentResolver == null);
    String[] arrayOfString1;
    String str2;
    String[] arrayOfString2;
    String str3;
    for (localObject = i; ; localObject = localContentResolver.query((Uri)localObject, arrayOfString1, str2, arrayOfString2, str3))
    {
      return localObject;
      if (paramInt > 0)
      {
        localObject = paramUri.buildUpon();
        String str1 = paramInt;
        localObject = ((Uri.Builder)localObject).appendQueryParameter("limit", str1);
        paramUri = ((Uri.Builder)localObject).build();
      }
      localObject = paramUri;
      arrayOfString1 = paramArrayOfString1;
      str2 = paramString1;
      arrayOfString2 = paramArrayOfString2;
      str3 = paramString2;
    }
  }
  catch (UnsupportedOperationException localUnsupportedOperationException)
  {
    while (true)
      Object localObject = i;
  }
}

    public void registerExternalStorageListener()
{
  if (this.mUnmountReceiver == null)
  {
    RecordList.6 local6 = new RecordList.6(this);
    this.mUnmountReceiver = local6;
    IntentFilter localIntentFilter = new IntentFilter();
    localIntentFilter.addAction("android.intent.action.MEDIA_EJECT");
    localIntentFilter.addAction("android.intent.action.MEDIA_MOUNTED");
    localIntentFilter.addDataScheme("file");
    BroadcastReceiver localBroadcastReceiver = this.mUnmountReceiver;
    registerReceiver(localBroadcastReceiver, localIntentFilter);
  }
}


    public long seek(long paramLong) {
        boolean bool = this.mPlayer.isInitialized();
        Object localObject;
        if (bool) {
            long l1 = paramLong < 0L;
            if (l1 < 0)
                paramLong = 0L;
            l1 = this.mPlayer.duration();
            l1 = paramLong < l1;
            if (l1 > 0) {
                localObject = this.mPlayer;
                paramLong = ((MultiPlayer) localObject).duration();
            }
            localObject = this.mPlayer.seek(paramLong);
        }
        while (true) {
            return localObject;
            long l2 = 65535L;
        }
    }

    public void stop() {
        if (this.mPlayer.isInitialized())
            this.mPlayer.stop();
        this.mIsSupposedToBePlaying = null;
    }

    public void stopByItemId(long paramLong) {
        int i = 2131099700;
        int j = 4;
        int k = 0;
        String str1 = this.TAG;
        String str2 = "stopByItemId itemid= " + paramLong;
        Log.d(str1, str2);
        ListView localListView = this.mTrackList;
        int m = (int) paramLong;
        View localView = localListView.findViewById(m);
        if (localView != null) {
            RecordListAdapter.ViewHolder localViewHolder = (RecordListAdapter.ViewHolder) localView
                    .getTag();
            this.mPlayingId = 0L;
            localViewHolder.pause.setVisibility(j);
            localViewHolder.play.setVisibility(k);
            localViewHolder.play_indicator.setVisibility(j);
        }
        stop();
        int n = this.mCurrentTime.getVisibility();
        this.mCurrentTime.setVisibility(k);
        TextView localTextView1 = this.mCurrentTime;
        String str3 = getString(i);
        localTextView1.setText(str3);
        TextView localTextView2 = this.mTotalTime;
        String str4 = getString(i);
        localTextView2.setText(str4);
        this.mProgress.setProgress(k);
    }

    public final class Invoke {
  public static Class getClass(String paramString)
  {
    try
    {
      Class localClass = Class.forName(paramString);
      return localClass;
    }
    catch (ClassNotFoundException localClassNotFoundException)
    {
      while (true)
        int i = 0;
    }
  }

        @Signature({ "(", "Ljava/lang/Class", "<*>;", "Ljava/lang/String;",
                "[", "Ljava/lang/Class", "<*>;)", "Ljava/lang/reflect/Method;" })
        public static Method getMethod(Class paramClass, String paramString,
                Class[] paramArrayOfClass) {
            int i = 0;
            if ((paramClass == null) || (TextUtils.isEmpty(paramString)))
                ;
            while (true) {
                return i;
                try {
                    Method localMethod = paramClass.getMethod(paramString,
                            paramArrayOfClass);
                } catch (SecurityException localSecurityException) {
                } catch (NoSuchMethodException localNoSuchMethodException) {
                }
            }
        }

        public static Object invoke(Object paramObject1, Object paramObject2,
                Method paramMethod, Object[] paramArrayOfObject) {
            if (paramMethod == null)
                ;
            while (true) {
                return paramObject2;
                try {
                    paramObject2 = paramMethod.invoke(paramObject1,
                            paramArrayOfObject);
                } catch (Exception localException) {
                }
            }
        }
    }

    class ModeCallback implements AbsListView.MultiChoiceModeListener {
        private View mMultiSelectActionBarView;
        private ImageView mSelectToggler;
        private TextView mSelectedConvCount;

        private ModeCallback() {
        }

        public boolean onActionItemClicked(ActionMode paramActionMode, MenuItem paramMenuItem)
  {
    int i = 1;
    boolean bool = null;
    Log.d(RecordList.this.TAG, "onActionItemClicked");
    switch (paramMenuItem.getItemId())
    {
    default:
    case 2131427374:
    }
    while (true)
    {
      RecordList.this.UpdateCounter(bool);
      return i;
      Resources localResources = RecordList.this.getResources();
      ListView localListView = RecordList.this.getListView();
      RecordList localRecordList1 = RecordList.this;
      long[] arrayOfLong = localListView.getCheckedItemIds();
      localRecordList1.mListToDelete = arrayOfLong;
      int j = RecordList.this.mListToDelete.length;
      Object[] arrayOfObject = new Object[i];
      Integer localInteger = Integer.valueOf(RecordList.this.mListToDelete.length);
      arrayOfObject[bool] = localInteger;
      String str = localResources.getQuantityString(2131165187, j, arrayOfObject);
      RecordList localRecordList2 = RecordList.this;
      AlertDialog.Builder localBuilder = new AlertDialog.Builder(localRecordList2).setTitle(2131099689).setMessage(str).setIcon(17301543);
      RecordList.ModeCallback.2 local2 = new RecordList.ModeCallback.2(this);
      localBuilder.setPositiveButton(17039370, local2).setNegativeButton(17039360, null).show();
      paramActionMode.finish();
    }
  }

        public boolean onCreateActionMode(ActionMode paramActionMode, Menu paramMenu)
  {
    Log.d(RecordList.this.TAG, "onCreateActionMode");
    RecordList.this.getMenuInflater().inflate(2131361794, paramMenu);
    if (this.mMultiSelectActionBarView == null)
    {
      ViewGroup localViewGroup = (ViewGroup)LayoutInflater.from(RecordList.this).inflate(2130968582, null);
      this.mMultiSelectActionBarView = localViewGroup;
      TextView localTextView = (TextView)this.mMultiSelectActionBarView.findViewById(2131427362);
      this.mSelectedConvCount = localTextView;
      ImageView localImageView1 = (ImageView)this.mMultiSelectActionBarView.findViewById(2131427363);
      this.mSelectToggler = localImageView1;
      this.mSelectToggler.setImageResource(2130837558);
      if (this.mSelectToggler != null)
      {
        ImageView localImageView2 = this.mSelectToggler;
        RecordList.ModeCallback.1 local1 = new RecordList.ModeCallback.1(this);
        localImageView2.setOnClickListener(local1);
      }
    }
    View localView = this.mMultiSelectActionBarView;
    paramActionMode.setCustomView(localView);
    ((TextView)this.mMultiSelectActionBarView.findViewById(2131427341)).setText(2131099699);
    return true;
  }

        public void onDestroyActionMode(ActionMode paramActionMode) {
            Log.d(RecordList.this.TAG, "onDestroyActionMode");
            RecordList.this.exitMultiSelectView();
            RecordList.this.UpdateCounter(true);
        }

        public void onItemCheckedStateChanged(ActionMode paramActionMode, int paramInt, long paramLong, boolean paramBoolean)
  {
    int i = RecordList.this.mTrackList.getCheckedItemCount();
    String str1 = RecordList.this.TAG;
    String str2 = "onItemCheckedStateChanged position=" + paramInt + " id=" + paramLong + " checked = " + ???;
    Log.d(str1, str2);
    if ((???) && (RecordList.this.mPlayingId == paramLong) && (RecordList.this.mIsSupposedToBePlaying))
      RecordList.this.stopByItemId(paramLong);
    TextView localTextView = this.mSelectedConvCount;
    String str3 = Integer.toString(RecordList.this.mTrackList.getCheckedItemCount());
    localTextView.setText(str3);
    int j = RecordList.this.mTrackList.getCheckedItemCount();
    int k = RecordList.this.mTrackList.getCount();
    if (j != k)
      this.mSelectToggler.setImageResource(2130837558);
    while (true)
    {
      RecordList.this.UpdateCounter(null);
      return;
      this.mSelectToggler.setImageResource(2130837564);
    }
  }

        public boolean onPrepareActionMode(ActionMode paramActionMode,
                Menu paramMenu) {
            Log.d(RecordList.this.TAG, "onPrepareActionMode");
            if (this.mMultiSelectActionBarView == null) {
                ViewGroup localViewGroup = (ViewGroup) LayoutInflater.from(
                        RecordList.this).inflate(2130968582, null);
                paramActionMode.setCustomView(localViewGroup);
            }
            return true;
        }
    }

    class MultiPlayer {
        MediaPlayer.OnErrorListener errorListener;
        MediaPlayer.OnCompletionListener listener;
        private Handler mHandler;
        private boolean mIsInitialized;
        private MediaPlayer mMediaPlayer;

        public MultiPlayer()
  {
    MediaPlayer localMediaPlayer = new MediaPlayer();
    this.mMediaPlayer = localMediaPlayer;
    this.mIsInitialized = null;
    RecordList.MultiPlayer.1 local1 = new RecordList.MultiPlayer.1(this);
    this.listener = local1;
    RecordList.MultiPlayer.2 local2 = new RecordList.MultiPlayer.2(this);
    this.errorListener = local2;
    this.mMediaPlayer.setWakeMode(RecordList.this, 1);
  }


        public void setVolume(float paramFloat) {
            this.mMediaPlayer.setVolume(paramFloat, paramFloat);
        }

        public void start() {
            Log.d(RecordList.this.TAG, "->>to start ");
            this.mMediaPlayer.start();
        }

        public void stop() {
            Log.d(RecordList.this.TAG, "->>to stop ");
            this.mMediaPlayer.reset();
            this.mIsInitialized = null;
        }
    }

    class TrackQueryHandler extends AsyncQueryHandler {
        TrackQueryHandler(ContentResolver arg2) {
            super();
        }

        public Cursor doQuery(Uri paramUri, String[] paramArrayOfString1,
                String paramString1, String[] paramArrayOfString2,
                String paramString2, boolean paramBoolean) {
            int i;
            if (paramBoolean) {
                Uri localUri1 = paramUri.buildUpon()
                        .appendQueryParameter("limit", "100").build();
                QueryArgs localQueryArgs = new QueryArgs();
                localQueryArgs.uri = paramUri;
                localQueryArgs.projection = paramArrayOfString1;
                localQueryArgs.selection = paramString1;
                localQueryArgs.selectionArgs = paramArrayOfString2;
                localQueryArgs.orderBy = paramString2;
                TrackQueryHandler localTrackQueryHandler = this;
                String[] arrayOfString1 = paramArrayOfString1;
                String str1 = paramString1;
                String[] arrayOfString2 = paramArrayOfString2;
                String str2 = paramString2;
                localTrackQueryHandler.startQuery(0, localQueryArgs, localUri1,
                        arrayOfString1, str1, arrayOfString2, str2);
                i = 0;
            }
            while (true) {
                return i;
                RecordList localRecordList = RecordList.this;
                Uri localUri2 = paramUri;
                String[] arrayOfString3 = paramArrayOfString1;
                String str3 = paramString1;
                String[] arrayOfString4 = paramArrayOfString2;
                String str4 = paramString2;
                Cursor localCursor = localRecordList.query(localUri2,
                        arrayOfString3, str3, arrayOfString4, str4);
            }
        }

        protected void onQueryComplete(int paramInt, Object paramObject,
                Cursor paramCursor) {
            RecordList.this.init(paramCursor);
            if ((paramInt == 0) && (paramObject != null)
                    && (paramCursor != null) && (paramCursor.getCount() >= 100)) {
                QueryArgs localQueryArgs = (QueryArgs) paramObject;
                Uri localUri = localQueryArgs.uri;
                String[] arrayOfString1 = localQueryArgs.projection;
                String str1 = localQueryArgs.selection;
                String[] arrayOfString2 = localQueryArgs.selectionArgs;
                String str2 = localQueryArgs.orderBy;
                startQuery(1, null, localUri, arrayOfString1, str1,
                        arrayOfString2, str2);
            }
        }

    }*/
}