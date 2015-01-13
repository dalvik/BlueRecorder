package com.android.audiorecorder;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class AboutActivity extends Activity {
    
    private String TAG = "AboutActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_about);
        findViewById(R.id.sms_settings_back).setOnClickListener(onClickListener);
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
                    AboutActivity.this.finish();
                    break;
                default:
                    break;
            }
        }
    };
        
}
