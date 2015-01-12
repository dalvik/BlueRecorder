package com.android.audiorecorder.engine;

import java.io.File;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.HTTP;

import com.android.audiorecorder.engine.ProgressOutHttpEntity.ProgressListener;

public class RecorderUploader extends Thread {

	private String url = "http://davmb.com/file_recv.php";
	private String mPath;
	private long mTransferedBytes;
	private int mResult;
	private UploadResult mUploadResult;
	private int mId;

	public RecorderUploader(UploadResult uploadResult) {
		this.mUploadResult = uploadResult;
		mResult = UploadResult.IDLE;
	}

	@Override
	public void run() {
		super.run();
		File file = new File(mPath);
		MultipartEntityBuilder entitys = MultipartEntityBuilder.create();
		entitys.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		entitys.setCharset(Charset.forName(HTTP.UTF_8));

		entitys.addPart("file", new FileBody(file));
		HttpEntity httpEntity = entitys.build();
		long totalSize = httpEntity.getContentLength();
		System.out.println("total size = " + totalSize);
		ProgressOutHttpEntity progressHttpEntity = new ProgressOutHttpEntity(
				httpEntity, new ProgressListener() {
					@Override
					public void transferred(long transferedBytes) {
						// publishProgress((int) (100 * transferedBytes /
						// totalSize));
						mTransferedBytes = transferedBytes;
					}
				});
		uploadFile(url, progressHttpEntity);
	}

	public void uploadFile(String url, ProgressOutHttpEntity entity) {
		HttpClient httpClient = new DefaultHttpClient();
		httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		// 设置连接超时时间
		httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
		HttpPost httpPost = new HttpPost(url);
		httpPost.setEntity(entity);
		try {
			mResult = UploadResult.PROCESS;
			HttpResponse httpResponse = httpClient.execute(httpPost);
			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				mResult = UploadResult.SUCCESS;
			}
		} catch (Exception e) {
			mResult = UploadResult.FAIL;
			e.printStackTrace();
		} finally {
			if (httpClient != null && httpClient.getConnectionManager() != null) {
				httpClient.getConnectionManager().shutdown();
			}
			if(mResult == UploadResult.SUCCESS){
				File f = new File(mPath);
				f.delete();
			}
			System.out.println("upload size = " + mTransferedBytes);
			mUploadResult.onResult(mResult, mId, mTransferedBytes);
			mResult = UploadResult.IDLE;
		}
	}
	
	public void setInfo(int id, String path){
		this.mPath = path;
		this.mId = id;
	}
	
	public int getResult(){
		return mResult;
	}
	
    public interface UploadResult{
    	
    	public final static int IDLE = -1;
    	public final static int PROCESS = 0;
    	public final static int SUCCESS = 1;
    	public final static int FAIL = 2;
    	
    	public void onResult(int result, int id, long progress);
    }

}
