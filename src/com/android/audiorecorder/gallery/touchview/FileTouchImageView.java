/*
 Copyright (c) 2012 Roman Truba

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial
 portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.android.audiorecorder.gallery.touchview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.android.audiorecorder.DebugConfig;
import com.android.audiorecorder.gallery.genuin.BitmapCache;
import com.android.audiorecorder.utils.StringUtil;
import com.lidroid.xutils.BitmapUtils;
import com.lidroid.xutils.bitmap.BitmapCommonUtils;
import com.lidroid.xutils.bitmap.BitmapDisplayConfig;
import com.lidroid.xutils.bitmap.callback.BitmapLoadCallBack;
import com.lidroid.xutils.bitmap.callback.BitmapLoadFrom;
import com.lidroid.xutils.bitmap.factory.BitmapFactory;

public class FileTouchImageView extends RelativeLayout{
    protected ProgressBar mProgressBar;
    protected TouchImageView mImageView;

    protected Context mContext;

    private BitmapUtils mBitmapUtils;

    private Bitmap imageThumb;
    
    private BitmapCache mCache;
    
    private BitmapDisplayConfig bd;
    
    private String TAG = "FileTouchImageView";
        
    public FileTouchImageView(Context ctx, BitmapUtils bitmapUtils, int fileType, BitmapCache cache) {
        super(ctx);
        mContext = ctx;
        mBitmapUtils = bitmapUtils;
        this.mCache = cache;
        init();

    }
    public FileTouchImageView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        mContext = ctx;
        init();
    }
    public TouchImageView getImageView() { return mImageView; }

    protected void init() {
        mImageView = new TouchImageView(mContext);
        mImageView.setRecycler(mCache);
        mImageView.setTag(0);
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mImageView.setLayoutParams(params);
        this.addView(mImageView);
        mImageView.setVisibility(GONE);

        mProgressBar = new ProgressBar(mContext, null, android.R.attr.progressBarStyleLarge);
        params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        params.setMargins(30, 0, 30, 0);
        mProgressBar.setLayoutParams(params);
        mProgressBar.setIndeterminate(true);
        this.addView(mProgressBar);
        bd = new BitmapDisplayConfig();
        bd.setBitmapConfig(Config.RGB_565);
        bd.setBitmapMaxSize(BitmapCommonUtils.getScreenSize(mContext));
    }

    public void setUrl(int pos, String imagePath) {
        if(DebugConfig.DEBUG) {
            Log.d(TAG, "===>setUrl imagePath = " + imagePath + " position = " + pos);
        }
        //imageThumb = mBitmapUtils.getBitmapFromMemCache(imagePath, null);
        if(imageThumb != null && !imageThumb.isRecycled()) {
            if(imageThumb != null) {
                if(StringUtil.toInt(mImageView.getTag()) == 0){
                    mImageView.setImageBitmap(imageThumb);
                }
            }
            mImageView.setVisibility(VISIBLE);
            mProgressBar.setVisibility(GONE);
        }else{
            //new ImageLoadTask().execute(imagePath);
            //mBitmapUtils.display(mImageView, imagePath);
            mBitmapUtils.display(mImageView, imagePath, new BitmapLoadCallBack<TouchImageView>(){

                @Override
                public void onLoadCompleted(TouchImageView view, String arg1,
                        Bitmap bitmap, BitmapDisplayConfig arg3,
                        BitmapLoadFrom arg4) {
                    mImageView.setImageBitmap(bitmap);
                    mImageView.setVisibility(VISIBLE);
                    mProgressBar.setVisibility(GONE);
                }

                @Override
                public void onLoadFailed(TouchImageView view, String arg1, Drawable failed) {
                    mImageView.setImageDrawable(failed);
                    mImageView.setVisibility(VISIBLE);
                    mProgressBar.setVisibility(GONE);
                }
                
            });
        }
    }
    
    //No caching load
    /*public class ImageLoadTask extends AsyncTask<String, Integer, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... strings) {
            ImageCache imageCache = mFetcher.getImageCache();
            if(imageCache != null) {
                imageThumb = imageCache.getBitmapFromDiskCache(strings[0]);
                if(imageThumb != null) {
                    BitmapDrawable drawable = null;
					drawable = new RecyclingBitmapDrawable(getResources(), imageThumb);
                    imageCache.addBitmapToCache(strings[0], drawable);
                }
                if(imageThumb == null || imageThumb.isRecycled()) {
                    imageThumb = mFetcher.processBitmap(strings[0]);
                    if(imageThumb != null) {
                        BitmapDrawable drawable = null;
						drawable = new RecyclingBitmapDrawable(getResources(), imageThumb);
                        imageCache.addBitmapToCache(strings[0], drawable);
                    }
                }
            }
            return imageThumb;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(imageThumb != null) {
                if(StringUtil.toInt(mImageView.getTag()) == 0){
                    mImageView.setImageBitmap(imageThumb);
                }
            }
            mImageView.setVisibility(VISIBLE);
            mProgressBar.setVisibility(GONE);
        }
    }*/
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(imageThumb != null && !imageThumb.isRecycled()) {
            imageThumb.recycle();
            imageThumb = null;
        }
        mImageView.setImageDrawable(null);
        if(DebugConfig.DEBUG) {
            Log.d(TAG, "===> touch image deteched from window");
        }
    }
    
}
