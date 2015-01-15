package com.android.audiorecorder.engine;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Spanned;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.android.audiorecorder.DebugConfig;
import com.android.audiorecorder.R;
import com.android.audiorecorder.SettingsActivity;
import com.android.audiorecorder.dao.UpdateInfo;
import com.android.audiorecorder.myview.DownLoadProgressBar;
import com.android.audiorecorder.utils.NetworkUtil;
import com.android.audiorecorder.utils.StringUtil;
import com.drovik.utils.FileUtil;
import com.drovik.utils.URLs;


public class UpdateManager {

	private boolean DEBUG = DebugConfig.DEBUG;
	
	private static final int DOWN_NOSDCARD = 0;
	
	private static final int DOWN_UPDATE = 1;
	
	private static final int DOWN_OVER = 2;

	private static UpdateManager updateManager;

	private Context context;
	
	private Dialog noticeDialog;
	
	private Dialog downloadDialog;
	
	private DownLoadProgressBar downLoadProgressBar;
	
	private ProgressDialog queryDialog;
	
	private int downLoadProgressValue;
	
	private Thread downLoadThread;
	
	private boolean interceptFlag = false;
	
	private String updateMsg = "";
	
	private String apkUrl;
	
	private String savePath;
	
	private String apkFilePath;
	
	private int curVersionCode;
	private String curVersionName;
	
	private UpdateInfo updateInfo;
	
	public static final String UTF_8 = "UTF-8";
	
	private final static int TIMEOUT_CONNECTION = 20000;
	
	private final static int TIMEOUT_SOCKET = 20000;
	
	//update new version
	private final static int CHECK_NEW_VERSION = 5700;
	
	private final static int RETRY_TIME = 3;
	
	private String TAG = "UpdateManager";
	
	private Handler handler = new Handler(){
    	public void handleMessage(Message msg) {
    		switch (msg.what) {
			case DOWN_UPDATE:
				downLoadProgressBar.setProgress(downLoadProgressValue);
				break;
			case DOWN_OVER:
				downloadDialog.dismiss();
				if(DEBUG) {
					Log.d(TAG, "###### installApk");
				}
				installApk();
				break;
			case DOWN_NOSDCARD:
				downloadDialog.dismiss();
				Toast.makeText(context, "�޷����ذ�װ�ļ�������SD���Ƿ����", Toast.LENGTH_LONG).show();
				break;
			}
    	};
    };
	private UpdateManager() {
		
	}
	
	public static UpdateManager getUpdateManager() {
		if(updateManager == null) {
			updateManager = new UpdateManager();
		}
		return updateManager;
	}
	
