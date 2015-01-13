package com.android.audiorecorder;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SuggestionActivity extends Activity {
    
    private String TAG = "SuggestionActivity";
    
    private EditText mSuggestionContent;
    
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
        setContentView(R.layout.layout_suggestion);
        mSuggestionContent = (EditText) findViewById(R.id.suggestion_content);
        findViewById(R.id.sms_settings_back).setOnClickListener(onClickListener);
        findViewById(R.id.suggestion_commit).setOnClickListener(onClickListener);
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
                    SuggestionActivity.this.finish();
                    break;
                case R.id.suggestion_commit:
                    if(mSuggestionContent.getText().length()<=0){
                        Toast.makeText(SuggestionActivity.this, "", Toast.LENGTH_SHORT).show();
                        return ;
                    }
                    if(mSuggestionContent.getText().length()>160){
                        Toast.makeText(SuggestionActivity.this, "", Toast.LENGTH_SHORT).show();
                        return ;
                    }
                    //
                    break;
                default:
                    break;
            }
        }
    };
        
}
