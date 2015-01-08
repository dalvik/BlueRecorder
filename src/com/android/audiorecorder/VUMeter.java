package com.android.audiorecorder;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class VUMeter extends View {
    static final long ANIMATION_INTERVAL = 40L;
    static final float DROPOFF_STEP = 0.18F;
    static final float PIVOT_RADIUS = 3.5F;
    static final float PIVOT_Y_OFFSET = 10.0F;
    static final float SHADOW_OFFSET = 2.0F;
    static final float SURGE_STEP = 0.35F;
    public static final String TAG = "VUMeter";
    private boolean DEBUG;
    public int count;
    public int lastvalues;
    private Bitmap mBitmap;
    public int mChoice;
    float mCurrentAngle;
    private Drawable mNiddle;
    Paint mPaint;
    Recorder mRecorder;
    Paint mShadow;
    private Resources res;

    public VUMeter(Context paramContext) {
        super(paramContext);
        Resources localResources = getResources();
        this.res = localResources;
        this.DEBUG = true;
        this.lastvalues = 0;
        this.count = 0;
        init(paramContext);
    }

    public VUMeter(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
        Resources localResources = getResources();
        this.res = localResources;
        this.DEBUG = true;
        this.lastvalues = 0;
        this.count = 0;
        init(paramContext);
    }

    void init(Context paramContext) {
        Paint localPaint1 = new Paint(1);
        this.mPaint = localPaint1;
        this.mPaint.setColor(-1);
        Paint localPaint2 = new Paint(1);
        this.mShadow = localPaint2;
        Paint localPaint3 = this.mShadow;
        int i = Color.argb(0, 0, 0, 0);
        localPaint3.setColor(i);
        this.mRecorder = null;
        this.mCurrentAngle = 0;
    }

    protected void onDraw(Canvas paramCanvas)
  {
    super.onDraw(paramCanvas);
    int i = -1080377238;
    this.mCurrentAngle = i;
    float f1 = 0;
    float f4 = 0;
    if (this.mRecorder != null)
    {
      if (this.DEBUG)
        Log.v("VUMeter", "mRecorder !=null");
      int j = this.mRecorder.getMaxAmplitude();
      int k = this.count;
      int m = 0;
      m++;
      this.count = k;
      j = this.lastvalues;
      if (this.count == 4)
        j = 0;
      this.lastvalues = j;
      if (this.DEBUG)
      {
        String str1 = "onDraw maxAmplitude =" + j;
        Log.v("VUMeter", str1);
      }
      float f2 = j;
      float f3 = 1073694808 * f2 / 1191182336;
      f1 = -1080377238 + f3;
      float n = this.mCurrentAngle;
      double d = f1 + 4596373779694328218L;
      f4 = (float)(4613937818241073152L * d / 4611686018427387904L + 4596373779694328218L);
    }
    
    float f9;
    for (this.mCurrentAngle = f4; ; this.mCurrentAngle = f9)
    {
      float i1 = this.mCurrentAngle;
      float f5 = Math.min(1069371334, i1);
      this.mCurrentAngle = f5;
      //if ((this.mRecorder.state() == 3) || (this.mRecorder.state() == 0))
      //  this.mCurrentAngle = -1080377238;
      if (this.DEBUG)
      {
        StringBuilder localStringBuilder = new StringBuilder("mCurrentAngle ==");
        float i2 = this.mCurrentAngle;
      }
      float f6 = (this.mCurrentAngle - 1041865114) * 1127481344 / 1078530011;
      int i3 = getWidth();
      int i4 = getHeight();
      int i5 = i3 / 2;
      int i6 = i4;
      PaintFlagsDrawFilter localPaintFlagsDrawFilter = new PaintFlagsDrawFilter(0, 3);
      paramCanvas.setDrawFilter(localPaintFlagsDrawFilter);
      float f7 = i5;
      float f8 = i6 - 1099169792;
      paramCanvas.rotate(f6, f7, f8);
      int i7 = this.mNiddle.getIntrinsicWidth();
      Drawable localDrawable1 = this.mNiddle;
      int i8 = i7 / 2;
      int i9 = i5 - i8;
      int i10 = i7 / 2 + i5;
      localDrawable1.setBounds(i9, i7, i10, i6);
      Drawable localDrawable2 = this.mNiddle;
      localDrawable2.draw(paramCanvas);
      //if ((this.mRecorder != null) && (this.mRecorder.state() == 1))
        postInvalidateDelayed(40L);
      //return;
      this.count = 0;
      //break;
      float i11 = this.mCurrentAngle;
      f9 = Math.max(f1, i11);
    }
  }

    public void setRecorder(Recorder paramRecorder) {
        this.mRecorder = paramRecorder;
        Drawable localDrawable = this.res.getDrawable(R.drawable.niddle);
        this.mNiddle = localDrawable;
        invalidate();
    }
}