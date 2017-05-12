# RefreshLoadLayout
A library for Android , swipe to refresh or load more  
[![License](https://img.shields.io/badge/license-Apache%202-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Download](https://api.bintray.com/packages/jellybean/RefreshLoadLayout/com.github.refreshloadlayout/images/download.svg) ](https://bintray.com/jellybean/RefreshLoadLayout/com.github.refreshloadlayout/_latestVersion)

#使用介绍
- 添加 gralde 依赖  
`compile 'com.github.refreshloadlayout:refreshloadlayout:latest.release'`  
`compile 'com.github.refreshloadlayout:refreshloadlayout:0.0.1' `  指定具体版本
如果收到支持库不统一的警告，可修改为  `compile ('com.github.refreshloadlayout:refreshloadlayout:latest.release'){exclude group: 'com.android.support'}`

- 在布局中使用

        <com.github.refreshloadlayout.RefreshLoadLayout
        android:id="@+id/refreshLayout"
        app:loadingEnabled="true"
        app:refreshingEnabled="true"
        android:layout_width="match_parent"
        android:layout_height="match_parent">
        <ListView
            android:id="@+id/listView"
            android:background="#fff"
            android:layout_width="match_parent"
            android:layout_height="match_parent"/>
        </com.github.refreshloadlayout.RefreshLoadLayout>
- 下拉刷新


        refreshLoadLayout.setOnRefreshListener(new RefreshLoadLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshLoadLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshLoadLayout.endRefreshing();
                    }
                }, 3000);
            }
        });
- 上滑加载

        refreshLoadLayout.setLoadingHandler(new RefreshLoadLayout.LoadingHandler() {
            @Override
            public boolean canLoadMore() {
                return listView.getCount()<26;
            }

            @Override
            public void onLoading() {
                refreshLoadLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ArrayAdapter<String> arrayAdapter= (ArrayAdapter<String>) listView.getAdapter();
                        arrayAdapter.add("a new data");
                        arrayAdapter.add("a new data");
                        refreshLoadLayout.endLoading();
                    }
                }, 2000);

            }
        });
其中 canLoadMore() 方法返回当前是否可以进行加载，即是否还有更多内容。

- 启用/关闭功能
默认开启下拉刷新关闭上滑加载更多，可以在xml中指定或者在代码中修改。
`refreshLoadLayout.setRefreshingEnabled(false);`
 `refreshLoadLayout.setLoadingEnabled(true);`

- UI定制
目前共有两种指示器显示效果，默认实现的是仿新浪微博的，还有一种是Material 效果。
首先可以通过 `refreshLoadLayout.getRefreshIndicator();` 和 `refreshLoadLayout.getLoadMoreIndicator();` 对当前的指示器进行调整。
也支持扩展 RefreshIndicator 和 LoadMoreIndicator 来打造自己的指示器。
比如设置 Material 效果的指示器：
        MaterialIndicator materialIndicator=new MaterialIndicator(this);
        materialIndicator.getProgressView().setColorSchemeColors(Color.RED,Color.YELLOW);
        refreshLoadLayout.setRefreshIndicator(materialIndicator);

## 有问题欢迎提issue，我会尽快处理。如果对你有帮助，还请右上角star :)