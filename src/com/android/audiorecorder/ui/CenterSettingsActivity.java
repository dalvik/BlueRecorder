package com.android.audiorecorder.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.android.audiorecorder.R;
import com.android.library.thirdpay.DonateActivity;
import com.android.library.ui.base.BaseSubActivity;
import com.android.library.ui.utils.ActivityUtils;
import com.android.library.ui.view.CenterDrawableRadioButton;

public class CenterSettingsActivity extends BaseSubActivity {
    
    public static final String KEY_RECORDER_START = "key_recorder_start";
    public static final String KEY_RECORDER_END = "key_recorder_end";
    public static final String KEY_UPLOAD_START = "key_upload_start";
    public static final String KEY_UPLOAD_END = "key_upload_end";
    
    public static final String KEY_CUR_VERSION_CODE = "cur_version_code";
    public static final String KEY_CUR_VERSION_NAME = "cur_version_name";
    
    public static final String KEY_NEW_VERSION_CODE = "new_version_code";
    public static final String KEY_NEW_VERSION_NAME = "new_version_name";
    
    public static final String KEY_NEW_VERSION_URL = "new_version_url";
    
    public static final String KEY_MAC_ADDRESS = "key_mac";
    
    public static final String KEY_UPLOAD_URL = "key_upload_url";
    public static final String DEFAULT_UPLOAD_URL = "http://alumb.sinaapp.com/file_recv.php";
    
    public static final String KEY_SUGGESTION_PHONE_NUMBER = "key_phone_number";
    
    
    private String TAG = "SettingsActivity";
    
    private TextView mCurrentVersion;
    private TextView mUpdateVersion;
    
    private SharedPreferences mSettings;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_ui_setting);
        setTitle(R.string.sms_setting_title);
        mSettings = getSharedPreferences(CenterSettingsActivity.class.getName(), MODE_PRIVATE);
        findViewById(R.id.feedBackRL).setOnClickListener(onClickListener);
        findViewById(R.id.payDonateRL).setOnClickListener(onClickListener);
        findViewById(R.id.helpRL).setOnClickListener(onClickListener);
        findViewById(R.id.aboutRL).setOnClickListener(onClickListener);
        findViewById(R.id.updateRL).setOnClickListener(onClickListener);
        mUpdateVersion = (TextView) findViewById(R.id.tipUpdateTv);
        mCurrentVersion = (TextView) findViewById(R.id.curVersionTv);
        String curVersion = mSettings.getString(KEY_CUR_VERSION_NAME, "");
        if(TextUtils.isEmpty(curVersion)){
        	findViewById(R.id.updateVersionRL).setVisibility(View.INVISIBLE);
        } else {
            mCurrentVersion.setText(curVersion);
        }
        if(mSettings.getInt(KEY_CUR_VERSION_CODE, 0) < mSettings.getInt(KEY_NEW_VERSION_CODE, 0)){
            mUpdateVersion.setText(getText(R.string.sms_setting_update) + getString(R.string.sms_setting_newer, mSettings.getString(KEY_CUR_VERSION_NAME, "")));
        }else{
            mUpdateVersion.setText(getText(R.string.sms_setting_newest));
        }
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
                case R.id.updateRL:
                    if(mSettings.getInt(KEY_CUR_VERSION_CODE, 0) <mSettings.getInt(KEY_NEW_VERSION_CODE, 0)){
                        String url = mSettings.getString(KEY_NEW_VERSION_URL, "");
                        Uri uri = Uri.parse(url);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                    }
                    break;
                case R.id.feedBackRL:
                    ActivityUtils.startActivity(CenterSettingsActivity.this, CenterSuggestionActivity.class);
                    break;
                case R.id.helpRL:
                    ActivityUtils.startActivity(CenterSettingsActivity.this, CenterHelpActivity.class);
                    break;
                case R.id.aboutRL:
                    ActivityUtils.startActivity(CenterSettingsActivity.this, CenterAboutActivity.class);
                    break;
                case R.id.payDonateRL:
                    ActivityUtils.startActivity(CenterSettingsActivity.this, DonateActivity.class);
                    break;
                default:
                    break;
            }
        }
    };
        
}
