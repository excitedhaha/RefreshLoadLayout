package com.example.refreshloadlayout;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 *  A material-style View ,both as RefreshIndicator and LoadMoreIndicator
 * Created by jellybean on 2017/5/11.
 */

public class MaterialIndicator extends FrameLayout implements RefreshIndicator,LoadMoreIndicator{
    private ProgressView progressView;

    // Max amount of circle that can be filled by progress during swipe gesture,
    // where 1.0 is a full circle
    private static final float MAX_PROGRESS_ANGLE = .8f;

    public MaterialIndicator(@NonNull Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.material_indicator,this);
        progressView= (ProgressView) findViewById(R.id.progress_view);
    }

    public ProgressView getProgressView() {
        return progressView;
    }

    @Override
    public void onStartLoading(RefreshLoadLayout refreshLoadLayout) {
        progressView.start();
    }

    @Override
    public void onEndLoading(RefreshLoadLayout refreshLoadLayout) {
        progressView.stop();
    }


    @Override
    public void onPullDown(RefreshLoadLayout refreshLoadLayout, int scrollDistance) {
        showProgress(scrollDistance,refreshLoadLayout);
    }

    private void showProgress(int scrollDistance ,RefreshLoadLayout refreshLoadLayout) {
        if (refreshLoadLayout.isRefreshing()){
            return;
        }
        float height= getHeight();
        float ratio=scrollDistance/height;
        progressView.setStartEndTrim(0,Math.min(MAX_PROGRESS_ANGLE,ratio*MAX_PROGRESS_ANGLE));
        progressView.setProgressRotation(ratio%height);
        progressView.setArrowScale(Math.min(ratio,1));
    }

    @Override
    public void onQualifiedRefreshing(RefreshLoadLayout refreshLoadLayout, int scrollDistance) {
        showProgress(scrollDistance,refreshLoadLayout);
    }

    @Override
    public void onStartRefreshing(RefreshLoadLayout refreshLoadLayout) {
        progressView.start();
    }

    @Override
    public void onEndRefreshing(RefreshLoadLayout refreshLoadLayout) {
        progressView.stop();
    }
}
