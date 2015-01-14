package com.android.audiorecorder.engine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SystemRebootReceiver extends BroadcastReceiver {

	private String TAG = "SystemRebootReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
	    Log.d(TAG, "===> " + intent.getAction());
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){
            context.startService(new Intent(context, AudioService.class));
        }
	}

}
