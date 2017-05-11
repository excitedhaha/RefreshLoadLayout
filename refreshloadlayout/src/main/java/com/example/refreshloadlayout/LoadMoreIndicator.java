package com.example.refreshloadlayout;

/**
 * 加载更多指示器
 * Created by jellybean on 2017/4/21.
 */

public interface LoadMoreIndicator {
    /**
     * 开始显示加载更多视图
     */
    void onStartLoading(RefreshLoadLayout refreshLoadLayout);

    /**
     * 加载结束
     */
    void onEndLoading(RefreshLoadLayout refreshLoadLayout);

}
