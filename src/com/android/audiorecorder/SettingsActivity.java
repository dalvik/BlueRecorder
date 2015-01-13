package com.android.audiorecorder;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class SettingsActivity extends Activity {
    
    public static final String KEY_CUR_VERSION_VALUE = "cur_version_value";
    public static final String KEY_CUR_VERSION_INFO = "cur_version_info";
    
    public static final String KEY_NEW_VERSION_VALUE = "new_version_value";
    public static final String KEY_NEW_VERSION_INFO = "new_version_info";
    
    public static final String KEY_NEW_VERSION_URL = "new_version_url";
    
    public static final String KEY_MAC_ADDRESS = "key_mac";
    
    public static final String key_upload_url = "key_upload_url";
    public static final String value_default_url = "http://davmb.com/file_recv.php";
    
    
    private String TAG = "SettingsActivity";
    
    private TextView mCurrentVersion;
    private TextView mUpdateVersion;
    
    private SharedPreferences mSettings;
    
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
        mSettings = getPreferences(MODE_PRIVATE);
        findViewById(R.id.sms_settings_back).setOnClickListener(onClickListener);
        findViewById(R.id.version).setOnClickListener(onClickListener);
        mCurrentVersion = (TextView) findViewById(R.id.version_info);
        mCurrentVersion.setText(mSettings.getString(KEY_CUR_VERSION_INFO, ""));
        findViewById(R.id.update).setOnClickListener(onClickListener);
        mUpdateVersion = (TextView) findViewById(R.id.update_info);
        if(mSettings.getInt(KEY_CUR_VERSION_VALUE, 0) <mSettings.getInt(KEY_NEW_VERSION_VALUE, 0)){
            mUpdateVersion.setText(getText(R.string.sms_setting_update) + getString(R.string.sms_setting_newer, mSettings.getString(KEY_CUR_VERSION_INFO, "")));
        }else{
            mUpdateVersion.setText(getText(R.string.sms_setting_newest));
        }
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
                    SettingsActivity.this.finish();
                    break;
                case R.id.update:
                    if(mSettings.getInt(KEY_CUR_VERSION_VALUE, 0) <mSettings.getInt(KEY_NEW_VERSION_VALUE, 0)){
                        String url = mSettings.getString(KEY_NEW_VERSION_URL, "");//open
                    }
                    break;
                case R.id.suggestion:
                    Intent suggestion = new Intent(SettingsActivity.this, SuggestionActivity.class);
                    startActivity(suggestion);
                    break;
                case R.id.help:
                    Intent help = new Intent(SettingsActivity.this, HelpActivity.class);
                    startActivity(help);
                    break;
                case R.id.about:
                    Intent about = new Intent(SettingsActivity.this, AboutActivity.class);
                    startActivity(about);
                    break;
                default:
                    break;
            }
        }
    };
        
}
