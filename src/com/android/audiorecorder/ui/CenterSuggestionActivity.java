package com.android.audiorecorder.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

import com.android.audiorecorder.R;
import com.android.library.ui.base.BaseSubActivity;
import com.baidu.mobstat.StatService;

public class CenterSuggestionActivity extends BaseSubActivity {
    
    private String TAG = "SuggestionActivity";
    
    private EditText mSuggestionContent;
    private SharedPreferences mSettings;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_suggestion);
        setTitle(R.string.sms_setting_suggestion);
        mSettings = getSharedPreferences(SettingsActivity.class.getName(), MODE_PRIVATE);
        mSuggestionContent = (EditText) findViewById(R.id.suggestion_content);
        findViewById(R.id.suggestion_commit).setOnClickListener(onClickListener);
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
    
    
    
    private OnClickListener onClickListener = new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.suggestion_commit:
                    if(mSuggestionContent.getText().toString().trim().length()<=0){
                        Toast.makeText(CenterSuggestionActivity.this, R.string.sms_setting_suggestion_null, Toast.LENGTH_SHORT).show();
                        return ;
                    }
                    if(mSuggestionContent.getText().toString().trim().length()>160){
                        Toast.makeText(CenterSuggestionActivity.this, R.string.sms_setting_suggestion_long, Toast.LENGTH_SHORT).show();
                        return ;
                    }
                    String phoneNumber = mSettings.getString(SettingsActivity.KEY_SUGGESTION_PHONE_NUMBER, "");
                    if(phoneNumber != null && phoneNumber.length()>0){
                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage(phoneNumber, null, mSuggestionContent.getText().toString().trim(), null, null);
                        Toast.makeText(CenterSuggestionActivity.this, R.string.sms_setting_suggestion_success, Toast.LENGTH_SHORT).show();
                        CenterSuggestionActivity.this.finish();
                    }
                    break;
                default:
                    break;
            }
        }
    };
        
}
