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
import com.lidroid.xutils.BitmapUtils;

public class ListViewFileAdapter extends BaseAdapter {

	private Context context;// 运行上下文
	private List<FileDetail> listItems;// 数据集合
	private LayoutInflater listContainer;// 视图容器
	private int itemViewResource;// 自定义项视图源
	private int mNumColumns = 0;
	private int mItemHeight = 0;
	private RelativeLayout.LayoutParams mImageViewLayoutParams;
	
	private BitmapUtils mBitmapUtils;
	
	public ListViewFileAdapter(Context context, List<FileDetail> data,
			int resource) {
		this.context = context;
		this.listContainer = LayoutInflater.from(context);
		listItems = data;
		itemViewResource = resource;
		mImageViewLayoutParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		mBitmapUtils = new BitmapUtils(context);
		mBitmapUtils.configDefaultLoadingImage(R.drawable.ic_launcher);
		mBitmapUtils.configDefaultLoadFailedImage(R.drawable.widget_frame_video);
		mBitmapUtils.configDefaultBitmapConfig(Config.RGB_565);
	}

	@Override
	public int getCount() {
		return listItems.size();
	}

	@Override
	public FileDetail getItem(int position) {
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
        FileDetail fileThumb = getItem(position);
        if(fileThumb != null){
            if (itemView.cover.getLayoutParams().height != mItemHeight) {
                itemView.cover.setLayoutParams(mImageViewLayoutParams);
            }
            mBitmapUtils.display(itemView.cover, fileThumb.getFilePath());
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
    
    static class ListItemView { // 自定义控件集合
        public com.android.audiorecorder.myview.RecyclingImageView cover;
        public TextView name;
        public TextView number;
    }

}
