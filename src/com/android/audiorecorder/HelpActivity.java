package com.android.audiorecorder;

import com.android.audiorecorder.utils.StringUtil;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class HelpActivity extends Activity {
    
    private String TAG = "HelpActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_help);
        TextView aboutTextView = (TextView) findViewById(R.id.setting_help_about_software);
        aboutTextView.setText(StringUtil.loadHtmlText(this, R.string.sms_setting_help_software_content));
        TextView functionContentTextView = (TextView) findViewById(R.id.setting_help_function_content);
        functionContentTextView.setText(StringUtil.loadHtmlText(this, R.string.sms_setting_help_function_content));
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
                    HelpActivity.this.finish();
                    break;
                default:
                    break;
            }
        }
    };
        
}
