package com.example.refreshloadlayout;

/**
 * 刷新指示器，子类可以根据回调方法适当地调整视图显示
 * Created by jellybean on 2017/4/13.
 */

public interface RefreshIndicator {
    /**
     * 下拉中，但未到达刷新距离
     */
    void onPullDown();

    /**
     * 满足刷新条件
     */
    void onQualifiedRefreshing();

    /**
     * 开始刷新
     */
    void onStartRefreshing();

    /**
     * 结束刷新
     */
    void onEndRefreshing();
}
