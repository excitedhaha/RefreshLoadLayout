package com.github.refreshloadlayout;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * 默认的加载更多指示器
 * Created by jellybean on 2017/4/21.
 */

public class DefaultLoadingMoreIndicator extends LinearLayout implements LoadMoreIndicator{
    private ProgressBar progressBar;
    private TextView textView;

    public DefaultLoadingMoreIndicator(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.default_load_indicator,this);
        progressBar= (ProgressBar) findViewById(R.id.progress_bar);
        textView= (TextView) findViewById(R.id.textView);
    }

    @Override
    public void onStartLoading(RefreshLoadLayout refreshLoadLayout) {
        progressBar.setVisibility(VISIBLE);
        textView.setText(R.string.on_loading);
    }

    @Override
    public void onEndLoading(RefreshLoadLayout refreshLoadLayout) {
        progressBar.setVisibility(GONE);

    }

}
