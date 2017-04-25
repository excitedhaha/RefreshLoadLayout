package com.example.refreshloadlayout;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;

import java.lang.reflect.Field;

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

    private OnLoadingListener mOnLoadingListener;

    private int mTouchSlop;

    private float initialY;

    /**
     * 是否处于刷新状态
     */
    private boolean mRefreshing;

    /**
     * 是否处于加载更多状态
     */
    private boolean mLoadingMore;

    /**
     * 是否启用下拉刷新
     */
    private boolean isRefreshingEnabled;

    /**
     * 是否启用加载更多
     */
    private boolean isLoadingEnabled;

    /**
     * 触发刷新所需的滑动距离,默认为刷新视图的高度
     */
    private int triggerDistance;

    private OnTargetScrollDownCallback mOnTargetScrollDownCallback;

    private OnTargetScrollUpCallback mOnTargetScrollUpCallback;


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
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mTarget!=null){
            if (mTarget instanceof AbsListView){
                setAbsListViewOnScrollListener((AbsListView) mTarget);
            }else if (mTarget instanceof RecyclerView){
                setRecyclerViewOnScrollListener((RecyclerView) mTarget);
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mTarget == null||mRefreshing||mLoadingMore) {
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
                if (dy>0){
                    //手指向下移动，下拉
                    return !canChildScrollUp();
                }else {
                    //手指向上移动，上滑
                    if (canChildScrollDown()){
                        return false;
                    }else {
                        //直接拦截并开始加载
                        startLoading();
                        return true;
                    }
                }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:

        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mTarget==null){
            return false;
        }
        if (mRefreshing||mLoadingMore){
            return false;
        }
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_MOVE:
                return handleMoveEvent(event);
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return handleUpEvent(event);

        }
        return false;
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
            //手指向下移动，下拉
            int offset=(int) (dy*DRAG_RATE);
            scrollTo(0, -offset);
            Log.d(TAG,"dy:"+dy+" scrollY:"+getScrollY()+" offset:"+offset);
            if (offset<triggerDistance){
                refreshIndicator.onPullDown(this);
            }else {
                refreshIndicator.onQualifiedRefreshing(this);
            }
        }else {
            //手指向上移动，上滑
            if (mLoadingMore){
                return true;
            }
        }
        return false;
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
        if (mOnTargetScrollUpCallback!=null){
            return mOnTargetScrollUpCallback.canChildScrollUp(this,mTarget);
        }
        return ViewCompat.canScrollVertically(mTarget, -1);
    }

    public boolean canChildScrollDown(){
        if (mOnTargetScrollDownCallback!=null){
            return mOnTargetScrollDownCallback.canChildScrollDown(this,mTarget);
        }
        return ViewCompat.canScrollVertically(mTarget,1);
    }

    public void setOnTargetScrollDownCallback(OnTargetScrollDownCallback callback){
        mOnTargetScrollDownCallback=callback;
    }

    public void setOnTargetScrollUpCallback(OnTargetScrollUpCallback callback){
        mOnTargetScrollUpCallback = callback;
    }

    public boolean isRefreshing(){
        return mRefreshing;
    }

    public void endRefreshing(){
        if (!mRefreshing){
            return;
        }
        scrollTo(0,0);
        refreshIndicator.onEndRefreshing(this);
        mRefreshing =false;
    }

    public void startRefreshing(){
        if (mRefreshing){
            // Already on refreshing
            return;
        }
        refreshIndicator.onStartRefreshing(this);
        scrollTo(0,-triggerDistance);
        mRefreshing =true;
        if (mOnRefreshListener!=null){
            mOnRefreshListener.onRefresh();
        }
    }

    private boolean canLoadingMore(){
        if (mLoadingMore||canChildScrollDown()){
            return false;
        }
        return true;
    }

    /**
     * 开始加载更多
     */
    public void startLoading(){

    }



    private void setRecyclerViewOnScrollListener(RecyclerView recyclerView) {
        if (recyclerView != null) {
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    if ((newState == RecyclerView.SCROLL_STATE_IDLE || newState == RecyclerView.SCROLL_STATE_SETTLING)&& canLoadingMore()) {
                        startLoading();
                    }
                }
            });
        }
    }



    private void setAbsListViewOnScrollListener(AbsListView absListView) {
        if (absListView != null) {
            try {
                // 通过反射获取开发者自定义的滚动监听器，并将其替换成自己的滚动监听器，触发滚动时也要通知开发者自定义的滚动监听器（非侵入式，不让开发者继承特定的控件）
                // mAbsListView.getClass().getDeclaredField("mOnScrollListener")获取不到mOnScrollListener，必须通过AbsListView.class.getDeclaredField("mOnScrollListener")获取
                Field field = AbsListView.class.getDeclaredField("mOnScrollListener");
                field.setAccessible(true);
                // 开发者自定义的滚动监听器
                final AbsListView.OnScrollListener onScrollListener = (AbsListView.OnScrollListener) field.get(absListView);
                absListView.setOnScrollListener(new AbsListView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                        if ((scrollState == SCROLL_STATE_IDLE || scrollState == SCROLL_STATE_FLING)&&canLoadingMore()) {
                            startLoading();
                        }

                        if (onScrollListener != null) {
                            onScrollListener.onScrollStateChanged(absListView, scrollState);
                        }
                    }

                    @Override
                    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        if (onScrollListener != null) {
                            onScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG,"反射获取AbsListView#OnScrollListener异常",e);
            }
        }
    }


    public interface OnRefreshListener{
        void onRefresh();
    }

    public interface OnLoadingListener{
        void OnLoading();
    }



    public interface OnTargetScrollUpCallback{
        /**
         * 目标子视图是否能够向上滚动
         * @param parent
         * @param target
         * @return
         */
        boolean canChildScrollUp(RefreshLoadLayout parent, @Nullable View target);
    }

    public interface OnTargetScrollDownCallback{
        /**
         * 目标子视图是否能够向下滚动
         * @param parent
         * @param target
         * @return
         */
        boolean canChildScrollDown(RefreshLoadLayout parent, @Nullable View target);
    }

}
