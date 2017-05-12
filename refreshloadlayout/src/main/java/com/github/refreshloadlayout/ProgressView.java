/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.refreshloadlayout;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.net.Uri;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.Animation;

/**
 * An Material Style Progress View
 */
public class ProgressView extends AppCompatImageView {

    public static final int DEFAULT_CIRCLE_BG_LIGHT = 0xFFFAFAFA;
    public static final int DEFAULT_CIRCLE_COLOR = 0xFFF00000;
    public static final int DEFAULT_TEXT_SIZE = 9;
    private static final int KEY_SHADOW_COLOR = 0x1E000000;
    private static final int FILL_SHADOW_COLOR = 0x3D000000;
    // PX
    private static final float X_OFFSET = 0f;
    private static final float Y_OFFSET = 1.75f;
    private static final float SHADOW_RADIUS = 3.5f;
    private static final int SHADOW_ELEVATION = 4;
    private static final int DEFAULT_CIRCLE_DIAMETER = 40;
    private static final int STROKE_WIDTH_LARGE = 3;
    public MaterialProgressDrawable mProgressDrawable;
    private Animation.AnimationListener mListener;
    private int mShadowRadius;
    private int mBackGroundColor = DEFAULT_CIRCLE_BG_LIGHT;
    private int mProgressColor = DEFAULT_CIRCLE_COLOR;
    private int mProgressStokeWidth;
    private int mArrowWidth;
    private int mArrowHeight;
    private int mProgress;
    private int mMax;
    private int mDiameter;
    private int mInnerRadius = -1;
    private Paint mTextPaint;
    private int mTextColor;
    private int mTextSize;
    private boolean mIfDrawText;
    private boolean mShowArrow;
    private ShapeDrawable mBgCircle;
    private boolean mCircleBackgroundEnabled;
    private int[] mColors = new int[]{Color.BLACK};

    public ProgressView(Context context) {
        this(context, null);
    }

    public ProgressView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    private void init(Context context) {
        final float density = context.getResources().getDisplayMetrics().density;
        mShadowRadius = (int) (density * SHADOW_RADIUS);
        mProgressStokeWidth = (int) (STROKE_WIDTH_LARGE * density);
        mArrowWidth = -1;
        mArrowHeight = -1;
        mTextSize = (int) (DEFAULT_TEXT_SIZE * density);
        mTextColor = Color.BLACK;
        mShowArrow = true;
        mCircleBackgroundEnabled = true;
        mProgress = 0;
        mMax = 100;

        mTextPaint = new Paint();
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setAntiAlias(true);
        mProgressDrawable = new MaterialProgressDrawable(getContext(), this);
        super.setImageDrawable(mProgressDrawable);
    }

    public void setProgressBackGroundColor(int color) {
        this.mBackGroundColor = color;
    }

    public void setTextColor(int color) {
        this.mTextColor = color;
    }

