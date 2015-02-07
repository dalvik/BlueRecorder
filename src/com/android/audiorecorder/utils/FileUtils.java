package com.android.audiorecorder.utils;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

import com.android.audiorecorder.R;

public class FileUtils {

    public static long getAvailableSize(String path) {// MB
        StatFs statfs = new StatFs(path);
        //long totalBlocks = statfs.getBlockCount();
        long blockSize = statfs.getBlockSize();
        long avaliableBlocks = statfs.getAvailableBlocks();
        //long freeBlocks = statfs.getFreeBlocks();
        //long extTotalSize = totalBlocks * blockSize /1024/1024;
        long extFreeSize = avaliableBlocks * blockSize / 1024/1024;
        return extFreeSize;
    }

    public static String getExternalStoragePath(Context ctx) {
        String ret = "";
        if (!Environment.isExternalStorageEmulated()
                && Environment.isExternalStorageRemovable()
                && Environment.getExternalStorageDirectory().canWrite()) {
            ret = Environment.getExternalStorageDirectory().getPath();
        } else {
            final String[] paths = ctx.getResources().getStringArray(R.array.external_sd_path);
            for (String one : paths) {
                File f = new File(one);
                if (f.isDirectory() && f.canRead() && f.canWrite()) {
                    ret = one;
                    break;
                }
            }
        }
        return ret;
    }

    /** 转换文件大小 **/
    public static String formetFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        if (fileS < 1024) {
            fileSizeString = fileS + " B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + " K";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + " M";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + " G";
        }
        return fileSizeString;
    }
    
    public static String parentPath = File.separator + "DownLoad" + File.separator;
    
    public static File getDiskCacheDir(Context context, String uniqueName) {
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        final String cachePath =
                Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                        !isExternalStorageRemovable() ? getExternalCacheDir(context).getPath() :
                                context.getCacheDir().getPath();
        File nomedia = new File(cachePath, ".nomedia");
        if(!nomedia.exists()){
            try {
                nomedia.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new File(cachePath + File.separator + uniqueName);
    }
    
    public static boolean isExternalStorageRemovable() {
        if (Utils.hasGingerbread()) {
            return Environment.isExternalStorageRemovable();
        }
        return true;
    }
    
    public static File getExternalCacheDir(Context context) {
        if (Utils.hasFroyo()) {
            return context.getExternalCacheDir();
        }
        // Before Froyo we need to construct the external cache dir ourselves
        final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
        return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
    }
}
