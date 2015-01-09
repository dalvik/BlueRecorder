package com.android.audiorecorder;

import java.io.File;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

public class FileUtils {

    public static int getAvailableSize(String path) {// MB
        StatFs statFs = new StatFs(path);
        int blockSize = statFs.getBlockSize();
        int avaliableBlck = statFs.getAvailableBlocks();
        System.out.println("path = " + path + " blockSize = " + blockSize
                + " avaliableBlck = " + avaliableBlck);
        return blockSize * avaliableBlck / 1024 / 1024;
    }

    public static String getExternalStoragePath(Context ctx) {
        String ret = "";
        if(Utils.hasHoneycomb()){
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
        } else {
            if(Environment.MEDIA_MOUNTED.equalsIgnoreCase(Environment.getExternalStorageState())){
                ret = Environment.getExternalStorageDirectory().getPath();
            }
        }
        return ret;
    }

}
