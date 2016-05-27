/**   
 * Copyright © 2016 浙江大华. All rights reserved.
 * 
 * @title: AbstractPlatForm.java
 * @description: TODO
 * @author: 23536   
 * @date: 2016年1月27日 上午10:13:56 
 */
package com.android.media;

import com.android.library.net.base.IDataCallback;
import com.android.library.net.manager.JSONHttpDataManager;
import com.android.library.net.req.DataReq;
import com.android.library.net.resp.DataResp;
import com.android.media.data.BaseData;


public abstract class AbstractPlatForm extends JSONHttpDataManager<BaseData<DataResp>, DataReq> implements IPlatform{

    public AbstractPlatForm(IDataCallback callback) {
        super(callback);
    }

}
