package com.github.refreshloadlayout;

/**
 * 刷新指示器，子类可以根据回调方法适当地调整视图显示
 * Created by jellybean on 2017/4/13.
 */

public interface RefreshIndicator {

    /**
     * 下拉中，但未到达刷新距离
     * @param refreshLoadLayout 刷新加载布局
     * @param scrollDistance 当前视图滑动的距离
     */
    void onPullDown(RefreshLoadLayout refreshLoadLayout , int scrollDistance);

    /**
     *  满足刷新条件
     * @param refreshLoadLayout 刷新加载布局
     * @param scrollDistance 当前视图滑动的距离
     */
    void onQualifiedRefreshing(RefreshLoadLayout refreshLoadLayout,int scrollDistance);

    /**
     * 开始刷新
     */
    void onStartRefreshing(RefreshLoadLayout refreshLoadLayout);

    /**
     * 结束刷新
     */
    void onEndRefreshing(RefreshLoadLayout refreshLoadLayout);
}
