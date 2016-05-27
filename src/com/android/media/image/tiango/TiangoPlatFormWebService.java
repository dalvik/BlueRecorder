/**   
 * Copyright © 2016 浙江大华. All rights reserved.
 * 
 * @title: TiangoPlatFormWebService.java
 * @description: TODO
 * @author: 23536   
 * @date: 2016年4月8日 下午5:02:50 
 */
package com.android.media.image.tiango;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.library.net.base.IDataCallback;
import com.android.library.net.utils.JSONType;
import com.android.media.AbstractPlatForm;
import com.android.media.data.BaseData;

/** 
 * @description: TODO
 * @author: 23536
 * @date: 2016年4月8日 下午5:02:50  
 */
public class TiangoPlatFormWebService extends AbstractPlatForm {

    private final static String URL_IMAGE_THUMB = "";
    
    public TiangoPlatFormWebService(IDataCallback callback) {
        super(callback);
    }

    /* (non Javadoc) 
     * @title: getThumbList
     * @description: TODO
     * @param json
     * @return 
     * @see com.android.media.IPlatform#getThumbList(java.lang.String) 
     */
    @Override
    public String getThumbList(String json) {
        try {
            JSONObject param = new JSONObject(json);
            TiangoImageThumbDataReq req = new TiangoImageThumbDataReq();
            //req.w = param.optString("w");
            //req.x = param.optString("x");
            //req.y = param.optString("y");
            doGetRequest(URL_IMAGE_THUMB, "", req, new JSONType<BaseData<TiangoImageThumbDataResp>>(){});
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /* (non Javadoc) 
     * @title: getFileList
     * @description: TODO
     * @param json
     * @return 
     * @see com.android.media.IPlatform#getFileList(java.lang.String) 
     */
    @Override
    public String getFileList(String json) {
        /*try {
            JSONObject param = new JSONObject(json);
            int type = param.optInt("http_type");
            HzBicycleReq req = new HzBicycleReq();
            req.w = param.optString("w");
            req.x = param.optString("x");
            req.y = param.optString("y");
            doGetRequest(REQUEST_URL_LOCATION, "", req, new JSONType<AbstractData<HzBicycleDataResp>>(){});
        } catch (JSONException e) {
            e.printStackTrace();
        }*/
        return null;
    }

}
