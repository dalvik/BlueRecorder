package com.android.audiorecorder.engine;

import com.android.audiorecorder.engine.IStateListener;
	
interface IRecordListener {
	void startRecord();
	
	void stopRecord();
	
	void regStateListener(IStateListener listener);
	
	void unregStateListener(IStateListener listener);
	
	int getRecorderTime();
	
	int getTalkTime();
	
	int getMaxAmplitude();
	
	boolean isRecorderStart();
	
	boolean isTalkStart();
	
	void setMode(int mode);
	
	int getMode();
	
	void adjustStreamVolume(int streamType, int direct, int flag);
}	