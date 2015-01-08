package com.android.audiorecorder;

import android.os.StatFs;

public class FileUtils {

    public static int getAvailableSize(String path){//MB
        StatFs statFs = new StatFs(path);
        int blockSize = statFs.getBlockSize();
        int avaliableBlck = statFs.getAvailableBlocks();
        System.out.println("path = " + path +  " blockSize = " + blockSize +  " avaliableBlck = " + avaliableBlck);
        return blockSize * avaliableBlck/1024/1024;
    }
}
