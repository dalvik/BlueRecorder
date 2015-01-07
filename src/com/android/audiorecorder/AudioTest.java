package com.android.audiorecorder;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AudioTest extends Activity{
    private final String EXTRA_ACTION_MAIN_CATERERY = "extra_action_category_index";
    private final int EXTAR_ACTION_MAIN_INDEX = 0x02;//image 0x00 video 0x01 audio 0x02
    
    private final int MSG_UPDATE_UI = 0x16;
    //private IRecordListener iRecordListener;
    
    private Recorder mRecorder;

    private RelativeLayout mAudioRecorderBg;
    private TextView mRecorderStatuLab;
    private VUMeter mVuMeter;
    private ImageView recordFileImageView;
    private ImageView recordImageView;
    final Handler mHandler = new Handler();
    private ImageView mHourHightImageView;
    private ImageView mHourLowImageView;
    private ImageView mMinuteHightImageView;
    private ImageView mMinuteLowImageView;
    private ImageView mSecondHightImageView;
    private ImageView mSecondLowImageView;
    private String packageName;
    private static String TAG = "AudioRecorderActivity";
    
    private Handler myHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch(msg.what) {
                /*case AudioService.MSG_STOP_RECORD:
                    mAudioRecorderBg.setBackgroundResource(R.drawable.record_get_ready);
                    recordImageView.setEnabled(true);
                    updateTimerView();   
                    mHandler.removeCallbacks(mUpdateTimer);
                    myHandler.sendEmptyMessage(MSG_UPDATE_UI);
                    break;
                case  AudioService.MSG_START_RECORD:
                    mAudioRecorderBg.setBackgroundResource(R.drawable.record_working);
                    recordImageView.setImageResource(R.drawable.stop_record);
                    recordImageView.setEnabled(true);
                    myHandler.sendEmptyMessage(MSG_UPDATE_UI);
                    break;
                case  AudioService.STATE_PREPARE:
                     recordImageView.setImageResource(R.drawable.record_stop_button_disable);
                     recordImageView.setEnabled(false);
                     break;*/
                case MSG_UPDATE_UI:
                    updateUi();
                    break;
                    default :
                        break;
                    
            }
        };
    };
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        /*Intent intent = new Intent(AudioService.Action_RecordListen);
        if(bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)){
            setContentView(R.layout.layout_audio);
            packageName = getPackageName();
        }else {
            Toast.makeText(this, "error", Toast.LENGTH_LONG).show();
            AudioTest.this.finish();
        }*/
        
    }

    private void initResourceRef() {
        /*mAudioRecorderBg=(RelativeLayout)findViewById(R.id.audio_recoder_bg);
        mRecorderStatuLab=(TextView)findViewById(R.id.audio_recorder_statu_lab);
        mRecorderStatuLab.setText("stop");
        mAudioRecorderBg.setBackgroundResource(R.drawable.record_get_ready);
        mHourHightImageView = (ImageView) findViewById(R.id.number_hour_height);
        mHourLowImageView=(ImageView)findViewById(R.id.number_hour_low);
        mMinuteHightImageView = (ImageView) findViewById(R.id.number_minute_height);
        mMinuteLowImageView = (ImageView) findViewById(R.id.number_minute_low);
        mSecondHightImageView = (ImageView) findViewById(R.id.number_second_height);
        mSecondLowImageView = (ImageView) findViewById(R.id.number_second_low);
        recordFileImageView = (ImageView) findViewById(R.id.recordFileButton);
        recordFileImageView.setOnClickListener(mOnButtonClickListener);
        recordImageView = (ImageView) findViewById(R.id.recordButton);
        recordImageView.setOnClickListener(mOnButtonClickListener);
        mVuMeter = (VUMeter) findViewById(R.id.uvMeter);
        mVuMeter.setRecorder(mRecorder);*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*if(iRecordListener != null) {
            try {
                iRecordListener.regStateListener(iStateListener);
                mHandler.postDelayed(mUpdateTimer, 1000);
            } catch (RemoteException e) {
                e.printStackTrace();
            } 
        }*/
    }
    
    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacks(mUpdateTimer);
        /*if(iRecordListener != null) {
            try {
                iRecordListener.unregStateListener(iStateListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }*/
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    
    Runnable mUpdateTimer = new Runnable() {
        public void run() { updateTimerView(); }
    };
    
    private void updateUi() {
        updateTimerView();   
        updateButtonStatus();
        mVuMeter.invalidate();
    }
    
    private void updateButtonStatus() {
        /*if(iRecordListener != null) {
            try {
                boolean start = iRecordListener.isRecorderStart();
                if(!start) {
                    mRecorderStatuLab.setText("stop");
                    recordImageView.setImageResource(R.drawable.start_record);
                    recordImageView.setEnabled(true);
                }else {
                    mRecorderStatuLab.setText("start");
                    recordImageView.setImageResource(R.drawable.stop_record);
                    recordImageView.setEnabled(true);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            } 
        }*/
    }
    
    private void updateTimerView() {
        Resources res = getResources();
        boolean start = false;
        /*if(iRecordListener != null) {
            try {
                start = iRecordListener.isRecorderStart();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }*/
        int time = start ? mRecorder.progress() : 0;
        //Log.d(TAG, "=======================> state = " + state + " ongoing =  " + ongoing + "  time = " + time);
        int hour = time/60/60;
        int minute = time/60%60;
        int second = time%60;
        //System.out.println("height=" + height  + " h = " + height/10 +" l = " + height%10  + " low =  " + low + " h = " + low/10 + "  l = " + low%10);
        mHourHightImageView.setBackgroundResource(getDraId("number_record_" + hour/10, res));
        mHourLowImageView.setBackgroundResource(getDraId("number_record_" + hour%10, res));
        mMinuteHightImageView.setBackgroundResource(getDraId("number_record_" + minute/10, res));
        mMinuteLowImageView.setBackgroundResource(getDraId("number_record_" + minute%10, res));
        mSecondHightImageView.setBackgroundResource(getDraId("number_record_" + second/10, res));
        mSecondLowImageView.setBackgroundResource(getDraId("number_record_" + second%10, res));
        if (start)
            mHandler.postDelayed(mUpdateTimer, 1000);
    }
    
    private OnClickListener mOnButtonClickListener = new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                /*case R.id.recordFileButton:
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    ComponentName componentName = new ComponentName("com.dahuatech.filemanager", "com.dahuatech.filemanager.ui.FileManagerActivity");
                    intent.setComponent(componentName);
                    intent.putExtra(EXTRA_ACTION_MAIN_CATERERY, EXTAR_ACTION_MAIN_INDEX);
                    startActivity(intent);
                    break;*/
                case R.id.recordButton:
                    recordOperation();
                    break;
            }
        }
    };
    
    private void recordOperation(){
        /*try {
            boolean start = iRecordListener.isRecorderStart();
            System.out.println("start = " + start);
            if(!start) {
                mHandler.sendEmptyMessage(AudioService.STATE_PREPARE);
                iRecordListener.startRecord();
            } else {
                iRecordListener.stopRecord();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }*/
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            /*iRecordListener = IRecordListener.Stub.asInterface(service);
            if(iRecordListener != null) {
                try {
                    iRecordListener.regStateListener(iStateListener);
                    mRecorder = new Recorder();
                    initResourceRef();
                    boolean start = iRecordListener.isRecorderStart();
                    if(start) {
                        mHandler.removeCallbacks(mUpdateTimer);
                        mHandler.post(mUpdateTimer);
                        myHandler.sendEmptyMessage(MSG_UPDATE_UI);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }else {
            }*/
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            //iRecordListener = null;
        }
        
    };

    
   /* private IStateListener.Stub iStateListener = new IStateListener.Stub() {
        
        @Override
        public void onStateChanged(int state) throws RemoteException {
            Log.d(TAG, "===> onStateChanged  state = " + state );
            myHandler.sendEmptyMessage(state);
        }
        
    };*/
    

    public class Recorder {
        
        public Recorder() {
            
        }
        
        public int getMaxAmplitude() {
            /*if (iRecordListener == null) {
                Log.d(TAG, "===> audio record state stop");
                return 0;
            }
            try {
                return iRecordListener.getMaxAmplitude();
            } catch (RemoteException e) {
                Log.d(TAG, "===> getMaxAmplitude error " + e.getMessage());
                e.printStackTrace();
            }*/
            return 0;
        }
        
        public int progress() {
            /*if (iRecordListener != null) {
                try {
                    return iRecordListener.getRecorderTime();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }*/
            return 0;
        }
        
        public boolean state() {
            /*if(iStateListener != null) {
                try {
                    return iRecordListener.isRecorderStart();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }*/
            return false;
        }
    }
    
    public int getDraId(String name, Resources res) {
        return res.getIdentifier(name, "drawable", packageName);
    }
    
    
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            /*if (DVRSystemManager.ACTION_DVR_MODE_CHANGE.equals(action) ) {
                final int state = intent.getIntExtra(DVRSystemManager.EXTRA_DVR_MODE, DvrChannel.DVR_MODE_FREE);
                Log.d(TAG, "******  action = " + action + " state = " + state );
                if ((state & DvrChannel.DVR_MODE_AUDIO_RECORDER) == DvrChannel.DVR_MODE_AUDIO_RECORDER) {
                    myHandler.sendEmptyMessage(AudioService.MSG_START_RECORD);
                }else {
                    myHandler.sendEmptyMessage(AudioService.MSG_STOP_RECORD);
                }
            }else if(DVRSystemManager.ACTION_SONIA_RESTART.equals(action)) {
                myHandler.sendEmptyMessage(AudioService.MSG_STOP_RECORD);
            }*/
        };
    };
}
