package com.android.audiorecorder.engine;

import com.android.audiorecorder.engine.IVideoStateListener;
	
interface IVideoService{
	
	int startVideoRecord();
	
	int stopVideoRecord();
	
	int videoCapture();
	
	int videoSnap();
	
	void regStateListener(IVideoStateListener listener);
	
	void unregStateListener(IVideoStateListener listener);
	
	int getRecorderTime();
	
	boolean isRecorderStart();
	
	void setMode(int mode);
	
	int getMode();
	
	void adjustStreamVolume(int streamType, int direct, int flag);
	
	long checkDiskCapacity();
	
}	