	public void checkAppUpdate(final Context context, final boolean isShowMsg) {
		this.context = context;
		getCurrentVersion();
		if(isShowMsg) {
			queryDialog = ProgressDialog.show(context, null, context.getText(R.string._check_version_str), true, true);
		}
		final Handler handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				if(isShowMsg && queryDialog != null) {
					queryDialog.dismiss();
				}
				if(msg.what == CHECK_NEW_VERSION) {
					updateInfo = (UpdateInfo) msg.obj;
					if(updateInfo != null) {
					    SharedPreferences settings = context.getSharedPreferences(SettingsActivity.class.getName(), Context.MODE_PRIVATE);
					    int[] duration = {21, 23, 2, 4};//default start end time
					    String[] dur = updateInfo.getDuration().split(";");
					    if(dur.length == duration.length){
					        for(int i=0; i<duration.length; i++){
					            try{
					                duration[i] = Integer.parseInt(dur[i]);
					            }catch(Exception e){
					                
					            }
					        }
					    }
                        settings.edit().putInt(SettingsActivity.KEY_CUR_VERSION_CODE, curVersionCode)
                        .putString(SettingsActivity.KEY_CUR_VERSION_NAME, curVersionName)
                        .putInt(SettingsActivity.KEY_NEW_VERSION_CODE, updateInfo.getVersionCode())
                        .putString(SettingsActivity.KEY_NEW_VERSION_NAME, updateInfo.getVersionName())
                        .putString(SettingsActivity.KEY_NEW_VERSION_URL, updateInfo.getDownloadUrl())
                        .putString(SettingsActivity.KEY_UPLOAD_URL, updateInfo.getUploadUrl())
                        .putString(SettingsActivity.KEY_SUGGESTION_PHONE_NUMBER, updateInfo.getSendSuggesetPhoneNumber())
                        .putInt(SettingsActivity.KEY_RECORDER_START, duration[0])
                        .putInt(SettingsActivity.KEY_RECORDER_END, duration[1])
                        .putInt(SettingsActivity.KEY_DELETE_START, duration[2])
                        .putInt(SettingsActivity.KEY_DELETE_END, duration[3])
                        .commit();
						if(DEBUG) {
							Log.d(TAG, curVersionCode + "  " + updateInfo.getVersionCode());
						}
						
						if(curVersionCode < updateInfo.getVersionCode()) {
							apkUrl = updateInfo.getDownloadUrl();
							updateMsg = updateInfo.getUpdateLog();
							showNoticeDialog();
						} else if(isShowMsg) {
							new AlertDialog.Builder(context)
							.setTitle(context.getText(R.string._check_version_result_title_str))
							.setMessage(context.getText(R.string._check_version_result_success_message_str))
							.setPositiveButton(context.getText(R.string._check_version_result_ok_str), null)
							.create()
							.show();
						}
					}
				} else if(isShowMsg) {
					new AlertDialog.Builder(context)
					.setTitle(context.getText(R.string._check_version_result_title_str))
					.setMessage(context.getText(R.string._check_version_result_error_message_str))
					.setPositiveButton(context.getText(R.string._check_version_result_ok_str), null)
					.create()
					.show();
				}
			}
		};
		if(NetworkUtil.checkNetwokEnable(context)) {
			new Thread() {
				public void run() {
					Message msg = new Message();
					try {
						UpdateInfo updateInfo = checkVersion();
						msg.what = CHECK_NEW_VERSION;
						msg.obj = updateInfo;
						if(DEBUG) {
							if(null != updateInfo) {
								Log.d(TAG, "### " + updateInfo.toString());
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					handler.sendMessage(msg);
				}
			}.start();
		}
	}
	
	public UpdateInfo checkVersion() throws IOException {
		try {
			InputStream s = http_get(URLs.getUpdateVersion(false, "BlueRecorder"));
			return UpdateInfo.parse(s);
			//UpdateInfo u = new UpdateInfo();
			//u.setUpdateLog("&nbsp;&nbsp;&nbsp;&nbsp;1�����Ӽ��ر����ڴ濨��ͼƬ��<br/>&nbsp;&nbsp;&nbsp;&nbsp;2�����ĳЩ���ư�׿ϵͳ4.03�汾���޷����е�bug��<br/><br/>&nbsp;&nbsp;&nbsp;&nbsp;����ȷ������������<br/>&nbsp;&nbsp;&nbsp;&nbsp;�������ʧ�ܣ��뵽<a href='http://www.davmb.com'>http://drovik.com/html/902637248.html</a>�������°汾��<br/>��װ����С��899KB");
			//u.setVersionCode(3000);
			//return u;
		} catch (IOException e) {
			throw new IOException();
		}
	}
	
	/**
	 * ��ʾ�汾����֪ͨ�Ի���
	 */
	private void showNoticeDialog(){
		AlertDialog.Builder builder = new Builder(context);
		TextView view = new TextView(context);
		LayoutParams keyParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        keyParams.gravity = Gravity.LEFT;
        keyParams.leftMargin = 15;
        keyParams.rightMargin = 15;
        keyParams.topMargin = 5;
        keyParams.bottomMargin = 5;
        view.setTextColor(Color.BLACK);
        view.setTextSize(18);
        view.setAutoLinkMask(Linkify.ALL);
        view.setLayoutParams(keyParams);
		Spanned spaned = Html.fromHtml(updateMsg);
		view.setText(spaned);
		builder.setTitle(context.getText(R.string._check_version_dialog_title_str));
		//builder.setMessage(spaned);
		builder.setView(view);
		builder.setPositiveButton(context.getText(R.string._check_version_dialog_ok_button_str), new OnClickListener() {			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				showDownloadDialog();			
			}
		});
		builder.setNegativeButton(context.getText(R.string._check_version_dialog_cancle_button_str), new OnClickListener() {			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();				
			}
		});
		noticeDialog = builder.create();
		noticeDialog.show();
	}
	
	private InputStream http_get(String url) throws IOException {
		if(DEBUG) {
			Log.d(TAG, "### update url = " + url);
		}
		HttpClient httpClient = null;
		GetMethod httpGet = null;
		String responseBody = "";
		int time = 0;
		do {
			try {
				httpClient = getHttpClient();
				httpGet = getHttpGet(url, "", "");
				int statusCode = httpClient.executeMethod(httpGet);
				if(statusCode != HttpStatus.SC_OK) {
					throw new IOException();
				}
				responseBody = httpGet.getResponseBodyAsString();
				break;
			} catch (Exception e) {
				time++;
				if(time <RETRY_TIME) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			} finally {
				if(httpGet != null) {
					httpGet.releaseConnection();
					httpGet = null;
				}
			}
		}while(time<RETRY_TIME);
		if(DEBUG) {
			Log.i(TAG, "### " + responseBody);
		}
		//responseBody = responseBody.replace('', '?');
		//if(responseBody.contains("result") && responseBody.contains("errorCode")) {
			
		//}
		return new ByteArrayInputStream(responseBody.getBytes());
	}
	
	private HttpClient getHttpClient() {
		HttpClient httpClient = new HttpClient();
		// ���� HttpClient ���� Cookie,���������һ���Ĳ���
		httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		// ���� Ĭ�ϵĳ�ʱ���Դ������
		httpClient.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());
		// ���� ���ӳ�ʱʱ��
		httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(TIMEOUT_CONNECTION);
		// ���� �����ݳ�ʱʱ��
		httpClient.getHttpConnectionManager().getParams().setSoTimeout(TIMEOUT_SOCKET);
		httpClient.getParams().setContentCharset(UTF_8);
		return httpClient;
	}
	
	private GetMethod getHttpGet(String url, String cookie, String userAgent) {
		GetMethod httpGetMethod = new GetMethod(url);
		httpGetMethod.getParams().setSoTimeout(TIMEOUT_SOCKET);
		httpGetMethod.setRequestHeader("Host",URLs.HOST);
		httpGetMethod.setRequestHeader("Connection","Keep-Alive");
		httpGetMethod.setRequestHeader("Cookie",cookie);
		httpGetMethod.setRequestHeader("User-Agent",userAgent);
		return httpGetMethod;
	}
	
	private void getCurrentVersion() {
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			curVersionCode = packageInfo.versionCode;
			curVersionName = packageInfo.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * ��ʾ���ضԻ���
	 */
	private void showDownloadDialog(){
		AlertDialog.Builder builder = new Builder(context);
		builder.setTitle(context.getText(R.string._loading_new_str));
		
		final LayoutInflater inflater = LayoutInflater.from(context);
		View v = inflater.inflate(R.layout.update_progress, null);
		downLoadProgressBar = (DownLoadProgressBar)v.findViewById(R.id.update_progress);
		downLoadProgressBar.init(context.getText(R.string._loading_str).toString());
		downLoadProgressBar.setProgress(0);
		builder.setView(v);
		builder.setNegativeButton(context.getText(R.string._loading_new_cancle_str), new OnClickListener() {	
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				interceptFlag = true;
			}
		});
		downloadDialog = builder.create();
		downloadDialog.show();

		downloadApk();
	}
	
	/**
	* ����apk
	* @param url
	*/	
	private void downloadApk(){
		downLoadThread = new Thread(downApkRunnable);
		downLoadThread.start();
	}
	
	private Runnable downApkRunnable = new Runnable() {	
		@Override
		public void run() {
			try {
				String apkName = context.getString(R.string.app_name) + "_"+updateInfo.getVersionName()+".apk";
				//�ж��Ƿ������SD��
				String storageState = Environment.getExternalStorageState();		
				if(storageState.equals(Environment.MEDIA_MOUNTED)){
					savePath = Environment.getExternalStorageDirectory().getAbsolutePath() + FileUtil.parentPath + "update" + File.separator;
					File file = new File(savePath);
					if(DEBUG) {
						Log.d(TAG, "### save path = " + savePath);
					}
					if(!file.exists()){
						file.mkdirs();
					}else {
						File[] allFile = file.listFiles();
						for(File tmp:allFile) {
							if(tmp.getName().toLowerCase().endsWith("apk")) {
								if(!tmp.getName().equalsIgnoreCase(apkName)) {
									tmp.delete();
								}
							}
						}
					}
					apkFilePath = savePath + apkName;
				}
				
				//û�й���SD�����޷������ļ�
				if(apkFilePath == null || apkFilePath == ""){
					handler.sendEmptyMessage(DOWN_NOSDCARD);
					return;
				}
				
				File ApkFile = new File(apkFilePath);
				//�Ƿ������ظ����ļ�
				if(ApkFile.exists()){
					downloadDialog.dismiss();
					if(DEBUG) {
						Log.d(TAG, "### installApk");
					}
					installApk();
					return;
				}
				
				FileOutputStream fos = new FileOutputStream(ApkFile);
				
				URL url = new URL(apkUrl);
				HttpURLConnection conn = (HttpURLConnection)url.openConnection();
				conn.connect();
				int length = conn.getContentLength();
				InputStream is = conn.getInputStream();
				int count = 0;
				byte buf[] = new byte[1024];
				
				do{   		   		
		    		int numread = is.read(buf);
		    		count += numread;
		    		downLoadProgressValue =(int)(((float)count / length) * 100);
		    	    //���½���
		    	    handler.sendEmptyMessage(DOWN_UPDATE);
		    		if(numread <= 0){	
		    			//�������֪ͨ��װ
		    			handler.sendEmptyMessage(DOWN_OVER);
		    			break;
		    		}
		    		fos.write(buf,0,numread);
		    	}while(!interceptFlag);//���ȡ����ֹͣ����
				
				fos.close();
				is.close();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch(IOException e){
				e.printStackTrace();
			}
			
		}
	};
	
		/**
	    * ��װapk
	    * @param url
	    */
		private void installApk(){
			File apkfile = new File(apkFilePath);
	        if (!apkfile.exists()) {
	            return;
	        }    
	        Intent i = new Intent(Intent.ACTION_VIEW);
	        i.setDataAndType(Uri.parse("file://" + apkfile.toString()), "application/vnd.android.package-archive"); 
	        context.startActivity(i);
		}
}
