package com.android.audiorecorder.utils;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;


public class StringUtil {
    
    public static int toInt(Object obj) {
        if(obj==null) return 0;
        return toInt(obj.toString(),0);
    }
    
    public static int toInt(String str, int defValue) {
        try{
            return Integer.parseInt(str);
        }catch(Exception e){}
        return defValue;
    }
    
    public static Spanned loadHtmlText(Context context, int resId){
       return Html.fromHtml(context.getString(resId));
    }
}
