package com.example.refreshloadlayout;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * 下拉刷新和上滑加载更多布局
 * Created by jellybean on 2017/4/12.
 */

public class RefreshLoadLayout extends ViewGroup {
    static final String TAG = "RefreshLoadLayout";


    private static final float DRAG_RATE = .5f;



    private View mTarget;

    private RefreshIndicator refreshIndicator;

    private OnRefreshListener mOnRefreshListener;

    private int mTouchSlop;

    private float initialY;

    /**
     * 是否处于刷新状态
     */
    private boolean mIsRefreshing;

    /**
     * 触发刷新所需的滑动距离,默认为刷新视图的高度
     */
    private int triggerDistance;


    public RefreshLoadLayout(Context context) {
        this(context,null);
    }

    public RefreshLoadLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }


    private void init(Context context) {
        int childCount = getChildCount();
        if (childCount > 1) {
            throw new IllegalStateException("Only support one child");
        }
        mTouchSlop= ViewConfiguration.get(context).getScaledTouchSlop();
        addDefaultRefreshView(context);
    }

    private void addDefaultRefreshView(Context context) {
        refreshIndicator = new DefaultRefreshIndicator(context);

        addView((View) refreshIndicator,LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
    }

    public void setOnRefreshListener(OnRefreshListener onRefreshListener){
        mOnRefreshListener=onRefreshListener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mTarget == null) {
            //find the child view
            findTarget();
        }
        if (mTarget == null) {
            return;
        }
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        if (triggerDistance==0){
            triggerDistance = ((View)refreshIndicator).getMeasuredHeight();
        }
    }

    private void findTarget() {
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view != refreshIndicator) {
                mTarget = view;
                break;
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mTarget == null) {
            findTarget();
        }
        if (mTarget == null) {
            return;
        }
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        mTarget.layout(getPaddingLeft(), getPaddingTop(), width - getPaddingRight(), height - getPaddingBottom());
        int refreshViewHeight = ((View)refreshIndicator).getMeasuredHeight();
        ((View)refreshIndicator).layout(0, -refreshViewHeight, width, 0);
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mTarget == null||isRefreshing()) {
            return true;
        }
        if (canChildScrollUp()){
            return false;
        }
        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                initialY=ev.getY();
                Log.d(TAG,"onInterceptTouchEvent downY："+initialY);
                return false;
            case MotionEvent.ACTION_MOVE:
                float y=ev.getY();
                float dy=y-initialY;
                if (Math.abs(dy)<mTouchSlop){
                    return false;
                }
                return dy > 0;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:

        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mTarget==null){
            return false;
        }
        if (mIsRefreshing){
            return true;
        }
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_MOVE:
                return handleMoveEvent(event);
            case MotionEvent.ACTION_UP:
                return handleUpEvent(event);
            case MotionEvent.ACTION_CANCEL:
                break;

        }
        return super.onTouchEvent(event);
    }

    private boolean handleUpEvent(MotionEvent event) {

        Log.d(TAG,"handleUpEvent scrollY:"+getScrollY()+"triggerDistance:"+triggerDistance);
        if (-getScrollY()>=triggerDistance){
            startRefreshing();
            Log.d(TAG,"释放时达到刷新距离，开始刷新");
        }else {
            scrollTo(0,0);
        }
        return true;
    }

    private boolean handleMoveEvent(MotionEvent event) {
        float y=event.getY();
        int dy= (int) (y-initialY);
        if (dy>0){
            int offset=(int) (dy*DRAG_RATE);
            scrollTo(0, -offset);
            Log.d(TAG,"dy:"+dy+" scrollY:"+getScrollY()+" offset:"+offset);
            if (offset<triggerDistance){
                refreshIndicator.onPullDown();
            }else {
                refreshIndicator.onQualifiedRefreshing();
            }
        }
        return true;
    }

    public void setRefreshIndicator(RefreshIndicator refreshIndicator){
        if (refreshIndicator==null){
            return;
        }
        if (!(refreshIndicator instanceof View)){
            throw new IllegalArgumentException("RefreshIndicator must be a view");
        }
        this.refreshIndicator=refreshIndicator;
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    public boolean canChildScrollUp() {
        return ViewCompat.canScrollVertically(mTarget, -1);
    }

    public boolean isRefreshing(){
        return mIsRefreshing;
    }

    public void endRefreshing(){
        if (!mIsRefreshing){
            return;
        }
        scrollTo(0,0);
        refreshIndicator.onEndRefreshing();
        mIsRefreshing=false;
    }

    public void startRefreshing(){
        if (mIsRefreshing){
            // Already on refreshing
            return;
        }
        refreshIndicator.onStartRefreshing();
        scrollTo(0,-triggerDistance);
        mIsRefreshing =true;
        if (mOnRefreshListener!=null){
            mOnRefreshListener.onRefresh();
        }
    }


    public interface OnRefreshListener{
        void onRefresh();
    }
}
