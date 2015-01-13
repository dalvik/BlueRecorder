package com.android.audiorecorder;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class HelpActivity extends Activity {
    
    private String TAG = "SettingsActivity";
    
    private TextView mCurrentVersion;
    private TextView mUpdateVersion;
    
    private Handler mHandler = new Handler() {
    
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                default:
                    break;
            }
        };
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_settings);
        findViewById(R.id.sms_settings_back).setOnClickListener(onClickListener);
        findViewById(R.id.version).setOnClickListener(onClickListener);
        mCurrentVersion = (TextView) findViewById(R.id.version_info);
        findViewById(R.id.update).setOnClickListener(onClickListener);
        mUpdateVersion = (TextView) findViewById(R.id.update_info);
        findViewById(R.id.suggestion).setOnClickListener(onClickListener);
        findViewById(R.id.help).setOnClickListener(onClickListener);
        findViewById(R.id.about).setOnClickListener(onClickListener);
    }
   
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    
    
    private OnClickListener onClickListener = new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.sms_settings_back:
                    HelpActivity.this.finish();
                    break;
                case R.id.update:
                    
                    break;
                case R.id.suggestion:
                    
                    break;
                case R.id.help:
                    
                    break;
                case R.id.about:
                    
                    break;
                default:
                    break;
            }
        }
    };
        
}
