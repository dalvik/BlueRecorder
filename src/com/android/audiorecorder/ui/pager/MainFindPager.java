package com.android.audiorecorder.ui.pager;

import com.android.library.R;
import com.android.library.ui.pager.BasePager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainFindPager extends BasePager {

    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	View view = inflater.inflate(R.layout.treat_list, null);
    	
        return view;
    }

    @Override
    public void reload() {
    }
    
    @Override
    protected View createView(LayoutInflater inflater, Bundle savedInstanceState) {
    	View view = inflater.inflate(R.layout.treat_list, null);
        return view;
    }

}
