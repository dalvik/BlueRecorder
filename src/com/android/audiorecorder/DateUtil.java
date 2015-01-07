package com.android.audiorecorder;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {
	
	public static String formatyyMMDDHHmmss(long time){
		Date date  = new Date(time);
		SimpleDateFormat sdf = new SimpleDateFormat("yy_MM_dd HH_mm_ss");
		return sdf.format(date);
	}
	
	public static String formatyyMMDDHHmm(long time){
		Date date  = new Date(time);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		return sdf.format(date);
	}
	
	public static String formatyyMMDDHHmm2(long time){
        Date date  = new Date(time);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(date);
    }
	
	public static String formatMMDD(long time){
        Date date  = new Date(time);
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd");
        return sdf.format(date);
    }
	
}
