package com.android.audiorecorder.adapter;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap.Config;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.audiorecorder.R;
import com.android.audiorecorder.dao.FileThumb;
import com.android.audiorecorder.provider.FileDetail;
import com.android.audiorecorder.provider.FileProvider;
import com.android.audiorecorder.utils.UIHelper;
import com.lidroid.xutils.BitmapUtils;

public class ListViewFileThumbAdapter extends BaseAdapter {

	private Context context;// 运行上下文
	private List<FileThumb> listItems;// 数据集合
	private LayoutInflater listContainer;// 视图容器
	private int itemViewResource;// 自定义项视图源
	private int mNumColumns = 0;
	private int mItemHeight = 0;
	private RelativeLayout.LayoutParams mImageViewLayoutParams;
	
	private BitmapUtils mBitmapUtils;
	
	public ListViewFileThumbAdapter(Context context, List<FileThumb> data,
			int resource) {
		this.context = context;
		this.listContainer = LayoutInflater.from(context);
		listItems = data;
		itemViewResource = resource;
		mImageViewLayoutParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		mBitmapUtils = new BitmapUtils(context);
		mBitmapUtils.configDefaultBitmapConfig(Config.RGB_565);
	}

	@Override
	public int getCount() {
		return listItems.size();
	}

	@Override
	public FileThumb getItem(int position) {
		return listItems.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ListItemView itemView = null;
        if(convertView == null) {
            convertView = listContainer.inflate(itemViewResource, null);
            itemView = new ListItemView();
            itemView.cover = (com.android.audiorecorder.myview.RecyclingImageView) convertView.findViewById(R.id.catalog_file_conver);
            itemView.cover.setLayoutParams(mImageViewLayoutParams);
            itemView.name = (TextView)convertView.findViewById(R.id.catalog_file_title);
            itemView.number = (TextView)convertView.findViewById(R.id.catalog_img_number);
            //设置控件集到convertView
            convertView.setTag(itemView);
        }else {
            itemView = (ListItemView)convertView.getTag();
        }
        FileThumb fileThumb = getItem(position);
        if(fileThumb != null){
            if (itemView.cover.getLayoutParams().height != mItemHeight) {
                itemView.cover.setLayoutParams(mImageViewLayoutParams);
            }
            itemView.name.setText(fileThumb.getName());
            itemView.number.setText(String.valueOf(fileThumb.getFileNumber()));
            if(fileThumb.getFileType() == FileProvider.FILE_TYPE_JEPG || fileThumb.getFileType() == FileProvider.FILE_TYPE_VIDEO){
                mBitmapUtils.display(itemView.cover, fileThumb.getCoverPath());
            } else {
                mBitmapUtils.display(itemView.cover, null);
                mBitmapUtils.configDefaultLoadingImage(R.drawable.ic_default_sound_record_thumb);
            }
        }
        return convertView;
    }

    public void setItemHeight(int height) {
        if (height == mItemHeight) {
            return;
        }
        mItemHeight = height;
        mImageViewLayoutParams =
                new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, mItemHeight);
        notifyDataSetChanged();
    }
    
    public void setNumColumns(int numColumns) {
        mNumColumns = numColumns;
    }

    public int getNumColumns() {
        return mNumColumns;
    }
    
    public void setFileType(int fileType){
        if(fileType == UIHelper.LISTVIEW_DATATYPE_LOCAL_IMAGE || fileType == UIHelper.LISTVIEW_DATATYPE_REMOTE_IMAGE){
            mBitmapUtils.configDefaultLoadingImage(R.drawable.ic_default_image_thumb);
            mBitmapUtils.configDefaultLoadFailedImage(R.drawable.ic_default_image_thumb);
        } else if(fileType == UIHelper.LISTVIEW_DATATYPE_LOCAL_VIDEO || fileType == UIHelper.LISTVIEW_DATATYPE_REMOTE_VIDEO){
            mBitmapUtils.configDefaultLoadingImage(R.drawable.widget_frame_video);
            mBitmapUtils.configDefaultLoadFailedImage(R.drawable.widget_frame_video);
        } else if(fileType == UIHelper.LISTVIEW_DATATYPE_LOCAL_AUDIO || fileType == UIHelper.LISTVIEW_DATATYPE_REMOTE_AUDIO){
            mBitmapUtils.configDefaultLoadFailedImage(R.drawable.ic_default_sound_record_thumb);
        } else if(fileType == UIHelper.LISTVIEW_DATATYPE_LOCAL_OTHER || fileType == UIHelper.LISTVIEW_DATATYPE_REMOTE_OTHER){
            mBitmapUtils.configDefaultLoadingImage(R.drawable.ic_default_other_thumb);
            mBitmapUtils.configDefaultLoadFailedImage(R.drawable.ic_default_other_thumb);
        }
    }
    
    static class ListItemView { // 自定义控件集合
        public com.android.audiorecorder.myview.RecyclingImageView cover;
        public TextView name;
        public TextView number;
    }

}
