package com.android.audiorecorder.utils;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

import com.android.audiorecorder.DebugConfig;
import com.android.audiorecorder.R;
import com.android.audiorecorder.engine.MultiMediaService;
import com.android.audiorecorder.provider.FileProvider;
import com.android.audiorecorder.provider.FileProviderService;
import com.android.audiorecorder.ui.SoundRecorder;

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
    
    public static long avliableDiskSize(Context ctx, int where){
        if(where == SoundRecorder.STORAGE_LOCATION_SD_CARD){
            if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                return 0;
            }
            String extStoragePath = Environment.getExternalStorageDirectory().getPath();
            if(DebugConfig.DEBUG){
                System.out.println("---> external storage path = " + extStoragePath);
            }
            return FileUtils.getAvailableSize(extStoragePath);
        } else {
            String interStoragePath = getExternalStoragePath(ctx);
            if(interStoragePath.length() > 0){
                if(DebugConfig.DEBUG){
                    System.out.println("---> internal storage path = " + interStoragePath);
                }
            } else {
                interStoragePath = Environment.getExternalStorageDirectory().getPath();
                if(DebugConfig.DEBUG){
                    System.out.println("---> internal storage do not exist, get default path = " + interStoragePath);
                }
            }
            return getAvailableSize(interStoragePath);
        }
    }
    
    //record /storage/sdcard0/MediaFile/Record/AUDIO/YYYY/MONTH/WEEK/file_name.wav
    public static String generalFilePath(Context ctx, int mode, int where, int fileType, String incommingNumber, String mac){
        String storagePath;
        String completeRecoderPath;
        if(where == SoundRecorder.STORAGE_LOCATION_SD_CARD){
            if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
                storagePath = Environment.getExternalStorageDirectory().getPath();
            } else {
                storagePath = getExternalStoragePath(ctx);
            }
        } else {
            storagePath = getExternalStoragePath(ctx);
            if(storagePath.length() > 0){
                if(DebugConfig.DEBUG){
                    System.out.println("file record " + "---> internal storage path = " + storagePath);
                }
            } else {
                storagePath = Environment.getExternalStorageDirectory().getPath();
                if(DebugConfig.DEBUG){
                    System.out.println("file record " + "---> internal storage do not exist, get default path = " + storagePath);
                }
            }
        }
        long now = System.currentTimeMillis();
        if(mode != MultiMediaService.LUNCH_MODE_AUTO){
            completeRecoderPath = storagePath + File.separator + FileProviderService.ROOT;
            File rootPath = new File(completeRecoderPath);
            if(!rootPath.exists()){
                rootPath.mkdirs();
            }
            File nomediaFile = new File(completeRecoderPath, ".nomedia");
            if(!nomediaFile.exists()){
                try {
                    nomediaFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            final String parent = File.separator + FileProviderService.CATE_RECORD + File.separator + FileProviderService.TYPE_AUDIO;
            completeRecoderPath += parent + File.separator + DateUtil.getYearMonthWeek(now);
            File completeFilePath = new File(completeRecoderPath);
            if(!completeFilePath.exists()){
                completeFilePath.mkdirs();
            }
            String pre = "";
            if(incommingNumber != null && incommingNumber.length()>0){
               pre = incommingNumber+"_";
            }
            String fileName = null;
            if(fileType == SoundRecorder.FILE_TYPE_3GPP){
                fileName = File.separator + pre + DateUtil.formatyyMMDDHHmmss(now)+".3gp";
            } else {
                fileName = File.separator + pre + DateUtil.formatyyMMDDHHmmss(now)+".wav";
            }
            completeRecoderPath += fileName;
       } else {//auto mode
           File catchPath = FileUtils.getDiskCacheDir(ctx, FileProviderService.CATE_RECORD);
           if(!catchPath.exists()){
               catchPath.mkdirs();
           }
           completeRecoderPath = catchPath.getPath() + File.separator + mac + DateUtil.formatyyMMDDHHmmss(now)+".wav";
       }
        return completeRecoderPath;
    }
    
    /**
     * eg:/storage/sdcard0/MediaFile/Record/Image/YYYY/MONTH/WEEK/
     * or:/storage/sdcard0/Android/data/com.xx.xxx/MediaFile/Record/AUDIO
     * or:null
     * @param context
     * @param luancheMode
     * @param fileType
     * @return
     */
    public static String getPathByModeAndType(Context context, int luancheMode, int fileType){
        String root = getRootPath(context);//storage/sdcard0
        /**
         * if audo mode, no need to check exteranl storage state
         * 
         */
        String fileTypeFoldName = getFileTypePath(fileType);
        if(luancheMode == MultiMediaService.LUNCH_MODE_AUTO){//no user launched, get cache path.
            File cachePath = FileUtils.getDiskCacheDir(context, FileProviderService.CATE_RECORD + File.separator + fileTypeFoldName);
            //cachePath: storage/sdcard0/Android/data/com.xx.xxx/Record/AUDIO
            if(!cachePath.exists()){
                cachePath.mkdirs();
            }
            return cachePath.getAbsolutePath();
        } else {
            if(root == null || root.length() == 0){// no valuable storage path.
                System.out.println("no valuable storage path for use.");
                return null;
            }
            // storage/sdcard0/MediaFile/Record/AUDIO
            final String PARENT_PATH = root + File.separator + FileProviderService.ROOT + File.separator;//storage/sdcard0/MediaFile
            final String completePath = PARENT_PATH + FileProviderService.CATE_RECORD + File.separator 
                    + fileTypeFoldName + File.separator + DateUtil.getYearMonthWeek(System.currentTimeMillis());
            File completeFilePath = new File(completePath);
            if(!completeFilePath.exists()){
                completeFilePath.mkdirs();
            }
            File nomediaFile = new File(PARENT_PATH, ".nomedia");
            createNewFile(nomediaFile);
            final String PARENT_PATH_OLD = root + File.separator + FileProviderService.ROOT_OLD + File.separator;//storage/sdcard0/BlueRecorder
            if(new File(PARENT_PATH_OLD).exists()){
                File nomediaFileOldPath = new File(PARENT_PATH_OLD, ".nomedia");
                createNewFile(nomediaFileOldPath);
            }
            return completePath;
        }
    }
    
    private static String getRootPath(Context context){
        if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
            return Environment.getExternalStorageDirectory().getPath();
        } else {
            return getExternalStoragePath(context);
        }
    }
    
    public static String getFileTypePath(int fileType){
        String fileTypeFolder = null;
        if(fileType == FileProvider.FILE_TYPE_AUDIO){
            fileTypeFolder = FileProviderService.TYPE_AUDIO;
        } else if(fileType == FileProvider.FILE_TYPE_JEPG) {
            fileTypeFolder = FileProviderService.TYPE_JPEG;
        } else if(fileType == FileProvider.FILE_TYPE_VIDEO){
            fileTypeFolder = FileProviderService.TYPE_VIDEO;
        } else {
            fileTypeFolder = FileProviderService.TYPE_Other;
        }
        return fileTypeFolder;
    }
    
    public static String getMimeName(int fileType){
        String mime = ".data";
        switch(fileType ){
            case SoundRecorder.FILE_TYPE_3GPP:
                mime = ".mp3";
                break;
            case SoundRecorder.FILE_TYPE_WAV:
                mime = ".wav";
                break;
                default:
                    break;
        }
        return mime;
    }
    
    public static Set<Integer> getLaunchModeSet(){
        Set<Integer> launchType = new HashSet<Integer>();
        launchType.add(MultiMediaService.LUNCH_MODE_CALL);
        launchType.add(MultiMediaService.LUNCH_MODE_MANLY);
        if(DebugConfig.DEBUG){
            launchType.add(MultiMediaService.LUNCH_MODE_AUTO);
        }
        return launchType;
    }
    
    public static Set<Integer> getLaunchModeSet(int type){
        Set<Integer> launchType = new HashSet<Integer>();
        launchType.add(type);
        if(DebugConfig.DEBUG){
            launchType.add(MultiMediaService.LUNCH_MODE_AUTO);
        }
        return launchType;
    }
	
	private static void createNewFile(File file){
        if(file != null && !file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void deleteEmptyDirectory(String parth){
        File file = new File(parth);
        if(file.isDirectory()){
            File[] files = file.listFiles();
            if(files.length == 0){
                file.delete();
                if(file.getParent() != null){
                    deleteEmptyDirectory(file.getParentFile().getAbsolutePath());
                }
            } else {
                for(File f:files){
                    deleteEmptyDirectory(f.getAbsolutePath());
                }
            }
        }
    }
    
    public static boolean deleteFolder(String folder){
        File file = new File(parentPath);
        if(file.isDirectory()){
            File[] files = file.listFiles();
            if(files != null && files.length == 0){
                return file.delete();
            }
        }
        return false;
    }
}
