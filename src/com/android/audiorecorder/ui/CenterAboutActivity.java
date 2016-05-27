package com.android.audiorecorder.ui;

import android.os.Bundle;
import android.widget.TextView;

import com.android.audiorecorder.R;
import com.android.audiorecorder.utils.StringUtil;
import com.android.library.ui.base.BaseSubActivity;
import com.baidu.mobstat.StatService;

public class CenterAboutActivity extends BaseSubActivity {
    
    private String TAG = "AboutActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_about);
        setTitle(R.string.sms_setting_about);
        TextView aboutTextView = (TextView) findViewById(R.id.sms_about_contact_content);
        aboutTextView.setText(StringUtil.loadHtmlText(this, R.string.sms_setting_about_contact_content));
        TextView cooperationTextView = (TextView) findViewById(R.id.setting_about_cooperation_id);
        cooperationTextView.setText(StringUtil.loadHtmlText(this, R.string.sms_setting_about_cooperation_content));
    }
   
	@Override
	protected void onResume() {
		super.onResume();
		StatService.onResume(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		StatService.onPause(this);
	}
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
}