    private boolean elevationSupported() {
        return android.os.Build.VERSION.SDK_INT >= 21;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!elevationSupported()) {
            setMeasuredDimension(getMeasuredWidth() + mShadowRadius * 2, getMeasuredHeight()
                    + mShadowRadius * 2);
        }
    }

    public int getProgressStokeWidth() {
        return mProgressStokeWidth;
    }

    public void setProgressStokeWidth(int mProgressStokeWidth) {
        final float density = getContext().getResources().getDisplayMetrics().density;
        this.mProgressStokeWidth = (int) (mProgressStokeWidth * density);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        final float density = getContext().getResources().getDisplayMetrics().density;
        mDiameter = Math.min(getMeasuredWidth(), getMeasuredHeight());
        if (mDiameter <= 0) {
            mDiameter = (int) density * DEFAULT_CIRCLE_DIAMETER;
        }
        if (getBackground() == null && mCircleBackgroundEnabled) {
            final int shadowYOffset = (int) (density * Y_OFFSET);
            final int shadowXOffset = (int) (density * X_OFFSET);
            if (elevationSupported()) {
                mBgCircle = new ShapeDrawable(new OvalShape());
                ViewCompat.setElevation(this, SHADOW_ELEVATION * density);
            } else {
                OvalShape oval = new OvalShadow(mShadowRadius, mDiameter - mShadowRadius * 2);
                mBgCircle = new ShapeDrawable(oval);
                ViewCompat.setLayerType(this, ViewCompat.LAYER_TYPE_SOFTWARE, mBgCircle.getPaint());
                mBgCircle.getPaint().setShadowLayer(mShadowRadius, shadowXOffset, shadowYOffset,
                        KEY_SHADOW_COLOR);
                final int padding = mShadowRadius;
                // set padding so the inner image sits correctly within the shadow.
                setPadding(padding, padding, padding, padding);
            }
            mBgCircle.getPaint().setColor(mBackGroundColor);
            setBackgroundDrawable(mBgCircle);
        }
        mProgressDrawable.setBackgroundColor(mBackGroundColor);
        mProgressDrawable.setColorSchemeColors(mColors);
        mProgressDrawable.setSizeParameters(mDiameter, mDiameter,
                mInnerRadius <= 0 ? (mDiameter - mProgressStokeWidth * 2) / 4 : mInnerRadius,
                mProgressStokeWidth,
                mArrowWidth < 0 ? mProgressStokeWidth * 4 : mArrowWidth,
                mArrowHeight < 0 ? mProgressStokeWidth * 2 : mArrowHeight);
        if (isShowArrow()) {
            mProgressDrawable.setArrowScale(1f);
            mProgressDrawable.showArrow(true);
        }
        super.setImageDrawable(null);
        super.setImageDrawable(mProgressDrawable);
        mProgressDrawable.setAlpha(255);
        if (getVisibility() == VISIBLE) {
            mProgressDrawable.setStartEndTrim(0, (float) 0.8);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mIfDrawText) {
            String text = String.format("%s%%", mProgress);
            int x = getWidth() / 2 - text.length() * mTextSize / 4;
            int y = getHeight() / 2 + mTextSize / 4;
            canvas.drawText(text, x, y, mTextPaint);
        }
    }

    @Override
    final public void setImageResource(int resId) {

    }


    public boolean isShowArrow() {
        return mShowArrow;
    }

    public void setShowArrow(boolean showArrow) {
        this.mShowArrow = showArrow;
    }


    @Override
    final public void setImageURI(Uri uri) {
        super.setImageURI(uri);
    }

    @Override
    final public void setImageDrawable(Drawable drawable) {
    }

    public void setAnimationListener(Animation.AnimationListener listener) {
        mListener = listener;
    }

    @Override
    public void onAnimationStart() {
        super.onAnimationStart();
        if (mListener != null) {
            mListener.onAnimationStart(getAnimation());
        }
    }

    @Override
    public void onAnimationEnd() {
        super.onAnimationEnd();
        if (mListener != null) {
            mListener.onAnimationEnd(getAnimation());
        }
    }


    /**
     * Set the color resources used in the progress animation from color resources.
     * The first color will also be the color of the bar that grows in response
     * to a user swipe gesture.
     *
     * @param colorResIds
     */
    public void setColorSchemeResources(int... colorResIds) {
        final Resources res = getResources();
        int[] colorRes = new int[colorResIds.length];
        for (int i = 0; i < colorResIds.length; i++) {
            colorRes[i] = res.getColor(colorResIds[i]);
        }
        setColorSchemeColors(colorRes);
    }

    /**
     * Set the colors used in the progress animation. The first
     * color will also be the color of the bar that grows in response to a user
     * swipe gesture.
     *
     * @param colors
     */
    public void setColorSchemeColors(int... colors) {
        mColors = colors;
        if (mProgressDrawable != null) {
            mProgressDrawable.setColorSchemeColors(colors);
        }
    }

    /**
     * Update the background color of the mBgCircle image view.
     */
    public void setBackgroundColorResource(int colorRes) {
        if (getBackground() instanceof ShapeDrawable) {
            final Resources res = getResources();
            ((ShapeDrawable) getBackground()).getPaint().setColor(res.getColor(colorRes));
        }
    }

    public void setBackgroundColor(int color) {
        if (getBackground() instanceof ShapeDrawable) {
            ((ShapeDrawable) getBackground()).getPaint().setColor(color);
        }
    }

    public boolean isShowProgressText() {
        return mIfDrawText;
    }

    public void setShowProgressText(boolean mIfDrawText) {
        this.mIfDrawText = mIfDrawText;
    }

    public int getMax() {
        return mMax;
    }

    public void setMax(int max) {
        mMax = max;
    }

    public int getProgress() {
        return mProgress;
    }

    public void setProgress(int progress) {
        if (getMax() > 0) {
            mProgress = progress;
        }
        invalidate();
        Log.i("cjj_log", "progress------->>>>" + progress);
    }


    public boolean circleBackgroundEnabled() {
        return mCircleBackgroundEnabled;
    }

    public void setCircleBackgroundEnabled(boolean enableCircleBackground) {
        this.mCircleBackgroundEnabled = enableCircleBackground;
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mProgressDrawable != null) {
            mProgressDrawable.stop();
            mProgressDrawable.setVisible(getVisibility() == VISIBLE, false);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mProgressDrawable != null) {
            mProgressDrawable.stop();
            mProgressDrawable.setVisible(false, false);
        }
    }

    public void setArrowScale(float scale) {
        mProgressDrawable.setArrowScale(scale);
    }


    /**
     * 开始动画
     */
    public void start() {
        mProgressDrawable.start();
    }

    /**
     * 设置动画起始位置
     */
    public void setStartEndTrim(float startAngle, float endAngle) {
        mProgressDrawable.setStartEndTrim(startAngle, endAngle);
    }

    /**
     * 停止动画
     */
    public void stop() {
        mProgressDrawable.stop();
    }

    public void setProgressRotation(float rotation) {
        mProgressDrawable.showArrow(true);
        mProgressDrawable.setProgressRotation(rotation);
    }

    private class OvalShadow extends OvalShape {
        private RadialGradient mRadialGradient;
        private int mShadowRadius;
        private Paint mShadowPaint;
        private int mCircleDiameter;

        public OvalShadow(int shadowRadius, int circleDiameter) {
            super();
            mShadowPaint = new Paint();
            mShadowRadius = shadowRadius;
            mCircleDiameter = circleDiameter;
            mRadialGradient = new RadialGradient(mCircleDiameter / 2, mCircleDiameter / 2,
                    mShadowRadius, new int[]{
                    FILL_SHADOW_COLOR, Color.TRANSPARENT
            }, null, Shader.TileMode.CLAMP);
            mShadowPaint.setShader(mRadialGradient);
        }

        @Override
        public void draw(Canvas canvas, Paint paint) {
            final int viewWidth = ProgressView.this.getWidth();
            final int viewHeight = ProgressView.this.getHeight();
            canvas.drawCircle(viewWidth / 2, viewHeight / 2, (mCircleDiameter / 2 + mShadowRadius),
                    mShadowPaint);
            canvas.drawCircle(viewWidth / 2, viewHeight / 2, (mCircleDiameter / 2), paint);
        }
    }
}

