package com.android.audiorecorder.ui;

import android.os.Bundle;
import android.widget.TextView;

import com.android.audiorecorder.R;
import com.android.audiorecorder.utils.StringUtil;
import com.android.library.ui.base.BaseSubActivity;
import com.baidu.mobstat.StatService;

public class CenterHelpActivity extends BaseSubActivity {
    
    private String TAG = "HelpActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_help);
        setTitle(R.string.sms_setting_help);
        TextView aboutTextView = (TextView) findViewById(R.id.setting_help_about_software);
        aboutTextView.setText(StringUtil.loadHtmlText(this, R.string.sms_setting_help_software_content));
        TextView functionContentTextView = (TextView) findViewById(R.id.setting_help_function_content);
        functionContentTextView.setText(StringUtil.loadHtmlText(this, R.string.sms_setting_help_function_content));
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
