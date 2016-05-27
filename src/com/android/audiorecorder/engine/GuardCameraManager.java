package com.android.audiorecorder.engine;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;

public class GuardCameraManager {

    private Context mContext;
    
    private Camera mCamera;
    
    private SurfaceTexture mTexture;
    
    private boolean mIsSupportAutoFocus;
    
    private static final int JPEGQUALITY = 70;
    
    public GuardCameraManager(Context context){
        this.mContext = context;
    }
    
    public boolean checkCameraHardware(String front){
        return mContext.getPackageManager().hasSystemFeature(front);
    }
    
    public int getCameraNumber(){
        return 0;
    }
    
    public Camera getCameraInstance(int front){
        mCamera = Camera.open(front);
        return mCamera;
    }
    
    public boolean startPreview(int textTureId, AutoFocusCallback autoFocusCallback){
        if(mTexture == null){
            mTexture = new SurfaceTexture(textTureId);
        }
        if(mCamera != null){
            try {
                mCamera.setPreviewTexture(mTexture);
                mCamera.startPreview();
                if(mIsSupportAutoFocus){
                    mCamera.autoFocus(autoFocusCallback);
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    
    public void getCameraInfo(){
        Camera.Parameters params = mCamera.getParameters();
        List<String> modes = params.getSupportedFocusModes();
        if(modes.contains(Camera.Parameters.FOCUS_MODE_AUTO)){
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            mIsSupportAutoFocus = true;
        }
        Resources res = mContext.getResources();
        System.out.println(res.getConfiguration().orientation+"---------->");
        if(res.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            params.set("rotation", 270);
        }else{
            params.set("rotation", -90);
        }
        if(isSupport(params, ImageFormat.JPEG)){
            params.setPictureFormat(ImageFormat.JPEG);
            params.setJpegQuality(JPEGQUALITY);
        }
        Size size = getMaxPictureSize(params);
        if(size != null){
            params.setPictureSize(size.width, size.height);
        }
        try{
            mCamera.setParameters(params);
        } catch(Exception e){
            e.printStackTrace();
            System.out.println("setParameters failed." + e.getLocalizedMessage());
        }
    }
    
    public boolean isSupportSilgent(){
        return false;
    }
    
    public boolean isSupportAutoFocus(){
        return mIsSupportAutoFocus;
    }
    
    public boolean setSurfaceTexture(SurfaceTexture texture){
        if(mCamera != null){
            try {
                mCamera.setPreviewTexture(texture);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    public void muteAll(){
        
    }
    
    public void takePicture(ShutterCallback shutter,  PictureCallback raw, PictureCallback jpegCallback){
        if(mCamera != null){
            mCamera.takePicture(shutter, raw, jpegCallback);
        }
    }
    
    public void releaseCamera(){
        if(mCamera != null){
            if(mIsSupportAutoFocus){
                mCamera.cancelAutoFocus();
            }
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
    
    private Size getMaxPictureSize(Camera.Parameters params){
        List<Size> pictureSizeList = params.getSupportedPictureSizes();
        int size = pictureSizeList.size();
        int defaultWidth = 0;
        Size pictureSize = null;
        for(int i=0;i<size; i++){
            Size temp = pictureSizeList.get(i);
            if(temp.width>defaultWidth){
                defaultWidth = temp.width;
                pictureSize = temp;
            }
        }
        return pictureSize;
    }
    
    private boolean isSupport(Camera.Parameters params, int format){
        List<Integer> pictureformats = params.getSupportedPictureFormats();
        for(Integer t:pictureformats){
            if(t == format){
                return true;
            }
        }
        return false;
    }
    
}
