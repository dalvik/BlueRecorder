/**   
 * Copyright © 2016 浙江大华. All rights reserved.
 * 
 * @title: IPlatform.java
 * @description: TODO
 * @author: 23536   
 * @date: 2016年4月8日 下午3:03:00 
 */
package com.android.media;

/** 
 * @description: 网络获取文件列表信息接口
 * @author: 23536
 * @date: 2016年4月8日 下午3:03:00  
 */
public interface IPlatform {

    /**
     * 
     * @title: getThumbList 
     * @description: 获取缩略图相册列表
     * @param json
     * @return String
     */
    public String getThumbList(String json);
    
    /**
     * 
     * @title: getFileList 
     * @description: 获取文件列表
     * @param json
     * @return String
     */
    public String getFileList(String json);
    
}
