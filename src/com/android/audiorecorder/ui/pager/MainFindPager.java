package com.android.audiorecorder.ui.pager;

import com.android.library.R;
import com.android.library.ui.pager.BasePager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainFindPager extends BasePager {

    private TextView mTextView;

    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mTextView = (TextView)inflater.from(getActivity()).inflate(R.layout.toast_view, null);
        System.out.println("onCreateView0");
        mTextView.setText("0000000000000");
        //getActivity().getActionBar().hide();
        return mTextView;
    }

    @Override
    public void reload() {
    }
    
    @Override
    protected View createView(LayoutInflater inflater, Bundle savedInstanceState) {
        return null;
    }

}
