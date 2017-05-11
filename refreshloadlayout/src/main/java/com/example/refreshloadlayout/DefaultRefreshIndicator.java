package com.example.refreshloadlayout;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 默认的刷新指示器
 * Created by jellybean on 2017/4/13.
 */

public class DefaultRefreshIndicator extends LinearLayout implements RefreshIndicator{
    private static final int FLIP_DURATION=300;
    private static final int ROTATE_DURATION=500;
    private ImageView imageView;
    private TextView textView;

    /**
     * 旋转箭头，朝上
     */
    private RotateAnimation flipUpAnimation;
    /**
     * 旋转箭头，朝下
     */
    private RotateAnimation flipDownAnimation;

    /**
     * 不断360度旋转
     */
    private RotateAnimation infiniteRotation;

    /**
     * 箭头是否朝下
     */
    private boolean arrowDown;

    public DefaultRefreshIndicator(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.default_refresh_indicator,this);
        imageView= (ImageView) findViewById(R.id.image);
        textView= (TextView) findViewById(R.id.text);
        initAnimation();
    }

    private void initAnimation() {
        LinearInterpolator linearInterpolator=new LinearInterpolator();
        flipUpAnimation =new RotateAnimation(0,-180,RotateAnimation.RELATIVE_TO_SELF,0.5f,RotateAnimation.RELATIVE_TO_SELF,0.5f);
        flipUpAnimation.setDuration(FLIP_DURATION);
        flipUpAnimation.setFillAfter(true);
        flipUpAnimation.setInterpolator(linearInterpolator);

        flipDownAnimation=new RotateAnimation(-180,0,RotateAnimation.RELATIVE_TO_SELF,0.5f,RotateAnimation.RELATIVE_TO_SELF,0.5f);
        flipDownAnimation.setDuration(FLIP_DURATION);
        flipDownAnimation.setFillAfter(true);
        flipDownAnimation.setInterpolator(linearInterpolator);

        infiniteRotation=new RotateAnimation(0,360,RotateAnimation.RELATIVE_TO_SELF,0.5f,RotateAnimation.RELATIVE_TO_SELF,0.5f);
        infiniteRotation.setDuration(ROTATE_DURATION);
        infiniteRotation.setRepeatCount(Animation.INFINITE);
        infiniteRotation.setInterpolator(linearInterpolator);
    }

    private void flipArrowUp(){
        imageView.startAnimation(flipUpAnimation);
        arrowDown=false;
    }

    private void flipArrowDown(){
        imageView.startAnimation(flipDownAnimation);
        arrowDown=true;
    }


    @Override
    public void onPullDown(RefreshLoadLayout refreshLoadLayout, int scrollDistance) {
        textView.setText(R.string.pull_down_refresh);
        if (!arrowDown){
            flipArrowDown();
        }
    }

    @Override
    public void onQualifiedRefreshing(RefreshLoadLayout refreshLoadLayout, int scrollDistance) {
        textView.setText(R.string.release_refresh);
        if (arrowDown){
            flipArrowUp();
        }
    }

    @Override
    public void onStartRefreshing(RefreshLoadLayout refreshLoadLayout) {
        textView.setText(R.string.on_loading);
        showProgress();
    }

    private void showProgress(){
        imageView.setImageResource(R.drawable.ic_refresh);
        imageView.startAnimation(infiniteRotation);
    }

    @Override
    public void onEndRefreshing(RefreshLoadLayout refreshLoadLayout) {
        textView.setText("");
        imageView.clearAnimation();
        imageView.setImageResource(R.drawable.ic_arrow);

    }
}
