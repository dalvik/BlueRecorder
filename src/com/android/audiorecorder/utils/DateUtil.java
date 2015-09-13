package com.android.audiorecorder.utils;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {
	
	public static String formatyyMMDDHHmmss(long time){
		Date date  = new Date(time);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
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
	
	public static String getYearMonthWeek(long time){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONDAY)+1;
        int week = calendar.get(Calendar.WEEK_OF_MONTH);
        return String.valueOf(year) + File.separator + String.valueOf(month) + File.separator + String.valueOf(week);
    }
}
