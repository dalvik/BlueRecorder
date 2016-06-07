package com.android.audiorecorder.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.android.audiorecorder.R;
import com.android.audiorecorder.ui.pager.MainCenterPager;
import com.android.audiorecorder.ui.pager.MainFindPager;
import com.android.audiorecorder.ui.pager.MainMessagePager;
import com.android.audiorecorder.ui.pager.MainRecordPager;
import com.android.library.ui.base.BaseCommonActivity;
import com.viewpagerindicator.IconPagerAdapter;
import com.viewpagerindicator.SelectableViewAdapter;
import com.viewpagerindicator.TabPageIndicator;

public class MainFrameActivity extends BaseCommonActivity {

    private static final int POSITION_MAIN = 0;
    private static final int POSITION_MSG = 1;
    private static final int POSITION_FIND = 2;
    public static final int POSITION_CENTER = 3;
    
    private static Fragment[] pagers = new Fragment[4];
    
    private ViewPager viewPager;
    private TabPageIndicator indicator;
    private MainAdapter adapter;
    
    private int mTitle[] = {R.string.main_tab_main, R.string.main_tab_msg, R.string.main_tab_find, R.string.main_tab_center};
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main_viewpager);
        viewPager = (ViewPager) findViewById(R.id.lib_id_viewpager_viewpager);
        indicator = (TabPageIndicator) findViewById(R.id.indicator);
        adapter = new MainAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        indicator.setViewPager(viewPager);
        indicator.setSmoothScrollingEnabled(false);
        indicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                setSelectedTab(position);
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }
    
    private void setSelectedTab(int position) {
        switch (position) {
            case POSITION_MAIN:
                //titleTv.setText(R.string.main_tab_main);
                break;
            case POSITION_MSG:
                //titleTv.setText(R.string.main_tab_msg);
                break;
            case POSITION_FIND:
                //titleTv.setText(R.string.main_tab_find);
                break;
            case POSITION_CENTER:
                //titleTv.setText(R.string.main_tab_center);
                break;
        }
        indicator.setCurrentItem(position);
    }
    
    private static class MainAdapter extends FragmentPagerAdapter implements SelectableViewAdapter, IconPagerAdapter {

        private int[] ICONS = new int[]{
                R.drawable.lib_drawable_viewpagerindicator_tab_main_selector,
                R.drawable.lib_drawable_viewpagerindicator_tab_msg_selector,
                R.drawable.lib_drawable_viewpagerindicator_tab_find_selector,
                R.drawable.lib_drawable_viewpagerindicator_tab_center_selector
                };

        public MainAdapter(FragmentManager fm) {
            super(fm);
            pagers[POSITION_MAIN] = new MainRecordPager();
            pagers[POSITION_MSG] = new MainMessagePager();
            pagers[POSITION_FIND] = new MainFindPager();
            pagers[POSITION_CENTER] = new MainCenterPager();
        }

        @Override
        public int getIconResId(int index) {
            return ICONS[index];
        }

        @Override
        public int getCount() {
            return pagers.length;
        }

        @Override
        public Fragment getItem(int position) {
            return pagers[position];
        }

        @Override
        public TabPageIndicator.SelectableView getView(int position) {
            return null;
        }
    }
}
