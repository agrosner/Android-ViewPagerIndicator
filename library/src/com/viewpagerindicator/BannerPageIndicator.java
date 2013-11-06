package com.viewpagerindicator;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;

/**
 * Enables for custom views to move inline with a viewpager, providing a parallax effect or a fluid horizontal scroll
 * effect when a viewpager is moving.
 * Date: 10/18/13
 * Contributors: Andrew Grosner
 */
public class BannerPageIndicator extends HorizontalScrollView implements PageIndicator{

    private ViewPager mPager;
    private ViewPager.OnPageChangeListener mListener;

    private int mSelectedTabIndex;
    private int scrollOrigin = 0;
    private double bannerWidth = 0, screenWidth;
    private boolean isTouchesEnabled = true;

    /**
     * The view that is placed inside the horizontalscrollview
     */
    private BannerView mBanner;
    private AttributeSet mAttrs;


    public BannerPageIndicator(Context context) {
        this(context, null);
    }

    public BannerPageIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        setHorizontalScrollBarEnabled(false);
        mAttrs = attrs;
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return !isTouchesEnabled;
            }
        });
    }

    /**
     * Disallow user interaction on this indicator, use this to prevent unwanted touches when implementing parallax
     */
    public void disableTouches(){
        isTouchesEnabled = false;
    }

    /**
     * Allow user interaction after calling disableTouches()
     */
    public void enableTouches(){
        isTouchesEnabled = true;
    }

    /**
     * Allows for a custom view to be shown (only one at a time)
     * @param bannerView
     */
    public void showCustomBannerView(BannerView bannerView){
        if(mBanner!=null){
            scrollTo(0,0);
            removeView(mBanner);
            mBanner = null;
        }

        mBanner = bannerView;

        //wait for view to be measured in order to get the bannerwidth to acheive accurate scrolling
        mBanner.post(new Runnable() {
            @Override
            public void run() {
                if(mBanner instanceof BannerView.IBannerView){
                    bannerWidth = ((BannerView.IBannerView) mBanner).getBannerWidth();
                } else bannerWidth = mBanner.getWidth();
            }
        });
        screenWidth = bannerView.getContext().getResources().getDisplayMetrics().widthPixels;
        addView(mBanner);
    }

    @Override
    public void setViewPager(ViewPager view) {
        if(mPager!=view){
            if(mPager!=null){
                mPager.setOnPageChangeListener(null);
            }

            if(mBanner==null){
                mBanner = new BannerView(getContext(), mAttrs, R.attr.vpiBannerViewStyle);

                addView(mBanner);
            }

            final PagerAdapter adapter = view.getAdapter();
            if(adapter==null){
                throw new IllegalStateException("View pager does not have an adapter instance.");
            } else if(adapter.getCount()<=0){
                throw new IllegalStateException("Adapter needs to have at least one element");
            }

            mPager = view;
            mPager.setOnPageChangeListener(this);
            notifyDataSetChanged();
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        final boolean lockedExpanded = widthMode == View.MeasureSpec.EXACTLY;
        setFillViewport(lockedExpanded);

        final int oldWidth = getMeasuredWidth();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int newWidth = getMeasuredWidth();

        if (lockedExpanded && oldWidth != newWidth) {
            // Recenter the tab display if we're at a new (scrollable) size.
            setCurrentItem(mSelectedTabIndex);
        }
    }


    @Override
    public void setViewPager(ViewPager view, int initialPosition) {
        setViewPager(view);
        setCurrentItem(initialPosition);
    }

    @Override
    public void setCurrentItem(int item) {
        if(mPager==null){
            throw new IllegalStateException("Viewpager has not been bound");
        }

        mSelectedTabIndex = item;
        mPager.setCurrentItem(item);
    }

    @Override
    public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        mListener = listener;
    }

    @Override
    public void notifyDataSetChanged() {
        requestLayout();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if(mListener!=null){
            mListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
        }

        //we scroll based on a fraction at a time of the count of items in the viewpager's adapter
        double scrollRate = (bannerWidth-screenWidth)/((double)mPager.getAdapter().getCount());
        smoothScrollTo((int) (scrollRate * (position + positionOffset)), getBottom());
    }

    @Override
    public void onPageSelected(int i) {
        setCurrentItem(i);
        if(mListener!=null){
            mListener.onPageSelected(i);
        }
    }

    @Override
    public void onPageScrollStateChanged(int i) {
        if(mListener!=null){
            mListener.onPageScrollStateChanged(i);
        }
    }
}