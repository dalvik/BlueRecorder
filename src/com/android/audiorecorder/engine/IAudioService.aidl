package com.android.audiorecorder.engine;

import com.android.audiorecorder.engine.IAudioStateListener;
	
interface IAudioService{

	void startRecord();
	
	void stopRecord();
	
	void regStateListener(IAudioStateListener listener);
	
	void unregStateListener(IAudioStateListener listener);
	
	int getRecorderTime();
	
	int getTalkTime();
	
	int getMaxAmplitude();
	
	int getAudioRecordState();
	
	boolean isTalkStart();
	
	void setMode(int mode);
	
	int getMode();
	
	void adjustStreamVolume(int streamType, int direct, int flag);
	
	long checkDiskCapacity();
	
}	