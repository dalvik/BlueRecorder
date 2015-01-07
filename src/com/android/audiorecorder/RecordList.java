package com.android.audiorecorder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class RecordList extends ListActivity implements
        View.OnCreateContextMenuListener {
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
    public static final int ITEM_OPERATION_DELETE = 0;
    public static final int ITEM_OPERATION_DETAILS = 3;
    public static final int ITEM_OPERATION_PLAY = 1;
    public static final int ITEM_OPERATION_RENAME = 2;
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
    private boolean isStop = true;
    //private RecordListAdapter mAdapter;
    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener;
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
    private Handler mMediaplayerHandler;
    //private MultiPlayer mPlayer;
    public long mPlayingId = 0L;
    private String[] mPlaylistMemberCols;
    private long mPosOverride = 65535L;
    private ProgressBar mProgress = null;
    private LinearLayout mProgressLayout;
    //private TrackQueryHandler mQueryHandler;

    private ArrayList mRecordList;
    private SeekBar.OnSeekBarChangeListener mSeekListener;
    public long mSelectedId;
    public int mSelectedPosition;
    private String mSortOrder;
    private TextView mTotalTime;
    private ListView mTrackList;
    private BroadcastReceiver mUnmountReceiver = null;
    private PowerManager.WakeLock mWakeLock;
    private String mWhereClause;

    public RecordList()
{
  int[] arrayOfInt = { 47, 42, 63, 92, 60, 62, 124, 58, 34 };
  this.NameCheckList = arrayOfInt;
  this.NameCheckList_Str = null;
  this.mListToDelete = null;
  /*this.mAdapter = null;
  this.mIndicator = null;
  this.mFormatBuilder = null;
  this.mFormatter = null;
  this.mProgressLayout = null;
  RecordList.1 local1 = new RecordList.1(this);
  this.mCounterhandler = local1;
  RecordList.2 local2 = new RecordList.2(this);
  this.mMediaplayerHandler = local2;
  RecordList.3 local3 = new RecordList.3(this);
  this.mSeekListener = local3;
  RecordList.4 local4 = new RecordList.4(this);
  this.mAudioFocusListener = local4;
  RecordList.5 local5 = new RecordList.5(this);
  this.mHandler = local5;*/
}

    private void MakeCursor() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
        ContentResolver localContentResolver = getContentResolver();
        RecordList localRecordList = this;
        //TrackQueryHandler localTrackQueryHandler1 = new TrackQueryHandler(localContentResolver);
        //this.mQueryHandler = localTrackQueryHandler1;
        String[] arrayOfString1 = new String[11];
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
        this.mPlaylistMemberCols = arrayOfString1;
        Resources localResources = getResources();
        Uri localUri1 = MediaStore.Audio.Playlists.getContentUri("external");
        String[] arrayOfString2 = new String[1];
        arrayOfString2[0] = "_id";
        String[] arrayOfString3 = new String[1];
        String str1 = localResources.getString(2131099669);
        arrayOfString3[0] = str1;
        /*Cursor localCursor = query(localUri1, arrayOfString2, "name=?",
                arrayOfString3, null);
        if (localCursor == null)
            Log.v(this.TAG, "query returns null");
        while (true) {
            return;
            localCursor.moveToFirst();
            if (!localCursor.isAfterLast()) {
                int i = localCursor.getInt(0);
                localCursor.close();
                String str2 = this.TAG;
                String str3 = "Playlist ID = " + i;
                Log.v(str2, str3);
                StringBuilder localStringBuilder = new StringBuilder();
                localStringBuilder.append("title != ''");
                long l = i;
                Uri localUri2 = MediaStore.Audio.Playlists.Members
                        .getContentUri("external", l);
                String str4 = this.TAG;
                String str5 = "mQueryHandler.doQuery uri =  " + localUri2;
                Log.v(str4, str5);
                this.mSortOrder = "play_order desc";
                TrackQueryHandler localTrackQueryHandler2 = this.mQueryHandler;
                String[] arrayOfString4 = this.mPlaylistMemberCols;
                String str6 = localStringBuilder.toString();
                String str7 = this.mSortOrder;
                Uri localUri3 = localUri2;
                mCursor = localTrackQueryHandler2.doQuery(localUri3,
                        arrayOfString4, str6, null, str7, true);
                continue;
            }
            localCursor.close();
        }*/
    }

    /*private void UpdateCounter(boolean paramBoolean) {
        int i = 1;
        int j = 0;
        if ((mCursor == null) || (mCursor.getCount() == 0)) {
            this.mIndicator.setVisibility(8);
            return;
        }
        if ((this.mFormatBuilder == null) || (this.mFormatter == null)) {
            StringBuilder localStringBuilder1 = new StringBuilder();
            this.mFormatBuilder = localStringBuilder1;
            StringBuilder localStringBuilder2 = this.mFormatBuilder;
            Locale localLocale = Locale.getDefault();
            Formatter localFormatter = new Formatter(localStringBuilder2,
                    localLocale);
            this.mFormatter = localFormatter;
        }
        int k = this.mTrackList.getCheckedItemCount();
        int m = mCursor.getCount();
        Resources localResources = getResources();
        String str = null;
        Object[] arrayOfObject1;
        if ((k > 0) && (!paramBoolean)) {
            arrayOfObject1 = new Object[i];
            Integer localInteger1 = Integer.valueOf(k);
            arrayOfObject1[j] = localInteger1;
        }
        Object[] arrayOfObject2;
        for (str = localResources.getQuantityString(2131165185, k,
                arrayOfObject1);; str = localResources.getQuantityString(
                2131165184, m, arrayOfObject2)) {
            this.mIndicator.setVisibility(j);
            this.mIndicator.setText(str);
            break;
            arrayOfObject2 = new Object[i];
            Integer localInteger2 = Integer.valueOf(m);
            arrayOfObject2[j] = localInteger2;
        }
    }

    private void doPauseResume() {
        if (this.mIsSupposedToBePlaying) {
            Log.d(this.TAG, "doPauseResume()-> to pause()");
            pause();
        }
        while (true) {
            refreshNow();
            return;
            Log.d(this.TAG, "doPauseResume()-> to play()");
            play();
        }
    }

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

    private void initNameCheckList_Str() {
        StringBuffer localStringBuffer = new StringBuffer();
        int[] arrayOfInt = this.NameCheckList;
        int i = arrayOfInt.length;
        Object localObject = null;
        while (true) {
            if (localObject >= i) {
                String str = localStringBuffer.toString();
                this.NameCheckList_Str = str;
                return;
            }
            char c = (char) arrayOfInt[localObject];
            localStringBuffer.append(c);
            localObject++;
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

    private long refreshNow()
{
  long l1 = 1000L;
  int i = 4;
  long l2 = 0L;
  int j = 0;
  long l3 = this.mPosOverride < l2;
  int k;
  long l5;
  if (k < 0)
  {
    long l4 = position();
    l5 = 900L;
    Object localObject;
    l3 = localObject < l2;
    if (k < 0)
      break label199;
    l3 = this.mDuration < l2;
    if (k <= 0)
      break label199;
    TextView localTextView1 = this.mCurrentTime;
    long l7 = localObject / l1;
    String str1 = Utils.makeTimeString(this, l7);
    localTextView1.setText(str1);
    boolean bool = this.mIsSupposedToBePlaying;
    if (!bool)
      break label157;
    this.mCurrentTime.setVisibility(j);
    ProgressBar localProgressBar = this.mProgress;
    long l8 = l1 * localObject;
    long l9 = this.mDuration;
    int m = (int)(l8 / l9);
    localProgressBar.setProgress(m);
  }
  while (true)
  {
    return l5;
    long l6 = this.mPosOverride;
    break;
    label157: int n = this.mCurrentTime.getVisibility();
    TextView localTextView2 = this.mCurrentTime;
    if (n == i);
    while (true)
    {
      localTextView2.setVisibility(j);
      l5 = 500L;
      break;
      j = i;
    }
    label199: TextView localTextView3 = this.mCurrentTime;
    String str2 = getString(2131099700);
    localTextView3.setText(str2);
    this.mProgress.setProgress(j);
  }
}

    private void showInfomationDlg() {
        LayoutInflater localLayoutInflater = (LayoutInflater) getSystemService("layout_inflater");
        ListView localListView = this.mTrackList;
        int i = (int) this.mSelectedId;
        View localView1 = localListView.findViewById(i);
        String str1 = this.TAG;
        StringBuilder localStringBuilder = new StringBuilder(
                "select position= ");
        int j = this.mSelectedPosition;
        String str2 = j;
        Log.d(str1, str2);
        RecordListAdapter.ViewHolder localViewHolder = (RecordListAdapter.ViewHolder) localView1
                .getTag();
        AlertDialog.Builder localBuilder = new AlertDialog.Builder(this);
        localBuilder.setTitle(2131099685);
        localBuilder.setIcon(17301659);
        localBuilder.setPositiveButton(17039370, null);
        View localView2 = localLayoutInflater.inflate(2130968578, null);
        if (localView2 != null) {
            TextView localTextView1 = (TextView) localView2
                    .findViewById(2131427341);
            String str3 = mCursor.getString(1);
            localTextView1.setText(str3);
            ImageView localImageView = (ImageView) localView2
                    .findViewById(2131427340);
            Drawable localDrawable = getResources().getDrawable(2130837525);
            localImageView.setImageDrawable(localDrawable);
            TextView localTextView2 = (TextView) localView2
                    .findViewById(2131427343);
            CharSequence localCharSequence = localViewHolder.duration.getText();
            localTextView2.setText(localCharSequence);
            TextView localTextView3 = (TextView) localView2
                    .findViewById(2131427342);
            String str4 = Utils.formatFileSizeString(mCursor.getInt(10));
            localTextView3.setText(str4);
            TextView localTextView4 = (TextView) localView2
                    .findViewById(2131427345);
            String str5 = mCursor.getString(2);
            localTextView4.setText(str5);
            TextView localTextView5 = (TextView) localView2
                    .findViewById(2131427344);
            String str6 = mCursor.getString(6);
            localTextView5.setText(str6);
        }
        localBuilder.setView(localView2);
        localBuilder.show();
    }

    private void showRenameDlg()
{
  LayoutInflater localLayoutInflater = (LayoutInflater)getSystemService("layout_inflater");
  ListView localListView = this.mTrackList;
  int i = (int)this.mSelectedId;
  View localView1 = localListView.findViewById(i);
  View localView2 = localLayoutInflater.inflate(2130968584, null);
  String str1 = this.TAG;
  StringBuilder localStringBuilder = new StringBuilder("select position= ");
  int j = this.mSelectedPosition;
  String str2 = j;
  Log.d(str1, str2);
  RecordListAdapter.ViewHolder localViewHolder = (RecordListAdapter.ViewHolder)localView1.getTag();
  AlertDialog.Builder localBuilder = new AlertDialog.Builder(this);
  localBuilder.setTitle(2131099686);
  localBuilder.setIcon(17301566);
  RecordList.8 local8 = new RecordList.8(this, localView2);
  localBuilder.setPositiveButton(17039370, local8);
  RecordList.9 local9 = new RecordList.9(this);
  localBuilder.setNegativeButton(17039360, local9);
  localBuilder.setView(localView2);
  AlertDialog localAlertDialog = localBuilder.create();
  localAlertDialog.show();
  Button localButton = localAlertDialog.getButton(-1);
  if (localView2 != null)
  {
    EditText localEditText = (EditText)localView2.findViewById(2131427367);
    String str3 = mCursor.getString(2);
    String str4 = SoundRecorder.getFileNameNoEx(new File(str3).getName());
    localEditText.setText(str4);
    Editable localEditable = localEditText.getText();
    int k = localEditable.length();
    Selection.setSelection(localEditable, k);
    RecordList.10 local10 = new RecordList.10(this, localButton);
    localEditText.addTextChangedListener(local10);
  }
}

    private void startPlayback(String paramString) {
        this.mProgressLayout.setVisibility(0);
        stop();
        this.mPlayer.setDataSource(paramString);
        play();
        long l1 = duration();
        Object localObject1;
        this.mDuration = localObject1;
        TextView localTextView = this.mTotalTime;
        long l2 = this.mDuration / 1000L;
        String str = Utils.makeTimeString(this, l2);
        localTextView.setText(str);
        long l3 = refreshNow();
        Object localObject2;
        queueNextRefresh(localObject2);
    }

    public void deleteTracks(Context paramContext, long[] paramArrayOfLong)
{
  if ((paramArrayOfLong == null) || (paramArrayOfLong.length < 1))
    return;
  String[] arrayOfString = new String[2];
  arrayOfString[0] = "_id";
  arrayOfString[1] = "_data";
  StringBuilder localStringBuilder1 = new StringBuilder();
  localStringBuilder1.append("_id IN (");
  int i = 0;
  label47: int j = paramArrayOfLong.length;
  Cursor localCursor;
  if (i >= j)
  {
    localStringBuilder1.append(")");
    String str1 = this.TAG;
    String str2 = "deleteTracks where =  " + localStringBuilder1;
    Log.i(str1, str2);
    Uri localUri1 = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    String str3 = localStringBuilder1.toString();
    localCursor = query(localUri1, arrayOfString, str3, null, null);
    if (localCursor != null)
      break label395;
    Log.i(this.TAG, "deleteTracks query cursor == null ");
    label142: if (localCursor != null)
    {
      localCursor.moveToFirst();
      label155: if (!localCursor.isAfterLast())
        break label445;
      ContentResolver localContentResolver1 = paramContext.getContentResolver();
      Uri localUri2 = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
      String str4 = localStringBuilder1.toString();
      localContentResolver1.delete(localUri2, str4, null);
      localCursor.moveToFirst();
    }
  }
  while (true)
  {
    if (localCursor.isAfterLast())
    {
      localCursor.close();
      Resources localResources = paramContext.getResources();
      int k = paramArrayOfLong.length;
      Object[] arrayOfObject = new Object[1];
      Integer localInteger = Integer.valueOf(paramArrayOfLong.length);
      arrayOfObject[0] = localInteger;
      String str5 = localResources.getQuantityString(2131165186, k, arrayOfObject);
      Toast.makeText(paramContext, str5, 0).show();
      ContentResolver localContentResolver2 = paramContext.getContentResolver();
      Uri localUri3 = Uri.parse("content://media");
      localContentResolver2.notifyChange(localUri3, null);
      break;
      String str6 = this.TAG;
      StringBuilder localStringBuilder2 = new StringBuilder("deleteTracks list[").append(i).append("]=");
      long l1 = paramArrayOfLong[i];
      String str7 = l1;
      Log.i(str6, str7);
      long l2 = paramArrayOfLong[i];
      localStringBuilder1.append(l2);
      int m = paramArrayOfLong.length;
      int n;
      n--;
      if (i < m)
        localStringBuilder1.append(",");
      i++;
      break label47;
      label395: String str8 = this.TAG;
      StringBuilder localStringBuilder3 = new StringBuilder("deleteTracks query cursor count = ");
      int i1 = localCursor.getCount();
      String str9 = i1;
      Log.i(str8, str9);
      break label142;
      label445: long l3 = localCursor.getLong(0);
      localCursor.moveToNext();
      break label155;
    }
    String str10 = localCursor.getString(1);
    File localFile = new File(str10);
    try
    {
      if (!localFile.delete())
      {
        String str11 = this.TAG;
        String str12 = "Failed to delete file " + str10;
        Log.e(str11, str12);
      }
      localCursor.moveToNext();
    }
    catch (SecurityException localSecurityException)
    {
      localCursor.moveToNext();
    }
  }
}

    public long duration()
{
  boolean bool = this.mPlayer.isInitialized();
  if (bool)
    long l1 = this.mPlayer.duration();
  while (true)
  {
    Object localObject;
    return localObject;
    long l2 = 65535L;
  }
}

    int getThemeColor(Context paramContext) {
        int i = 1;
        int j = 0;
        Resources localResources = paramContext.getResources();
        Class localClass = Invoke.getClass("android.content.res.Resources");
        Class[] arrayOfClass = new Class[i];
        arrayOfClass[j] = String.class;
        Method localMethod = Invoke.getMethod(localClass, "getThemeColor",
                arrayOfClass);
        int k = null;
        if (localMethod != null) {
            Long localLong = Long.valueOf(0L);
            Object[] arrayOfObject = new Object[i];
            arrayOfObject[j] = "sound_plg";
            k = ((Integer) Invoke.invoke(localResources, localLong,
                    localMethod, arrayOfObject)).intValue();
        }
        return k;
    }

    public void init(Cursor paramCursor)
{
  int i = 0;
  if (paramCursor == null)
  {
    MakeCursor();
    UpdateCounter(i);
    if ((mCursor != null) && (mCursor.getCount() > 0))
    {
      if (this.mAdapter != null)
        break label106;
      Cursor localCursor1 = mCursor;
      RecordListAdapter localRecordListAdapter1 = new RecordListAdapter(this, 2130968581, localCursor1, i);
      this.mAdapter = localRecordListAdapter1;
      this.mAdapter.setActivity(this);
      RecordListAdapter localRecordListAdapter2 = this.mAdapter;
      setListAdapter(localRecordListAdapter2);
    }
  }
  while (true)
  {
    return;
    if (mCursor != null)
      mCursor.close();
    mCursor = paramCursor;
    break;
    label106: RecordListAdapter localRecordListAdapter3 = this.mAdapter;
    Cursor localCursor2 = mCursor;
    localRecordListAdapter3.changeCursor(localCursor2);
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

    public void onConfigurationChanged(Configuration paramConfiguration) {
        super.onConfigurationChanged(paramConfiguration);
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

    public void onCreate(Bundle paramBundle) {
        ModeCallback localModeCallback1 = null;
        boolean bool = null;
        super.onCreate(paramBundle);
        setVolumeControlStream(3);
        setContentView(2130968583);
        ListView localListView1 = getListView();
        this.mTrackList = localListView1;
        this.mTrackList.setChoiceMode(3);
        ListView localListView2 = this.mTrackList;
        ModeCallback localModeCallback2 = new ModeCallback(localModeCallback1);
        localListView2.setMultiChoiceModeListener(localModeCallback2);
        setTitle(2131099687);
        ListView localListView3 = getListView();
        registerForContextMenu(localListView3);
        TextView localTextView1 = (TextView) findViewById(2131427364);
        this.mIndicator = localTextView1;
        ProgressBar localProgressBar = (ProgressBar) findViewById(16908301);
        this.mProgress = localProgressBar;
        LinearLayout localLinearLayout1 = (LinearLayout) findViewById(2131427359);
        this.mProgressLayout = localLinearLayout1;
        TextView localTextView2 = (TextView) findViewById(2131427365);
        this.mCurrentTime = localTextView2;
        TextView localTextView3 = (TextView) findViewById(2131427366);
        this.mTotalTime = localTextView3;
        LinearLayout localLinearLayout2 = this.mProgressLayout;
        int i = getThemeColor(this);
        localLinearLayout2.setBackgroundColor(i);
        this.mProgressLayout.setVisibility(8);
        PowerManager localPowerManager = (PowerManager) getSystemService("power");
        String str1 = getClass().getName();
        PowerManager.WakeLock localWakeLock = localPowerManager.newWakeLock(1,
                str1);
        this.mWakeLock = localWakeLock;
        this.mWakeLock.setReferenceCounted(bool);
        AudioManager localAudioManager = (AudioManager) getSystemService("audio");
        this.mAudioManager = localAudioManager;
        MultiPlayer localMultiPlayer1 = new MultiPlayer();
        this.mPlayer = localMultiPlayer1;
        MultiPlayer localMultiPlayer2 = this.mPlayer;
        Handler localHandler = this.mMediaplayerHandler;
        localMultiPlayer2.setHandler(localHandler);
        registerExternalStorageListener();
        if ((this.mProgress instanceof SeekBar)) {
            Log.d(this.TAG, "setOnSeekBarChangeListener");
            SeekBar localSeekBar = (SeekBar) this.mProgress;
            SeekBar.OnSeekBarChangeListener localOnSeekBarChangeListener = this.mSeekListener;
            localSeekBar
                    .setOnSeekBarChangeListener(localOnSeekBarChangeListener);
        }
        this.mProgress.setMax(1000);
        this.isStop = bool;
        init(localModeCallback1);
        ActionBar localActionBar = getActionBar();
        if (localActionBar != null) {
            Drawable localDrawable = getResources().getDrawable(2130837560);
            localActionBar.setBackgroundDrawable(localDrawable);
            localActionBar.setCustomView(2130968580);
            localActionBar.setDisplayOptions(18);
        }
        String str2 = this.TAG;
        String str3 = "actionbar" + localActionBar;
        Log.v(str2, str3);
        initNameCheckList_Str();
    }

    public boolean onCreateOptionsMenu(Menu paramMenu) {
        getMenuInflater().inflate(2131361793, paramMenu);
        return super.onCreateOptionsMenu(paramMenu);
    }

    public void onDestroy() {
        int i = 0;
        AudioManager localAudioManager = this.mAudioManager;
        AudioManager.OnAudioFocusChangeListener localOnAudioFocusChangeListener = this.mAudioFocusListener;
        localAudioManager.abandonAudioFocus(localOnAudioFocusChangeListener);
        this.mMediaplayerHandler.removeCallbacksAndMessages(i);
        if (this.mUnmountReceiver != null) {
            BroadcastReceiver localBroadcastReceiver = this.mUnmountReceiver;
            unregisterReceiver(localBroadcastReceiver);
            this.mUnmountReceiver = i;
        }
        super.onDestroy();
        this.mPlayer.release();
        this.mPlayer = i;
        this.mWakeLock.release();
        if (mCursor != null) {
            mCursor.close();
            mCursor = i;
        }
    }

    protected void onListItemClick(ListView paramListView, View paramView,
            int paramInt, long paramLong) {
        int i = paramView.getId();
        String str1 = this.TAG;
        String str2 = "onListItemClick position= " + paramInt + " id= "
                + paramLong + " v.id=" + i;
        Log.i(str1, str2);
        if ((mCursor == null) || (mCursor.getCount() <= 0))
            setTitle(2131099690);
        while (true) {
            return;
            mCursor.moveToPosition(paramInt);
            Cursor localCursor = mCursor;
            int j = mCursor.getColumnIndexOrThrow("_data");
            String str3 = localCursor.getString(j);
            long l1 = i;
            playByItemId(l1, null);
            String str4 = this.TAG;
            StringBuilder localStringBuilder = new StringBuilder(
                    "onListItemClick mSelectedId= ");
            long l2 = this.mPlayingId;
            String str5 = l2;
            Log.i(str4, str5);
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
  if (???)
  {
    long l1 = this.mPlayingId;
    if (paramLong != l1)
    {
      String str5 = localViewHolder2.path;
      startPlayback(str5);
      if (localViewHolder1 != 0)
      {
        localViewHolder1.pause.setVisibility(paramBoolean);
        localViewHolder1.play.setVisibility(i);
        localViewHolder1.play_indicator.setVisibility(paramBoolean);
      }
      label229: localViewHolder2.pause.setVisibility(i);
      localViewHolder2.play.setVisibility(paramBoolean);
    }
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
    if (paramLong != l2)
    {
      String str6 = localViewHolder2.path;
      startPlayback(str6);
      if (localViewHolder1 != 0)
      {
        localViewHolder1.pause.setVisibility(paramBoolean);
        localViewHolder1.play.setVisibility(i);
        localViewHolder1.play_indicator.setVisibility(paramBoolean);
      }
      localViewHolder2.pause.setVisibility(i);
      localViewHolder2.play.setVisibility(paramBoolean);
      continue;
    }
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

    public Boolean renameByItemId(long paramLong, String paramString) {
        // Byte code:
        // 0: getstatic 736
        // android/provider/MediaStore$Audio$Media:EXTERNAL_CONTENT_URI
        // Landroid/net/Uri;
        // 3: astore_3
        // 4: new 1190 android/content/ContentValues
        // 7: dup
        // 8: invokespecial 1191 android/content/ContentValues:<init> ()V
        // 11: astore 4
        // 13: aload_0
        // 14: invokevirtual 207
        // com/android/soundrecorder/RecordList:getContentResolver
        // ()Landroid/content/ContentResolver;
        // 17: astore 5
        // 19: aload_2
        // 20: astore 6
        // 22: aload 4
        // 24: ldc 218
        // 26: aload 6
        // 28: invokevirtual 1195 android/content/ContentValues:put
        // (Ljava/lang/String;Ljava/lang/String;)V
        // 31: lload_1
        // 32: lstore 7
        // 34: aload_3
        // 35: lload 7
        // 37: invokestatic 1201 android/content/ContentUris:withAppendedId
        // (Landroid/net/Uri;J)Landroid/net/Uri;
        // 40: astore 9
        // 42: aload 5
        // 44: aload 9
        // 46: aconst_null
        // 47: aconst_null
        // 48: aconst_null
        // 49: aconst_null
        // 50: invokevirtual 1164 android/content/ContentResolver:query
        // (Landroid/net/Uri;[Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Landroid/database/Cursor;
        // 53: astore 10
        // 55: aload 10
        // 57: invokeinterface 275 1 0
        // 62: pop
        // 63: aload 10
        // 65: ldc 220
        // 67: invokeinterface 1204 2 0
        // 72: astore 9
        // 74: aload 10
        // 76: iload 9
        // 78: invokeinterface 602 2 0
        // 83: astore 11
        // 85: aload 11
        // 87: invokevirtual 1205 java/lang/String:length ()I
        // 90: astore 9
        // 92: iinc 9 252
        // 95: aload 11
        // 97: invokevirtual 1205 java/lang/String:length ()I
        // 100: astore 12
        // 102: aload 11
        // 104: iload 9
        // 106: iload 12
        // 108: invokevirtual 1209 java/lang/String:substring
        // (II)Ljava/lang/String;
        // 111: astore 13
        // 113: aload_2
        // 114: invokestatic 1212 java/lang/String:valueOf
        // (Ljava/lang/Object;)Ljava/lang/String;
        // 117: astore 14
        // 119: new 284 java/lang/StringBuilder
        // 122: dup
        // 123: aload 14
        // 125: invokespecial 289 java/lang/StringBuilder:<init>
        // (Ljava/lang/String;)V
        // 128: aload 13
        // 130: invokevirtual 303 java/lang/StringBuilder:append
        // (Ljava/lang/String;)Ljava/lang/StringBuilder;
        // 133: invokevirtual 297 java/lang/StringBuilder:toString
        // ()Ljava/lang/String;
        // 136: astore_2
        // 137: new 667 java/io/File
        // 140: dup
        // 141: aload 11
        // 143: invokespecial 668 java/io/File:<init> (Ljava/lang/String;)V
        // 146: astore 15
        // 148: aload 15
        // 150: invokevirtual 1215 java/io/File:getParent ()Ljava/lang/String;
        // 153: astore 9
        // 155: aload_2
        // 156: astore 16
        // 158: new 667 java/io/File
        // 161: dup
        // 162: aload 9
        // 164: aload 16
        // 166: invokespecial 1217 java/io/File:<init>
        // (Ljava/lang/String;Ljava/lang/String;)V
        // 169: astore 17
        // 171: aload 17
        // 173: invokevirtual 1220 java/io/File:exists ()Z
        // 176: astore 9
        // 178: iload 9
        // 180: ifeq +12 -> 192
        // 183: aconst_null
        // 184: invokestatic 495 java/lang/Boolean:valueOf
        // (Z)Ljava/lang/Boolean;
        // 187: astore 9
        // 189: aload 9
        // 191: areturn
        // 192: aload 15
        // 194: aload 17
        // 196: invokevirtual 1224 java/io/File:renameTo (Ljava/io/File;)Z
        // 199: pop
        // 200: aload 15
        // 202: invokevirtual 1215 java/io/File:getParent ()Ljava/lang/String;
        // 205: invokestatic 1212 java/lang/String:valueOf
        // (Ljava/lang/Object;)Ljava/lang/String;
        // 208: astore 18
        // 210: new 284 java/lang/StringBuilder
        // 213: dup
        // 214: aload 18
        // 216: invokespecial 289 java/lang/StringBuilder:<init>
        // (Ljava/lang/String;)V
        // 219: ldc_w 1226
        // 222: invokevirtual 303 java/lang/StringBuilder:append
        // (Ljava/lang/String;)Ljava/lang/StringBuilder;
        // 225: astore 19
        // 227: aload_2
        // 228: astore 20
        // 230: aload 19
        // 232: aload 20
        // 234: invokevirtual 303 java/lang/StringBuilder:append
        // (Ljava/lang/String;)Ljava/lang/StringBuilder;
        // 237: invokevirtual 297 java/lang/StringBuilder:toString
        // ()Ljava/lang/String;
        // 240: astore 21
        // 242: aload 4
        // 244: ldc 220
        // 246: aload 21
        // 248: invokevirtual 1195 android/content/ContentValues:put
        // (Ljava/lang/String;Ljava/lang/String;)V
        // 251: lload_1
        // 252: lstore 22
        // 254: aload_3
        // 255: lload 22
        // 257: invokestatic 1201 android/content/ContentUris:withAppendedId
        // (Landroid/net/Uri;J)Landroid/net/Uri;
        // 260: astore 9
        // 262: aload 5
        // 264: aload 9
        // 266: aload 4
        // 268: aconst_null
        // 269: aconst_null
        // 270: invokevirtual 1230 android/content/ContentResolver:update
        // (Landroid/net/Uri;Landroid/content/ContentValues;Ljava/lang/String;[Ljava/lang/String;)I
        // 273: astore 24
        // 275: aload_0
        // 276: invokevirtual 207
        // com/android/soundrecorder/RecordList:getContentResolver
        // ()Landroid/content/ContentResolver;
        // 279: astore 9
        // 281: ldc_w 758
        // 284: invokestatic 763 android/net/Uri:parse
        // (Ljava/lang/String;)Landroid/net/Uri;
        // 287: astore 25
        // 289: aload 9
        // 291: aload 25
        // 293: aconst_null
        // 294: invokevirtual 767 android/content/ContentResolver:notifyChange
        // (Landroid/net/Uri;Landroid/database/ContentObserver;)V
        // 297: iconst_1
        // 298: invokestatic 495 java/lang/Boolean:valueOf
        // (Z)Ljava/lang/Boolean;
        // 301: astore 9
        // 303: goto -114 -> 189
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

        public long duration() {
            return this.mMediaPlayer.getDuration();
        }

        public int getAudioSessionId() {
            return this.mMediaPlayer.getAudioSessionId();
        }

        public boolean isInitialized() {
            return this.mIsInitialized;
        }

        public void pause() {
            Log.d(RecordList.this.TAG, "->>to pause ");
            this.mMediaPlayer.pause();
        }

        public long position() {
            return this.mMediaPlayer.getCurrentPosition();
        }

        public void release() {
            Log.d(RecordList.this.TAG, "->>to release ");
            stop();
            this.mMediaPlayer.release();
        }

        public long seek(long paramLong) {
            MediaPlayer localMediaPlayer = this.mMediaPlayer;
            int i = (int) paramLong;
            localMediaPlayer.seekTo(i);
            return paramLong;
        }

        public void setAudioSessionId(int paramInt) {
            this.mMediaPlayer.setAudioSessionId(paramInt);
        }

        public void setDataSource(String paramString) {
            Object localObject = null;
            try {
                this.mMediaPlayer.reset();
                this.mMediaPlayer.setOnPreparedListener(null);
                if (paramString.startsWith("content://")) {
                    MediaPlayer localMediaPlayer1 = this.mMediaPlayer;
                    RecordList localRecordList = RecordList.this;
                    Uri localUri = Uri.parse(paramString);
                    localMediaPlayer1.setDataSource(localRecordList, localUri);
                }
                while (true) {
                    this.mMediaPlayer.setAudioStreamType(3);
                    this.mMediaPlayer.prepare();
                    MediaPlayer localMediaPlayer2 = this.mMediaPlayer;
                    MediaPlayer.OnCompletionListener localOnCompletionListener = this.listener;
                    localMediaPlayer2
                            .setOnCompletionListener(localOnCompletionListener);
                    MediaPlayer localMediaPlayer3 = this.mMediaPlayer;
                    MediaPlayer.OnErrorListener localOnErrorListener = this.errorListener;
                    localMediaPlayer3.setOnErrorListener(localOnErrorListener);
                    Intent localIntent = new Intent(
                            "android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION");
                    int i = getAudioSessionId();
                    localIntent
                            .putExtra("android.media.extra.AUDIO_SESSION", i);
                    String str = RecordList.this.getPackageName();
                    localIntent.putExtra("android.media.extra.PACKAGE_NAME",
                            str);
                    RecordList.this.sendBroadcast(localIntent);
                    this.mIsInitialized = true;
                    return;
                    this.mMediaPlayer.setDataSource(paramString);
                }
            } catch (IOException localIOException) {
                while (true)
                    this.mIsInitialized = localObject;
            } catch (IllegalArgumentException localIllegalArgumentException) {
                while (true)
                    this.mIsInitialized = localObject;
            }
        }

        public void setHandler(Handler paramHandler) {
            this.mHandler = paramHandler;
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

        class QueryArgs {
            public String orderBy;
            public String[] projection;
            public String selection;
            public String[] selectionArgs;
            public Uri uri;

            QueryArgs() {
            }
        }
    }*/
}