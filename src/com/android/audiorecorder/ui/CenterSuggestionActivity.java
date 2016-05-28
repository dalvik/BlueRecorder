package com.android.audiorecorder.ui;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import com.android.audiorecorder.R;
import com.android.library.ui.base.BaseSubActivity;
import com.baidu.mobstat.StatService;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

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
        mSuggestionContent.addTextChangedListener(new TextWatcher() {
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                String cleanString = mSuggestionContent.getText().toString().trim();
                if (cleanString.equals("") || cleanString.length()==0) {
                	findViewById(R.id.suggestion_commit).setEnabled(false);
                } else {
                	findViewById(R.id.suggestion_commit).setEnabled(true);
                }
            }
        });
        findViewById(R.id.suggestion_commit).setOnClickListener(onClickListener);
        findViewById(R.id.suggestion_commit).setEnabled(false);
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
