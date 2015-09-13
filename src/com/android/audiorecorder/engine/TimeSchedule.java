package com.android.audiorecorder.engine;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.android.audiorecorder.ui.SettingsActivity;

public class TimeSchedule {
    
   
    public static final String ACTION_TIMER_ALARM = "android.recorder.action.TIMER_ALARM";
    
    private Context mContext;
    private PendingIntent mTimerPendingIntent;
    private AlarmManager mAlarmManager;
    
    public TimeSchedule(Context context){
        this.mContext = context;
    }

    public void start(){
        System.out.println("TimeSchedule init.");
        setRtcTimerAlarm();
    }
    
    public void setRtcTimerAlarm() {
        cancleRtcTimerAlarm();
        Intent intent = new Intent(ACTION_TIMER_ALARM);
        mTimerPendingIntent = PendingIntent.getBroadcast(mContext, 0, intent , PendingIntent.FLAG_ONE_SHOT);
        if (mAlarmManager == null) {
            mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        }
        mAlarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + SettingsActivity.MAX_RECORDER_SET*1000, mTimerPendingIntent);
        //mAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), 3*1000, mTimerPendingIntent);
    }
    
    private void cancleRtcTimerAlarm(){
        if ((mAlarmManager != null) && (mTimerPendingIntent != null)) {
            mAlarmManager.cancel(mTimerPendingIntent);
        }
    }
    
}
