package com.example.refreshloadlayout;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
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

    private RefreshIndicator mRefreshIndicator;

    private LoadMoreIndicator mLoadMoreIndicator;

    private OnRefreshListener mOnRefreshListener;

    private LoadingHandler mLoadingHandler;

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
    private boolean refreshingEnabled;

    /**
     * 是否启用加载更多
     */
    private boolean loadingEnabled;

    /**
     * 是否展示没有更多内容的视图
     */
    private boolean showNoMore;

    /**
     * 触发刷新所需的滑动距离,默认为刷新视图的高度
     */
    private int triggerDistance;

    private int loadIndicatorHeight;

    private OnTargetScrollDownCallback mOnTargetScrollDownCallback;

    private OnTargetScrollUpCallback mOnTargetScrollUpCallback;

    private RecyclerScrollListener recyclerScrollListener;

    private AbsListViewScrollListener absListViewScrollListener;


    public RefreshLoadLayout(Context context) {
        this(context, null);
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
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        if (refreshingEnabled) {
            addDefaultRefreshView();
        }
        if (loadingEnabled) {
            addDefaultLoadingIndicator();
        }
    }

    private void addDefaultLoadingIndicator() {
        mLoadMoreIndicator = new DefaultLoadingMoreIndicator(getContext());
        addView((View) mLoadMoreIndicator, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    private void addDefaultRefreshView() {
        mRefreshIndicator = new DefaultRefreshIndicator(getContext());
        addView((View) mRefreshIndicator, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
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
        if (triggerDistance == 0 && mRefreshIndicator != null) {
            triggerDistance = ((View) mRefreshIndicator).getMeasuredHeight();
        }
    }

    private void findTarget() {
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view != mRefreshIndicator) {
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
        if (mRefreshIndicator != null) {
            int refreshViewHeight = ((View) mRefreshIndicator).getMeasuredHeight();
            ((View) mRefreshIndicator).layout(0, -refreshViewHeight, width, 0);
        }
        if (mLoadMoreIndicator != null) {
            loadIndicatorHeight = ((View) mLoadMoreIndicator).getMeasuredHeight();
            ((View) mLoadMoreIndicator).layout(0, height, width, height + loadIndicatorHeight);
        }
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mTarget == null || mRefreshing || mLoadingMore) {
            return false;
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialY = ev.getY();
                Log.d(TAG, "onInterceptTouchEvent downY：" + initialY);
                return false;
            case MotionEvent.ACTION_MOVE:
                float y = ev.getY();
                float dy = y - initialY;
                if (Math.abs(dy) < mTouchSlop) {
                    return false;
                }
                if (dy > 0) {
                    //手指向下移动，下拉
                    return refreshingEnabled && !canChildScrollUp();
                } else {
                    //手指向上移动，上滑
                    return canLoadingMore();
                }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:

        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mTarget == null) {
            return false;
        }
        if (mRefreshing || mLoadingMore) {
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
        if (mLoadingMore || mRefreshing) {
            return false;
        }
        int dy = (int) (event.getY() - initialY);
        if (dy > 0 && refreshingEnabled) {
            int scrollY = getScrollY();
            if (scrollY < 0) {
                if (-scrollY >= triggerDistance) {
                    startRefreshing();
                } else {
                    scrollTo(0, 0);
                }
                return true;
            }
        }

        return tryLoadingMore();
    }

    private boolean handleMoveEvent(MotionEvent event) {
        if (mLoadingMore || mRefreshing) {
            return false;
        }
        float y = event.getY();
        int dy = (int) (y - initialY);
        if (dy > 0) {
            //手指向下移动，下拉
            if (!refreshingEnabled) {
                return false;
            }
            int offset = (int) (dy * DRAG_RATE);
            scrollTo(0, -offset);
            Log.d(TAG, "dy:" + dy + " scrollY:" + getScrollY() + " offset:" + offset);
            if (offset < triggerDistance) {
                mRefreshIndicator.onPullDown(this);
            } else {
                mRefreshIndicator.onQualifiedRefreshing(this);
            }
        } else {
            //手指向上移动，上滑
            return tryLoadingMore();

        }
        return false;
    }


    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    public boolean canChildScrollUp() {
        if (mOnTargetScrollUpCallback != null) {
            return mOnTargetScrollUpCallback.canChildScrollUp(this, mTarget);
        }
        return ViewCompat.canScrollVertically(mTarget, -1);
    }

    public boolean canChildScrollDown() {
        if (mOnTargetScrollDownCallback != null) {
            return mOnTargetScrollDownCallback.canChildScrollDown(this, mTarget);
        }
        return ViewCompat.canScrollVertically(mTarget, 1);
    }

    public void setOnTargetScrollDownCallback(OnTargetScrollDownCallback callback) {
        mOnTargetScrollDownCallback = callback;
    }

    public void setOnTargetScrollUpCallback(OnTargetScrollUpCallback callback) {
        mOnTargetScrollUpCallback = callback;
    }

    public boolean isRefreshingEnabled() {
        return refreshingEnabled;
    }

    public void setRefreshingEnabled(boolean refreshingEnabled) {
        this.refreshingEnabled = refreshingEnabled;
        if (mRefreshIndicator == null) {
            addDefaultRefreshView();
        }
    }

    public boolean isLoadingEnabled() {
        return loadingEnabled;
    }

    public void setLoadingEnabled(boolean loadingEnabled) {
        this.loadingEnabled = loadingEnabled;
        if (mLoadMoreIndicator == null) {
            addDefaultLoadingIndicator();
        }
    }

    public boolean isShowNoMore() {
        return showNoMore;
    }

    public void setShowNoMore(boolean showNoMore) {
        this.showNoMore = showNoMore;
    }

    public void setRefreshIndicator(RefreshIndicator refreshIndicator) {
        if (refreshIndicator == null) {
            return;
        }
        if (!(refreshIndicator instanceof View)) {
            throw new IllegalArgumentException("RefreshIndicator must be a view");
        }
        if (mRefreshIndicator != null) {
            removeView((View) mRefreshIndicator);
        }
        addView((View) refreshIndicator, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        mRefreshIndicator = refreshIndicator;
    }

    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        mOnRefreshListener = onRefreshListener;
    }

    /**
     * 设置加载处理器
     *
     * @param loadingHandler
     */
    public void setLoadingHandler(LoadingHandler loadingHandler) {
        this.mLoadingHandler = loadingHandler;
    }

    public boolean isLoadingMore() {
        return mLoadingMore;
    }

    public boolean isRefreshing() {
        return mRefreshing;
    }

    public void endRefreshing() {
        if (!mRefreshing) {
            return;
        }
        scrollTo(0, 0);
        mRefreshIndicator.onEndRefreshing(this);
        mRefreshing = false;
    }

    public void startRefreshing() {
        if (mRefreshing) {
            // Already on refreshing
            return;
        }
        refreshingEnabled = true;
        if (mRefreshIndicator != null) {
            mRefreshIndicator.onStartRefreshing(this);
        }
        scrollTo(0, -triggerDistance);
        mRefreshing = true;
        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh();
        }
    }

    private boolean tryLoadingMore() {
        if (canLoadingMore()) {
            startLoading();
            return true;
        }else if (showNoMore){
            showNoMoreView();
        }
        return false;
    }

    /**
     * 展示没有更多内容的视图
     */
    private void showNoMoreView() {
        if (mLoadMoreIndicator != null) {
            mLoadMoreIndicator.onNoMoreContent(this);
            scrollTo(0, loadIndicatorHeight);
        }
    }

    private boolean canLoadingMore() {
        if (!loadingEnabled || mLoadingMore || canChildScrollDown()) {
            return false;
        }
        if (mLoadingHandler==null){
            throw new IllegalStateException("You must set a LoadingHandler if want to implement the Load-More feature!");
        }
        return mLoadingHandler.canLoadMore();
    }

    /**
     * 开始加载更多
     */
    public void startLoading() {
        if (mLoadingMore) {
            return;
        }

        mLoadingMore = true;

        if (mLoadingHandler != null) {
            mLoadingHandler.onLoading();
        }
        if (mLoadMoreIndicator != null) {
            mLoadMoreIndicator.onStartLoading(this);
            scrollTo(0, loadIndicatorHeight);
        }
    }

    /**
     * 结束加载更多
     */
    public void endLoading() {
        if (mLoadingMore) {
            mLoadingMore = false;
            scrollTo(0, 0);
            scrollChildToNextItem();
            if (mLoadMoreIndicator != null) {
                mLoadMoreIndicator.onEndLoading(this);
            }
        }
    }

    /**
     * 将子视图滑动到下一个单元的位置
     */
    private void scrollChildToNextItem() {
        if (!canChildScrollDown()) {
            return;
        }
        if (mTarget instanceof AbsListView) {
            AbsListView absListView = (AbsListView) mTarget;
            int currentPosition = absListView.getLastVisiblePosition();
            if (absListViewScrollListener!=null){
                absListViewScrollListener.setSwallowNextScroll(true);
            }
            absListView.smoothScrollToPosition(currentPosition + 1);
        } else if (mTarget instanceof RecyclerView) {
            RecyclerView.LayoutManager layoutManager = ((RecyclerView) mTarget).getLayoutManager();
            int lastPosition = 1;
            if (layoutManager instanceof LinearLayoutManager) {
                lastPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition() + 1;
            } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                int[] positions = ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(null);
                for (int position : positions) {
                    if (position > lastPosition) {
                        lastPosition = position;
                    }
                }
            }
            if (recyclerScrollListener!=null){
                recyclerScrollListener.setSwallowNextScroll(true);
            }
            layoutManager.scrollToPosition(lastPosition + 1);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setScrollListener();
    }

    /**
     * 设置滑动监听，主要用于检测fling停止后是否可以向下滑动
     */
    private void setScrollListener() {
        findTarget();
        if (mTarget != null) {
            if (mTarget instanceof AbsListView) {
                setAbsListViewOnScrollListener((AbsListView) mTarget);
            } else if (mTarget instanceof RecyclerView) {
                setRecyclerViewOnScrollListener((RecyclerView) mTarget);
            }
        }
    }

    private void setRecyclerViewOnScrollListener(RecyclerView recyclerView) {
        if (recyclerView != null) {
            if (recyclerScrollListener==null){
                recyclerScrollListener=new RecyclerScrollListener();
            }
            recyclerView.addOnScrollListener(recyclerScrollListener);
        }
    }

    private class RecyclerScrollListener extends RecyclerView.OnScrollListener{
        /**
         * 是否忽略下此次滑动事件,避免在刚结束加载并滑动一个单元的距离后
         * {@link #scrollChildToNextItem()}连续触发加载
         */
        private boolean swallowNextScroll;

        void setSwallowNextScroll(boolean swallowNextScroll) {
            this.swallowNextScroll = swallowNextScroll;
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (swallowNextScroll){
                if (newState == RecyclerView.SCROLL_STATE_IDLE){
                    swallowNextScroll=false;
                }
                return;
            }
            if ((newState == RecyclerView.SCROLL_STATE_IDLE || newState == RecyclerView.SCROLL_STATE_SETTLING)) {
                tryLoadingMore();
            }
        }
    }


    private void setAbsListViewOnScrollListener(AbsListView absListView) {
        if (absListView != null) {
            try {
                // 通过反射获取滚动监听器，设置自己的滚动监听器后通知之前已有的监听
                Field field = AbsListView.class.getDeclaredField("mOnScrollListener");
                field.setAccessible(true);
                // 开发者自定义的滚动监听器
                AbsListView.OnScrollListener onScrollListener = (AbsListView.OnScrollListener) field.get(absListView);
                if (absListViewScrollListener==null){
                    absListViewScrollListener=new AbsListViewScrollListener();
                }
                absListViewScrollListener.setOriginalListener(onScrollListener);
                absListView.setOnScrollListener(absListViewScrollListener);
            } catch (Exception e) {
                Log.e(TAG, "反射获取AbsListView#OnScrollListener异常", e);
            }
        }
    }

    private class AbsListViewScrollListener implements AbsListView.OnScrollListener{
        private AbsListView.OnScrollListener originalListener;

        /**
         * 是否忽略下此次滑动事件,避免在刚结束加载并滑动一个单元的距离后
         * {@link #scrollChildToNextItem()}连续触发加载
         */
        private boolean swallowNextScroll;

        void setSwallowNextScroll(boolean swallowNextScroll) {
            this.swallowNextScroll = swallowNextScroll;
        }

        void setOriginalListener(AbsListView.OnScrollListener originalListener) {
            this.originalListener = originalListener;
        }

        @Override
        public void onScrollStateChanged(AbsListView absListView, int scrollState) {
            Log.d(TAG, scrollState + "");
            if (swallowNextScroll){
                if (scrollState == SCROLL_STATE_IDLE){
                    swallowNextScroll=false;
                }
                return;
            }
            if ((scrollState == SCROLL_STATE_IDLE || scrollState == SCROLL_STATE_FLING)) {
                tryLoadingMore();
            }

            if (originalListener != null) {
                originalListener.onScrollStateChanged(absListView, scrollState);
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (originalListener != null) {
                originalListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
            }
        }
    }

    /**
     * 下拉刷新监听
     */
    public interface OnRefreshListener {
        void onRefresh();
    }

    /**
     * 上滑加载处理器
     */
    public interface LoadingHandler {
        /**
         * 检查是否可以加载更多
         */
        boolean canLoadMore();

        /**
         * 加载回调
         */
        void onLoading();
    }


    public interface OnTargetScrollUpCallback {
        /**
         * 目标子视图是否能够向上滚动
         *
         * @param parent
         * @param target
         * @return
         */
        boolean canChildScrollUp(RefreshLoadLayout parent, @Nullable View target);
    }

    public interface OnTargetScrollDownCallback {
        /**
         * 目标子视图是否能够向下滚动
         *
         * @param parent
         * @param target
         * @return
         */
        boolean canChildScrollDown(RefreshLoadLayout parent, @Nullable View target);
    }

}
