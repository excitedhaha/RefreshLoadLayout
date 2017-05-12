package com.github.refreshloadlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
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

public class RefreshLoadLayout extends ViewGroup implements NestedScrollingParent {
    static final String TAG = "RefreshLoadLayout";

    private static final float DRAG_RATE = .5f;


    private View mTarget;

    private RefreshIndicator mRefreshIndicator;

    private LoadMoreIndicator mLoadMoreIndicator;

    private OnRefreshListener mOnRefreshListener;

    private LoadingHandler mLoadingHandler;

    private int mTouchSlop;

    private float initialY;

    private float mTotalUnconsumed;

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
     * 触发刷新所需的滑动距离,默认为刷新视图的高度
     */
    private int triggerDistance;

    private int loadIndicatorHeight;

    private OnTargetScrollDownCallback mOnTargetScrollDownCallback;

    private OnTargetScrollUpCallback mOnTargetScrollUpCallback;

    private RecyclerScrollListener recyclerScrollListener;

    private AbsListViewScrollListener absListViewScrollListener;

    private NestedScrollingParentHelper nestedScrollingParentHelper;

    public RefreshLoadLayout(Context context) {
        this(context, null);
    }

    public RefreshLoadLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseAttributeSet(context, attrs);
        init(context);
    }

    private void parseAttributeSet(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RefreshLoadLayout);
        refreshingEnabled = typedArray.getBoolean(R.styleable.RefreshLoadLayout_refreshingEnabled, true);
        loadingEnabled = typedArray.getBoolean(R.styleable.RefreshLoadLayout_loadingEnabled, false);
        triggerDistance = typedArray.getInt(R.styleable.RefreshLoadLayout_triggerDistance, 0);
        typedArray.recycle();
    }


    private void init(Context context) {
        int childCount = getChildCount();
        if (childCount > 1) {
            throw new IllegalStateException("Only support one child");
        }
        nestedScrollingParentHelper = new NestedScrollingParentHelper(this);
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
        if (mTarget != null) {
            return;
        }
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view != mRefreshIndicator && view != mLoadMoreIndicator) {
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
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();


        mTarget.layout(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom);
        if (mRefreshIndicator != null) {
            int refreshViewHeight = ((View) mRefreshIndicator).getMeasuredHeight();
            int refreshViewWidth = ((View) mRefreshIndicator).getMeasuredWidth();
            LayoutParams layoutParams = ((View) mRefreshIndicator).getLayoutParams();
            int marginTop = 0;
            int marginBottom = 0;
            if (layoutParams != null && layoutParams instanceof MarginLayoutParams) {
                marginTop = ((MarginLayoutParams) layoutParams).topMargin;
                marginBottom = ((MarginLayoutParams) layoutParams).bottomMargin;
            }
            int left = width / 2 - refreshViewWidth / 2;
            int bottom = -marginBottom;
            int top = bottom - refreshViewHeight - marginTop;
            int right = left + refreshViewWidth;

            ((View) mRefreshIndicator).layout(left, top, right, bottom);
        }
        if (mLoadMoreIndicator != null) {
            loadIndicatorHeight = ((View) mLoadMoreIndicator).getMeasuredHeight();
            int loadIndicatorWidth = ((View) mLoadMoreIndicator).getMeasuredWidth();
            LayoutParams layoutParams = ((View) mLoadMoreIndicator).getLayoutParams();
            int marginTop = 0;
            int marginBottom = 0;
            if (layoutParams != null && layoutParams instanceof MarginLayoutParams) {
                marginTop = ((MarginLayoutParams) layoutParams).topMargin;
                marginBottom = ((MarginLayoutParams) layoutParams).bottomMargin;
            }
            int left = width / 2 - loadIndicatorWidth / 2;
            int top = height + marginTop;
            int right = left + loadIndicatorWidth;
            int bottom = top + loadIndicatorHeight + marginBottom;
            ((View) mLoadMoreIndicator).layout(left, top, right, bottom);
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
                    scrollBack();
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
            if (refreshingEnabled) {
                scrollDown(dy);
                return true;
            }
        } else {
            //手指向上移动，上滑
            return tryLoadingMore();

        }
        return false;
    }

    /**
     * 将head向下滑动到指定距离
     *
     * @param y 目标位置
     */
    private void scrollDown(int y) {
        int offset = (int) (y * DRAG_RATE);
        scrollTo(0, -offset);
        Log.d(TAG, "dy:" + y + " scrollY:" + getScrollY() + " offset:" + offset);
        if (offset < triggerDistance) {
            mRefreshIndicator.onPullDown(this, offset);
        } else {
            mRefreshIndicator.onQualifiedRefreshing(this, offset);
        }
    }

    /**
     * 复位
     */
    private void scrollBack() {
        scrollTo(0, 0);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mRefreshIndicator != null && mRefreshing) {
            mRefreshIndicator.onEndRefreshing(this);
        }
        if (mLoadMoreIndicator != null && mLoadingMore) {
            mLoadMoreIndicator.onEndLoading(this);
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        if ((android.os.Build.VERSION.SDK_INT < 21 && mTarget instanceof AbsListView)
                || (mTarget != null && !ViewCompat.isNestedScrollingEnabled(mTarget))) {
            // Nope.
        } else {
            super.requestDisallowInterceptTouchEvent(b);
        }
    }

    // NestedScrollingParent

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return !(mRefreshing || mLoadingMore) && nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        nestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
    }

    @Override
    public void onStopNestedScroll(View child) {
        nestedScrollingParentHelper.onStopNestedScroll(child);
        super.onStopNestedScroll(child);
        Log.d(TAG, "onStopNestedScroll");
        if (mTotalUnconsumed < 0) {
            int unconsumed = (int) Math.abs(mTotalUnconsumed);
            if (unconsumed >= triggerDistance) {
                startRefreshing();
            } else {
                scrollBack();
            }
        }
        mTotalUnconsumed = 0;
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        Log.d(TAG, "onNestedScroll dxConsumed:" + dxConsumed + " dyConsumed:" + dyConsumed + " dxUnconsumed:" + dxUnconsumed + " dyUnconsumed:" + dyUnconsumed);
        super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
        if (refreshingEnabled && dyUnconsumed < 0) {
            mTotalUnconsumed += dyUnconsumed;
            int unconsumed = (int) Math.abs(mTotalUnconsumed);
            scrollDown(unconsumed);
        }
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        Log.d(TAG, "onNestedPreScroll dx:" + dx + " dy:" + dy + " consumedX:" + consumed[0] + " consumedY:" + consumed[1]);
        super.onNestedPreScroll(target, dx, dy, consumed);
        if (dy > 0 && mTotalUnconsumed < 0) {
            int unconsumed = (int) Math.abs(mTotalUnconsumed);
            if (dy > unconsumed) {
                consumed[1] = unconsumed;
                mTotalUnconsumed = 0;
                scrollBack();
            } else {
                consumed[1] = dy;
                mTotalUnconsumed += dy;
                scrollDown(unconsumed - dy);
            }
        }
    }

    @Override
    public int getNestedScrollAxes() {
        return nestedScrollingParentHelper.getNestedScrollAxes();
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

    public void setTriggerDistance(int triggerDistance) {
        this.triggerDistance = triggerDistance;
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


    public void setRefreshIndicator(RefreshIndicator refreshIndicator) {
        if (refreshIndicator == null || refreshIndicator == mRefreshIndicator) {
            return;
        }
        if (!(refreshIndicator instanceof View)) {
            throw new IllegalArgumentException("RefreshIndicator must be a view");
        }
        if (mRefreshIndicator != null) {
            removeView((View) mRefreshIndicator);
        }
        addView((View) refreshIndicator, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        mRefreshIndicator = refreshIndicator;
    }

    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        mOnRefreshListener = onRefreshListener;
    }

    public void setLoadingIndicator(LoadMoreIndicator loadingIndicator) {
        if (loadingIndicator == null || loadingIndicator == mLoadMoreIndicator) {
            return;
        }
        if (!(loadingIndicator instanceof View)) {
            throw new IllegalArgumentException("LoadingIndicator must be a view");
        }
        if (mLoadMoreIndicator != null) {
            removeView((View) mLoadMoreIndicator);
        }
        addView((View) loadingIndicator, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        mLoadMoreIndicator = loadingIndicator;
    }

    /**
     * 设置加载处理器
     *
     * @param loadingHandler
     */
    public void setLoadingHandler(LoadingHandler loadingHandler) {
        this.mLoadingHandler = loadingHandler;
    }

    public RefreshIndicator getRefreshIndicator() {
        return mRefreshIndicator;
    }

    public LoadMoreIndicator getLoadMoreIndicator() {
        return mLoadMoreIndicator;
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
        scrollBack();
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
        }
        return false;
    }


    private boolean canLoadingMore() {
        if (!loadingEnabled || mLoadingMore || canChildScrollDown()) {
            return false;
        }
        if (mLoadingHandler == null) {
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
            scrollBack();
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
            if (absListViewScrollListener != null) {
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
            if (recyclerScrollListener != null) {
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
            if (recyclerScrollListener == null) {
                recyclerScrollListener = new RecyclerScrollListener();
            }
            recyclerView.addOnScrollListener(recyclerScrollListener);
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
                if (absListViewScrollListener == null) {
                    absListViewScrollListener = new AbsListViewScrollListener();
                }
                absListViewScrollListener.setOriginalListener(onScrollListener);
                absListView.setOnScrollListener(absListViewScrollListener);
            } catch (Exception e) {
                Log.e(TAG, "反射获取AbsListView#OnScrollListener异常", e);
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

    private class RecyclerScrollListener extends RecyclerView.OnScrollListener {
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
            if (swallowNextScroll) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    swallowNextScroll = false;
                }
                return;
            }
            if ((newState == RecyclerView.SCROLL_STATE_IDLE || newState == RecyclerView.SCROLL_STATE_SETTLING)) {
                tryLoadingMore();
            }
        }
    }

    private class AbsListViewScrollListener implements AbsListView.OnScrollListener {
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
            if (swallowNextScroll) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    swallowNextScroll = false;
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

}
