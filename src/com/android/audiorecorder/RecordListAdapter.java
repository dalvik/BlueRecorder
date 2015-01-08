package com.android.audiorecorder;

import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

public class RecordListAdapter extends BaseAdapter {

    private Context mContext;
    
    private LayoutInflater mInflater;
    
    private List<RecorderFile> mFileList;
    
    private int mPlayId;

    private View mView;
    
    public RecordListAdapter(Context context, List<RecorderFile> fileList){
        this.mContext = context;
        mInflater = LayoutInflater.from(mContext);
        this.mFileList = fileList;
    }

    @Override
    public int getCount() {
        return mFileList.size();
    }

    @Override
    public RecorderFile getItem(int position) {
        return mFileList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @SuppressLint("NewApi")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        RecorderFile file = getItem(position);
        if(convertView == null){
            viewHolder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.recordlist_items, null);
            viewHolder.title = (TextView)convertView.findViewById(R.id.title);
            viewHolder.duration = (TextView)convertView.findViewById(R.id.duration);
            viewHolder.size = (TextView)convertView.findViewById(R.id.size);
            viewHolder.play = (ImageButton)convertView.findViewById(R.id.play);
            viewHolder.play.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    
                }
            });
            viewHolder.pause = (ImageButton)convertView.findViewById(R.id.pause);
            viewHolder.pause.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    
                }
            });
            viewHolder.play_indicator = (ImageView)convertView.findViewById(R.id.play_indicator);
            viewHolder.ibListItemMenu = (ImageButton)convertView.findViewById(R.id.list_item_menu);
            viewHolder.ibListItemMenu.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    
                    PopupMenu localPopupMenu = new PopupMenu(mContext, viewHolder.ibListItemMenu);
                    Menu localMenu = localPopupMenu.getMenu();
                    localMenu.add(1, 1, 1, R.string.play);
                    localMenu.add(1, 2, 1, R.string.delete_item);
                    localMenu.add(1, 3, 1, R.string.information);
                    localPopupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            return false;
                        }
                    });
                    localPopupMenu.show();
                }
            });
            convertView.setTag(viewHolder);
        } else {
            viewHolder =  (ViewHolder) convertView.getTag();
        }
        viewHolder.id = file.getId();
        if(viewHolder.id == mPlayId){
            viewHolder.play_indicator.setVisibility(View.INVISIBLE);
            viewHolder.play.setVisibility(View.VISIBLE);
            viewHolder.pause.setVisibility(View.INVISIBLE);
        } else {
            viewHolder.play_indicator.setVisibility(View.INVISIBLE);
            viewHolder.play.setVisibility(View.INVISIBLE);
            viewHolder.pause.setVisibility(View.INVISIBLE);
        }
        viewHolder.title.setText(file.getName());
        viewHolder.duration.setText(String.valueOf(file.getDuration()));
        viewHolder.size.setText(String.valueOf(file.getSize()));
        return convertView;
    }
    
    private void popupWindow(View view){
        
    }
    
    public void setPlayId(int id){
        this.mPlayId = id;
    }
    
    class ViewHolder {
      TextView duration;
      ImageButton ibListItemMenu;
      int id;
      String path;
      ImageButton pause;
      ImageButton play;
      ImageView play_indicator;
      int position;
      TextView size;
      TextView title;
    }
}
